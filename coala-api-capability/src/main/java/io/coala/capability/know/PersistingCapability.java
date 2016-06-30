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
package io.coala.capability.know;

import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import io.coala.name.Identifiable;
import io.coala.name.Identifier;

/**
 * {@link PersistingCapability} provides functionality similar to JPA entity
 * management
 */
@Deprecated
public interface PersistingCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<PersistingCapability>
	{
		// empty
	}

	/**
	 * TODO Check JPA or JDO (incl. NoSQL) APIs and tooling
	 * 
	 * TODO Compare optimistic vs. pessimistic locking approaches
	 */

	/**
	 * @param identifiable the {@link Identifiable}
	 * @return the (persisted/merged) {@link Identifiable}
	 */
	<T extends Identifiable<?>> T persist( T identifiable );

	/**
	 * @param identifier the persisted {@link Identifiable}'s {@link Identifier}
	 * @return the persisted {@link Identifiable} or {@code null} if unknown
	 */
	<ID extends Identifier<?, ?>, T extends Identifiable<ID>> T
		retrieve( ID identifier );

}
