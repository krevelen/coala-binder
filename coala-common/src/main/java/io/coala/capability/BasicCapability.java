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
package io.coala.capability;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.bind.Binder;
import io.coala.capability.configure.ConfiguringCapability;
import io.coala.config.PropertyGetter;

/**
 * {@link BasicCapability}
 */
@Deprecated
public abstract class BasicCapability extends AbstractCapability<CapabilityID>
	implements Capability<BasicCapabilityStatus>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link BasicCapability} constructor
	 * 
	 * @param binder
	 */
	protected BasicCapability( final Binder binder )
	{
		this( null, binder );
		setID( new CapabilityID( binder.getID(), getClass() ) );
	}

	/**
	 * {@link BasicCapability} constructor
	 * 
	 * @param id
	 * @param binder
	 */
	protected BasicCapability( final CapabilityID id, final Binder binder )
	{
		super( id, binder );
	}

	/**
	 * helper method
	 * 
	 * @param key
	 * @return
	 */
	@JsonIgnore
	protected PropertyGetter getProperty( final String key )
	{
		return getBinder().inject( ConfiguringCapability.class )
				.getProperty( key );
	}

}
