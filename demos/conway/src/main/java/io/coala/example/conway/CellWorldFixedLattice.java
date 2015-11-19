/* $Id: bb830e6692b6329df6ad5516530bc652aed51cea $
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
import java.util.TreeSet;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.joda.time.Duration;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
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
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class CellWorldFixedLattice extends BasicCapability implements CellWorld
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private final transient SimTime endCycle = getBinder()
			.inject(SimTime.Factory.class)
			.create(getProperty(CYCLE_TOTAL_CONFIG_KEY)
					.getNumber(CYCLE_TOTAL_DEFAULT), TimeUnit.TICKS);

	/** */
	private final transient SimDuration cycleDuration = new SimDuration(
			getProperty(CYCLE_DURATION_CONFIG_KEY)
					.getNumber(CYCLE_DURATION_DEFAULT),
			TimeUnit.TICKS);

	/** */
	private final transient Subject<CellState, CellState> myStates = PublishSubject
			.create();

	/** */
	private final transient Map<CellID, Subscription> myNeighbors = new HashMap<>();

	/** */
	@InjectLogger
	private transient Logger LOG;

	/** TODO use persistence capability? */
	private final Map<LifeStatus, Integer> myNeighborStateCount = new EnumMap<>(
			LifeStatus.class);

	/** TODO use persistence capability? */
	private final Collection<CellID> missing = new TreeSet<>();

	/** TODO use persistence capability? */
	private CellState myState = null;

	/**
	 * {@link CellWorldFixedLattice} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	public CellWorldFixedLattice(final Binder binder)
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
			throw new IllegalStateException("Can't remain in same state");

		LOG.trace(this.myNeighborStateCount + " -> " + state.getState());
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
		this.myStates.map(new Func1<CellState, CellState>()
		{
			public CellState call(final CellState state)
			{
				// forward local state stream to owner
				// LOG.trace("Forwarding "
				// + getID().getOwnerID().getValue() + " > "
				// + linkUpdate.getNeighborID().getValue());
				return state.copyFor(getID().getOwnerID().getParentID());
			}
		}).subscribe(getBinder().inject(SendingCapability.class)
				.outgoing(Duration.millis(1000)));

		setState(CellWorld.Util.parseInitialState(getBinder()));
		for (LifeStatus status : LifeStatus.values())
			this.myNeighborStateCount.put(status, Integer.valueOf(0));
	}

	@Override
	public void finish()
	{
		// stop Util.GLOBAL_SNIFFER from sniffing this cell
		this.myStates.onCompleted();
		for (Subscription sub : this.myNeighbors.values())
			if (sub != null)
				sub.unsubscribe();
	}

	@Override
	public SimTime getTime()
	{
		return this.myState == null ? null : this.myState.getGeneration();
	}

	@Override
	public Observable<CellState> myStates(final Observable<CellState> incoming)
	{
		// subscribe to all incoming neighbor events
		incoming.subscribe(new Observer<CellState>()
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
				// System.err.println("HANDLING: " + state);
				handleNeighborState(state);
			}
		});
		return this.myStates.asObservable();
	}

	/**
	 * TODO move to {@link BasicCell}?
	 * 
	 * @param linkUpdate
	 */
	protected void handleLinkUpdate(final CellLink linkUpdate)
	{
		final Subscription sub;
		synchronized (this.myNeighbors)
		{
			switch (linkUpdate.getType())
			{
			case CONNECTED:
				sub = this.myStates.map(new Func1<CellState, CellState>()
				{
					public CellState call(final CellState state)
					{
						// forward local state stream to new neighbor
						// LOG.trace("Forwarding "
						// + getID().getOwnerID().getValue() + " > "
						// + linkUpdate.getNeighborID().getValue());
						return state.copyFor(linkUpdate.getNeighborID());
					}
				}).subscribe(getBinder().inject(SendingCapability.class)
						.outgoing(Duration.millis(1000)));
				this.myNeighbors.put(linkUpdate.getNeighborID(), sub);
				this.missing.add(linkUpdate.getNeighborID());
				break;

			case DISCONNECTED:
				// stop listening to disconnected neighbor
				this.missing.remove(linkUpdate.getNeighborID());
				sub = this.myNeighbors.remove(linkUpdate.getNeighborID());
				if (sub == null)
					LOG.warn("Unexpected: already disconnected " + linkUpdate);
				else // cancel broadcasts to disconnected peer
					sub.unsubscribe();
				break;

			default:
				LOG.warn("Unexpected link event: " + linkUpdate.getType());
			}
		}
	}

	private final Collection<CellState> pending = new TreeSet<>();

	/**
	 * TODO move to {@link BasicCell}?
	 * 
	 * @param state
	 */
	protected void handleNeighborState(final CellState state)
	{
		// LOG.trace("Handling " + state.getCellID().getValue() + " > "
		// + getID().getOwnerID().getValue());

		if (state.getTime().compareTo(this.endCycle) >= 0)
		{
			// world is complete, end it
			LOG.info("End cycle reached: " + state.getTime() + ">="
					+ this.endCycle + ", ending world...");
			this.myStates.onCompleted();
			return;
		}

		if (state.getTime().compareTo(myState().getTime()) > 0)
		{
			// connected neighbor and self are out-of-sync
			// LOG.warn("Backlogging pending neighbor state: " + state
			// + ", tally: " + this.myNeighborStateCount + ", missing: "
			// + this.missing);
			synchronized (this.pending)
			{
				this.pending.add(state);
				return;
			}
		}

		synchronized (this.myNeighbors)
		{
			if (!this.missing.remove(state.getCellID()))
			{
				LOG.warn("Ignoring unexpected update",
						new IllegalStateException(state.getCellID()
								+ " not yet missing: " + this.missing));
				return;
			}
			synchronized (this.myNeighborStateCount)
			{
				this.myNeighborStateCount.put(state.getState(),
						this.myNeighborStateCount.get(state.getState()) + 1);
				this.myNeighborStateCount.notifyAll();
			}
			// cycle incomplete
			if (!this.missing.isEmpty())
			{
				// LOG.trace(
				// "Got " + (this.myNeighbors.size() - this.missing.size())
				// + " of " + this.myNeighbors.size() + " "
				// + this.myNeighborStateCount + ", received: "
				// + state + ", missing: " + this.missing);
				return;
			}

			// reset counters
			this.missing.addAll(this.myNeighbors.keySet());
			final Collection<CellState> pending;
			synchronized (this.pending)
			{
				pending = new TreeSet<>(this.pending);
				this.pending.clear();
			}

			// LOG.trace("Got all " + this.myNeighbors.size() + " "
			// + this.myNeighborStateCount + ", transitioning...");
			setState(this.myState.next(this.cycleDuration,
					this.myNeighborStateCount));
			synchronized (this.myNeighborStateCount)
			{
				this.myNeighborStateCount.clear();
				for (LifeStatus status : LifeStatus.values())
					this.myNeighborStateCount.put(status, Integer.valueOf(0));
			}

			for (CellState pendingState : pending)
				handleNeighborState(pendingState);
		}
	}

}
