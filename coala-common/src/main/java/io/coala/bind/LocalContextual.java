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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Converter;
import org.aeonbits.owner.Mutable;

import com.eaio.uuid.UUID;

import io.coala.bind.LocalBinder.Config;
import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;
import io.coala.json.Contextual;
import io.coala.name.Identified;

/**
 * {@link LocalContextual}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface LocalContextual extends Contextual, Identified.Ordinal<String>
{

	static int hashCode( final LocalContextual self )
	{
		return self.id().hashCode();
	}

	static boolean equals( final LocalContextual self, final Object other )
	{
		return self.id().equals( other );
	}

	static String toString( final String id )
	{
		return '(' + id.toString() + ')';
	}

	static String toString( final LocalContextual self )
	{
		return self.getClass().getName() + toString( self.id() )
				+ self.context();
	}

	/**
	 * {@link LocalConfig}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 * @see ConfigFactory#create(Class, Map[])
	 * @see ConfigCache#getOrCreate(Class, Map[])
	 */
	interface LocalConfig extends GlobalConfig, Mutable
	{

		String ID_KEY = "id";

		String ID_DEFAULT = "";

		String ID_PREFIX = "${" + ID_KEY + "}";

		/**
		 * @param id the {@link #id()} value to set
		 * @param type the type of {@link LocalConfig} to create
		 * @param imports additional property key-value {@link Map}
		 * @return the (cached) {@link Config} instance
		 * @see ConfigCache#getOrCreate(Class, Map[])
		 */
		static <T extends LocalConfig> T create( final String id,
			final Class<T> type, final Map<?, ?>... imports )
		{
			return ConfigFactory.create( type, ConfigUtil.join(
					Collections.singletonMap( Config.ID_KEY, id ), imports ) );
		}

		/**
		 * @param id the {@link #id()} value to set
		 * @param type the type of {@link LocalConfig} to get/create
		 * @param imports additional property key-value {@link Map}
		 * @return the (cached) {@link Config} instance
		 * @see ConfigCache#getOrCreate(Class, Map[])
		 */
		static <T extends LocalConfig> T getOrCreate( final String id,
			final Class<T> type, final Map<?, ?>... imports )
		{
			return ConfigCache.getOrCreate( id, type, ConfigUtil.join(
					Collections.singletonMap( Config.ID_KEY, id ), imports ) );
		}

		/**
		 * @param base the key prefix key {@link String} to filter
		 * @param expansions transforms: <code>"${x}" &rarr; "y"</code>
		 * @return a {@link Collection} of unique {@link String} sub-keys
		 */
		default Collection<String> enumerate( final String base,
			final Map<String, String> expansions )
		{
			String prefix = base.replace( ID_PREFIX, id() ) + KEY_SEP;
			if( expansions != null )
				for( Map.Entry<String, String> entry : expansions.entrySet() )
				prefix = prefix.replace( "${" + entry.getKey() + "}", entry.getValue() );
			return ConfigUtil.enumerate( this, prefix, null );
		}

		@Key( ID_KEY )
		@DefaultValue( ID_DEFAULT )
		@ConverterClass( AnonymousConverter.class )
		String id();

		/** the (relative) {@link #EXTENDS_KEY} */
		String EXTENDS_PREFIX = ID_PREFIX + KEY_SEP + "extend";

		default Collection<String> extendsKeys()
		{
			return enumerate( EXTENDS_PREFIX, null );
		}

		String EXTENDS_INDEX_KEY = "extendsIndex";

		@Key( EXTENDS_INDEX_KEY )
		String extendsIndex();

		@Key( EXTENDS_PREFIX + KEY_SEP + "${" + EXTENDS_INDEX_KEY + "}" )
		String extend();

		String CONTEXT_KEY = "context";

		@Key( ID_PREFIX + CONTEXT_KEY )
		@ConverterClass( Context.ConfigConverter.class )
		Context context();

		class AnonymousConverter implements Converter<String>
		{
			@Override
			public String convert( final Method method, final String input )
			{
				return input == null || input.isEmpty() ? "anon|" + (new UUID())
						: input;
			}
		}
	}
}