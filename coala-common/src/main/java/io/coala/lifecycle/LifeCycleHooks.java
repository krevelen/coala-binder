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
package io.coala.lifecycle;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link LifeCycleHooks} provides the default hooks of a managed
 * {@link LifeCycle} object
 * 
 * @version $Id$
 */
public interface LifeCycleHooks
{

	/**
	 * @return the type of {@link #activate()} phase management for this
	 *         {@link LifeCycle} object
	 */
	@JsonIgnore
	ActivationType getActivationType();

	/** Hook called by its container to setup the {@link LifeCycle} object */
	void initialize() throws Exception;

	/**
	 * Hook called by its container to start the {@link LifeCycle} object,
	 * depending on the {@link ActivationType} returned by
	 * {@link #getActivationType()}
	 */
	void activate() throws Exception;

	/**
	 * Hook called by its container to pause the {@link LifeCycle} object,
	 * depending on the {@link ActivationType} returned by
	 * {@link #getActivationType()}
	 */
	void deactivate() throws Exception;

	/** Hook called by its container to finalize the {@link LifeCycle} object */
	void finish() throws Exception;

}
