/* $Id: b82e5b2a4df66af46d627c6b303ac414bc6bad47 $
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
package io.coala.agent;

import org.apache.logging.log4j.Logger;

import io.coala.lifecycle.ActivationType;
import io.coala.lifecycle.LifeCycleHooks;
import io.coala.lifecycle.LifeCycleStatus;
import io.coala.lifecycle.MachineStatus;
import io.coala.log.LogUtil;

/**
 * {@link BasicAgentStatus}
 * 
 * @version $Id: b82e5b2a4df66af46d627c6b303ac414bc6bad47 $
 * @author Rick van Krevelen
 */
@Deprecated
public enum BasicAgentStatus implements AgentStatus<BasicAgentStatus>
{
	/** constructed */
	CREATED,

	/** ready to start */
	INITIALIZED,

	/** started/busy */
	ACTIVE,

	/** waiting to react */
	PASSIVE,

	/** activity done */
	COMPLETE,

	/** cleaned up */
	FINISHED,

	/** error occurred */
	FAILED,

	;

	/**
	 * @see MachineStatus#permitsTransitionFrom(MachineStatus)
	 */
	@Override
	public boolean permitsTransitionFrom( final BasicAgentStatus status )
	{
		return status.permitsTransitionTo( this );
	}

	/**
	 * @see MachineStatus#permitsTransitionTo(MachineStatus)
	 */
	@Override
	public boolean permitsTransitionTo( final BasicAgentStatus status )
	{
		switch( status )
		{

		case FAILED:
			return true; // always allowed to transition to FAILED

		case CREATED:
			return this == CREATED;

		case INITIALIZED:
			return this == CREATED;

		case PASSIVE:
			return this == INITIALIZED || this == PASSIVE || this == ACTIVE;

		case ACTIVE:
			return this == INITIALIZED || this == PASSIVE || this == ACTIVE;

		case COMPLETE:
			return this == CREATED || this == INITIALIZED || this == PASSIVE
					|| this == ACTIVE;

		case FINISHED:
			return this == INITIALIZED || this == COMPLETE || this == FINISHED;
		}
		return false;
	}

	/**
	 * @see LifeCycleStatus#isCreatedStatus()
	 */
	@Override
	public boolean isCreatedStatus()
	{
		return this == CREATED;
	}

	/**
	 * @see LifeCycleStatus#isInitializedStatus()
	 */
	@Override
	public boolean isInitializedStatus()
	{
		return this == INITIALIZED;
	}

	/**
	 * @see LifeCycleStatus#isActiveStatus()
	 */
	@Override
	public boolean isActiveStatus()
	{
		return this == ACTIVE;
	}

	/**
	 * @see LifeCycleStatus#isPassiveStatus()
	 */
	@Override
	public boolean isPassiveStatus()
	{
		return this == PASSIVE;
	}

	/**
	 * @see LifeCycleStatus#isCompleteStatus()
	 */
	@Override
	public boolean isCompleteStatus()
	{
		return this == COMPLETE;
	}

	/**
	 * @see LifeCycleStatus#isFinishedStatus()
	 */
	@Override
	public boolean isFinishedStatus()
	{
		return this == FINISHED;
	}

	/**
	 * @see LifeCycleStatus#isFailedStatus()
	 */
	@Override
	public boolean isFailedStatus()
	{
		return this == FAILED;
	}

	private static final Logger LOG = LogUtil
			.getLogger( BasicAgentStatus.class );

	/**
	 * @param agent the {@link Agent} to examine
	 * @return either {@link BasicAgentStatus#COMPLETE COMPLETE} for normal
	 *         termination, or {@link BasicAgentStatus#FINISHED FINISHED} for
	 *         abnormal termination (already completed/failed/finished)
	 */
	public static BasicAgentStatus determineKillStatus( final Agent agent )
	{
		final BasicAgentStatus currentStatus = agent.getStatus();
		final ActivationType activationType = ((LifeCycleHooks) agent)
				.getActivationType();
		switch( currentStatus )
		{
		case COMPLETE:
		case FAILED:
		case FINISHED: // done
			LOG.warn( "Race condition? Agent " + agent.getID() + " is "
					+ currentStatus
					+ " and should already have been finished" );
			return FINISHED;

		case CREATED:
		case INITIALIZED:
			return COMPLETE;

		case ACTIVE:
			switch( activationType )
			{
			case ACTIVATE_NEVER:
				LOG.warn( "Strange! Agent " + agent.getID() + " was "
						+ currentStatus
						+ " when it should never be activated" );
			case ACTIVATE_AND_FINISH:
			case ACTIVATE_ONCE:
			case ACTIVATE_MANY:
				// FIXME interrupt any running threads
			}
			return COMPLETE;

		case PASSIVE:
			// FIXME interrupt any running threads

		default:
			// should never happen
			LOG.warn( "Assuming " + COMPLETE
					+ " to kill agent from current status: " + currentStatus
					+ " for activation type: " + activationType );
			return COMPLETE;
		}
	}
}