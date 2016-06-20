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
package io.coala.lifecycle;

import java.io.Serializable;

/**
 * {@link MachineStatus} represents a state of a {@link LifeCycle} object, a
 * kind of state machine with standard states for "started" and "stopped"
 * 
 * @param <THIS> the (sub)type of {@link MachineStatus}
 */
public interface MachineStatus<THIS extends MachineStatus<THIS>>
	extends Serializable
{

	/** @return the {@link Set} of states that may follow this one */
	//Collection<? extends THIS> getPermittedTransitions();

	/**
	 * @param status
	 * @return {@code true} if specified state's permitted transitions contains
	 *         this state, {@code false} otherwise
	 */
	boolean permitsTransitionFrom( THIS status );

	/**
	 * @param status
	 * @return {@code true} if this state's permitted transitions contains
	 *         specified state, {@code false} otherwise
	 */
	boolean permitsTransitionTo( THIS status );

}
