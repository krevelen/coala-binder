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

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import io.coala.exception.Thrower;

/**
 * {@link JsonConfigurable} objects can be (re)configured via JSON trees
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@FunctionalInterface
public interface JsonConfigurable<THIS extends JsonConfigurable<?>>
{

	/** @return the (current) configuration */
	JsonNode config();

	default String stringify()
	{
		return config() == null ? getClass().getSimpleName() + "{}"
				: getClass().getSimpleName() + config();
	}

	default <T> Class<? extends T> fromConfig( final String key,
		final Class<T> returnType ) throws ClassNotFoundException
	{
		return fromConfig( key, returnType, null );
	}

	default <T> Class<? extends T> fromConfig( final String key,
		final Class<T> returnType, final Class<? extends T> defaultValue )
		throws ClassNotFoundException
	{
		if( config() == null ) return defaultValue;
		final JsonNode node = config().get( key );
		return node == null || node.isNull() ? defaultValue
				: Class.forName( node.asText() ).asSubclass( returnType );
	}

	default boolean fromConfig( final String key, final boolean defaultValue )
	{
		if( config() == null ) return defaultValue;
		final JsonNode node = config().get( key );
		return node.isNumber() ? node.asDouble() > 0
				: node.asBoolean( defaultValue );
	}

	default String fromConfig( final String key, final String defaultValue )
	{
		if( config() == null ) return defaultValue;
		final JsonNode node = config().get( key );
		return node == null || node.isNull() ? defaultValue : node.asText();
	}

	default String fromConfigNonEmpty( final String key )
	{
		final String result = Objects
				.requireNonNull( config().get( key ),
						"Missing '" + key + "' in config: " + config() )
				.asText();
		if( result.isEmpty() ) Thrower.throwNew( IllegalStateException::new,
				() -> "Empty: " + key );
		return result;
	}

	// TODO create all primitive property getters
}
