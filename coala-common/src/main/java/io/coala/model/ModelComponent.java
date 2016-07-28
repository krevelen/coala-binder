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
package io.coala.model;

import java.io.Serializable;

import io.coala.agent.AgentID;
import io.coala.name.Identifiable;
import io.coala.time.AbstractInstant;

/**
 * {@link ModelComponent}
 * 
 * @param <ID> the type of {@link ModelComponentID}
 * @param <THIS> the concrete type of {@link ModelComponent}
 */
@Deprecated
public interface ModelComponent<ID extends ModelComponentID<?>>
	extends Identifiable<ID>, Serializable
{

	/**
	 * @return the {@link AgentID} of the owner of this {@link ModelComponent}
	 */
	AgentID getOwnerID();

	/**
	 * @return
	 */
	AbstractInstant<?> getTime();

	/**
	 * {@link Builder}
	 * 
	 * @param <ID>
	 * @param <M>
	 * @param <THIS>
	 */
	public interface Builder<ID extends ModelComponentID<?>, M extends ModelComponent<ID>, THIS extends Builder<ID, M, THIS>>
		extends Identifiable.Builder<ID, M, THIS>
	{
		/**
		 * @param ownerID the {@link AgentID} to set
		 * @return this {@link Builder}
		 */
		THIS withOwnerID( AgentID ownerID );
	}

}
