/* $Id: 4763c79154f4747b77a03998edfad358223c04b3 $
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
package io.coala.random;

/**
 * {@link RandomNumberDistribution}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface RandomNumberDistribution<T extends Number> extends
		RandomDistribution<T>
{

	/**
	 * @return the next pseudo-random value
	 */
	T draw();

}