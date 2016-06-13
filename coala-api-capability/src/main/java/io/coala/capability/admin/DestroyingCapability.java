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
 * 
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.capability.admin;

import io.coala.agent.AgentID;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import io.coala.lifecycle.LifeCycleHooks;

/**
 * {@link DestroyingCapability} helps to invoke another {@link Agent}'s
 * {@link LifeCycleHooks#finish()} method;
 * 
 * @version $Id$
 * @author <a href="mailto:suki@almende.org">suki</a>
 */
public interface DestroyingCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 * 
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	interface Factory extends CapabilityFactory<DestroyingCapability>
	{
		// empty
	}

	/**
	 * @return the agent's {@link AgentID} once it has been killed
	 * @throws Exception if kill failed
	 */
	AgentID destroy() throws Exception;

	/**
	 * @param id the {@link AgentID}
	 * @return the specified {@link AgentID} again once it has been killed
	 * @throws Exception if kill failed
	 */
	AgentID destroy(AgentID id) throws Exception;

}