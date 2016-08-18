package io.coala.dsol3;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import javax.measure.Measurable;
import javax.measure.quantity.Quantity;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;

import io.coala.exception.ExceptionFactory;
import io.coala.function.Caller;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.log.LogUtil;
import io.coala.time.Duration;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
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

	public static <Q extends Quantity> Dsol3Scheduler<Q> of( final String id,
		final Instant start, final Duration duration,
		final ThrowingRunnable<?> modelInitializer )
	{
		return of( id, start, duration, Caller.of( modelInitializer )::ignore );
	}

	public static <Q extends Quantity> Dsol3Scheduler<Q> of( final String id,
		final Instant start, final Duration duration,
		final ThrowingConsumer<Scheduler, ?> modelInitializer )
	{
		return new Dsol3Scheduler<Q>( id, start,
				Duration.of( 0, start.unwrap().getUnit() ), duration,
				Caller.rethrow( modelInitializer ) );
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( Dsol3Scheduler.class );

	private Instant last = null;

	/** the time */
	private final Subject<Instant, Instant> time = PublishSubject.create();

	/** the listeners */
	private final NavigableMap<Instant, Subject<Instant, Instant>> listeners = new ConcurrentSkipListMap<>();

	/** the scheduler */
	private final DEVSSimulator<Measurable<Q>, BigDecimal, DsolTime<Q>> scheduler;

	/**
	 * {@link Dsol3Scheduler} constructor
	 * 
	 * @param threadName
	 */
	@SuppressWarnings( { "unchecked", "serial", "rawtypes" } )
	public Dsol3Scheduler( final String id, final Instant startTime,
		final Duration warmUp, final Duration length,
		final Consumer<Scheduler> onInitialize )
	{
		this.scheduler = DsolTime.createDEVSSimulator( DEVSSimulator.class );
//		this.scheduler.setPauseOnError( false );
		try
		{
			final DsolTime start = DsolTime.valueOf( startTime );

			final DSOLModel model = new DSOLModel()
			{
				@Override
				public void constructModel( final SimulatorInterface simulator )
					throws RemoteException, SimRuntimeException
				{
					// schedule first event to rename the worker thread
					((DEVSSimulatorInterface<Measurable<Q>, BigDecimal, DsolTime<Q>>) simulator)
							.scheduleEvent( new SimEvent<DsolTime<Q>>( start,
									simulator, new Runnable()
									{
										@Override
										public void run()
										{
											Thread.currentThread()
													.setName( id );
										}
									}, "run", null ) );

					// trigger onInitialize function
					onInitialize.accept( Dsol3Scheduler.this );
				}

				@Override
				public SimulatorInterface getSimulator()
				{
					return scheduler;
				}
			};

			// initialize the simulator
			this.scheduler.initialize(
					DsolTime.createReplication( id, start,
							warmUp.unwrap().to( startTime.unwrap().getUnit() )
									.getValue(),
							length.unwrap().to( startTime.unwrap().getUnit() )
									.getValue(),
							model ),
					ReplicationMode.TERMINATING );

			// observe time changes
			this.scheduler.addListener( event ->
			{
				final Instant t = ((DsolTime) event.getContent()).toInstant();
				if( t.equals( this.last ) ) return;

				this.last = t;
				synchronized( this.listeners )
				{
					this.time.onNext( t );
					this.listeners.computeIfPresent( t, ( t1, timeProxy ) ->
					{
						try
						{
							timeProxy.onNext( t );
						} catch( final Throwable e )
						{
//							timeProxy.onError( e );
//							this.time.onError( e );
						}
						timeProxy.onCompleted();
						return null; // i.e. remove
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
				}
			}, SimulatorInterface.END_OF_REPLICATION_EVENT );
		} catch( final RemoteException | SimRuntimeException
				| NamingException e )
		{
			this.time.onError( e );
			throw ExceptionFactory.createUnchecked( e,
					"Problem creating scheduler" );
		}
	}

	@Override
	public Instant now()
	{
		return this.scheduler.getSimulatorTime().toInstant();
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
				this.scheduler.start();
			}
		} catch( final SimRuntimeException e )
		{
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
//		if( now().equals( when ) )
//		{
//			try
//			{
//				this.scheduler.scheduleEvent( new SimEvent<DsolTime<Q>>(
//						this.scheduler.getSimulatorTime(), this, what, "accept",
//						new Object[]
//						{ when } ) );
//			} catch( final Exception e )
//			{
//				this.time.onError( e );
//			}
//			return null; // TODO provide a way to cancel instantaneous events?
//		}
		synchronized( this.listeners )
		{
			return Expectation.of( this, when,
					this.listeners.computeIfAbsent( when, t ->
					{
						// create proxy and schedule the actual invocation of "onNext"
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
						} catch( final Exception e )
						{
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
							this.time.onError( e );
						}
					} ) );
		}
	}
}