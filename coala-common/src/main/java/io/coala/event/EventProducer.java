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

import io.coala.model.ModelComponentID;
import io.coala.name.Identifiable;
import rx.Observable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link EventProducer} tags originators of {@link Event}s
 * 
 * @param <ID> the type of {@link ModelComponentID}
 * @param <E> the (super)type of {@link Event}s being produced
 */
public interface EventProducer<ID extends ModelComponentID<?>, E extends Event<?>>
	extends Identifiable<ID>
{

	/** @param listener the {@link EventListener} to add */
	// <T extends EventListener<? super E>> void register(T listener);

	/**
	 * @return an {@link Observable} of the {@link Event}s produced by this
	 *         {@link EventProducer}
	 */
	@JsonIgnore
	Observable<E> getEvents();

}
