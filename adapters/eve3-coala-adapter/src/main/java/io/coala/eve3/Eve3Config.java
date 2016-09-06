/* $Id: b3752a83c21eae03a6823438121a4c70ae5a140e $
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Mutable;
import org.apache.logging.log4j.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentBuilder;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.protocol.Protocol;
import com.almende.eve.scheduling.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.transport.Transport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.coala.bind.LocalConfig;
import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;
import io.coala.config.YamlUtil;
import io.coala.log.LogUtil;
import io.coala.util.FileUtil;

/**
 * {@link Eve3Config}
 * 
 * @version $Id: b3752a83c21eae03a6823438121a4c70ae5a140e $
 * @author Rick van Krevelen
 */
public interface Eve3Config extends GlobalConfig, Mutable
{

	String CONFIG_PATH_KEY = "eve.config-path";

	String CONFIG_PATH_DEFAULT = "eve.yaml";

	String AGENTS_KEY = "agents";

	String TEMPLATES_KEY = "templates";

	@Key( CONFIG_PATH_KEY )
	@DefaultValue( CONFIG_PATH_DEFAULT )
	String yamlConfigPath();

	default Map<String, Eve3AgentConfig>
		agentConfigs( final Map<?, ?>... imports )
	{
		return subConfigs( AGENTS_KEY, Eve3AgentConfig.class, imports );
	}

	default Map<String, Eve3AgentConfig>
		templateConfigs( final Map<?, ?>... imports )
	{
		return subConfigs( TEMPLATES_KEY, Eve3AgentConfig.class, imports );
	}

	default Eve3AgentConfig forAgent( final String id,
		final Map<?, ?>... imports )
	{
		final Eve3DefaultAgentConfig defaults = ConfigFactory.create(
				Eve3DefaultAgentConfig.class,
				ConfigUtil.join(
						Collections.singletonMap( LocalConfig.ID_KEY, id ),
						imports ) );
		// try agent with given id
		for( Map.Entry<String, Eve3AgentConfig> entry : agentConfigs( imports )
				.entrySet() )
		{
			final ObjectNode agentConfig = (ObjectNode) entry.getValue()
					.toJSON();
			// match agent id
			if( !id.equals(
					agentConfig.get( Eve3AgentConfig.ID_KEY ).asText() ) )
				continue;
			// expand 'extends' from templates
			if( agentConfig.has( Eve3AgentConfig.EXTENDS_KEY ) )
			{
				final String tplName = agentConfig
						.get( Eve3AgentConfig.EXTENDS_KEY ).asText();
				final Eve3AgentConfig tpl = templateConfigs( imports )
						.get( tplName );
				if( tpl == null )
					getLogger().warn( "Missing template '{}' for agent '{}'",
							tplName, id );
				copyMissing( tpl.toJSON(), agentConfig );
			}
			// add missing defaults
			copyMissing( defaults.toJSON(), agentConfig );
			return ConfigFactory.create( Eve3AgentConfig.class,
					ConfigUtil.flatten( agentConfig ) );
		}
		// no agent config, try template with given id
		final Eve3AgentConfig tpl = templateConfigs( imports ).get( id );
		if( tpl != null )
		{
			getLogger().trace( "Using template for agent '{}'", id, id );
			final ObjectNode agentConfig = (ObjectNode) tpl.toJSON();
			copyMissing( defaults.toJSON(), agentConfig );
			return ConfigFactory.create( Eve3AgentConfig.class,
					ConfigUtil.flatten( agentConfig ) );
		}
		// no agent or template of given id, return defaults
		getLogger().trace( "Using defaults for agent '{}'", id );
		return defaults;
	}

	/**
	 * @param imports
	 * @return this {@link Eve3Config} initialized from the (default) path
	 *         specified with {@link #CONFIG_PATH_KEY} (if accessible)
	 */
	default Eve3Config load( final Map<?, ?>... imports )
	{
		return load( yamlConfigPath(), imports );
	}

	/**
	 * @param path
	 * @param imports
	 * @return this {@link Eve3Config} initialized from the specified path (if
	 *         accessible)
	 */
	default Eve3Config load( final String path, final Map<?, ?>... imports )
	{
		try( final InputStream is = FileUtil.toInputStream( path ) )
		{
			if( is != null )
				YamlUtil.flattenYaml( is ).forEach( ( key, value ) ->
				{
					setProperty( key.toString(), value.toString() );
				} );
		} catch( final IOException e )
		{
			getLogger().info( e.getMessage() );
		}
		return this;
	}

	static Logger getLogger()
	{
		return LogUtil.getLogger( Eve3Config.class );
	}

	static void copyMissing( final JsonNode source, final ObjectNode target )
	{
		final Iterator<Entry<String, JsonNode>> fields = source.fields();
		while( fields.hasNext() )
		{
			final Entry<String, JsonNode> field = fields.next();
			if( !target.has( field.getKey() ) )
				target.set( field.getKey(), field.getValue() );
		}
	}

	/**
	 * {@link Eve3AgentConfig}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Eve3AgentConfig extends LocalConfig
	{
		String EXTENDS_KEY = "extends";

		@Key( ID_KEY )
		// removes @DefaultValue
		String rawId();

		default AgentConfig builderConfig()
		{
			return new AgentConfig( (ObjectNode) toJSON() );
		}

		default Eve3Container buildAgent()
		{
			final AgentConfig config = builderConfig();
			final Eve3Container result = (Eve3Container) new AgentBuilder()
					.with( config ).build();
			getLogger().trace( "Built agent, urls: {}", result.getUrls() );
			return result;
		}

	}

	/**
	 * {@link Eve3DefaultAgentConfig}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Eve3DefaultAgentConfig extends Eve3AgentConfig
	{
		String CLASS_KEY = "class";
		String STATE_BUILDER_KEY = "state.class";
		String STATE_PATH_KEY = "state.path";
		String SCHEDULER_BUILDER_KEY = "scheduler.class";
		String HTTP_TRANSPORT_BUILDER_KEY = "transports.0.class";
		String HTTP_TRANSPORT_SERVLET_URL_KEY = "transports.0.servletUrl";
		String HTTP_TRANSPORT_AUTHENTICATE_KEY = "transports.0.doAuthentication";
		String JSONRPC_PROTOCOL_BUILDER_KEY = "protocols.0.class";
		String JSONRPC_PROTOCOL_TIMEOUT_SECS_KEY = "protocols.0.rpcTimeout";

		@Key( EXTENDS_KEY )
		String extend();

		@Key( CLASS_KEY )
		@DefaultValue( "io.coala.eve3.Eve3Container$Simple" )
		Class<? extends Agent> agentClass();

		@Key( SCHEDULER_BUILDER_KEY )
		@DefaultValue( "com.almende.eve.scheduling.SimpleSchedulerBuilder" )
		Class<? extends Scheduler> schedulerBuilder();

		@Key( STATE_BUILDER_KEY )
		@DefaultValue( "com.almende.eve.state.memory.MemoryStateBuilder" )
		Class<? extends State> stateBuilder();

		@Key( STATE_PATH_KEY )
		@DefaultValue( ".eve_agents" )
		Class<? extends State> statePath();

		@Key( HTTP_TRANSPORT_BUILDER_KEY )
		@DefaultValue( "com.almende.eve.transport.http.HttpTransportBuilder" )
		Class<? extends Transport> httpBuilder();

		@Key( HTTP_TRANSPORT_SERVLET_URL_KEY )
		@DefaultValue( "http://127.0.0.1:8080/agents/" )
		String transportServletUrl();

		@Key( HTTP_TRANSPORT_AUTHENTICATE_KEY )
		@DefaultValue( "" + false )
		boolean transportAuthenticate();

		@Key( JSONRPC_PROTOCOL_BUILDER_KEY )
		@DefaultValue( "com.almende.eve.protocol.jsonrpc.JSONRpcProtocolBuilder" )
		Class<? extends Protocol> jsonrpcBuilder();

		@Key( JSONRPC_PROTOCOL_TIMEOUT_SECS_KEY )
		@DefaultValue( "" + Integer.MAX_VALUE ) // Invoker proxy handles timeout
		int jsonrpcTimeout();
	}
}
