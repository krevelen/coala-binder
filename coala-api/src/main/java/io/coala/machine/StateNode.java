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
package io.coala.machine;

/**
 * {@link StateNode} represents a node in a state machine's directed transition
 * graph
 * 
 * @param <THIS> the concrete type of {@link StateNode}
 */
public interface StateNode<THIS extends StateNode<THIS>>
{

	/**
	 * @param successor a {@link StateNode}
	 * @return {@code true} if specified {@link StateNode} is a valid direct
	 *         successor of this {@link StateNode}, {@code false} otherwise
	 */
	boolean mayPrecede( THIS successor );

	/**
	 * @param precessor a {@link StateNode}
	 * @return {@code true} if this {@link StateNode} is a valid direct
	 *         successor of specified {@link StateNode}, {@code false} otherwise
	 */
	@SuppressWarnings( "unchecked" )
	default boolean maySucceed( THIS precessor )
	{
		return precessor.mayPrecede( (THIS) this );
	}

}
