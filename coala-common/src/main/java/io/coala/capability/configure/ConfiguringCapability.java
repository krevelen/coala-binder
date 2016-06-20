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
package io.coala.capability.configure;

import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import io.coala.config.CoalaPropertyGetter;
import io.coala.config.PropertyGetter;

/**
 * {@link ConfiguringCapability} links agents for lookup or directory purposes
 * 
 * @param <THIS> the (sub)type of {@link ConfiguringCapability} to build
 */
@Deprecated
public interface ConfiguringCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<ConfiguringCapability>
	{
		// empty
	}

	/**
	 * @param key the property's key
	 * @param key the property key prefixes
	 * @return the specified property's {@link CoalaPropertyGetter}
	 */
	PropertyGetter getProperty( String key, String... prefixes );

}
