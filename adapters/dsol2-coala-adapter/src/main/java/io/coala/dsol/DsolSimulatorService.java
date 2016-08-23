/* $Id: 89cfd12ede2065a2f68cdd22f6b145efd8a4b4e4 $
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.coala.dsol;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.measure.unit.SI;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.replicate.RandomizingCapability;
import io.coala.capability.replicate.ReplicatingCapability;
import io.coala.capability.replicate.ReplicationConfig;
import io.coala.config.CoalaProperty;
import io.coala.dsol.util.ExperimentBuilder;
import io.coala.dsol.util.ReplicationBuilder;
import io.coala.exception.ExceptionFactory;
import io.coala.log.InjectLogger;
import io.coala.log.LogUtil;
import io.coala.name.Identifier;
import io.coala.process.Job;
import io.coala.random.RandomNumberStream;
import io.coala.time.ClockID;
import io.coala.time.Instant;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import io.coala.time.Timing;
import nl.tudelft.simulation.dsol.ModelInterface;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEvent;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.event.EventInterface;
import nl.tudelft.simulation.event.EventListenerInterface;
import nl.tudelft.simulation.event.EventType;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

/**
 * {@link DsolSimulatorService} combines a {@link SchedulingCapability} and
 * 
 * @version $Id: 89cfd12ede2065a2f68cdd22f6b145efd8a4b4e4 $
 * @author Rick van Krevelen
 */
@Deprecated
public class DsolSimulatorService extends BasicCapability
	implements ReplicatingCapability, RandomizingCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final Map<ClockID, ReplicationBuilder> SIMULATORS = Collections
			.synchronizedMap( new HashMap<ClockID, ReplicationBuilder>() );

	private final Map<RandomNumberStream.ID, RandomNumberStream> rng = Collections
			.synchronizedMap(
					new HashMap<RandomNumberStream.ID, RandomNumberStream>() );

	/** */
	@InjectLogger // not useful if logging occurs during construction
	private transient Logger LOG;

	/** */
	private transient ReplicationConfig config;

	/** */
	private transient SimTime.Factory newTime;

	/** */
	private transient ClockID clockID;

	/** */
	private transient TimeUnit baseTimeUnit;

	/** */
	private boolean complete = false;

	/** */
	private volatile SimTime time = null;

	/** */
	private transient Subject<SimTime, SimTime> timeUpdates;

	/** */
//	private transient Subject<ClockStatusUpdate, ClockStatusUpdate> statusUpdates;

	/**
	 * @param clockID
	 * @return the local VM's simulator instance for specified {@link ClockID}
	 * @throws NamingException
	 */
	protected static ReplicationBuilder getReplication( final ClockID clockID,
		final ReplicationConfig config ) throws NamingException
	{
		synchronized( SIMULATORS )
		{
			if( !SIMULATORS.containsKey( clockID ) )
			{
				final DEVSSimulatorInterface simulator = new DEVSSimulator();

				final ModelInterface model = new ModelWrapper();
				final ReplicationBuilder repl = new ExperimentBuilder()
						.withSimulator( simulator ).withModel( model )
						.newTreatment().withTimeUnit( config.getBaseTimeUnit() )
						.withRunInterval( config.getInterval() )
						.newReplication( config.getReplicationID().getValue() )
						.withStream( config.getSeed() );

				System.err.println( "Built clock: " + clockID );
				SIMULATORS.put( clockID, repl );
			}

			return SIMULATORS.get( clockID );
		}
	}

	private static class ModelWrapper implements ModelInterface
	{

		/** */
		private static final long serialVersionUID = 1L;

		/** */
		private static final Logger LOG = LogUtil
				.getLogger( DsolSimulatorService.ModelWrapper.class );

		/** */
		private SimulatorInterface simulator = null;

		@Override
		public void constructModel( final SimulatorInterface simulator )
		{
			this.simulator = simulator;
			try
			{
				LOG.trace( "[" + Thread.currentThread().getName()
						+ "] CONSTRUCTED model " + simulator.getReplication()
								.getContext().getNameInNamespace() );
			} catch( final Exception e )
			{
				e.printStackTrace();
			}
		}

		@Override
		public SimulatorInterface getSimulator()
		{
			return this.simulator;
		}

	}

	/**
	 * {@link DsolSimulatorService} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected DsolSimulatorService( final Binder binder )
	{
		super( binder );
	}

	@Override
	public void initialize() throws Exception
	{
		super.initialize();

		/*
		 * final long seed = 123; final DateTime start = new
		 * DateTime(getBinder().inject(DateTime.class)); final ClockID clockID =
		 * getBinder().inject(ClockID.class); final TimeUnit baseTimeUnit =
		 * getBinder().inject(TimeUnit.class); final Interval interval = new
		 * Interval(start, start.plus(getBinder() .inject(Period.class))); final
		 * SimTimeFactory newTime = getBinder().inject(SimTimeFactory.class);
		 */
		this.config = getBinder().inject( ReplicationConfig.class );
		this.clockID = this.config.getClockID();
		this.baseTimeUnit = this.config.getBaseTimeUnit();
		this.newTime = this.config.newTime();
//		this.statusUpdates = BehaviorSubject.create(
//				(ClockStatusUpdate) new ClockStatusUpdateImpl( getClockID(),
//						DsolSimulatorStatus.CREATED ) );
		this.timeUpdates = BehaviorSubject
				.create( this.newTime.create( Double.NaN, this.baseTimeUnit ) );

		synchronized( SIMULATORS )
		{
			final ReplicationBuilder repl = getReplication( getClockID(),
					this.config );
			final DEVSSimulatorInterface simulator = (DEVSSimulatorInterface) repl
					.getExperiment().getSimulator();

			// subscribe to (time-less) simulator events
			for( EventType type : new EventType[] {
					SimulatorInterface.START_REPLICATION_EVENT,
					SimulatorInterface.WARMUP_EVENT,
					SimulatorInterface.START_EVENT,
					SimulatorInterface.STEP_EVENT,
					SimulatorInterface.STOP_EVENT,
					SimulatorInterface.END_OF_REPLICATION_EVENT } )
				try
				{
					simulator.addListener( new EventListenerInterface()
					{
						@Override
						public void notify( final EventInterface event )
						{
//							final DsolSimulatorStatus status = DsolSimulatorStatus
//									.of( event.getType() );
//							statusUpdates.onNext( new ClockStatusUpdateImpl(
//									getClockID(), status ) );
						}
					}, type );
				} catch( final RemoteException e )
				{
					e.printStackTrace();
				}

			// subscribe to simulator time change events
			try
			{
				simulator.addListener( new EventListenerInterface()
				{
					@Override
					public void notify( final EventInterface event )
					{
						if( Thread.currentThread().getName().startsWith(
								ModelInterface.class.getPackage().getName() ) )
						{
							System.err.println( "Resetting thread name: "
									+ Thread.currentThread().getName() );
							Thread.currentThread()
									.setName( getClockID().getValue() );
						}
						setTime( (Double) event.getContent() );
					}
				}, SimulatorInterface.TIME_CHANGED_EVENT );
			} catch( final RemoteException e )
			{
				e.printStackTrace();
			}
			try
			{
				simulator.addListener( new EventListenerInterface()
				{
					@Override
					public void notify( final EventInterface event )
					{
						setComplete();
					}
				}, SimulatorInterface.END_OF_REPLICATION_EVENT );
			} catch( final RemoteException e )
			{
				e.printStackTrace();
			}

			if( !simulator.isRunning() )
			{
				repl.initialize();
				LOG.info( "[t=0] INITIALIZED replication " + getClockID() );
				// simulator.start(); // needed to initialize time to 0
				// simulator.stop();
				setTime( new Double( 0 ) );
			} else
			{
				setTime( simulator.getSimulatorTime() );
				LOG.info( "JOINED [t=" + getTime().getValue() + "] replication "
						+ getClockID() );
			}
		}
	}

	@Override
	public void pause()
	{
		try
		{
			if( getSimulator().isRunning() )
			{
				getSimulator().stop();
				LOG.warn( "Simulator " + getClockID() + " stopped" );
			} else
				LOG.warn( "Simulator " + getClockID() + " already stopped" );
		} catch( final Exception e )
		{
			LOG.error( "Problem stopping simulator " + getClockID(), e );
			e.printStackTrace();
		}
	}

	@Override
	public void start()
	{
		try
		{
			if( !getSimulator().isRunning() )
			{
				getReplication( getClockID(), this.config ).start();
				LOG.warn( "Simulator " + getClockID() + " running" );
			} else
				LOG.warn( "Simulator " + getClockID() + " already running" );
		} catch( final Exception e )
		{
			LOG.error( "Problem starting simulator " + getClockID(), e );
			e.printStackTrace();
		}
	}

	protected void setComplete()
	{
		this.complete = true;
	}

	protected synchronized void setTime( final Double value )
	{
		if( this.time != null && value == this.time.doubleValue() ) return;

		// System.err.println("SETTING TIME: " + value);
		final SimTime time = this.newTime.create( value, this.baseTimeUnit );
		this.time = time;
		this.timeUpdates.onNext( time );
		// notifyAll();
	}

//	@Override
	public synchronized SimTime getTime()
	{
		// while (this.time == null)
		// {
		// new IllegalStateException("AWAITING TIME...").printStackTrace();
		// try
		// {
		// wait(1000);
		// } catch (final InterruptedException ignore)
		// {
		// }
		// }
		// System.err.println("GETTING TIME: " + this.time);
		return this.time;
	}

//	@Override
	public ClockID getClockID()
	{
		return this.clockID;
	}

	protected DEVSSimulatorInterface getSimulator()
	{
		try
		{
			return (DEVSSimulatorInterface) getReplication( getClockID(),
					this.config ).getExperiment().getSimulator();
		} catch( final NamingException e )
		{
			throw ExceptionFactory.createUnchecked( e, "Problem with sim" );
		}
	}

	private Map<Identifier<?, ?>, List<SimEvent>> pendingEvents = new HashMap<>();

	/**
	 * {@link CallableSimEvent} wraps a {@link SimEvent} as {@link Callable}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class CallableSimEvent extends SimEvent
		implements Callable<Void>
	{

		/** */
		private static final long serialVersionUID = 1L;

		/**
		 * {@link CallableSimEvent} constructor
		 * 
		 * @param absTime
		 * @param job
		 */
		@SuppressWarnings( "unchecked" )
		public CallableSimEvent( final TimeUnit baseTimeUnit,
			final Instant absTime, final Callable<Void> job )
		{
			super( absTime.toMeasure().doubleValue( SI.MILLI( SI.SECOND ) ),
					SimEventInterface.MIN_PRIORITY, job, job, "call", null );
		}

		@Override
		public synchronized void execute() throws SimRuntimeException
		{
			try
			{
				// System.err.println("Executing sim event for: " +
				// this.method);
				call();
				// System.err.println("Executed sim event for: " + this.method);
			} catch( final Exception e )
			{
				new SimRuntimeException(
						"Problem executing call to " + this.method, e )
								.printStackTrace();
			}
		}

		@Override
		public synchronized Void call() throws Exception
		{
			// System.err.println("Calling sim event for " + this.method);
			super.execute();
			// System.err.println("Called sim event for " + this.method);
			return null;
		}

	}

//	@Override
	public void schedule( final Job<?> job, final Timing trigger )
	{
		if( isComplete() ) throw new IllegalStateException(
				"Can't schedule, already complete!" );
		final DEVSSimulatorInterface simulator = getSimulator();
		final List<SimEvent> simEvents = new ArrayList<>();
		final List<SimEvent> oldEvents = this.pendingEvents.put( job.getID(),
				simEvents );
		if( oldEvents != null )
		{
			LOG.warn(
					"Already scheduled this job: " + job.getID()
							+ ", pending events: " + oldEvents.size()
							+ ", originator stack: " + job.getStackTrace(),
					new IllegalStateException(
							"Already scheduled " + job.getID() ) );
			simEvents.addAll( oldEvents );
		}
		if( job instanceof Callable )
		{
			// final CountDownLatch latch = new CountDownLatch(1);
			// Schedulers.computation().createWorker().schedule(new Action0()
			// {
			// @Override
			// public void call()
			// {
			trigger.stream( config.getOffset() )
					.all( new Func1<Instant, Boolean>()
					{
						@SuppressWarnings( "unchecked" )
						@Override
						public Boolean call( final Instant time )
						{
							// TODO extend SimEvent to await any local prior
							// grant from
							// parent/root simulator still pending

							try
							{
								LOG.trace( "Scheduling " + job.getID() + " @ "
										+ time );
								// System.err.println(-1);
								final SimEvent event = new CallableSimEvent(
										baseTimeUnit, time,
										(Callable<Void>) job );
								// System.err.println(0);
								simulator.scheduleEvent( event );
								// System.err.println(1);
								simEvents.add( event );
								// System.err.println(2);
								simulator.scheduleEvent( new SimEvent(
										event.getAbsoluteExecutionTime(),
										SimEventInterface.MIN_PRIORITY, this,
										this, "removeEvent",
										new Object[]
										{ job.getID(), event } ) );
								// System.err.println(3);
								return Boolean.TRUE;
							} catch( final Throwable t )
							{
								LOG.error( "Problem scheduling " + job + " @ "
										+ time, t );
								return Boolean.FALSE;
							}
						}

						/**
						 * remove pending event from cache after it was called
						 * by the simulator
						 * 
						 * @param jobID
						 * @param event
						 */
						@SuppressWarnings( "unused" )
						public void removeEvent( final Identifier<?, ?> jobID,
							final SimEvent event )
						{
							final List<SimEvent> jobEvents = pendingEvents
									.get( job.getID() );

							if( jobEvents == null ) return;

							jobEvents.remove( event );

							if( jobEvents.isEmpty() )
								pendingEvents.remove( jobID );
						}
					} ).subscribe( new Action1<Boolean>()
					{
						@Override
						public void call( final Boolean success )
						{
							if( !success ) LOG
									.error( "Not all trigger events were scheduled for job: "
											+ job.getID() );
							// latch.countDown();
						}
					} );

			// }
			// });
			// while (latch.getCount() > 0)
			// try
			// {
			// LOG.info("Waiting for job/events to be scheduled...");
			// latch.await(1, java.util.concurrent.TimeUnit.SECONDS);
			// } catch (final InterruptedException e)
			// {
			// // LOG.error("Problem scheduling next job/event", e);
			// }
		} else
			LOG.error( "Unable to schedule non-Callable job type: "
					+ job.getClass().getName() );
	}

//	@Override
//	public boolean unschedule( final Job<?> job )
//	{
//		final List<SimEvent> events = this.pendingEvents.remove( job.getID() );
//
//		if( events == null ) return false;
//
//		final DEVSSimulatorInterface simulator = getSimulator();
//		for( SimEvent event : events )
//			try
//			{
//				simulator.cancelEvent( event );
//			} catch( final RemoteException e )
//			{
//				throw new IllegalStateException( "UNEXPECTED", e );
//			}
//		return true;
//	}

	@Override
	public boolean isRunning()
	{
		try
		{
			return getSimulator().isRunning();
		} catch( final RemoteException e )
		{
			LOG.error( "Problem reaching simulator", e );
			return false;
		}
	}

	@Override
	public boolean isComplete()
	{
		return this.complete;
	}

	@Override
	public RandomNumberStream getRNG()
	{
		return getRNG( MAIN_RNG_ID );
	}

	@Override
	public synchronized RandomNumberStream
		getRNG( final RandomNumberStream.ID rngID )
	{
		if( !this.rng.containsKey( rngID ) )
			this.rng.put( rngID, newRNG( rngID ) );
		return this.rng.get( rngID );
	}

	private RandomNumberStream newRNG( final RandomNumberStream.ID streamID )
	{
		return getBinder().inject( RandomNumberStream.Factory.class )
				.create( streamID, CoalaProperty.randomSeed.value()
						.getLong( System.currentTimeMillis() ) );
	}

//	@Override
//	public Observable<ClockStatusUpdate> getStatusUpdates()
//	{
//		return this.statusUpdates.asObservable();
//	}
//
//	@Override
//	public Observable<SimTime> getTimeUpdates()
//	{
//		return this.timeUpdates.asObservable();
//	}
//
//	@Override
//	public SimTime getVirtualOffset()
//	{
//		// TODO implement
//		throw new IllegalStateException( "NOT IMPLEMENTED" );
//	}
//
//	@Override
//	public SimTime toActualTime( final SimTime virtualTime )
//	{
//		// TODO implement
//		throw new IllegalStateException( "NOT IMPLEMENTED" );
//	}
//
//	@Override
//	public SimTime getActualOffset()
//	{
//		// TODO implement
//		throw new IllegalStateException( "NOT IMPLEMENTED" );
//	}
//
//	@Override
//	public SimTime toVirtualTime( final SimTime actualTime )
//	{
//		// TODO implement
//		throw new IllegalStateException( "NOT IMPLEMENTED" );
//	}
//
//	@Override
//	public Number getApproximateSpeedFactor()
//	{
//		// TODO implement
//		throw new IllegalStateException( "NOT IMPLEMENTED" );
//	}

}
