/* $Id: e4b0c8b2f193027454030f4c994a666b6713110c $
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.agent.AgentStatus;
import io.coala.agent.AgentStatusUpdate;
import io.coala.agent.BasicAgentStatus;
import io.coala.bind.BinderFactory;
import io.coala.bind.BinderFactoryConfig;
import io.coala.exception.CoalaExceptionFactory;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link GuiceBinderFactory}
 * 
 * @version $Id: e4b0c8b2f193027454030f4c994a666b6713110c $
 * @author Rick van Krevelen
 */
public class GuiceBinderFactory implements BinderFactory
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( GuiceBinderFactory.class );

	/** */
	private final Map<AgentID, GuiceBinder> binderCache = Collections
			.synchronizedMap( new HashMap<AgentID, GuiceBinder>() );

	/** */
	private BinderFactoryConfig config = null;

	/** */
	private Subject<AgentStatusUpdate, AgentStatusUpdate> statusUpdates = PublishSubject
			.create();

	/**
	 * {@link GuiceBinderFactory} zero-argument constructor for instantiation by
	 * reflection
	 */
	public GuiceBinderFactory()
	{
		// empty
	}

	/**
	 * @see BinderFactory#initialize(BinderFactoryConfig, Observable)
	 */
	@Override
	public BinderFactory initialize( final BinderFactoryConfig config,
		final Observable<AgentStatusUpdate> ownerStatus )
	{
		this.config = config;
		// System.err.println("Initialized binder factory for model: "
		// + config.getModelID());
		ownerStatus.subscribe( this.statusUpdates );
		return this;
	}

	/**
	 * @see BinderFactory#getConfig()
	 */
	@Override
	public BinderFactoryConfig getConfig()
	{
		return this.config;
	}

	/**
	 * @see BinderFactory#create(AgentID)
	 */
	@Override
	public synchronized GuiceBinder create( final String agentName )

	{
		return create( getConfig().getReplicationConfig().newID()
				.createAgentID( agentName ) );
	}

	/**
	 * @see BinderFactory#create(AgentID)
	 */
	@Override
	public synchronized GuiceBinder create( final String agentName,
		final Class<? extends Agent> agentType )
	{
		return create( getConfig().getReplicationConfig().newID()
				.createAgentID( agentName ), agentType );
	}

	@Override
	public synchronized GuiceBinder create( final AgentID agentID )
	{
		return create( agentID, null );
	}

	@Override
	public synchronized GuiceBinder create( final AgentID agentID,
		final Class<? extends Agent> agentType ) // throws CoalaException
	{
		final GuiceBinder cached = this.binderCache.get( agentID );
		if( cached != null )
		{
			if( !agentID.isOrphan() )
				LOG.warn( "UNEXPECTED: re-using binder for: " + agentID );
			return cached;
		}
		if( getConfig() == null ) { throw CoalaExceptionFactory.VALUE_NOT_CONFIGURED
				.createRuntime( "config",
						"use BinderFactory#initialize(BinderFactoryConfig)" ); }

		final AgentStatusUpdate defaultValue = new AgentStatusUpdate()
		{

			@Override
			public AgentID getAgentID()
			{
				return agentID;
			}

			@Override
			public AgentStatus<?> getStatus()
			{
				return BasicAgentStatus.CREATED;
			}

			@Override
			public String toString()
			{
				return JsonUtil.toString( this );
			}
		};

		final Subject<AgentStatusUpdate, AgentStatusUpdate> behaviorSubject = BehaviorSubject
				.create( defaultValue );

		this.statusUpdates.filter( new Func1<AgentStatusUpdate, Boolean>()
		{
			@Override
			public Boolean call( final AgentStatusUpdate update )
			{
				return update.getAgentID().equals( agentID );
			}
		} ).subscribe( behaviorSubject );

		final GuiceBinder result = new GuiceBinder( getConfig(), agentID,
				agentType, behaviorSubject.asObservable() );

		this.binderCache.put( agentID, result );
		LOG.trace( "Cached new binder for agent: " + agentID );

		return result;
	}

	/**
	 * @see BinderFactory#remove(AgentID)
	 */
	@Override
	public GuiceBinder remove( final AgentID agentID )
	{
		return this.binderCache.remove( agentID );
	}

}
