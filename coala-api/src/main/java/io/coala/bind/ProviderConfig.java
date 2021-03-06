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

import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;

/**
 * {@link ProviderConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface ProviderConfig extends GlobalConfig
{

	String INITABLE_KEY = "init";

	@Key( INITABLE_KEY )
	@DefaultValue( "false" )
	boolean initable();

	String MUTABLE_KEY = "mutable";

	@Key( MUTABLE_KEY )
	@DefaultValue( "false" )
	boolean mutable();

	String SINGLETON_KEY = "singleton";

	@Key( SINGLETON_KEY )
	@DefaultValue( "false" )
	boolean singleton();

	String IMPLEMENTATION_KEY = "impl";

	@Key( IMPLEMENTATION_KEY )
	Class<?> implementation();

	String CONFIG_KEY = "config";

//	@Key( PARAMETERS_KEY )
//	@ConverterClass( JsonConverter.class )
	default JsonNode config()
	{
		final Pattern pattern = Pattern.compile(
				"^" + Pattern.quote( CONFIG_KEY + KEY_SEP ) + "(?<sub>.*)" );
//		System.err.println( pattern + " -> " + ConfigUtil.export( this )
//				+ " :: " + ConfigUtil.export( this, pattern, "${sub}" ) );
		return ConfigUtil
				.expand( ConfigUtil.export( this, pattern, "${sub}" ) );
	}

	String BINDINGS_KEY = "bindings";

	default Map<String, BindingConfig>
		bindingConfigs( final Map<?, ?>... imports )
	{
		return subConfigs( BINDINGS_KEY, BindingConfig.class, imports );
	}
}