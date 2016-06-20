/* $Id$
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
 * @version $Id$
 */
@SuppressWarnings( "serial" )
public class CellState extends AbstractMessage<CellState.ID>
{

	public static class ID extends MessageID<UUID, SimTime>
	{
		/**
		 * {@link ID} zero-arg bean constructor
		 */
		protected ID()
		{
			super();
		}

		public ID( final ModelID modelID, final SimTime time )
		{
			super( modelID, new UUID(), time );
		}
	}

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
	public CellState( final SimTime generation, final CellID fromID,
		final LifeStatus lifeState )
	{
		this( generation, fromID, fromID, lifeState );
	}

	/**
	 * {@link CellState} constructor, for copies etc.
	 * 
	 * @param generation
	 * @param fromID
	 * @param toID
	 * @param lifeState
	 */
	protected CellState( final SimTime generation, final CellID fromID,
		final AgentID toID, final LifeStatus lifeState )
	{
		super( new ID( fromID.getModelID(), generation ), fromID, fromID,
				toID );
		this.state = lifeState;
	}

	@Override
	public ID getID()
	{
		return super.getID();
	}

	/**
	 * @return the {@link LifeStatus}
	 */
	public LifeStatus getState()
	{
		return this.state;
	}

	/**
	 * @return the origin {@link CellID}
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

	@Override
	public String toString()
	{
		return String.format( "%s|%s|%s", getCellID().getValue(),
				getGeneration().intValue(), getState() );
	}

	/**
	 * @param receiverID {@link AgentID} of the receiver to inform
	 * @return a new {@link CellState} copy
	 */
	public CellState copyFor( final AgentID receiverID )
	{
		return new CellState( getGeneration(), getCellID(), receiverID,
				getState() );
	}

	/**
	 * @param delta generation time delta to add
	 * @param myNeighborStateCount neighborhood values
	 * @return new value for this {@link CellState} after transition
	 */
	public CellState next( final SimDuration delta,
		final Map<LifeStatus, Integer> myNeighborStateCount )
	{
		final LifeStatus toState = getState()
				.getTransition( myNeighborStateCount );
		return new CellState( getGeneration().plus( delta ), getCellID(),
				toState );
	}

}