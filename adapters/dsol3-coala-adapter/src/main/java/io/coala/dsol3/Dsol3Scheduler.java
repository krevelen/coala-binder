package io.coala.dsol3;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.inject.Inject;
import javax.measure.Measurable;
import javax.measure.quantity.Quantity;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;

import io.coala.config.InjectConfig;
import io.coala.exception.Thrower;
import io.coala.function.Caller;
import io.coala.function.ThrowingConsumer;
import io.coala.log.LogUtil;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.util.Compare;
import nl.tudelft.simulation.dsol.DSOLModel;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.experiment.ReplicationMode;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEvent;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Dsol3Scheduler}
 * 
 * @version $Id: 5b42b06bdc16c0e0c750f6c8cc214f6e8860fbf8 $
 * @author Rick van Krevelen
 */
public class Dsol3Scheduler<Q extends Quantity> implements Scheduler
{

//	public static <Q extends Quantity> Dsol3Scheduler<Q> of( final String id,
//		final Duration duration, final boolean pauseOnError,
//		final ReplicationMode mode, final ThrowingRunnable<?> modelInitializer )
//	{
//		return of( id, Instant.of( 0, duration.unwrap().getUnit() ), duration,
//				pauseOnError, mode, modelInitializer );
//	}
//
//	public static <Q extends Quantity> Dsol3Scheduler<Q> of( final String id,
//		final Duration duration, final boolean pauseOnError,
//		final ReplicationMode mode,
//		final ThrowingConsumer<Scheduler, ?> modelInitializer )
//	{
//		return of( id, Instant.of( 0, duration.unwrap().getUnit() ), duration,
//				pauseOnError, mode, modelInitializer );
//	}
//
//	public static <Q extends Quantity> Dsol3Scheduler<Q> of( final String id,
//		final Instant start, final Duration duration,
//		final boolean pauseOnError, final ReplicationMode mode,
//		final ThrowingRunnable<?> modelInitializer )
//	{
//		return of( id, start, duration, pauseOnError, mode,
//				Caller.of( modelInitializer )::ignore );
//	}
//
//	public static <Q extends Quantity> Dsol3Scheduler<Q> of( final String id,
//		final Instant start, final Duration duration,
//		final boolean pauseOnError, final ReplicationMode mode,
//		final ThrowingConsumer<Scheduler, ?> modelInitializer )
//	{
//		return new Dsol3Scheduler<Q>( id, start,
//				Duration.of( 0, start.unwrap().getUnit() ), duration,
//				pauseOnError, mode, Caller.rethrow( modelInitializer ) );
//	}

	public static <Q extends Quantity> Dsol3Scheduler<Q>
		of( final Map<?, ?>... imports )
	{
		return of( ConfigCache.getOrCreate( Dsol3Config.class, imports ) );
	}

	public static <Q extends Quantity> Dsol3Scheduler<Q>
		of( final Dsol3Config config )
	{
		return new Dsol3Scheduler<Q>( config );
	}

	public static <Q extends Quantity> Dsol3Scheduler<Q> of(
		final Dsol3Config config,
		final ThrowingConsumer<Scheduler, ?> onInitialize )
	{
		return new Dsol3Scheduler<Q>();
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( Dsol3Scheduler.class );

	/** the time */
	private final Subject<Instant, Instant> time = PublishSubject.create();

	/** the listeners */
	private final NavigableMap<Instant, Subject<Instant, Instant>> listeners = new ConcurrentSkipListMap<>();

	@InjectConfig
	private Dsol3Config config;

	private boolean initialized = false;

	/** the start time and unit */
	private Instant first;

	/** the current time */
	private Instant last = null;

	/** the scheduler */
	private DEVSSimulator<Measurable<Q>, BigDecimal, DsolTime<Q>> scheduler;

	/**
	 * {@link Dsol3Scheduler} constructor
	 */
	@Inject
	public Dsol3Scheduler()
	{
		this( Dsol3Config.get() );
	}

	public Dsol3Scheduler( final Dsol3Config config )
	{
		this( config, config.initer() );
	}

	public Dsol3Scheduler( final Dsol3Config config,
		final ThrowingConsumer<Scheduler, ?> onInitialize )
	{
		this.config = config;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void initialize()
	{
		final Class<? extends DEVSSimulator> type = config.simulatorType();
		this.scheduler = DsolTime.createDEVSSimulator( type );
		try
		{
			// observe time changes
			this.scheduler.addListener( event ->
			{
				final Instant t = ((DsolTime) event.getContent()).toInstant();

				synchronized( this.listeners )
				{
					if( this.last != null && Compare.eq( t, this.last ) )
						return;
					this.last = t;
					// publish time externally
					this.time.onNext( this.last.to( this.first.unit() ) );
					this.listeners.computeIfPresent( t, ( t1, timeProxy ) ->
					{
						try
						{
							// publish to registered listeners
							timeProxy.onNext( t );
						} catch( final Throwable e )
						{
							// execution errors already propagated
						}
						timeProxy.onCompleted();
						return null; // i.e. remove proxy for current instant
					} );
				}
			}, SimulatorInterface.TIME_CHANGED_EVENT );

			// observe simulation completed
			this.scheduler.addListener( event ->
			{
				synchronized( this.listeners )
				{
					this.listeners.values().removeIf( timeProxy ->
					{
						timeProxy.onCompleted();
						return true;
					} );
					this.time.onCompleted();
					this.scheduler.cleanUp();
				}
			}, SimulatorInterface.END_OF_REPLICATION_EVENT );

			this.scheduler.setPauseOnError( this.config.pauseOnError() );
			final String id = config.id();
			final DsolTime startTime = config.startTime();
			final BigDecimal warmUp = config.warmUpLength();
			final BigDecimal length = config.runLength();
			final ReplicationMode mode = config.replicationMode();
			final ThrowingConsumer<Scheduler, ?> onInitialize = config.initer();

			this.last = this.first = startTime.toInstant();
			final DSOLModel model = modelOf( id, startTime,
					Caller.ofThrowingConsumer( onInitialize, this ) );

			// initialize the simulator
			((DEVSSimulator) this.scheduler).initialize( DsolTime
					.createReplication( id, startTime, warmUp, length, model ),
					mode );
			this.initialized = true;
		} catch( final Exception e )
		{
			this.time.onError( e );
			this.scheduler.cleanUp();
			Thrower.rethrowUnchecked( e );
		}
	}

	@Override
	public Instant now()
	{
		return this.last;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public void resume()
	{
		try
		{
			if( !this.initialized ) this.initialize();
			if( !this.scheduler.isRunning() )
			{
				this.scheduler.scheduleEventNow(
						DEVSSimulatorInterface.FIRST_POSITION, () ->
						{
							LOG.trace( "resuming, t={}, #events={}",
									now().to( this.first.unit() ),
									this.scheduler.getEventList().size() );
							this.time.onNext( this.last );
						} );
				this.scheduler.start();
			}
		} catch( final Throwable e )
		{
			// propagate scheduling error
			this.time.onError( e );
		}
	}

	@Override
	public Observable<Instant> time()
	{
		return this.time.asObservable();
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Expectation schedule( final Instant when,
		final ThrowingConsumer<Instant, ?> what )
	{
		synchronized( this.listeners )
		{
			return Expectation.of( this, when,
					this.listeners.computeIfAbsent( when, t ->
					{
						// create missing proxy and schedule the "onNext" call
						final Subject<Instant, Instant> result = PublishSubject
								.create();
						try
						{
							this.scheduler
									.scheduleEvent( new SimEvent<DsolTime<Q>>(
											(DsolTime<Q>) DsolTime.valueOf( t ),
											this, result, "onNext",
											new Object[]
											{ t } ) );
						} catch( final SimRuntimeException e )
						{
							// propagate scheduling error
							this.time.onError( e );
						}
						return result;
					} ).subscribe( t ->
					{
						try
						{
							what.accept( t );
						} catch( final Throwable e )
						{
							// propagate execution error
							this.time.onError( e );
						}
					} ) );
		}
	}

	@SuppressWarnings( "serial" )
	private static <Q extends Quantity>
		DSOLModel<Measurable<Q>, BigDecimal, DsolTime<Q>>
		modelOf( final String id, final DsolTime<Q> startTime,
			final Runnable callback )
	{
		return new DSOLModel<Measurable<Q>, BigDecimal, DsolTime<Q>>()
		{
			private SimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>> simulator = null;

			@Override
			public SimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>>
				getSimulator()
			{
				return this.simulator;
			}

			@SuppressWarnings( "unchecked" )
			@Override
			public void constructModel(
				final SimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>> simulator )
				throws RemoteException, SimRuntimeException
			{
				this.simulator = simulator;
				// schedule first event to rename the worker thread
				((DEVSSimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>>) simulator)
						.scheduleEventNow(
								DEVSSimulatorInterface.FIRST_POSITION, () ->
								{
//				((DEVSSimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>>) simulator)
//						.scheduleEvent( new SimEvent<DsolTime<Q>>( startTime,
//								simulator, new Runnable()
//								{
//									@Override
//									public void run()
//									{
									Thread.currentThread().setName( id );
									callback.run();
//									}
//								}, "run", null ) );
								} );
			}
		};
	}
}