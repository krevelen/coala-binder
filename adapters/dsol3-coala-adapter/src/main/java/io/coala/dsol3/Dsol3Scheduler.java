package io.coala.dsol3;

import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.bind.InjectConfig;
import io.coala.bind.LocalBinder;
import io.coala.function.ThrowingConsumer;
import io.coala.log.LogUtil;
import io.coala.math.QuantityUtil;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.time.SchedulerConfig;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
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

	// TODO handle tie-reset in separate 'experimenter'
	private final PublishSubject<Scheduler> reset = PublishSubject.create();

	private final PublishSubject<Instant> time = PublishSubject.create();
	private final AtomicReference<Instant> t = new AtomicReference<>();

	private final String binderId;

	@InjectConfig
	private SchedulerConfig config;

	private DEVSSimulator<BaseTimeQ, BigDecimal, SimTimeQ> sim = null;
	private ZonedDateTime offsetCache;
	private Unit<?> baseUnitCache;

	@Inject
	public Dsol3Scheduler( final LocalBinder binder )
	{
		this.binderId = binder.id().toString();
	}

	public Dsol3Scheduler( final SchedulerConfig config )
	{
		this.binderId = config.rawId();
		this.config = config;
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
		return this.t.get();
	}

	@Override
	public Observable<Instant> time()
	{
		return this.time;//.distinctUntilChanged();
	}

	@Override
	public void fail( final Throwable e )
	{
		this.time.onError( e );
		if( this.sim != null )
		{
			if( this.sim.isRunning() ) this.sim.stop( false );
			this.sim.cleanUp();
		}
		this.sim = null;
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
				fail( e );
			}
		}, this::fail );
	}

	private void advanceTo( final Instant t )
	{
		this.t.updateAndGet( old ->
		{
			if( !t.equals( old ) ) this.time.onNext( t );
			return t;
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
					: this.binderId != null ? this.binderId
							: "repl-" + (System.currentTimeMillis()
									& System.nanoTime());
//			Runtime.getRuntime().addShutdownHook( new Thread( this.sim::cleanUp ) );
			final SimTimeQ startTime = SimTimeQ.zero( timeUnit() );
			advanceTo( startTime.toInstant() );
			this.sim.initialize(
					new Replication<BaseTimeQ, BigDecimal, SimTimeQ>( name,
							startTime, BigDecimal.ZERO,
							this.config.rawDuration(), (Model) sim ->
							{
								try
								{
									// rename the simulation worker thread
									this.sim.scheduleEventNow( () -> Thread
											.currentThread().setName( name ) );
									// publish time instants
									sim.addListener(
											e -> advanceTo(
													((SimTimeQ) e.getContent())
															.toInstant() ),
											SimulatorInterface.TIME_CHANGED_EVENT );
									// complete time instants at replication end
									sim.addListener( e ->
									{
										this.time.onComplete();
										this.sim.cleanUp();
									}, SimulatorInterface.END_OF_REPLICATION_EVENT );
									// scheduler ready, publish
									this.reset.onNext( this );
								} catch( final Throwable e )
								{
									fail( e );
								}
							} ),
					ReplicationMode.TERMINATING );
		} catch( final Throwable e )
		{
			fail( e );
		}
		// start the simulation worker thread
		if( this.sim != null && !this.sim.isRunning() ) try
		{
			this.sim.start();
		} catch( final Throwable e )
		{
			fail( e );
		}
	}

	@Override
	public Expectation schedule( final Instant when,
		final ThrowingConsumer<Instant, ?> what )
	{
		if( this.sim == null ) return null; // sim/model failed
		try
		{
			final SimTimeQ t;
			final short priority;
			if( now() == when )
			{
				t = this.sim.getSimulatorTime();
				priority = SimEventInterface.MIN_PRIORITY;
//				new IllegalStateException( "same instant" ).printStackTrace();
			} else
			{
				t = new SimTimeQ( new BaseTimeQ( when, timeUnit() ) );
				priority = SimEventInterface.NORMAL_PRIORITY;
			}
			final SimEventInterface<SimTimeQ> event = this.sim
					.scheduleEventAbs( t, priority, () ->
					{
						try
						{
							what.accept( when );
						} catch( final Throwable e )
						{
							fail( e );
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
					if( !this.cancelled && sim != null )
						this.cancelled = sim.cancelEvent( event );
				}
			} );
		} catch( final Exception e )
		{
			fail( e );
			return null;
		}
	}

	/**
	 * {@link BaseTimeQ} is a minimal thread-safe {@link Quantity} wrapper for
	 * efficient comparison
	 */
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
							: QuantityUtil.decimalValue(
									instant.toQuantity( baseUnit ) );
		}

		BaseTimeQ add( final BigDecimal dt )
		{
			// assume time delta is in baseUnit amounts
			return new BaseTimeQ( this.decimalCache.add( dt ),
					this.instant.unit(), this.instant.unit() );
		}

		@Override
		public String toString()
		{
			return "t=" + this.decimalCache + "(" + this.instant + ")";
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

		@JsonIgnore
		private int hashCode = 0;

		@Override
		public int hashCode()
		{
			return this.hashCode == 0 ? (this.hashCode = super.hashCode())
					: this.hashCode;
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