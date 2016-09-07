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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Converter;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;
import io.coala.json.Contextual.Context;
import io.coala.json.JsonUtil;

/**
 * {@link LocalConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 * @see ConfigFactory#create(Class, Map[])
 * @see ConfigCache#getOrCreate(Class, Map[])
 */
public interface LocalConfig extends GlobalConfig
{

	String ID_KEY = "id";

	String ID_DEFAULT = "";

	String ID_PREFIX = "${" + ID_KEY + "}";

	static LocalConfig create( final Map<?, ?>... imports )
	{
		return ConfigFactory.create( LocalConfig.class, imports );
	}

	static LocalConfig of( final String id, final Map<?, ?>... imports )
	{
		return ConfigFactory.create( LocalConfig.class, ConfigUtil
				.join( Collections.singletonMap( ID_KEY, id ), imports ) );
	}

	static JsonBuilder builder()
	{
		return new JsonBuilder();
	}

	class AnonymousConverter implements Converter<String>
	{
		@Override
		public String convert( final Method method, final String input )
		{
			return input == null || input.isEmpty()
					|| input.equals( ID_DEFAULT ) ? "anon|" + (new UUID())
							: input;
		}
	}

	@Key( ID_KEY )
	@DefaultValue( ID_DEFAULT )
	@ConverterClass( LocalConfig.AnonymousConverter.class )
	String rawId();

	// TODO add 'extends' key to inherit/import from other contexts

	String CONTEXT_KEY = "context";

	@Key( CONTEXT_KEY )
	@ConverterClass( Context.ConfigConverter.class )
	Context context();

	String BINDER_KEY = "binder";

	/**
	 * @param imports
	 * @return the (cached) {@link BinderConfig} instance
	 * @see ConfigCache#getOrCreate(Class, Map[])
	 */
	default BinderConfig binderConfig( final Map<?, ?>... imports )
	{
		return subConfig( BINDER_KEY, BinderConfig.class, imports );
	}

	class JsonBuilder
	{
		private ObjectNode tree = JsonUtil.getJOM().createObjectNode();

		private List<Map<?, ?>> configs = new ArrayList<>();

		public JsonBuilder withId( final String id )
		{
			this.tree.put( ID_KEY, id );
			return this;
		}

		public JsonBuilder withConfig( final Map<?, ?> config )
		{
			this.configs.add( config );
			return this;
		}

		public JsonBuilder withConfig( final String key, final String value )
		{
			return withConfig( Collections.singletonMap( key, value ) );
		}

		public JsonBuilder withConfig( final Accessible config )
		{
			return withConfig( ConfigUtil.export( config ) );
		}

		/**
		 * @param class1
		 * @param class2
		 * @return
		 */
		public JsonBuilder withSingleton( final Class<?> type,
			final Class<?> impl )
		{
			// TODO implement recursively?
			final ObjectNode provider = JsonUtil.getJOM().createObjectNode()
					.put( ProviderConfig.IMPLEMENTATION_KEY, impl.getName() )
					.put( ProviderConfig.SINGLETON_KEY, true )
					.put( ProviderConfig.MUTABLE_KEY, false )
					.put( ProviderConfig.INITABLE_KEY, false );
			provider.withArray( ProviderConfig.BINDINGS_KEY )
					.add( JsonUtil.getJOM().createObjectNode()
							.put( BindingConfig.TYPE_KEY, type.getName() ) );
			this.tree.with( BINDER_KEY ).withArray( BinderConfig.PROVIDERS_KEY )
					.add( provider );
			return this;
		}

		/**
		 * @param type the type to bind
		 * @param impl the implementation type to provide (immutable, lazy)
		 * @return this builder for chaining
		 */
		public JsonBuilder withProvider( final Class<?> type,
			final Class<?> impl )
		{
			return withProvider( type, impl, false );
		}

		/**
		 * @param type
		 * @param impl
		 * @param mutable
		 * @return
		 */
		public JsonBuilder withProvider( final Class<?> type,
			final Class<?> impl, final boolean mutable )
		{
			return withProvider( type, impl, mutable, false );
		}

		public JsonBuilder withProvider( final Class<?> type,
			final Class<?> impl, final boolean mutable, final boolean init )
		{
			// TODO implement recursively?
			final ObjectNode provider = JsonUtil.getJOM().createObjectNode()
					.put( ProviderConfig.IMPLEMENTATION_KEY, impl.getName() )
					.put( ProviderConfig.MUTABLE_KEY, mutable )
					.put( ProviderConfig.INITABLE_KEY, init );
			provider.withArray( ProviderConfig.BINDINGS_KEY )
					.add( JsonUtil.getJOM().createObjectNode()
							.put( BindingConfig.TYPE_KEY, type.getName() ) );
			this.tree.with( BINDER_KEY ).withArray( BinderConfig.PROVIDERS_KEY )
					.add( provider );
			return this;
		}

		public LocalConfig build()
		{
			final Properties props = ConfigUtil.flatten( this.tree );
//			LogUtil.getLogger( this ).trace( "flattened: {} into: {}",
//					JsonUtil.toJSON( this.tree ), props );
			return LocalConfig.create( ConfigUtil.join( props,
					this.configs.toArray( new Map[0] ) ) );
		}
	}
}