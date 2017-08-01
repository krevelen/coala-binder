package io.coala.dsol3;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Singleton;
import javax.measure.Unit;

import io.coala.bind.InjectConfig;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
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
import tec.uom.se.ComparableQuantity;

/**
 * {@link Dsol3Scheduler}
 * 
 * @version $Id: 92c8de500f12866a73a63b235cefc3ca42dec31c $
 * @author Rick van Krevelen
 */
@Singleton
public class Dsol3Scheduler//<Q extends Quantity<Q>> 
	implements Scheduler
{

	static class BaseTimeQ implements Comparable<BaseTimeQ>, Cloneable
	{
		public static BaseTimeQ of( final Number value, final Unit<?> unit,
			final Unit<?> baseUnit )
		{
			return of( QuantityUtil.valueOf( value, unit ), baseUnit );
		}

		/**
		 * @param when
		 * @return
		 */
		@SuppressWarnings( "unchecked" )
		public static BaseTimeQ of( final ComparableQuantity<?> quantity,
			final Unit<?> baseUnit )
		{
			final BaseTimeQ result = new BaseTimeQ();
			result.quantity = quantity;
			result.decimalCache = QuantityUtil.toBigDecimal(
					baseUnit == null || baseUnit.equals( quantity.getUnit() )
							? result.quantity
							: result.quantity.to( baseUnit ) );
			return result;
		}

		@SuppressWarnings( "rawtypes" )
		private ComparableQuantity quantity;

		private BigDecimal decimalCache;

		@SuppressWarnings( "unchecked" )
		@Override
		public int compareTo( final BaseTimeQ o )
		{
			return this.decimalCache.compareTo( o.decimalCache );
		}
	}

	static class SimTimeQ extends SimTime<BaseTimeQ, BigDecimal, SimTimeQ>
	{

		/** */
		private static final long serialVersionUID = 1L;

		/** value represents the value in milliseconds. */
		private BaseTimeQ absoluteTime;

		/**
		 * @param time the initial time.
		 */
		public SimTimeQ( final BaseTimeQ time )
		{
			super( time );
		}

		@Deprecated
		@Override
		public final void add( final BigDecimal relativeTime )
		{
			// used twice by Treatment#Treatment(...)
			this.absoluteTime = BaseTimeQ.of(
					this.absoluteTime.decimalCache.add( relativeTime ),
					this.absoluteTime.quantity.getUnit(),
					this.absoluteTime.quantity.getUnit() );
		}

		@Deprecated
		@Override
		public final void subtract( final BigDecimal relativeTime )
		{
			throw new IllegalStateException( "not thread-safe" );
		}

		@Override
		public final BigDecimal minus( final SimTimeQ simTime )
		{
			throw new IllegalStateException( "not thread-safe" );
		}

		private static final Map<Unit<?>, SimTimeQ> ZEROES = new HashMap<>();

		@Override
		public final SimTimeQ setZero()
		{
			return ZEROES.computeIfAbsent( get().quantity.getUnit(),
					unit -> new SimTimeQ(
							BaseTimeQ.of( BigDecimal.ZERO, unit, null ) ) );
		}

		@Override
		public final SimTimeQ copy()
		{
			return clone();
		}

		@Override
		public final SimTimeQ clone()
		{
			return new SimTimeQ( get() );
		}

		@Deprecated
		@Override
		public final void set( final BaseTimeQ absoluteTime )
		{
			this.absoluteTime = absoluteTime;
		}

		@Override
		public final BaseTimeQ get()
		{
			return this.absoluteTime;
		}

	}

	interface Model extends DSOLModel<BaseTimeQ, BigDecimal, SimTimeQ>
	{
		@SuppressWarnings( "serial" )
		static Model create(
			final ThrowingConsumer<SimulatorInterface<BaseTimeQ, BigDecimal, SimTimeQ>, ?> onReset,
			final Consumer<SimTimeQ> onTimeChanged, final Runnable onEnd )
		{
			return new Model()
			{
				private SimulatorInterface<BaseTimeQ, BigDecimal, SimTimeQ> sim = null;

				@Override
				public void constructModel(
					final SimulatorInterface<BaseTimeQ, BigDecimal, SimTimeQ> simulator )
					throws SimRuntimeException, RemoteException
				{
					this.sim = simulator;
					onTimeChanged.accept( simulator.getSimulatorTime() );
					this.sim.addListener(
							e -> onTimeChanged
									.accept( (SimTimeQ) e.getContent() ),
							SimulatorInterface.TIME_CHANGED_EVENT );
					this.sim.addListener( e -> onEnd.run(),
							SimulatorInterface.END_OF_REPLICATION_EVENT );
					try
					{
						onReset.accept( this.sim );
					} catch( final Throwable e )
					{
						throw new SimRuntimeException(
								"Problem constructing model", e );
					}
				}

				@Override
				public SimulatorInterface<BaseTimeQ, BigDecimal, SimTimeQ>
					getSimulator()
				{
					return this.sim;
				}
			};
		}
	}

	private final BehaviorSubject<Instant> time = BehaviorSubject.create();
	private final PublishSubject<Scheduler> reset = PublishSubject.create();

	private DEVSSimulator<BaseTimeQ, BigDecimal, SimTimeQ> sim = null;

	@InjectConfig
	private SchedulerConfig config;
	private ZonedDateTime offsetCache;
	private Unit<?> baseUnitCache;

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

	@Override
	public Disposable onReset( final ThrowingConsumer<Scheduler, ?> consumer )
	{
		return this.reset.subscribe( scheduler ->
		{
			try
			{
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
			final String name = this.config.rawId();
			Runtime.getRuntime()
					.addShutdownHook( new Thread( this.sim::cleanUp ) );
			final Model model = Model.create( devs ->
			{
				// set thread name to replication id (for logger)
				this.sim.scheduleEventNow(
						() -> Thread.currentThread().setName( name ) );
				// publish ready-made scheduler
				this.reset.onNext( this );
			}, t -> this.time.onNext( Instant.of( t.absoluteTime.quantity ) ),
					this.time::onComplete );
			this.sim.initialize(
					new Replication<BaseTimeQ, BigDecimal, SimTimeQ>( name,
							new SimTimeQ( BaseTimeQ.of( BigDecimal.ZERO,
									timeUnit(), timeUnit() ) ),
							BigDecimal.ZERO, this.config.rawDuration(), model ),
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
			// start the simulation thread
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
			final SimTimeQ t = new SimTimeQ(
					BaseTimeQ.of( when.toQuantity(), timeUnit() ) );
			final SimEventInterface<SimTimeQ> event = this.sim
					.scheduleEventAbs( t, () ->
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
					{
						sim.cancelEvent( event );
						this.cancelled = true;
					}
				}
			} );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}
}