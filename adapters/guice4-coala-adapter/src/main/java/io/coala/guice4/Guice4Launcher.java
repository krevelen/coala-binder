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
package io.coala.guice4;

import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.coala.bind.Launcher;

/**
 * {@link Guice4Launcher}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Guice4Launcher implements Launcher
{
	/** */
	private static final Logger LOG = LogManager
			.getLogger( Guice4Launcher.class );

	/**
	 * @param config
	 * @return a new {@link Guice4Launcher}
	 */
	public static Guice4Launcher of( final LaunchConfig config )
	{
		final Guice4Launcher result = new Guice4Launcher();
		result.config = config;
		final Collection<String> ids = config.launchIds();
		if( ids.isEmpty() )
			LOG.trace( "Nothing to launch in config: {}", config );
		else
			for( String id : ids )
				result.launch( id );
		return result;
	}

	private LaunchConfig config;

	@Override
	public void launch( final String id )
	{
		Guice4LocalBinder.of( this.config.localConfig( id ),
				Collections.singletonMap( Launcher.class, this ) );
		LOG.trace( "Launched {}", id );
	}

//		@Override
//		public String toString()
//		{
//			return getClass().getSimpleName() + '<' + id() + '>';
//		}
}