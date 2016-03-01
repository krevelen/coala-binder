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

import static org.aeonbits.owner.util.Collections.entry;
import static org.aeonbits.owner.util.Collections.map;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

import org.aeonbits.owner.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.protocol.Protocol;
import com.almende.eve.protocol.jsonrpc.JSONRpcProtocolBuilder;
import com.almende.eve.scheduling.Scheduler;
import com.almende.eve.scheduling.SimpleSchedulerBuilder;
import com.almende.eve.state.State;
import com.almende.eve.state.memory.MemoryStateBuilder;
import com.almende.eve.transport.Transport;
import com.almende.eve.transport.http.HttpTransportBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.coala.capability.replicate.ReplicationConfig;
import io.coala.exception.CoalaException;
import io.coala.json.JsonUtil;
import io.coala.resource.FileUtil;

/**
 * {@link EveAgentConfig}
 * 
 * @version $Id: b3752a83c21eae03a6823438121a4c70ae5a140e $
 * @author Rick van Krevelen
 */
public interface EveAgentConfig extends ReplicationConfig
{

	/** */
	String AGENT_ID_KEY = "master.agent.id";

	/** */
	String AGENT_ID_DEFAULT = "my-agent";

	/** */
	String AGENT_CONFIG_FILE_KEY = "agent.config-uri";

	/** */
	String AGENT_CONFIG_FILE_DEFAULT = "eve-wrapper.yaml";

	/** */
	String AGENT_CLASS_KEY = "master.agent.class";

	/** */
	Class<?> AGENT_CLASS_DEFAULT = EveWrapperAgent.class;

	/** */
	String STATE_BUILDER_KEY = "master.state.builder";

	/** */
	Class<?> STATE_BUILDER_DEFAULT = MemoryStateBuilder.class;

	/** */
	String SCHEDULER_BUILDER_KEY = "master.scheduler.builder";

	/** */
	Class<?> SCHEDULER_BUILDER_DEFAULT = SimpleSchedulerBuilder.class;

	/** */
	String HTTP_TRANSPORT_BUILDER_KEY = "master.transport.http.builder";

	/** */
	Class<?> HTTP_TRANSPORT_BUILDER_DEFAULT = HttpTransportBuilder.class;

	/** */
	String HTTP_TRANSPORT_SERVLET_URL_KEY = "master.transport.http.servletUrl";

	/** */
	String HTTP_TRANSPORT_SERVLET_URL_DEFAULT = "http://127.0.0.1:8080/agents/";

	/** */
	String HTTP_TRANSPORT_AUTHENTICATE_KEY = "master.transport.http.doAuthentication";

	/** */
	boolean HTTP_TRANSPORT_AUTHENTICATE_DEFAULT = false;

	/** */
	String STATE_CONFIG_KEY = "master.state";

	/** */
	String SCHEDULER_CONFIG_KEY = "master.scheduler";

	/** */
	String HTTP_TRANSPORT_CONFIG_KEY = "master.transport.http";

	/** */
	String JSONRPC_PROTOCOL_BUILDER_KEY = "master.protocol.jsonrpc.builder";

	/** */
	Class<?> JSONRPC_BUILDER_DEFAULT = JSONRpcProtocolBuilder.class;

	/** */
	String JSONRPC_PROTOCOL_CONFIG_KEY = "master.protocol.jsonrpc";

	/** */
	String TRANSPORTS_CONFIG_KEY = "master.transports";

	/**
	 * Maps default values that are not String constants, e.g. class names
	 */
	@SuppressWarnings( "unchecked" )
	Map<String, String> DEFAULT_VALUES = map(
			entry( AGENT_CLASS_KEY, AGENT_CLASS_DEFAULT.getName() ),
			entry( STATE_BUILDER_KEY, STATE_BUILDER_DEFAULT.getName() ),
			entry( SCHEDULER_BUILDER_KEY, SCHEDULER_BUILDER_DEFAULT.getName() ),
			entry( HTTP_TRANSPORT_BUILDER_KEY,
					HTTP_TRANSPORT_BUILDER_DEFAULT.getName() ) );

	@Key( AGENT_ID_KEY )
	@DefaultValue( AGENT_ID_DEFAULT )
		String agentName();

	@Key( AGENT_CONFIG_FILE_KEY )
	@DefaultValue( AGENT_CONFIG_FILE_DEFAULT )
		String agentConfigUri();

	@DefaultValue( "${" + AGENT_CONFIG_FILE_KEY + "}" )
	@ConverterClass( InputStreamConverter.class )
		InputStream agentConfigStream();

	@Key( AGENT_CLASS_KEY )
		Class<? extends Agent> agentClass();

	@Key( SCHEDULER_BUILDER_KEY )
		Class<? extends Scheduler> schedulerBuilder();

	@Key( SCHEDULER_CONFIG_KEY )
	@DefaultValue( "{\"class\":\"${" + SCHEDULER_BUILDER_KEY + "}\"}" )
		JsonNode schedulerConfig();

	@Key( STATE_BUILDER_KEY )
		Class<? extends State> stateBuilder();

	@Key( STATE_CONFIG_KEY )
	@DefaultValue( "{\"class\":\"${" + STATE_BUILDER_KEY + "}\"}" )
		JsonNode stateConfig();

	@Key( HTTP_TRANSPORT_BUILDER_KEY )
		Class<? extends Transport> httpBuilder();

	@Key( JSONRPC_PROTOCOL_BUILDER_KEY )
		Class<? extends Protocol> jsonrpcBuilder();

	@Key( HTTP_TRANSPORT_SERVLET_URL_KEY )
	@DefaultValue( HTTP_TRANSPORT_SERVLET_URL_DEFAULT )
		String transportServletUrl();

	@Key( HTTP_TRANSPORT_AUTHENTICATE_KEY )
	@DefaultValue( "" + HTTP_TRANSPORT_AUTHENTICATE_DEFAULT )
		boolean transportAuthenticate();

	@Key( HTTP_TRANSPORT_CONFIG_KEY )
	@DefaultValue( "{\"class\":\"${" + HTTP_TRANSPORT_BUILDER_KEY
			+ "}\",\"servletUrl\":\"${" + HTTP_TRANSPORT_SERVLET_URL_KEY
			+ "}\",\"doAuthentication\":${" + HTTP_TRANSPORT_AUTHENTICATE_KEY
			+ "},\"doShortcut\":true,"
			+ "\"servletLauncher\":\"JettyLauncher\","
			// \"initParams\":[{\"key\":\"servletUrl\",\"value\":\"${" +
			// TRANSPORT_SERVLET_URL_KEY + "}\"}],"
			+ "\"servletClass\":\"com.almende.eve.transport.http.DebugServlet\"}" )
	@ConverterClass( JsonNodeConverter.class )
		JsonNode httpTransportConfig();

	@Key( JSONRPC_PROTOCOL_CONFIG_KEY )
	@DefaultValue( "{\"class\":\"${" + JSONRPC_PROTOCOL_BUILDER_KEY
			+ "}\",rpcTimeout:1}" )
	@ConverterClass( JsonNodeConverter.class )
		JsonNode jsonrpcProtocolConfig();

	// protocols:
	// - class: com.almende.eve.protocol.jsonrpc.JSONRpcProtocolBuilder
	// rpcTimeout: 1

	@ConverterClass( AgentConfigConverter.class )
	@DefaultValue( "{\"id\":\"${" + AGENT_ID_KEY + "}\",\"class\":\"${"
			+ AGENT_CLASS_KEY + "}\",\"state\":${" + STATE_CONFIG_KEY
			+ "},\"scheduler\":${" + SCHEDULER_CONFIG_KEY
			+ "},\"transports\":[${" + HTTP_TRANSPORT_CONFIG_KEY
			+ "}],\"protocols\":[${" + HTTP_TRANSPORT_CONFIG_KEY + "}]}" )
		AgentConfig agentConfig();

	/**
	 * {@link JsonNodeConverter}
	 *
	 * @date $Date$
	 * @version $Id: b3752a83c21eae03a6823438121a4c70ae5a140e $
	 * @author <a href="mailto:rick@almende.org">Rick</a>
	 */
	public class JsonNodeConverter implements Converter<JsonNode>
	{
		@Override
		public JsonNode convert( final Method method, final String input )
		{
			return JsonUtil.toTree( input );
		}
	}

	/**
	 * {@link AgentConfigConverter}
	 *
	 * @date $Date$
	 * @version $Id: b3752a83c21eae03a6823438121a4c70ae5a140e $
	 * @author <a href="mailto:rick@almende.org">Rick</a>
	 */
	public class AgentConfigConverter implements Converter<AgentConfig>
	{
		@Override
		public AgentConfig convert( final Method method, final String input )
		{
			return new AgentConfig( (ObjectNode) JsonUtil.toTree( input ) );
		}
	}

	/**
	 * {@link AgentConfigConverter}
	 * 
	 * @date $Date$
	 * @version $Id: b3752a83c21eae03a6823438121a4c70ae5a140e $
	 * @author <a href="mailto:rick@almende.org">Rick</a>
	 */
	public class InputStreamConverter implements Converter<InputStream>
	{
		/** */
		private static final Logger LOG = LogManager
				.getLogger( EveAgentConfig.InputStreamConverter.class );

		@Override
		public InputStream convert( final Method method, final String input )
		{
			try
			{
				return FileUtil.getFileAsInputStream( input );
			} catch( final CoalaException e )
			{
				LOG.warn( e.getMessage() );
				return null;
			}
		}
	}

}
