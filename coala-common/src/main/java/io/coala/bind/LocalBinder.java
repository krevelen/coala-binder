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
import io.coala.util.Instantiator;
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
	 * @param type the factory type to (re)bind
	 * @param provider the instance {@link Provider}
	 * @return the (wrapped) {@link LocalProvider}
	 */
	<T> LocalProvider<T> bind( Class<T> type, Provider<T> provider );

	/**
	 * @param type the type to (re)bind
	 * @param instance the constant or singleton instance to bind
	 * @return the new {@link LocalProvider}
	 */
	default <T> LocalProvider<T> bind( final Class<T> type, final T instance )
	{
		return bind( type, LocalProvider.of( this, instance ) );
	}

	/**
	 * @param type the {@link Singleton} type to (re)bind
	 * @param args the parameter constants to use for each instantiation
	 * @return the new {@link LocalProvider}
	 */
	default <T> LocalProvider<T> bind( final Class<T> type,
		final Object... args )
	{
		return bind( type, LocalProvider.of( this,
				Instantiator.providerOf( type, args ), false ) );
	}

	/**
	 * @return an {@link Observable} stream of all (re)bound {@link Class}s
	 */
	Observable<Class<?>> emitBindings();

	/**
	 * {@link LocalProvider} is a {@link LocalContextual} {@link Provider}
	 * 
	 * @param <T>
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface LocalProvider<T> extends Provider<T>, LocalContextual
	{

		@SuppressWarnings( "unchecked" )
		static <T> LocalProvider<T> of( final LocalContextual origin,
			final Class<T> type )
		{
			return of( origin,
					Provider.class.isAssignableFrom( type )
							? (Provider<T>) Instantiator.instantiate( type )
							: Instantiator.providerOf( type ),
					false );
		}

		static <T> LocalProvider<T> of( final LocalContextual origin,
			final T instance )
		{
			return of( origin, () ->
			{
				return instance;
			}, true );
		}

		static <T> LocalProvider<T> of( final LocalContextual origin,
			final Provider<T> provider, final boolean caching )
		{
			if( provider instanceof LocalProvider && LocalContextual
					.equals( (LocalContextual) provider, origin ) )
				return (LocalProvider<T>) provider;

			final Wrapped<T> result = caching ? new Caching<T>()
					: new Wrapped<T>();
			result.origin = origin;
			result.provider = provider;
			return result;
		}

		class Wrapped<T> implements LocalProvider<T>
		{
			protected LocalContextual origin;
			protected Provider<T> provider;

			@Override
			public T get()
			{
				return this.provider.get();
			}

			@Override
			public Context context()
			{
				return this.origin.context();
			}

			@Override
			public String id()
			{
				return this.origin.id();
			}
		}

		/**
		 * {@link Caching}
		 * 
		 * @param <T>
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		class Caching<T> extends Wrapped<T>
		{
			T cache = null;

			@Override
			public synchronized T get()
			{
				return this.cache == null ? this.cache = super.get()
						: this.cache;
			}
		}
	}

	/**
	 * {@link MutableProvider} is a {@link LocalProvider} with
	 * {@link #reset(LocalProvider)} function
	 * 
	 * @param <T>
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface MutableProvider<T> extends LocalProvider<T>
	{
		/**
		 * @param newProvider the new {@link LocalProvider} instance
		 * @return this {@link MutableProvider} to allow chaining
		 */
		MutableProvider<T> reset( LocalProvider<T> newProvider );

		static <T> MutableProvider<T> of( final LocalContextual origin,
			final Class<T> type )
		{
			return of( LocalProvider.of( origin, type ) );
		}

		static <T> MutableProvider<T> of( final LocalContextual origin,
			final T instance )
		{
			return of( LocalProvider.of( origin, instance ) );
		}

		static <T> MutableProvider<T> of( final LocalProvider<T> provider )
		{
			return new Caching<T>().reset( provider );
		}

		/**
		 * {@link Caching}
		 * 
		 * @param <T>
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		class Caching<T> extends LocalProvider.Caching<T>
			implements MutableProvider<T>
		{
			@Override
			public synchronized MutableProvider<T>
				reset( final LocalProvider<T> provider )
			{
				this.origin = provider;
				this.provider = provider;
				this.cache = null;
				return this;
			}

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

		/** the (relative) {@link #FIXED_TYPES_KEY} */
		String FIXED_TYPES_KEY = "fixed";

		@Key( FIXED_TYPES_KEY )
		Class<?>[] fixedTypes();

		/** the (relative) {@link #MUTABLE_TYPES_KEY} */
		String MUTABLE_TYPES_KEY = "mutable";

		@Key( MUTABLE_TYPES_KEY )
		Class<?>[] mutableTypes();

		/** the (relative) {@link #LAUNCH_TYPES_KEY} */
		String LAUNCH_TYPES_KEY = "launch";

		@Key( LAUNCH_TYPES_KEY )
		Class<?>[] launchTypes();

		/** the (relative) {@link #EXTENDS_KEY} */
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

		/**
		 * @param imports
		 * @return
		 * @see ConfigCache#getOrCreate(Class, Map...)
		 */
		static LauncherConfig getOrCreate( final Map<?, ?>... imports )
		{
			return ConfigCache.getOrCreate( LauncherConfig.class, imports );
		}

		static String keyFor( final String... keys )
		{
			if( keys == null || keys.length < 2 )
				throw new IllegalArgumentException(
						"Key list must contain <id> and at least another key" );
			final String[] list = new String[keys.length + 1];
			int n = 0;
			list[n++] = "binder";
			for( String key : keys )
				list[n++] = key;
			return String.join( KEY_SEP, list );
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
				final String prefix = keyFor( id, "" );
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
