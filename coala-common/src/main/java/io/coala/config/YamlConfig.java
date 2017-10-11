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
package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.ConfigCache;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@link YamlConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface YamlConfig extends Accessible
{
	static <T extends org.aeonbits.owner.Config> T fromYAML(
		Class<T> configType, final File yamlPath, final Map<?, ?>... imports )
		throws IOException
	{
		final Map<?, ?>[] total = new Map<?, ?>[imports == null ? 1
				: imports.length + 1];
		total[0] = YamlUtil.flattenYaml( yamlPath );
		for( int i = 0; imports != null && i < imports.length; i++ )
			total[i + 1] = imports[i];
		return ConfigCache.getOrCreate( configType, total );
	}

	default Map<String, Object> export( final Map<?, ?>... maps )
	{
		return ConfigUtil.export( this, maps );
	}

	default Map<String, Object> export( final Pattern keyFilter )
	{
		return ConfigUtil.export( this, keyFilter );
	}

	default Map<String, Object> export( final Pattern keyFilter,
		final String keyReplacement )
	{
		return ConfigUtil.export( this, keyFilter, keyReplacement );
	}

	default JsonNode toJSON( final String... baseKeys )
	{
		return ConfigUtil.expand( ConfigUtil.export( this ), baseKeys );
	}

	default String toYAML()
	{
		return toYAML( null );
	}

	default String toYAML( final String comment, final String... baseKeys )
	{
		return YamlUtil.toYAML( comment, toJSON( baseKeys ) );
	}

	default void storeToYAML( final OutputStream out, final String comment )
		throws IOException
	{
		final Map<String, String> props = new HashMap<>();
		for( String key : propertyNames() )
			props.put( key, getProperty( key ) );
		YamlUtil.toYAML( comment, out, props );
	}
}
