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
package io.coala.capability.interact;

import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link ExposingCapability}
 */
@Deprecated
public interface ExposingCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<ExposingCapability>
	{
		// empty
	}

	/** @return the {@link URI}s at which the exposed API can be reached */
	List<URI> getAddresses();

	/**
	 * expose some Java interface TODO add channel/transport identifier TODO
	 * return the URI of exposed/advertised implementation
	 */
	@JsonIgnore
	<T extends Serializable> void expose( Class<T> api, T implementation );

	@JsonIgnore
	<T extends Capability<?> & Serializable> void expose( Class<T> api );

}
