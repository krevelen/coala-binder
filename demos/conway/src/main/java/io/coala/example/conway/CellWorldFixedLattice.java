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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.configure.ConfiguringCapability;
import io.coala.capability.interact.ReceivingCapability;
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
	private static final String CYCLE_DURATION_CONFIG_KEY = "cycleDuration";

	/** */
	@InjectLogger
	private transient Logger LOG;

	/** */
	private final SimDuration cycleDuration;

	/** */
	private CellState myState = null;

	/** */
	private final Subject<CellState, CellState> myStates = PublishSubject
			.create();

	/** */
	private final Map<CellID, Subscription> myNeighbors = Collections
			.synchronizedMap(new HashMap<CellID, Subscription>());

	/** */
	private final Map<LifeStatus, Integer> myNeighborStateCount = Collections
			.synchronizedMap(
					new EnumMap<LifeStatus, Integer>(LifeStatus.class));

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

		this.cycleDuration = new SimDuration(
				getProperty(CYCLE_DURATION_CONFIG_KEY).getNumber(),
				TimeUnit.TICKS);
		this.myStates.subscribe(Util.GLOBAL_TRANSITIONS);
		binder.inject(ReceivingCapability.class).getIncoming()
				.ofType(CellState.class).subscribe(this.neighborStateHandler);
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
		this.myState = state;
		this.myStates.onNext(state);

		// reset counters
		this.myNeighborStateCount.clear();
		for (LifeStatus status : LifeStatus.values())
			this.myNeighborStateCount.put(status, Integer.valueOf(0));
	}

	private final Observer<CellLink> linkStateHandler = new Observer<CellLink>()
	{

		@Override
		public void onCompleted()
		{
			LOG.trace("Simulation complete?!?");
		}

		@Override
		public void onError(final Throwable e)
		{
			e.printStackTrace();
		}

		@Override
		public void onNext(final CellLink percept)
		{
			synchronized (myNeighbors)
			{
				switch (percept.getType())
				{
				case CONNECTED:
					myNeighbors.put(percept.getNeighborID(),
							myStates.subscribe(new Observer<CellState>()
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
									.copyFor(percept.getNeighborID());
							try
							{
								getBinder().inject(SendingCapability.class)
										.send(carbonCopy);
							} catch (final Exception e)
							{
								LOG.warn("Problem broadcasting state update: "
										+ carbonCopy, e);
							}
						}
					}));
					break;

				case DISCONNECTED:
					final Subscription sub = myNeighbors
							.remove(percept.getNeighborID());

					if (sub == null)
						LOG.warn("Aready disconnected: " + percept);
					else // cancel broadcasts to disconnected peer
						sub.unsubscribe();
					break;

				default:
					LOG.warn("Unexpected link event: " + percept.getType());
				}
			}
		}
	};

	private final SimTime endCycle = getBinder().inject(SimTime.Factory.class)
			.create(100, TimeUnit.TICKS); // TODO from config

	private final Observer<CellState> neighborStateHandler = new Observer<CellState>()
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
			if (state.getTime().compareTo(endCycle) >= 0)
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

			synchronized (myNeighborStateCount)
			{
				myNeighborStateCount.put(state.getState(),
						myNeighborStateCount.get(state.getState()) + 1);
				myNeighborStateCount.notifyAll();
				LOG.trace("Got neighbor state: " + state + ", tally at: "
						+ myNeighborStateCount);
			}
		}
	};

	/**
	 * @return an {@link Observable} stream of {@link CellLink} updates based on
	 *         this world's current topology (default: torus)
	 * @throws CoalaException
	 */
	protected Observable<CellLink> myLinks() throws CoalaException
	{
		return Observable
				.from(Util.getTorusNeighborIDs((CellID) getID().getOwnerID(),
						getBinder().inject(ConfiguringCapability.class)))
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
		if (getID().getOwnerID() instanceof CellID == false)
		{
			LOG.warn("Unexpected owner ID type: "
					+ getID().getOwnerID().getClass().getName());
			return;
		}

		final List<Map<CellID, LifeStatus>> initialStates = Util.importLattice(
				getID().getModelID(),
				getBinder().inject(ConfiguringCapability.class));

		final CellID myID = (CellID) getID().getOwnerID();
		myLinks().subscribe(this.linkStateHandler);

		final LifeStatus startState = initialStates.get(myID.getRow())
				.get(myID);
		final SimTime startCycle = getBinder().inject(SimTime.Factory.class)
				.create(0, TimeUnit.TICKS);
		final CellState initialState = new CellState(startCycle, myID,
				startState);
		LOG.trace("Initializing to state: " + initialState);
		setState(initialState);
	}

	@Override
	public void proceed()
	{
		try
		{
			LifeStatus.blockUntilTotalStatesReached(this.myNeighborStateCount,
					8);
			LOG.trace(
					"Got all neighbors' status: " + this.myNeighborStateCount);
			synchronized (this.myNeighborStateCount)
			{
				setState(this.myState.next(this.cycleDuration,
						this.myNeighborStateCount));
			}
			LOG.trace("Performed transition to: " + this.myState);
		} catch (final InterruptedException e)
		{
			LOG.trace("Problem awaiting neighbor status, got: "
					+ this.myNeighborStateCount);
		}
	}

}
