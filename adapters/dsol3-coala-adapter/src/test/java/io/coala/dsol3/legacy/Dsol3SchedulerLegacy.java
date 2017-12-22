package io.coala.dsol3.legacy;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.apache.logging.log4j.Logger;

import io.coala.bind.InjectConfig;
import io.coala.config.ConfigUtil;
import io.coala.dsol3.legacy.DsolTime.DsolQuantity;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
import io.coala.log.LogUtil;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.time.SchedulerConfig;
import io.coala.util.Compare;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import nl.tudelft.simulation.dsol.DSOLModel;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.experiment.ReplicationMode;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEvent;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;

/**
 * {@link Dsol3SchedulerLegacy}
 * 
 * @version $Id: 6d0552f8470fe91db6904753d90a5ab9b51baddc $
 * @author Rick van Krevelen
 */
@Singleton
public class Dsol3SchedulerLegacy<Q extends Quantity<Q>> implements Scheduler
{

	@SafeVarargs
	public static <Q extends Quantity<Q>> Dsol3SchedulerLegacy<Q>
		of( final Map<String, Object>... imports )
	{
		return of( Dsol3Config.of( imports ) );
	}

	public static <Q extends Quantity<Q>> Dsol3SchedulerLegacy<Q>
		of( final Dsol3Config config )
	{
		return new Dsol3SchedulerLegacy<Q>( config );
	}

	public static <Q extends Quantity<Q>> Dsol3SchedulerLegacy<Q> of(
		final Dsol3Config config,
		final ThrowingConsumer<Scheduler, ?> onInitialize )
	{
		final Dsol3SchedulerLegacy<Q> result = of( config );
		result.onReset( onInitialize );
		return result;
	}

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( Dsol3SchedulerLegacy.class );

	/** the time */
	private final Subject<Instant> time = PublishSubject.create();

	/** the time */
	private final Subject<Scheduler> reset = PublishSubject.create();

	/** the listeners */
	private final NavigableMap<Instant, Subject<Instant>> listeners = new ConcurrentSkipListMap<>();

//	@InjectConfig
	private Dsol3Config config;

	@InjectConfig
	private SchedulerConfig replConfig;

	private boolean initialized = false;

	/** the start time and unit */
	private Instant first;

	/** the current time */
	private Instant last = null;

	/** the scheduler */
	private DEVSSimulator<DsolQuantity<Q>, BigDecimal, DsolTime<Q>> scheduler;
	private transient Unit<?> timeUnitCache;
	private transient ZonedDateTime offsetCache;

	@Override
	public SchedulerConfig config()
	{
		return this.replConfig;
	}

	@Override
	public ZonedDateTime offset()
	{
		return this.offsetCache != null ? this.offsetCache
				: (this.offsetCache = ConfigUtil.cachedValue( config(),
						config()::offset ));
	}

	@Override
	public Unit<?> timeUnit()
	{
		return this.timeUnitCache != null ? this.timeUnitCache
				: (this.timeUnitCache = ConfigUtil.cachedValue( config(),
						config()::timeUnit ));
	}

	/**
	 * {@link Dsol3SchedulerLegacy} constructor
	 */
	@Inject
	public Dsol3SchedulerLegacy()
	{
	}

	public Dsol3SchedulerLegacy( final Dsol3Config config )
	{
		this.config = config;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	private void initialize()
	{
		if( this.config == null ) this.config = this.replConfig == null
				? Dsol3Config.get() : Dsol3Config.of( this.replConfig );
//		LOG.trace( "Imported config: {}", this.replConfig );
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
						timeProxy.onComplete();
						return null; // i.e. remove proxy for current instant
					} );
				}
			}, SimulatorInterface.TIME_CHANGED_EVENT );

			// observe simulation completed
			this.scheduler.addListener( event ->
			{
				synchronized( this.listeners )
				{
					// publish end time
					final Instant t = this.scheduler.getSimulatorTime()
							.toInstant();
					if( this.last == null || !Compare.eq( t, this.last ) )
						this.time.onNext( t.to( this.first.unit() ) );

					// unsubscribe all event listeners
					this.listeners.values().removeIf( timeProxy ->
					{
						timeProxy.onComplete();
						return true;
					} );
					this.time.onComplete();

					// clean up
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
			final ObservableDSOLModel model = ObservableDSOLModel.of( id,
					() -> this.reset.onNext( this ) );

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

//	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Override
	public void resume()
	{
		try
		{
			if( !this.initialized ) this.initialize();
			if( !this.scheduler.isRunning() )
			{
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
			}
		} catch( final Throwable e )
		{
			// propagate scheduling error
			this.time.onError( e );
		}
	}

	@Override
	public Disposable onReset( final ThrowingConsumer<Scheduler, ?> consumer )
	{
		return this.reset.subscribe( s ->
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
			// error propagated through time-stream
		} );
	}

	@Override
	public Observable<Instant> time()
	{
		return this.time;
	}

	@Override
	public void fail( final Throwable e )
	{
		throw new Error( "Not implemented" );
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
						final Subject<Instant> result = PublishSubject.create();
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
					}, e -> LOG.error( "Problem", e ) ) );
		}
	}

	interface ObservableDSOLModel<Q extends Quantity<Q>>
		extends DSOLModel<DsolQuantity<Q>, BigDecimal, DsolTime<Q>>
	{
		@SuppressWarnings( { "serial"/* , "unchecked" */ } )
		static <Q extends Quantity<Q>> ObservableDSOLModel<Q>
			of( final String id, final Runnable onReset )
		{
			return new ObservableDSOLModel<Q>()
			{
				private SimulatorInterface<DsolQuantity<Q>, BigDecimal, DsolTime<Q>> sim;

				@Override
				public
					SimulatorInterface<DsolQuantity<Q>, BigDecimal, DsolTime<Q>>
					getSimulator()
				{
					return this.sim;
				}

				@Override
				public void constructModel(
					final SimulatorInterface<DsolQuantity<Q>, BigDecimal, DsolTime<Q>> simulator )
					throws RemoteException, SimRuntimeException
				{
					this.sim = simulator;
					final DEVSSimulatorInterface<DsolQuantity<Q>, BigDecimal, DsolTime<Q>> devsSim = (DEVSSimulatorInterface<DsolQuantity<Q>, BigDecimal, DsolTime<Q>>) simulator;
					devsSim.scheduleEventNow(
							DEVSSimulatorInterface.FIRST_POSITION, () ->
							{
								// first: rename the worker thread
								Thread.currentThread().setName( id );
								onReset.run();
							} );

					LOG.trace( "initialized, t={}, #events={}",
							simulator.getSimulatorTime(),
							devsSim.getEventList().size() );
				}
			};
		}
	}
}