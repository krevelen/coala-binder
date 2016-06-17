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
package io.coala.bind;

import java.util.Set;

import javax.inject.Provider;

import io.coala.agent.Agent;
import io.coala.capability.Capability;
import io.coala.factory.Factory;

/**
 * {@link Binder} can contain {@link Agent}s and provides services
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Deprecated
public interface Binder //extends Identifiable<AgentID>
{

	/** */
	String AGENT_TYPE = "agentType";

	/**
	 * Injection of an object, e.g. a {@link Capability} or {@link Factory}
	 * 
	 * @param type the type of object to inject
	 * @return the object resulting from a (contextualized) binding to some
	 *         instance, factory or provider
	 */
	<T> T inject( Class<T> type );

	Set<Class<?>> getBindings();

	<T> Provider<T> rebind( Class<T> type, T instance );

	<T> Provider<T> rebind( Class<T> type, Provider<? extends T> provider );

	<T> Provider<T> rebind( Class<T> type, Class<? extends T> factory );

}
