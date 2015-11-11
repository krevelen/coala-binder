/* $Id: bb830e6692b6329df6ad5516530bc652aed51cea $
 * $URL: https://dev.almende.com/svn/abms/coala-examples/src/main/java/io/coala/example/conway/CellWorldLattice.java $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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
 * 
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.example.conway;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.interact.SendingCapability;
import io.coala.example.conway.CellLink.CellLinkStatus;
import io.coala.exception.CoalaException;
import io.coala.log.InjectLogger;
import io.coala.time.SimDuration;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link CellWorldFixedLattice}
 * 
 * @date $Date: 2014-06-20 12:27:58 +0200 (Fri, 20 Jun 2014) $
 * @version $Revision: 312 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 */
public class CellWorldFixedLattice extends BasicCapability implements CellWorld
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final int TRANSITION_THRESHOLD = 8;

	/** TODO from config */
	private final transient SimTime endCycle = getBinder()
			.inject(SimTime.Factory.class)
			.create(getProperty(CYCLE_DURATION_CONFIG_KEY)
					.getNumber(CYCLE_DURATION_DEFAULT), TimeUnit.TICKS);

	/** */
	private final transient SimDuration cycleDuration = new SimDuration(
			getProperty(CYCLE_TOTAL_CONFIG_KEY).getNumber(CYCLE_TOTAL_DEFAULT),
			TimeUnit.TICKS);

	/** */
	private final transient Subject<CellState, CellState> myStates = PublishSubject
			.create();

	/** */
	private final transient Map<CellID, Subscription> myNeighbors = new HashMap<>();

	/** TODO use persistence capability? */
	private final Map<LifeStatus, Integer> myNeighborStateCount = new EnumMap<>(
			LifeStatus.class);

	/** */
	@InjectLogger
	private transient Logger LOG;

	/** */
	private final CellID myID = (CellID) getID().getOwnerID();

	/** TODO use persistence capability? */
	private CellState myState = null;

	/**
	 * {@link CellWorldFixedLattice} constructor
	 * 
	 * @param host
	 * @throws CoalaException
	 */
	@Inject
	public CellWorldFixedLattice(final Binder binder) throws CoalaException
	{
		super(binder);
	}

	@Override
	public CellState myState()
	{
		return this.myState;
	}

	@Override
	public Collection<CellID> myNeighbors()
	{
		return Collections.unmodifiableCollection(this.myNeighbors.keySet());
	}

	/**
	 * @param state
	 */
	protected synchronized void setState(final CellState state)
	{
		if (this.myState == null && state == null)
			throw new IllegalStateException("Can't remain in null state");
		if (this.myState != null && state != null && this.myState.equals(state))
			throw new IllegalStateException("Can't remain in same tick");

		LOG.trace("Transitioning to: " + state);
		this.myState = state;
		this.myStates.onNext(state);
	}

	/**
	 * @return an {@link Observable} stream of {@link CellLink} updates based on
	 *         this world's current topology (default: torus)
	 * @throws CoalaException
	 */
	protected Observable<CellLink> myLinks() throws CoalaException
	{
		return Observable.from(Util.determineTorusNeighbors(getBinder()))
				.map(new Func1<CellID, CellLink>()
				{
					@Override
					public CellLink call(final CellID cellID)
					{
						return new CellLink(cellID, CellLinkStatus.CONNECTED);
					}
				});
	}

	@Override
	public synchronized void initialize() throws CoalaException
	{
		this.myNeighbors.put(myID,
				this.myStates.subscribe(GLOBAL_TRANSITION_SNIFFER));
		myLinks().subscribe(new Observer<CellLink>()
		{

			@Override
			public void onCompleted()
			{
				LOG.trace("Lattice complete");
			}

			@Override
			public void onError(final Throwable e)
			{
				e.printStackTrace();
			}

			@Override
			public void onNext(final CellLink link)
			{
				handleLinkUpdate(link);
			}
		});

		setState(CellWorld.Util.parseInitialState(getBinder()));
		for (LifeStatus status : LifeStatus.values())
			this.myNeighborStateCount.put(status, Integer.valueOf(0));
	}

	@Override
	public void finish()
	{
		// stop Util.GLOBAL_SNIFFER from sniffing this cell
		this.myNeighbors.get(this.myID).unsubscribe();
	}

	@Override
	public Observable<CellState> myStates(
			final Observable<CellState> neighborStates)
	{
		neighborStates.subscribe(new Observer<CellState>()
		{
			@Override
			public void onCompleted()
			{
				// neighbors won't talk to me anymore
			}

			@Override
			public void onError(final Throwable e)
			{
				// neighbors can't talk to me anymore
				e.printStackTrace();
			}

			@Override
			public void onNext(final CellState state)
			{
				handleNeighborState(state);
			}
		});
		return this.myStates.asObservable();
	}

	protected void handleLinkUpdate(final CellLink linkUpdate)
	{
		synchronized (this.myNeighbors)
		{
			switch (linkUpdate.getType())
			{
			case CONNECTED:
				// listen for incoming events from newly connected neighbor
				// send outgoing events to newly connected neighbor
				this.myNeighbors.put(linkUpdate.getNeighborID(),
						this.myStates.subscribe(new Observer<CellState>()
						{
							@Override
							public void onCompleted()
							{
								// state updates completed
							}

							@Override
							public void onError(final Throwable e)
							{
								// state updates failed
								e.printStackTrace();
							}

							@Override
							public void onNext(final CellState t)
							{
								final CellState carbonCopy = t
										.copyFor(linkUpdate.getNeighborID());
								try
								{
									getBinder().inject(SendingCapability.class)
											.send(carbonCopy);
								} catch (final Exception e)
								{
									LOG.warn(
											"Problem broadcasting state update: "
													+ carbonCopy,
											e);
								}
							}
						}));
				break;

			case DISCONNECTED:
				// stop listening to disconnected neighbor
				final Subscription sub = this.myNeighbors
						.remove(linkUpdate.getNeighborID());

				if (sub == null)
					LOG.warn("Unexpected: aready disconnected " + linkUpdate);
				else // cancel broadcasts to disconnected peer
					sub.unsubscribe();
				break;

			default:
				LOG.warn("Unexpected link event: " + linkUpdate.getType());
			}
		}
	}

	protected void handleNeighborState(final CellState state)
	{
		if (state.getTime().compareTo(this.endCycle) >= 0)
		{
			// world is complete, end it
			LOG.info("Simulation complete, dying...");
			setStatus(BasicCapabilityStatus.COMPLETE);
		}

		if (state.getTime().compareTo(myState().getTime()) != 0)
		{
			// connected neighbor and self are out-of-sync
			LOG.warn("Ignoring neighbor state out-of-sync: self="
					+ myState().getTime() + " <> them=" + state.getTime());
			return;
		}

		final int total;
		synchronized (this.myNeighborStateCount)
		{
			this.myNeighborStateCount.put(state.getState(),
					this.myNeighborStateCount.get(state.getState()) + 1);
			this.myNeighborStateCount.notifyAll();
			total = this.myNeighborStateCount.get(LifeStatus.ALIVE)
					+ this.myNeighborStateCount.get(LifeStatus.DEAD);
		}
		if (total == TRANSITION_THRESHOLD)
		{
			// LifeStatus.blockUntilTotalStatesReached(this.myNeighborStateCount,
			// 8);
			LOG.trace("Got all " + total + " " + this.myNeighborStateCount
					+ ", transitioning...");
			setState(this.myState.next(this.cycleDuration,
					this.myNeighborStateCount));

			// reset counters
			synchronized (this.myNeighborStateCount)
			{
				this.myNeighborStateCount.clear();
				for (LifeStatus status : LifeStatus.values())
					this.myNeighborStateCount.put(status, Integer.valueOf(0));
			}
		} else
			LOG.trace("Tally at " + total + " " + this.myNeighborStateCount
					+ ", got neighbor state: " + state);
	}

}
