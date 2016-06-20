/* $Id: b17619a6b4279adbd4286009d1dbc48c3f69d4bc $
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
package io.coala.capability.replicate;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.plan.ClockStatus;
import io.coala.capability.plan.ClockStatusUpdate;
import io.coala.capability.plan.ClockStatusUpdateImpl;
import io.coala.capability.plan.SchedulingCapability;
import io.coala.log.InjectLogger;
import io.coala.name.AbstractIdentifiable;
import io.coala.process.Job;
import io.coala.random.RandomNumberStream;
import io.coala.time.ClockID;
import io.coala.time.Instant;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import io.coala.time.Trigger;
import rx.Observable;
import rx.Observer;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

/**
 * {@link BasicReplicatingCapability}
 * 
 * @version $Id: b17619a6b4279adbd4286009d1dbc48c3f69d4bc $
 * @author Rick van Krevelen
 */
@Deprecated
public class BasicReplicatingCapability extends BasicCapability implements
	ReplicatingCapability, RandomizingCapability, SchedulingCapability<SimTime>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final Map<ClockID, Clock> CLOCK_CACHE = new HashMap<>();

	/** */
	private final transient Subject<SimTime, SimTime> timeUpdates;

	/** */
	private final transient Subject<ClockStatusUpdate, ClockStatusUpdate> statusUpdates;

	/** */
	private final Map<RandomNumberStream.ID, RandomNumberStream> rng = Collections
			.synchronizedMap(
					new HashMap<RandomNumberStream.ID, RandomNumberStream>() );

	/** */
	private final ClockID clockID;

	@InjectLogger
	private transient Logger LOG;

	/** */
	private final TimeUnit timeUnit;

	/**
	 * {@link ClockStatusEnum}
	 * 
	 * @version $Id: b17619a6b4279adbd4286009d1dbc48c3f69d4bc $
	 */
	private enum ClockStatusEnum implements ClockStatus
	{
		/** */
		CREATED,

		/** */
		RUNNING,

		/** */
		FINISHED,

		;

		@Override
		public boolean isRunning()
		{
			return this == RUNNING;
		}

		@Override
		public boolean isFinished()
		{
			return this == FINISHED;
		}

	}

	/**
	 * {@link BasicReplicatingCapability} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected BasicReplicatingCapability( final Binder binder )
	{
		super( binder );
		final ReplicationConfig cfg = binder.inject( ReplicationConfig.class );
		this.clockID = cfg.getClockID();
		this.timeUnit = cfg.getBaseTimeUnit();
		this.timeUpdates = BehaviorSubject
				.create( binder.inject( SimTime.Factory.class )
						.create( Double.NaN, this.timeUnit ) );
		final ClockStatusUpdate initStatus = new ClockStatusUpdateImpl(
				this.clockID, ClockStatusEnum.CREATED );
		this.statusUpdates = BehaviorSubject.create( initStatus );
	}

	@Override
	public void pause()
	{
		// TODO suspend all clock executors, update status
		throw new IllegalStateException( "NOT IMPLEMENTED" );
	}

	@Override
	public void start()
	{
		// TODO resume all clock executors, update status
		throw new IllegalStateException( "NOT IMPLEMENTED" );
	}

	public static class Clock extends AbstractIdentifiable<ClockID>
	{
		/** */
		private static final long serialVersionUID = 1L;

		private final SortedMap<Instant<?>, Set<Job<?>>> pending = new TreeMap<>();

		private final ExecutorService executor = Executors
				.newCachedThreadPool();

		private final SimTime offset;

		protected Clock( final ClockID id, final Date offset )
		{
			super( id );
			this.offset = new SimTime( id, offset.getTime(), TimeUnit.MILLIS,
					offset );
		}

		public void schedule( final Job<?> job, final Trigger<?> trigger )
		{
			trigger.getInstants().subscribe( new Observer<Instant<?>>()
			{
				@Override
				public void onCompleted()
				{
					// ?
				}

				@Override
				public void onError( final Throwable e )
				{
					e.printStackTrace();
				}

				@Override
				public void onNext( final Instant<?> instant )
				{
					schedule( job, instant );
				}
			} );
		}

		public void schedule( final Job<?> job, final Instant<?> time )
		{
			synchronized( pending )
			{
				if( !this.pending.containsKey( time ) )
					this.pending.put( time, new HashSet<Job<?>>() );

				this.pending.get( time ).add( job );

				this.executor.execute( new Runnable()
				{
					@Override
					public void run()
					{
						synchronized( pending )
						{
							pending.get( time ).remove( job );
						}
					}
				} );
			}
		}

		public SimTime getVirtualOffset()
		{
			return this.offset;
		}

		public SimTime getActualOffset()
		{
			return this.offset;
		}

		public Number getApproximateSpeedFactor()
		{
			return 1;
		}
	}

	@Override
	public SimTime getTime()
	{
		return getBinder().inject( SimTime.Factory.class ).create( 0,
				this.timeUnit );
	}

	protected synchronized Clock getClock()
	{
		LOG.info( "Obtaining clock with id: " + getClockID().getValue()
				+ " from " + CLOCK_CACHE + " :: RESULT : "
				+ CLOCK_CACHE.get( getClockID() ) );

		if( !CLOCK_CACHE.containsKey( getClockID() ) ) CLOCK_CACHE
				.put( getClockID(), new Clock( getClockID(), new Date() ) );

		return CLOCK_CACHE.get( getClockID() );
	}

	@Override
	public void schedule( final Job<?> job, final Trigger<?> trigger )
	{
		getClock().schedule( job, trigger );
	}

	@Override
	public boolean unschedule( final Job<?> job )
	{
		throw new IllegalStateException( "NOT IMPLEMENTED" );
	}

	@Override
	public ClockID getClockID()
	{
		return this.clockID;
	}

	@Override
	public SimTime getVirtualOffset()
	{
		return getClock().getVirtualOffset();
	}

	@Override
	public SimTime getActualOffset()
	{
		return getClock().getActualOffset();
	}

	@Override
	public SimTime toActualTime( final SimTime virtualTime )
	{
		return getClock().getActualOffset()
				.plus( virtualTime.min( getClock().getVirtualOffset() )
						.dividedBy( getClock().getApproximateSpeedFactor() ) );
	}

	@Override
	public SimTime toVirtualTime( final SimTime actualTime )
	{
		return getClock().getVirtualOffset()
				.plus( actualTime.min( getClock().getActualOffset() )
						.multipliedBy(
								getClock().getApproximateSpeedFactor() ) );
	}

	@Override
	public Number getApproximateSpeedFactor()
	{
		return getClock().getApproximateSpeedFactor();
	}

	@Override
	public boolean isRunning()
	{
		return !CLOCK_CACHE.get( getClockID() ).executor.isShutdown();
	}

	@Override
	public boolean isComplete()
	{
		return CLOCK_CACHE.get( getClockID() ).executor.isShutdown();
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
				.create( streamID, System.currentTimeMillis() );
	}

	@Override
	public Observable<ClockStatusUpdate> getStatusUpdates()
	{
		return this.statusUpdates.asObservable();
	}

	@Override
	public Observable<SimTime> getTimeUpdates()
	{
		return this.timeUpdates.asObservable();
	}

}
