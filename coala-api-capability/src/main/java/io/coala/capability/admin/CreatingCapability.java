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
package io.coala.capability.admin;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.agent.AgentStatusUpdate;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import rx.Observable;

/**
 * {@link CreatingCapability} links agents for lookup or directory purposes
 */
@Deprecated
public interface CreatingCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<CreatingCapability>
	{
		// empty
	}

	Observable<AgentID> getChildIDs( boolean currentOnly );

	/**
	 * @param agentID the agent identifier
	 * @return an {@link Observable} of the new agent's
	 *         {@link AgentStatusUpdate}
	 */
	Observable<AgentStatusUpdate> createAgent( String agentID );

	/**
	 * @param agentID the agent identifier
	 * @param agentType the type of {@link Agent} to boot
	 * @return an {@link Observable} of the new agent's
	 *         {@link AgentStatusUpdate}
	 */
	<A extends Agent> Observable<AgentStatusUpdate> createAgent( String agentID,
		Class<A> agentType );

	/**
	 * @param agentID the agent identifier
	 * @return an {@link Observable} of the new agent's
	 *         {@link AgentStatusUpdate}
	 */
	Observable<AgentStatusUpdate> createAgent( AgentID agentID );

	/**
	 * @param <A> extends {@link Agent}
	 * @param agentID the agent identifier
	 * @param agentType the concrete type of {@link Agent} to boot
	 * @return an {@link Observable} of the new agent's
	 *         {@link AgentStatusUpdate}
	 */
	<A extends Agent> Observable<AgentStatusUpdate>
		createAgent( AgentID agentID, Class<A> agentType );

}
