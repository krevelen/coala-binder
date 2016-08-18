/* $Id: 543357c10325d0fda120f6997e43b31b2383c5e3 $
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
package io.coala.eve3;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.coala.agent.AbstractAgentManager;
import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.agent.AgentStatus;
import io.coala.bind.Binder;
import io.coala.bind.BinderFactory;
import io.coala.exception.Thrower;

/**
 * {@link EveAgentManager}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Deprecated
public class EveAgentManager extends AbstractAgentManager
{

	/** */
	// private static final Logger LOG =
	// LogUtil.getLogger(EveAgentManager.class);

	/** */
	private static EveAgentManager INSTANCE;

	/**
	 * @return the singleton {@link EveAgentManager}
	 */
	public synchronized static EveAgentManager getInstance()
	{
		if( INSTANCE == null ) return getInstance( (String) null );

		return INSTANCE;
	}

	/**
	 * @param configPath or {@code null} for default config
	 * @return the singleton {@link EveAgentManager}
	 */
	public synchronized static EveAgentManager
		getInstance( final String configPath )
	{
		if( INSTANCE == null ) try
		{
			INSTANCE = getInstance(
					BinderFactory.Builder.fromFile( configPath ) );
		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
		}
		return INSTANCE;
	}

	/**
	 * @param binder
	 * @return
	 */
	public synchronized static EveAgentManager
		getInstance( final Binder binder )
	{
		if( INSTANCE == null )
		{
			INSTANCE = new EveAgentManager( binder );

			INSTANCE.bootAgents();
		}

		return INSTANCE;
	}

	/**
	 * @param binderFactoryBuilder or {@code null} for default config
	 * @return the singleton {@link EveAgentManager}
	 */
	public synchronized static EveAgentManager
		getInstance( final BinderFactory.Builder binderFactoryBuilder )
	{
		if( INSTANCE == null )
		{
			INSTANCE = new EveAgentManager( binderFactoryBuilder );

			INSTANCE.bootAgents();
		}

		return INSTANCE;
	}

	// TODO store wrapper agent address(es) in (distributed) hash map

	/** */
	private final Map<AgentID, List<URI>> agentURLs = Collections
			.synchronizedMap( new HashMap<AgentID, List<URI>>() );

	/**
	 * {@link EveAgentManager} constructor
	 * 
	 * @param binderFactoryBuilder
	 */
	protected EveAgentManager(
		final BinderFactory.Builder binderFactoryBuilder )
	{
		super( binderFactoryBuilder );
	}

	/**
	 * {@link EveAgentManager} constructor
	 * 
	 * @param binder
	 */
	protected EveAgentManager( final Binder binder )
	{
		super( binder );
	}

	/* exposes the super method within this package */
	@Override
	protected void updateWrapperAgentStatus( final AgentID agentID,
		final AgentStatus<?> status )
	{
		super.updateWrapperAgentStatus( agentID, status );
	}

	/**
	 * @param agent
	 * @return the {@link EveWrapperAgent} for the created agent
	 * @throws Exception
	 */
	@Override
	protected AgentID boot( final Agent agent ) throws Exception
	{
		EveUtil.getWrapperAgent( agent.getID(), true );
		return agent.getID();
	}

	@Override
	protected boolean delete( final AgentID agentID )
	{
		return super.delete( agentID );
	}

	protected Binder getBinder( final AgentID agentID )
	{
		return super.getBinderFactory().create( agentID );
	}

	@Override
	protected void shutdown()
	{
		// FIXME destroy/cleanup eve host somehow
	}

	protected void setExposed( final AgentID agentID, final Object exposed )
		throws Exception
	{
		((EveExposingAgent) EveUtil.getWrapperAgent( agentID, true ))
				.setExposed( exposed );
	}

	/**
	 * Overrides the current addresses of an Eve wrapper agent
	 * 
	 * @param agentID
	 * @param eveURLs
	 */
	protected void setAddress( final String eveId, final List<URI> eveURLs )
	{
		this.agentURLs.put( EveUtil.toAgentID( eveId ), eveURLs );
	}

	/**
	 * @param agentID
	 * @return the Eve wrapper agent's current addresses
	 */
	protected List<URI> getAddress( final AgentID agentID )
	{
		return this.agentURLs.get( agentID );
	}

	/**
	 * @return
	 */
	protected Agent getAgent( final AgentID agentID, final boolean block )
	{
		return get( agentID, block );
	}
}
