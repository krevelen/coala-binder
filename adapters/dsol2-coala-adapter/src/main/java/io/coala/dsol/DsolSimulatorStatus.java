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
package io.coala.dsol;

import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.event.EventType;

/**
 * {@link DsolSimulatorStatus}
 */
public enum DsolSimulatorStatus //implements ClockStatus
{
	/** constructed */
	CREATED,

	/** @see {SimulatorInterface#START_REPLICATION_EVENT} */
	INITIALIZED( SimulatorInterface.START_REPLICATION_EVENT ),

	/** @see {SimulatorInterface#WARMUP_EVENT} */
	WARMED_UP( SimulatorInterface.WARMUP_EVENT ),

	/** @see {SimulatorInterface#TIME_CHANGED_EVENT} */
	TIME_CHANGED( SimulatorInterface.TIME_CHANGED_EVENT ),

	/** @see {SimulatorInterface#START_EVENT} */
	STARTED( SimulatorInterface.START_EVENT ),

	/** @see {SimulatorInterface#STEP_EVENT} */
	STEPPED( SimulatorInterface.STEP_EVENT ),

	/** @see {SimulatorInterface#STOP_EVENT} */
	STOPPED( SimulatorInterface.STOP_EVENT ),

	/** @see {SimulatorInterface#END_OF_REPLICATION_EVENT} */
	FINISHED( SimulatorInterface.END_OF_REPLICATION_EVENT ),

	/** @see {SimulatorInterface#START_REPLICATION_EVENT} */
	FAILED(),

	;

	private final EventType[] eventTypes;

	private DsolSimulatorStatus( final EventType... eventTypes )
	{
		this.eventTypes = eventTypes;
	}

	public static DsolSimulatorStatus of( final EventType eventType )
	{
		for( DsolSimulatorStatus value : values() )
			if( value.eventTypes != null && value.eventTypes.length > 0 )
				for( EventType type : value.eventTypes )
				if( type.equals( eventType ) ) return value;

		throw new IllegalArgumentException(
				"Unknown event type: " + eventType );
	}

//	/** @see ClockStatus#isRunning() */
//	@Override
//	public boolean isRunning()
//	{
//		return this == STARTED;
//	}
//
//	@Override
//	public boolean isFinished()
//	{
//		return this == FINISHED || this == FAILED;
//	}

}