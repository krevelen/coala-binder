/* $Id$
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
package io.coala.bind;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Converter;

import io.coala.config.GlobalConfig;
import io.coala.config.YamlConfig;
import io.coala.name.Identified;
import rx.Observable;

/**
 * {@link LocalBinder} maintains {@link LocalProvider} bindings for providing
 * objects {@link LocalContextual}, i.e. within its own locally
 * {@link Identified} {@link Context}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface LocalBinder extends LocalContextual
{

	/**
	 * @param type the expected type of object
	 * @return an instance as provided by a {@link LocalProvider}
	 */
	<T> T inject( Class<T> type );

	/**
	 * @param type the type to (re)bind
	 * @param instance the constant or singleton instance to bind
	 * @return the new {@link LocalProvider}
	 */
	<T> LocalProvider<T> bind( Class<T> type, T instance );

	/**
	 * @param type the factory type to (re)bind
	 * @param provider the instance {@link Provider}
	 * @return the (wrapped) {@link LocalProvider}
	 */
	<T> LocalProvider<T> bind( Class<T> type, Provider<T> provider );

	/**
	 * @param type the {@link Singleton} type to (re)bind
	 * @param args the parameter constants to use for each instantiation
	 * @return the new {@link LocalProvider}
	 */
	<T> LocalProvider<T> bind( Class<T> type, Object... args );

	/**
	 * @return an {@link Observable} stream of all (re)bound {@link Class}s
	 */
	Observable<Class<?>> emitBindings();

	/**
	 * {@link LocalProvider} is a contextual or local {@link Provider}
	 * 
	 * @param <T>
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface LocalProvider<T> extends Provider<T>, LocalContextual
	{

		static <T> LocalProvider<T> of( final LocalContextual origin,
			final T instance )
		{
			return of( origin, () ->
			{
				return instance;
			} );
		}

		static <T> LocalProvider<T> of( final LocalContextual origin,
			final Provider<T> p )
		{
			if( p instanceof LocalProvider
					&& LocalContextual.equals( (LocalContextual) p, origin ) )
				return (LocalProvider<T>) p;

			return new LocalProvider<T>()
			{
				@Override
				public T get()
				{
					return p.get();
				}

				@Override
				public Context context()
				{
					return origin.context();
				}

				@Override
				public String id()
				{
					return origin.id();
				}
			};
		}
	}

	class MutableCachingProvider<T> implements LocalProvider<T>
	{
		public static <T> MutableCachingProvider<T>
			of( final LocalProvider<T> source )
		{
			final MutableCachingProvider<T> result = new MutableCachingProvider<>();
			result.source = source;
			return result;
		}

		private LocalProvider<T> source;

		private T cache = null;

		/**
		 * {@link MutableCachingProvider} zero-arg bean constructor
		 */
		protected MutableCachingProvider()
		{
			// empty
		}

		@Override
		public synchronized T get()
		{
			return this.cache == null ? (this.cache = this.source.get())
					: this.cache;
		}

		public synchronized void reset( final LocalProvider<T> source )
		{
			this.source = source;
			this.cache = null;
		}

		@Override
		public String id()
		{
			return this.source.id();
		}

		@Override
		public Context context()
		{
			return this.source.context();
		}
	}

	/**
	 * {@link Config}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Config extends LocalConfig
	{

		/** the (relative) PROVIDER_TYPES_KEY */
		String PROVIDER_TYPES_KEY = "providers";

		@Key( PROVIDER_TYPES_KEY )
		Class<?>[] launchTypes();

		/** the (relative) EXTEND_KEY */
		String EXTENDS_KEY = "extends";

		@Key( EXTENDS_KEY )
		String[] extend();

		/**
		 * @param id
		 * @param imports
		 * @return the cached {@link Config} instance
		 * @see ConfigCache#getOrCreate(Class, Map[])
		 */
		static Config getOrCreate( final String id, final Map<?, ?>... imports )
		{
			return ConfigCache.getOrCreate( id, Config.class, imports );
		}
	}

	/**
	 * {@link Launcher}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Launcher
	{

		void launch( Config config );
	}

	/**
	 * {@link LauncherConfig}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface LauncherConfig extends GlobalConfig
	{
		String LAUNCH_IDENTIFIERS_KEY = "launcher" + KEY_SEP + "ids";

		@Key( LAUNCH_IDENTIFIERS_KEY )
		String[] launchIdentifiers();

		@DefaultValue( "%s" )
		@ConverterClass( BinderConfigConverter.class )
		Config binderConfigFor( String id );

		static LauncherConfig fromYAML( final File yamlPath,
			final Map<?, ?>... imports ) throws IOException
		{
			return YamlConfig.fromYAML( LauncherConfig.class, yamlPath,
					imports );
		}

		static String binderKeyPrefixFor( final String id )
		{
			return String.join( KEY_SEP, "binder", id, "" );
		}

		class BinderConfigConverter implements Converter<Config>
		{
			@Override
			public Config convert( final Method method, final String id )
			{
				final LauncherConfig launcherConfig = ConfigCache
						.getOrCreate( LauncherConfig.class );

				final Map<String, String> imports = new HashMap<>();
				imports.put( ID_KEY, id );
				final String prefix = binderKeyPrefixFor( id );
				for( String key : launcherConfig.propertyNames() )
					if( key.startsWith( prefix ) )
						imports.put( key.substring( prefix.length() ),
								launcherConfig.getProperty( key ) );
				
				// TODO extend another id's config values

				// TODO multiple numbered instances (similar to 'extends')

				return Config.getOrCreate( id, imports );
			}
		}
	}
}
