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
package io.coala.enterprise.role;

import io.coala.agent.AgentID;
import io.coala.capability.CapabilityID;

/**
 * {@link ActorRoleID}
 */
@Deprecated
public class ActorRoleID extends CapabilityID
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link ActorRoleID} constructor
	 * 
	 * @param ownerID the owner agent's {@link AgentID}
	 * @param roleType the concrete type of identified {@link ActorRole}
	 */
	protected <T extends ActorRole<?>> ActorRoleID( final AgentID ownerID,
		final Class<T> roleType )
	{
		super( ownerID, roleType );
	}

}
