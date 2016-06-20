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
package io.coala.experimental.control;

/**
 * {@link Controllable}
 */
public @interface Controllable //extends Identifiable<String>
{

	/**
	 * @return a {@link Set} containing each {@link Control} of this
	 *         {@link Controllable}
	 */
	//Set<Control> getControls();

	/**
	 * @param controlType the type of {@link Control} to return
	 * @return the {@link Control} of specified {@code controlType} for this
	 *         {@link Controllable}
	 */
	//<T extends Control> T getControl(Class<T> controlType);

}
