package io.coala.dsol3;

import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.time.ZonedDateTime;
import java.util.Objects;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.apache.logging.log4j.Logger;

import io.coala.bind.InjectConfig;
import io.coala.bind.LocalBinder;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
import io.coala.log.LogUtil;
import io.coala.math.QuantityUtil;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.time.SchedulerConfig;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import nl.tudelft.simulation.dsol.DSOLModel;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.experiment.Replication;
import nl.tudelft.simulation.dsol.experiment.ReplicationMode;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface;
import nl.tudelft.simulation.dsol.simtime.SimTime;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;

/**
 * {@link Dsol3Scheduler} wraps a {@link DEVSSimulator} in a {@link Scheduler}
 * using {@link BigDecimal} time precision and any {@link Unit} time units
 * 
 * @version $Id: 92c8de500f12866a73a63b235cefc3ca42dec31c $
 * @author Rick van Krevelen
 */
@Singleton
public class Dsol3Scheduler//<Q extends Quantity<Q>> 
	implements Scheduler
{

	private final BehaviorSubject<Instant> time = BehaviorSubject.create();
	private final PublishSubject<Scheduler> reset = PublishSubject.create();

	private final LocalBinder binder;

	@InjectConfig
	private SchedulerConfig config;

	private DEVSSimulator<BaseTimeQ, BigDecimal, SimTimeQ> sim = null;
	private ZonedDateTime offsetCache;
	private Unit<?> baseUnitCache;

	@Inject
	public Dsol3Scheduler( final LocalBinder binder )
	{
		this.binder = binder;
	}

	@Override
	public ZonedDateTime offset()
	{
		return this.offsetCache != null ? this.offsetCache
				: (this.offsetCache = Objects
						.requireNonNull( this.config, "not configured?" )
						.offset());
	}

	@Override
	public Unit<?> timeUnit()
	{
		return this.baseUnitCache != null ? this.baseUnitCache
				: (this.baseUnitCache = Objects
						.requireNonNull( this.config, "not configured?" )
						.timeUnit());
	}

	@Override
	public SchedulerConfig config()
	{
		return this.config;
	}

	@Override
	public Instant now()
	{
		return this.time.getValue();
	}

	@Override
	public Observable<Instant> time()
	{
		return this.time;
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( Dsol3Scheduler.class );

	@Override
	public Disposable onReset( final ThrowingConsumer<Scheduler, ?> consumer )
	{
		return this.reset.subscribe( scheduler ->
		{
			try
			{
				LOG.trace( "Using config: {}", this.config.toJSON() );
				consumer.accept( scheduler );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			}
		} );
	}

	@Override
	public void resume()
	{
		if( this.sim == null ) try
		{
			this.sim = new DEVSSimulator<>();
			// reset the scheduler / time-line
			this.sim.setPauseOnError( true );
			// clean up if VM terminates suddenly (e.g. jUnit test)
			final String id = this.config.rawId(), name = id != null ? id
					: this.binder != null ? this.binder.id().toString()
							: "repl-" + (System.currentTimeMillis()
									& System.nanoTime());
			Runtime.getRuntime()
					.addShutdownHook( new Thread( this.sim::cleanUp ) );
			this.sim.initialize(
					new Replication<BaseTimeQ, BigDecimal, SimTimeQ>( name,
							SimTimeQ.zero( timeUnit() ), BigDecimal.ZERO,
							this.config.rawDuration(), (Model) sim ->
							{
								try
								{
									// rename the simulation worker thread
									this.sim.scheduleEventNow( () -> Thread
											.currentThread().setName( name ) );
									// initialize start time
									this.time.onNext( sim.getSimulatorTime()
											.toInstant() );
									// publish time instants
									sim.addListener(
											e -> this.time.onNext(
													((SimTimeQ) e.getContent())
															.toInstant() ),
											SimulatorInterface.TIME_CHANGED_EVENT );
									// complete time instants at replication end
									sim.addListener(
											e -> this.time.onComplete(),
											SimulatorInterface.END_OF_REPLICATION_EVENT );
									// scheduler ready, publish
									this.reset.onNext( this );
								} catch( final Throwable e )
								{
									this.time.onError( e );
								}
							} ),
					ReplicationMode.TERMINATING );
		} catch( final Throwable e )
		{
			this.time.onError( e );
			this.sim.cleanUp();
			this.sim = null;
			Thrower.rethrowUnchecked( e );
		}
		if( !this.sim.isRunning() ) try
		{
			// start the simulation worker thread
			this.sim.start();
		} catch( final Throwable e )
		{
			this.time.onError( e );
			this.sim.cleanUp();
			this.sim = null;
			Thrower.rethrowUnchecked( e );
		}
	}

	@Override
	public Expectation schedule( final Instant when,
		final ThrowingConsumer<Instant, ?> what )
	{
		try
		{
			final SimEventInterface<SimTimeQ> event = this.sim.scheduleEventAbs(
					new SimTimeQ( new BaseTimeQ( when, timeUnit() ) ), () ->
					{
						try
						{
							what.accept( when );
						} catch( final Throwable e )
						{
							this.time.onError( e );
						}
					} );

			return Expectation.of( this, now(), new Disposable()
			{
				private boolean cancelled = false;

				@Override
				public boolean isDisposed()
				{
					return this.cancelled;
				}

				@SuppressWarnings( "unchecked" )
				@Override
				public void dispose()
				{
					if( !this.cancelled )
						this.cancelled = sim.cancelEvent( event );
				}
			} );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * {@link BaseTimeQ} is a minimal thread-safe {@link Quantity} wrapper for
	 * efficient comparison
	 */
	@ThreadSafe
	static class BaseTimeQ implements Comparable<BaseTimeQ>, Serializable
	{
		/** the serialVersionUID */
		private static final long serialVersionUID = -8304339278326796545L;

		/** the original time quantity and units */
		@SuppressWarnings( "rawtypes" )
		private Instant instant;

		/** the time quantity in base units for quick sorting and scheduling */
		private BigDecimal decimalCache;

		BaseTimeQ( final Number value, final Unit<?> unit,
			final Unit<?> baseUnit )
		{
			this( Instant.of( value, unit ), baseUnit );
		}

		@SuppressWarnings( "unchecked" )
		BaseTimeQ( final Instant instant, final Unit<?> baseUnit )
		{
			this.instant = instant;
			this.decimalCache = baseUnit == null
					|| baseUnit.equals( instant.unit() ) ? instant.decimal()
							: QuantityUtil.toBigDecimal(
									instant.toQuantity( baseUnit ) );
		}

		BaseTimeQ add( final BigDecimal dt )
		{
			// assume time delta is in baseUnit amounts
			return new BaseTimeQ( this.decimalCache.add( dt ),
					this.instant.unit(), this.instant.unit() );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public int compareTo( final BaseTimeQ o )
		{
			return this.decimalCache.compareTo( o.decimalCache );
		}
	}

	/**
	 * {@link SimTimeQ} is a minimal thread-safe implementation of the abstract
	 * {@link SimTime} using {@link BaseTimeQ} wrappers for quick comparisons of
	 * some {@link Quantity} of time
	 */
	@ThreadSafe
	static class SimTimeQ extends SimTime<BaseTimeQ, BigDecimal, SimTimeQ>
	{

		/** the serialVersionUID */
		private static final long serialVersionUID = -2990605798573111216L;

		/** value represents the value in milliseconds. */
		private BaseTimeQ absoluteTime;

		static SimTimeQ zero( final Unit<?> unit )
		{
			return new SimTimeQ( new BaseTimeQ( BigDecimal.ZERO, unit, unit ) );
		}

		/**
		 * @return
		 */
		Instant toInstant()
		{
			return get().instant;
		}

		SimTimeQ( final BaseTimeQ absoluteTime )
		{
			super( absoluteTime );
		}

		@Override
		public BaseTimeQ get()
		{
			return this.absoluteTime;
		}

		@Deprecated
		@Override
		public void add( final BigDecimal relativeTime )
		{
			// implementation required by Treatment#Treatment(...)
			this.absoluteTime = this.absoluteTime.add( relativeTime );
		}

		@Deprecated
		@Override
		public void subtract( final BigDecimal relativeTime )
		{
			throw new Error( "Not implemented" );
		}

		@Deprecated
		@Override
		public BigDecimal minus( final SimTimeQ simTime )
		{
			throw new Error( "Not implemented" );
		}

		@Deprecated
		@Override
		public SimTimeQ setZero()
		{
			throw new Error( "Not implemented" );
		}

		@Deprecated
		@Override
		public SimTimeQ copy()
		{
			// implementation required by Treatment#Treatment(...)
			return new SimTimeQ( get() );
		}

		@Deprecated
		@Override
		public void set( final BaseTimeQ absoluteTime )
		{
			// implementation required by super constructor SimTime#SimTime(..)
			this.absoluteTime = absoluteTime;
		}
	}

	/**
	 * {@link Model} is a functional {@link DSOLModel} with type arguments
	 * compatible for {@link Dsol3Scheduler} and minimal (default) plumbing
	 */
	@FunctionalInterface
	interface Model extends DSOLModel<BaseTimeQ, BigDecimal, SimTimeQ>
	{
		void onReset(
			SimulatorInterface<BaseTimeQ, BigDecimal, SimTimeQ> simulator );

		@Override
		default void constructModel(
			final SimulatorInterface<BaseTimeQ, BigDecimal, SimTimeQ> simulator )
			throws RemoteException, SimRuntimeException
		{
			onReset( simulator );
		}

		@Deprecated
		@Override
		default public SimulatorInterface<BaseTimeQ, BigDecimal, SimTimeQ>
			getSimulator()
		{
			throw new Error( "Not implemented" );
		}
	}
}