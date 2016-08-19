package io.coala.dsol3;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.measure.Measurable;
import javax.measure.quantity.Quantity;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;

import io.coala.exception.Thrower;
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

	public static <Q extends Quantity> Dsol3Scheduler<Q> of(
		final ThrowingConsumer<Scheduler, ?> modelInitializer,
		final Map<?, ?>... imports )
	{
		return of( ConfigCache.getOrCreate( Dsol3Config.class, imports ),
				modelInitializer );
	}

	public static <Q extends Quantity> Dsol3Scheduler<Q> of(
		final Dsol3Config config,
		final ThrowingConsumer<Scheduler, ?> modelInitializer )
	{
		return new Dsol3Scheduler<Q>( config, modelInitializer );
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( Dsol3Scheduler.class );

	/** the start time and unit */
	private final Instant first;

	/** the current time */
	private Instant last = null;

	/** the time */
	private final Subject<Instant, Instant> time = PublishSubject.create();

	/** the listeners */
	private final NavigableMap<Instant, Subject<Instant, Instant>> listeners = new ConcurrentSkipListMap<>();

	/** the scheduler */
	private final DEVSSimulator<Measurable<Q>, BigDecimal, DsolTime<Q>> scheduler;

	/**
	 * {@link Dsol3Scheduler} constructor
	 */
	public Dsol3Scheduler( final Dsol3Config config,
		final ThrowingConsumer<Scheduler, ?> onInitialize )
	{
		this( config.id(), config.startTime(), config.warmUpLength(),
				config.runLength(), config.simulatorType(),
				config.replicationMode(), config.pauseOnError(), onInitialize );
	}

	/**
	 * {@link Dsol3Scheduler} constructor
	 * 
	 * @param threadName
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public Dsol3Scheduler( final String id, final DsolTime startTime,
		final BigDecimal warmUp, final BigDecimal length,
		final Class<? extends DEVSSimulator> type, final ReplicationMode mode,
		final boolean pauseOnError,
		final ThrowingConsumer<Scheduler, ?> onInitialize )
	{
		this.last = this.first = startTime.toInstant();
		this.scheduler = DsolTime.createDEVSSimulator( type );
		this.scheduler.setPauseOnError( pauseOnError );
		try
		{
			@SuppressWarnings( "serial" )
			final DSOLModel model = new DSOLModel()
			{
				@Override
				public void constructModel( final SimulatorInterface simulator )
					throws RemoteException, SimRuntimeException
				{
					// schedule first event to rename the worker thread
					((DEVSSimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>>) simulator)
							.scheduleEvent( new SimEvent<DsolTime<Q>>(
									startTime, simulator, new Runnable()
									{
										@Override
										public void run()
										{
											Thread.currentThread()
													.setName( id );
										}
									}, "run", null ) );

					// trigger onInitialize function
					try
					{
						onInitialize.accept( Dsol3Scheduler.this );
					} catch( final Throwable e )
					{
						Thrower.rethrowUnchecked( e );
					}
				}

				@Override
				public SimulatorInterface getSimulator()
				{
					return scheduler;
				}
			};

			// initialize the simulator
			this.scheduler.initialize( DsolTime.createReplication( id,
					startTime, warmUp, length, model ), mode );

			// observe time changes
			this.scheduler.addListener( event ->
			{
				final Instant t = ((DsolTime) event.getContent()).toInstant();
				if( this.last != null && Compare.eq( t, this.last ) ) return;

				this.last = t;
				synchronized( this.listeners )
				{
					// publish time externally
					this.time.onNext( this.last );
					// publish to registered listeners
					this.listeners.computeIfPresent( t, ( t1, timeProxy ) ->
					{
						try
						{
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

	@Override
	public void resume()
	{
		try
		{
			if( !this.scheduler.isRunning() )
			{
				LOG.trace( "resuming, t={}, #events={}", now(),
						this.scheduler.getEventList().size() );
				this.time.onNext( this.last );
				this.scheduler.start();
			}
		} catch( final SimRuntimeException e )
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
		final Instant time = when.to( this.first.unwrap().getUnit() );
//		LOG.trace( "Converted {} to {}", when, time );
		synchronized( this.listeners )
		{
			return Expectation.of( this, time,
					this.listeners.computeIfAbsent( time, t ->
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
}