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
package io.coala.enterprise.organization;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.capability.replicate.ReplicatingCapability;
import io.coala.model.ModelComponent;
import io.coala.time.SimTime;

/**
 * {@link Organization}
 */
@Deprecated
public interface Organization extends Agent, ModelComponent<AgentID>
{

	/**
	 * @return the {@link SimulatorService}
	 */
	ReplicatingCapability getSimulator();

	/**
	 * @return the current {@link SimTime}
	 */
	SimTime getTime();
}
