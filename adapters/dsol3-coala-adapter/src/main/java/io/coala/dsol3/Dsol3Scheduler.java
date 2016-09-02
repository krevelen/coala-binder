package io.coala.dsol3;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Measurable;
import javax.measure.quantity.Quantity;

import org.apache.logging.log4j.Logger;

import io.coala.config.InjectConfig;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.log.LogUtil;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.ReplicateConfig;
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
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Dsol3Scheduler}
 * 
 * @version $Id: 5b42b06bdc16c0e0c750f6c8cc214f6e8860fbf8 $
 * @author Rick van Krevelen
 */
@Singleton
public class Dsol3Scheduler<Q extends Quantity> implements Scheduler
{

	@SafeVarargs
	public static <Q extends Quantity> Dsol3Scheduler<Q>
		of( final Map<String, String>... imports )
	{
		return of( Dsol3Config.of( imports ) );
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
		final Dsol3Scheduler<Q> result = of( config );
		result.onReset( onInitialize );
		return result;
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( Dsol3Scheduler.class );

	/** the time */
	private final Subject<Instant, Instant> time = PublishSubject.create();

	/** the time */
	private final Observable<Scheduler> reset = time.first().map( t ->
	{
		return this;
	} );

	/** the listeners */
	private final NavigableMap<Instant, Subject<Instant, Instant>> listeners = new ConcurrentSkipListMap<>();

//	@InjectConfig
	private Dsol3Config config;

	@InjectConfig
	private ReplicateConfig replConfig;

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
	}

	public Dsol3Scheduler( final Dsol3Config config )
	{
		this.config = config;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void initialize()
	{
		if( this.config == null ) this.config = this.replConfig == null
				? Dsol3Config.get() : Dsol3Config.of( this.replConfig );
		LOG.trace( "Using config: {}", this.config );
		final Class<? extends DEVSSimulator> type = this.config.simulatorType();
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
					this.scheduler = null;
				}
			}, SimulatorInterface.END_OF_REPLICATION_EVENT );

			this.scheduler.setPauseOnError( this.config.pauseOnError() );
			final String id = this.config.id();
			final DsolTime startTime = this.config.startTime();
			final BigDecimal warmUp = this.config.warmUpLength();
			final BigDecimal length = this.config.runLength();
			final ReplicationMode mode = this.config.replicationMode();
//			final ThrowingConsumer<Scheduler, ?> onInitialize = config.initer();

			this.last = this.first = startTime.toInstant();
			final ObservableDSOLModel model = ObservableDSOLModel.of( id );

			// initialize the simulator
			((DEVSSimulator) this.scheduler).initialize( DsolTime
					.createReplication( id, startTime, warmUp, length, model ),
					mode );
			this.initialized = true;
		} catch( final Exception e )
		{
			// propagate initialization error
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
//				this.scheduler.scheduleEventNow(
//						DEVSSimulatorInterface.FIRST_POSITION, () ->
//						{
				this.time.onNext( this.last );
				try
				{
					LOG.trace( "resuming, t={}, #events={}",
							now().to( this.first.unit() ),
							this.scheduler.getEventList().size() );
					this.scheduler.start();
				} catch( final Throwable e )
				{
					this.time.onError( e );
				}
//						} );
			}
		} catch( final Throwable e )
		{
			// propagate scheduling error
			this.time.onError( e );
		}
	}

	@Override
	public Observable<Scheduler> onReset()
	{
		return this.reset.asObservable();
	}

	@Override
	public Subscription onReset( final ThrowingRunnable<?> runnable )
	{
		return onReset( s ->
		{
			runnable.run();
		} );
	}

	@Override
	public Subscription onReset( final ThrowingConsumer<Scheduler, ?> consumer )
	{
		return onReset().subscribe( s ->
		{
			try
			{
				consumer.accept( s );
			} catch( final Throwable e )
			{
				this.time.onError( e );
			}
		}, e ->
		{
		} );
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

	interface ObservableDSOLModel<Q extends Quantity>
		extends DSOLModel<Measurable<Q>, BigDecimal, DsolTime<Q>>
	{
		@SuppressWarnings( { "serial", "rawtypes", "unchecked" } )
		static <Q extends Quantity> ObservableDSOLModel<Q> of( final String id )
		{
			return new ObservableDSOLModel<Q>()
			{
				private SimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>> sim;

				@Override
				public
					SimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>>
					getSimulator()
				{
					return this.sim;
				}

				@Override
				public void constructModel(
					final SimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>> simulator )
					throws RemoteException, SimRuntimeException
				{
					this.sim = simulator;
					final DEVSSimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>> devsSim = (DEVSSimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>>) simulator;
					devsSim.scheduleEventNow(
							DEVSSimulatorInterface.FIRST_POSITION, () ->
							{
								// first: rename the worker thread
								Thread.currentThread().setName( id );
							} );
					LOG.trace( "initializing, t={}, #events={}",
							simulator.getSimulatorTime(),
							devsSim.getEventList().size() );
				}
			};
		}
	}
}