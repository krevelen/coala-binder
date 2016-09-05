/* $Id: 11fdb210b9731c946976369479741d6bcd781b56 $
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
package io.coala.name;

/**
 * {@link Identifiable} owns an {@link AbstractIdentifier}
 * 
 * @version $Id: 11fdb210b9731c946976369479741d6bcd781b56 $
 * @author Rick van Krevelen
 * 
 * @param <ID> the {@link Identifier} type
 * @param <THIS> the concrete {@link Identifiable} type
 * @deprecated please use {@link io.coala.json.Wrapper}
 */
@Deprecated
public interface Identifiable<ID extends Identifier<?, ?>>
	extends Comparable<Identifiable<ID>>
{

	/**
	 * @return the {@link ID} value
	 */
	ID getID();

	/**
	 * {@link Builder}
	 * 
	 * @version $Id: 11fdb210b9731c946976369479741d6bcd781b56 $
	 * 
	 * @param <ID> the {@link Identifiable}'s {@link Identifier} type
	 * @param <T> the {@link Identifiable} type
	 * @param <THIS> the concrete {@link Builder} type
	 */
	public interface Builder<ID extends Identifier<?, ?>, T extends Identifiable<ID>, THIS extends Builder<ID, T, THIS>>
	{
		/**
		 * @param id the {@link ID} value to set
		 * @return this {@link Builder} for chaining
		 */
		THIS withID( ID id );

		/**
		 * @return the built {@link T} result
		 */
		T build();
	}

}
