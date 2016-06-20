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
package io.coala.event;

import io.coala.model.ModelComponent;
import io.coala.model.ModelComponentID;
import io.coala.process.Job;

import java.io.Serializable;

/**
 * {@link Event} t (local) event types
 * 
 * @param <ID> the type of {@link EventID} for this {@link Event}
 */
public interface Event<ID extends EventID<?>>
	extends ModelComponent<ID>, Job<ID>, Serializable
{

	/**
	 * @return the {@link ModelComponentID} identifying the
	 *         {@link EventProducer} producing this {@link Event}
	 */
	ModelComponentID<?> getProducerID();

	/**
	 * {@link Builder}
	 * 
	 * @param <ID>
	 * @param <E>
	 * @param <THIS>
	 */
	public interface Builder<ID extends EventID<?>, E extends Event<ID>, THIS extends Builder<ID, E, THIS>>
		extends ModelComponent.Builder<ID, E, THIS>
	{

		/**
		 * @param producerID the {@link EventProducerID} to set
		 * @return this {@link Builder}
		 */
		THIS withProducerID( ModelComponentID<?> producerID );
	}

}
