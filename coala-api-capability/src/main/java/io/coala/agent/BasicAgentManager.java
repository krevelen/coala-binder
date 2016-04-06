/* $Id: 3de53a3f9a729f49ee88b0cf3f69e2bcc20eeae7 $
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
package io.coala.agent;

import io.coala.bind.Binder;
import io.coala.bind.BinderFactory;
import io.coala.exception.ExceptionFactory;

/**
 * {@link BasicAgentManager}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class BasicAgentManager extends AbstractAgentManager
{

	/** */
	private static BasicAgentManager INSTANCE;

	/**
	 * @return the singleton {@link BasicAgentManager}
	 */
	public synchronized static BasicAgentManager getInstance()
	{
		if( INSTANCE == null ) return getInstance( (String) null );

		return INSTANCE;
	}

	/**
	 * @param configPath or {@code null} for default config
	 * @return the singleton {@link BasicAgentManager}
	 */
	public synchronized static BasicAgentManager
		getInstance( final String configPath )
	{
		if( INSTANCE == null ) try
		{
			INSTANCE = getInstance(
					BinderFactory.Builder.fromFile( configPath ) );
		} catch( final Exception e )
		{
			throw ExceptionFactory.createUnchecked( e,
					"Problem reading config from {}", configPath );
		}

		return INSTANCE;
	}

	/**
	 * @param binder
	 * @return
	 */
	public synchronized static BasicAgentManager
		getInstance( final Binder binder )
	{
		if( INSTANCE == null )
		{
			INSTANCE = new BasicAgentManager( binder );

			INSTANCE.bootAgents();
		}

		return INSTANCE;
	}

	/**
	 * @param binderFactoryBuilder or {@code null} for default config
	 * @return the singleton {@link BasicAgentManager}
	 */
	public synchronized static BasicAgentManager
		getInstance( final BinderFactory.Builder binderFactoryBuilder )
	{
		if( INSTANCE == null )
		{
			INSTANCE = new BasicAgentManager( binderFactoryBuilder );

			INSTANCE.bootAgents();
		}

		return INSTANCE;
	}

	/**
	 * {@link BasicAgentManager} constructor
	 * 
	 * @param binderFactoryBuilder
	 */
	protected BasicAgentManager(
		final BinderFactory.Builder binderFactoryBuilder )
	{
		super( binderFactoryBuilder );
	}

	/**
	 * {@link BasicAgentManager} constructor
	 * 
	 * @param binder
	 */
	protected BasicAgentManager( final Binder binder )
	{
		super( binder );
	}

	/**
	 * @param agent
	 * @return the {@link EveWrapperAgent} for the created agent
	 */
	@Override
	protected AgentID boot( final Agent agent )
	{
		// LOG.warn("Oops !", new IllegalStateException("NOT IMPLEMENTED"));
		return agent.getID();
	}

	/** @see AbstractAgentManager#shutdown() */
	@Override
	protected void shutdown()
	{
		// FIXME destroy/cleanup proxy host somehow
	}

}
