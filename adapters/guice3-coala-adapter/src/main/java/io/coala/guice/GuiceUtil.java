/* $Id: 075c20f507c2e30717e7b41881794c424f05c691 $
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
package io.coala.guice;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.coala.agent.AgentID;
import io.coala.log.LogUtil;
import io.coala.util.Util;

/**
 * {@link GuiceUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class GuiceUtil implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( GuiceUtil.class );

	/** the singleton instance */
	private static final Map<AgentID, GuiceUtil> INSTANCES = Collections
			.synchronizedMap( new HashMap<AgentID, GuiceUtil>() );

	/** @return the singleton {@link GuiceUtil} instance */
	public synchronized static GuiceUtil getInstance( final AgentID clientID )
	{
		if( !INSTANCES.containsKey( clientID ) )
			LOG.error( "No injector set yet for host: " + clientID );

		return INSTANCES.get( clientID );
	}

	/** @return the singleton {@link GuiceUtil} instance */
	public synchronized static GuiceUtil getInstance( final AgentID clientID,
		final Module... modules )
	{
		return getInstance( clientID,
				modules == null ? null : Arrays.asList( modules ) );
	}

	/** @return the singleton {@link GuiceUtil} instance */
	public synchronized static GuiceUtil getInstance( final AgentID clientID,
		final Collection<Module> modules )
	{
		if( !INSTANCES.containsKey( clientID ) )
			INSTANCES.put( clientID, new GuiceUtil( clientID,
					Guice.createInjector( modules ) ) );
		else if( !modules.isEmpty() )
			LOG.warn( "Ignoring new modules, injector already configured" );

		return INSTANCES.get( clientID );
	}

	/** */
	private final AgentID clientID;

	/** */
	private final Injector injector;

	/** {@link GuiceUtil} constructor */
	private GuiceUtil( final AgentID clientID, final Injector injector )
	{
		this.clientID = clientID;
		this.injector = injector;
	}

	public AgentID getClientID()
	{
		return this.clientID;
	}

	public Injector getInjector()
	{
		return this.injector;
	}
}
