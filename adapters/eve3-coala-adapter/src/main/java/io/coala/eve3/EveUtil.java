/* $Id: 13876ecf6459e1a3cfaa95f5a401c1b6bf9a392a $
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentBuilder;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.capabilities.Config;
import com.almende.eve.config.YamlReader;
import com.almende.eve.deploy.Boot;
import com.almende.eve.instantiation.InstantiationServiceConfig;
import com.almende.eve.state.file.FileStateConfig;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.coala.agent.AgentID;
import io.coala.bind.Binder;
import io.coala.capability.interact.ReceivingCapability;
import io.coala.exception.CoalaException;
import io.coala.exception.CoalaExceptionFactory;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.message.Message;
import io.coala.message.MessageHandler;
import io.coala.util.Util;
import io.coala.web.WebUtil;

/**
 * {@link EveUtil}
 * 
 * @version $Id: 13876ecf6459e1a3cfaa95f5a401c1b6bf9a392a $
 * @author Rick van Krevelen
 */
public class EveUtil implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( EveUtil.class );

	/**
	 * {@link EveUtil} singleton constructor
	 */
	private EveUtil()
	{
		// empty
	}

	/** */
	private static final Map<String, AgentID> AGENT_ID_CACHE = new HashMap<>();

	/** */
	private static final Map<String, EveWrapperAgent> WRAPPER_AGENT_CACHE = new HashMap<>();

	/**
	 * @param id
	 * @return
	 */
	protected static AgentID toAgentID( final String id )
	{
		// FIXME create/employ global lookup service/agent

		final AgentID result = AGENT_ID_CACHE.get( id );
		if( id == null || result == null ) throw new NullPointerException(
				"Unknown wrapper agentID for Eve agent Id: " + id );

		return AGENT_ID_CACHE.get( id );
	}

	/**
	 * @param agentID
	 * @return
	 */
	public static String toEveAgentId( final AgentID agentID )
	{
		// be robust against spaces, weird characters, etc.
		return WebUtil.urlEncode( agentID.toString() );
	}

	/**
	 * @param agentID
	 * @return
	 */
	public static URI getAddress( final AgentID agentID )
	{
		// FIXME create/employ global lookup service/agent
		return URI.create( "local:" + toEveAgentId( agentID ) );
	}

	/**
	 * @deprecated should use proxy somehow
	 * @param msg
	 * @throws Exception
	 */
	@Deprecated
	protected static <M extends Message<?>> void
		receiveMessageByPointer( final M msg )
	{
		final io.coala.agent.Agent ag = EveAgentManager.getInstance()
				.getAgent( msg.getReceiverID(), false );
		final Binder binder;
		if( ag == null )
		{
			// allow delivery for orphan-binders (created programmatically)
			if( msg.getReceiverID().isOrphan() )
				binder = EveAgentManager.getInstance()
						.getBinder( msg.getReceiverID() );
			else
				throw CoalaExceptionFactory.AGENT_UNAVAILABLE
						.createRuntime( msg.getReceiverID() );
		} else
			binder = ag.getBinder();

		((MessageHandler) binder.inject( ReceivingCapability.class ))
				.onMessage( msg );
	}

	/**
	 * @param msg
	 * @throws Exception
	 */
	// protected static <M extends Message<?>> void receiveMessageByProxy(
	// final M msg)
	// {
	// try
	// {
	// // FIXME use when agent proxy+delete works:
	// // getEveHost().getAgent(toEveAgentId(msg.getSenderID())),
	// getEveHost().createAgentProxy(getAddress(msg.getReceiverID()),
	// EveReceiverAgent.class).doReceive(msg);
	// } catch (final Throwable t)
	// {
	// throw CoalaExceptionFactory.AGENT_UNAVAILABLE.createRuntime(msg
	// .getReceiverID());
	// }
	// }

	/**
	 * @deprecated please use {@link #receiveMessageByPointer(Message)}
	 * @param msg
	 * @throws Exception
	 */
	@Deprecated
	protected static <M extends Message<?>> void
		sendMessageByPointer( final M msg )
	{
		try
		{
			final URI receiverURI = getAddress( msg.getReceiverID() );
			final JsonNode payload = JsonUtil.toTree( msg );
			getWrapperAgent( msg.getSenderID(), true ).doSend( payload,
					receiverURI );
		} catch( final Throwable t )
		{
			throw CoalaExceptionFactory.AGENT_UNAVAILABLE
					.createRuntime( msg.getSenderID() );
		}
	}

	/**
	 * @deprecated please use {@link #receiveMessageByPointer(Message)}
	 * @param msg
	 * @throws Exception
	 */
	// @Deprecated
	// protected static void sendMessageByProxy(final Message<?> msg)
	// {
	// try
	// {
	// getEveHost().createAgentProxy(null,
	// // getEveHost().getAgent(toEveAgentId(msg.getSenderID())),
	// getAddress(msg.getSenderID()), EveSenderAgent.class)
	// .doSend(msg);
	// } catch (final Throwable t)
	// {
	// throw CoalaExceptionFactory.AGENT_UNAVAILABLE.createRuntime(msg
	// .getReceiverID());
	// }
	// }

	/**
	 * @param id
	 * @return
	 * @throws CoalaException
	 */
	public static List<URI> getAddresses( final AgentID id )
		throws CoalaException
	{
		try
		{
			final Agent agent = getWrapperAgent( id, false );
			if( agent != null ) return agent.getUrls();
		} catch( final Exception e )
		{
			throw CoalaExceptionFactory.AGENT_UNAVAILABLE.create( id );
		}
		return Collections.emptyList();
	}

	/**
	 * @param eveAgentID
	 * @return
	 * @throws CoalaException
	 */
	protected static boolean hasWrapperAgent( final String eveAgentID )
		throws CoalaException
	{
		synchronized( WRAPPER_AGENT_CACHE )
		{
			return WRAPPER_AGENT_CACHE.containsKey( eveAgentID );
		}
	}

	/**
	 * @param eveAgentID
	 */
	protected static EveWrapperAgent createWrapperAgent( final AgentID agentID )
	{
		final EveWrapperAgent result;
		synchronized( WRAPPER_AGENT_CACHE )
		{
			final String eveAgentID = toEveAgentId( agentID );
			if( WRAPPER_AGENT_CACHE.containsKey( eveAgentID ) )
			{
				LOG.warn( "Duplicate wrapper for " + agentID + " > "
						+ eveAgentID );
				return WRAPPER_AGENT_CACHE.get( eveAgentID );
			}
			result = valueOf( agentID, EveWrapperAgent.class );
			WRAPPER_AGENT_CACHE.put( eveAgentID, result );
		}
		result.onBoot();
		return result;
	}

	/**
	 * @param eveAgentID
	 */
	protected static EveWrapperAgent getWrapperAgent( final AgentID agentID,
		final boolean createIfNone )
	{
		synchronized( WRAPPER_AGENT_CACHE )
		{
			final String eveAgentID = toEveAgentId( agentID );
			final EveWrapperAgent result = WRAPPER_AGENT_CACHE
					.get( eveAgentID );
			if( result != null ) return result;
		}
		if( !createIfNone ) return null;
		return createWrapperAgent( agentID );
	}

	private static void boot()
	{
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

	/** */
	@SuppressWarnings( "unchecked" )
	@SafeVarargs
	public static final <T extends Agent> T valueOf(
		final AgentConfig agentConfig, final Class<T> agentType,
		final Map.Entry<String, ? extends JsonNode>... parameters )
	{

		boot();

		// checkRegistered(agentType);

		if( parameters != null && parameters.length != 0 )
			for( Map.Entry<String, ? extends JsonNode> param : parameters )
			agentConfig.set( param.getKey(), param.getValue() );

		final T result = (T) new AgentBuilder().with( agentConfig ).build();
		LOG.trace( "Created " + agentType.getSimpleName() + " with config: "
				+ agentConfig );
		return result;
	}

	/** */
	@SafeVarargs
	public static final <T extends Agent> T valueOf( final AgentID agentId,
		final Class<T> agentType,
		final Map.Entry<String, ? extends JsonNode>... parameters )
	{
		final String eveId = toEveAgentId( agentId );
		AGENT_ID_CACHE.put( eveId, agentId );
		@SuppressWarnings( "serial" )
		final EveAgentConfig cfg = ConfigFactory.create( EveAgentConfig.class,
				EveAgentConfig.DEFAULT_VALUES, new HashMap<String, String>()
				{
					{
						put( EveAgentConfig.AGENT_CLASS_KEY,
								agentType.getName() );
						put( EveAgentConfig.AGENT_ID_KEY, eveId );
					}
				} );

		try( final InputStream is = cfg.agentConfigStream() )
		{
			final Config config = YamlReader.load( is );// .expand();
			final ArrayNode agentConfigs = (ArrayNode) config.get( "agents" );
			if( agentConfigs != null )
				for( final JsonNode agent : agentConfigs )
			{
				final JsonNode idNode = agent.get( "id" );
				if( idNode != null
						&& !idNode.asText().equals( eveId.toString() ) )
					continue;

				LOG.trace( "Creating agent " + eveId + " from config at "
						+ cfg.agentConfigUri() );
				return valueOf( new AgentConfig( (ObjectNode) agent ),
						agentType, parameters );
			}
		} catch( final IOException e )
		{
			LOG.warn( "Problem creating agent " + eveId + " from config at "
					+ cfg.agentConfigUri(), e );
		}
		LOG.trace( "No valid config for agent " + eveId + " found at: "
				+ cfg.agentConfigUri() + ". Using default config" );
		return valueOf( cfg.agentConfig(), agentType, parameters );
	}

}
