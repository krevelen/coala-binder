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
package io.coala.random.x;

import javax.measure.quantity.Quantity;

import org.jscience.physics.amount.Amount;

import io.coala.random.RandomDistribution;

/**
 * {@link RandomAmountDistribution}
 * 
 * @param
 * 			<Q>the concrete type of {@link Quantity} for produced
 *            {@link Amount}s
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface RandomAmountDistribution<Q extends Quantity>
	extends RandomDistribution<Amount<Q>>
{

	/**
	 * @return the next pseudo-random value
	 */
	Amount<Q> draw();

}