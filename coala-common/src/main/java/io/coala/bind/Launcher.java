/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.bind;

import java.util.Collection;
import java.util.Map;

import org.aeonbits.owner.ConfigCache;

import io.coala.bind.LocalBinder.BinderConfig;
import io.coala.bind.LocalContextual.LocalConfig;
import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;

/**
 * {@link Launcher}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Launcher
{
	/**
	 * @param id the identifier of the {@link LocalBinder} to launch
	 */
	void launch( String id );

	/**
	 * {@link BinderConfig}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface LaunchConfig extends GlobalConfig
	{

		String LAUNCH_KEY = "launch";

//		@Key( LAUNCH_KEY )
//		@DefaultValue( "true" )
//		boolean launch();

		default Collection<String> launchIds()
		{
			return ConfigUtil.enumerate( this, null, KEY_SEP + LAUNCH_KEY );
		};

		/**
		 * @param id
		 * @param imports
		 * @return the (cached) {@link LocalConfigs} instance
		 * @see ConfigCache#getOrCreate(Object, Class, Map[])
		 */
		default LocalConfig localConfig( final String id,
			final Map<?, ?>... imports )
		{
			return subConfig( id, LocalConfig.class, imports );
		}
	}
}