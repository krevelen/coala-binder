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
package io.coala.eve3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.aeonbits.owner.ConfigCache;

import com.almende.eve.deploy.Boot;
import com.almende.eve.instantiation.InstantiationServiceConfig;
import com.almende.eve.state.file.FileStateConfig;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.coala.bind.InjectConfig;
import io.coala.bind.LocalId;
import io.coala.json.JsonUtil;

/**
 * {@link Eve3Factory}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Singleton // FIXME make JVM static?
public class Eve3Factory
{
	// not static: lives in localbinder context as 'singleton'
	private final Map<LocalId, Eve3Container> eveContainers = new ConcurrentHashMap<>();

	@InjectConfig
	private Eve3Config localEveConfig;

	private Eve3Config eveConfig;

	protected Eve3Config getConfig( final Map<?, ?>... imports )
	{
		if( this.eveConfig == null )
		{
			this.eveConfig = this.localEveConfig == null
					? ConfigCache.getOrCreate( Eve3Config.class, imports )
					: this.localEveConfig;
			if( imports != null ) for( Map<?, ?> map : imports )
				if( map != null ) map.forEach( ( key, value ) ->
				{
					this.eveConfig.setProperty( key.toString(),
							value.toString() );
				} );
			this.eveConfig.load();
		}
		return this.eveConfig;
	}

	protected Eve3Container getAgent( final LocalId id,
		final Map<?, ?>... imports )
	{
		return this.eveContainers.computeIfAbsent( id, uri ->
		{
			return getConfig( imports ).forAgent( id, imports ).buildAgent();
		} );
	}

	protected Eve3Factory()
	{
		JsonUtil.initialize( JOM.getInstance() );

		// TODO prevent multiple boots?

		final InstantiationServiceConfig instantiationConfig = new InstantiationServiceConfig();
		final FileStateConfig state = new FileStateConfig();
		state.setPath( ".wakeservices" );
		state.setId( "testWakeService" );
		instantiationConfig.setState( state );
		final ObjectNode config = (ObjectNode) JOM.createObjectNode().set(
				"instantiationServices",
				JOM.createArrayNode().add( instantiationConfig ) );

		// Basic boot action:
		Boot.boot( config );
	}
}
