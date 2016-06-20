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
package io.coala.time;

import io.coala.factory.Factory;

/**
 * {@link SimTimeFactory}
 * 
 * @version $Id$
 * @deprecated use {@link SimTime.Factory}
 */
@Deprecated
public interface SimTimeFactory extends Factory
{

	/**
	 * @param value
	 * @param unit
	 * @return the new {@link SimTime} object
	 */
	SimTime create( final Number value, final TimeUnit unit );

}