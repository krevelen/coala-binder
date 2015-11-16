/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-examples/src/main/java/io/coala/example/conway/CellState.java $
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

import java.util.Map;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.agent.AgentID;
import io.coala.message.AbstractMessage;
import io.coala.message.MessageID;
import io.coala.model.ModelID;
import io.coala.time.SimDuration;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;

/**
 * {@link CellState}
 * 
 * @date $Date: 2014-06-17 15:03:44 +0200 (Tue, 17 Jun 2014) $
 * @version $Revision: 302 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class CellState extends AbstractMessage<CellState.ID>
// implements Serializable, Comparable<CellState>, Timed<SimTime>
{

	@SuppressWarnings("serial")
	public static class ID extends MessageID<UUID, SimTime>
	{
		public ID(final ModelID modelID, final SimTime time)
		{
			super(modelID, new UUID(), time);
		}
	}

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private LifeStatus state = null;

	/**
	 * {@link CellState} zero-arg bean constructor
	 */
	protected CellState()
	{
		super();
	}

	/**
	 * {@link CellState} constructor
	 * 
	 * @param generation
	 * @param fromID
	 * @param lifeState
	 */
	public CellState(final SimTime generation, final CellID fromID,
			final LifeStatus lifeState)
	{
		this(generation, fromID, fromID, lifeState);
	}

	/**
	 * {@link CellState} constructor for copies etc.
	 * 
	 * @param generation
	 * @param fromID
	 * @param toID
	 * @param lifeState
	 */
	protected CellState(final SimTime generation, final CellID fromID,
			final AgentID toID, final LifeStatus lifeState)
	{
		super(new ID(fromID.getModelID(), generation), fromID, fromID, toID);
		this.state = lifeState;
	}

	/**
	 * @return the cellID
	 */
	@JsonIgnore
	public CellID getCellID()
	{
		return (CellID) getSenderID();
	}

	/**
	 * @return the generation in {@link TimeUnit#TICKS}
	 */
	@JsonIgnore
	public SimTime getGeneration()
	{
		return getID().getTime();
	}

	/**
	 * @return the lifeState
	 */
	public LifeStatus getState()
	{
		return this.state;
	}

	/**
	 * @param receiverID {@link AgentID} of the receiver to inform
	 * @return a new {@link CellState} copy
	 */
	public CellState copyFor(final AgentID receiverID)
	{
		return new CellState(getGeneration(), getCellID(), receiverID,
				getState());
	}

	public CellState next(final SimDuration cycleDuration,
			final Map<LifeStatus, Integer> myNeighborStateCount)
	{
		final SimTime toTime = getID().getTime().plus(cycleDuration);
		final LifeStatus toState = getState()
				.getTransition(myNeighborStateCount);
		return new CellState(toTime, getCellID(), toState);
	}

	@Override
	public String toString()
	{
		return String.format("%s|%s|%s", getCellID().getValue(),
				getGeneration().intValue(), getState());
	}

}