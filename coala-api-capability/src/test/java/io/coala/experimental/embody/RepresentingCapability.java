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
package io.coala.experimental.embody;

import io.coala.agent.Agent;
import io.coala.capability.CapabilityFactory;
import io.coala.capability.embody.PerceivingCapability;

/**
 * {@link RepresentingCapability} embodies an {@link Agent}s within some
 * environment in order to connect sensors (via publish-subscribe pattern) and
 * effectors
 */
public interface RepresentingCapability extends PerceivingCapability
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<RepresentingCapability>
	{

	}

	/**
	 * @return
	 */
	Object getEntity();
}
