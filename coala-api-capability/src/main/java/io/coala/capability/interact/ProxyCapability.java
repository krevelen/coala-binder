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
package io.coala.capability.interact;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;

/**
 * {@link ProxyCapability}
 */
@Deprecated
public interface ProxyCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<ProxyCapability>
	{
		// empty
	}

	/**
	 * @param agentID
	 * @param agentType
	 * @return a proxy for (remote) procedure calls to the specified agent
	 * @throws Exception
	 */
	<A extends Agent> A getProxy( AgentID agentID, Class<A> agentType )
		throws Exception;
}
