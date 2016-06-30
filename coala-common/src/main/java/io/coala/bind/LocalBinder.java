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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.aeonbits.owner.ConfigCache;

import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;
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
	 * @return this {@link LocalBinder} to allow chaining
	 */
	<T> LocalBinder reset( Class<T> type, Provider<T> provider );

	/**
	 * @param type the type to (re)bind
	 * @param instance the constant or singleton instance to bind
	 * @return this {@link LocalBinder} to allow chaining
	 */
	default <T> LocalBinder reset( final Class<T> type, final T instance )
	{
		return reset( type, LocalProvider.of( this, instance ) );
	}

	/**
	 * @param type the {@link Singleton} type to (re)bind
	 * @param args the parameter constants to use for each instantiation
	 * @return this {@link LocalBinder} to allow chaining
	 */
	default <T> LocalBinder reset( final Class<T> type,
		final Class<? extends T> impl, final Object... args )
	{
		return reset( type, LocalProvider.of( this,
				Instantiator.providerOf( impl.asSubclass( type ), args ),
				false ) );
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
			return of( origin, new Provider<T>()
			{
				public T get()
				{
					return instance;
				}

				@Override
				public String toString()
				{
					return instance.toString();
				}
			}, true );
		}

		static <T> LocalProvider<T> of( final LocalContextual origin,
			final Provider<T> provider, final boolean caching )
		{
			if( provider instanceof LocalProvider && LocalContextual
					.equals( (LocalContextual) provider, origin ) )
				return (LocalProvider<T>) provider;

			final LocalWrapped<T> result = caching ? new LocalCaching<T>()
					: new LocalWrapped<T>();
			result.origin = origin;
			result.provider = provider;
			return result;
		}

		class LocalWrapped<T> implements LocalProvider<T>
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

			@Override
			public String toString()
			{
				return getClass().getSimpleName() + "::"
						+ this.provider.toString();
			}
		}

		/**
		 * {@link LocalCaching}
		 * 
		 * @param <T>
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		class LocalCaching<T> extends LocalWrapped<T>
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
			return new MutableCaching<T>().reset( provider );
		}

		/**
		 * {@link MutableCaching}
		 * 
		 * @param <T>
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		class MutableCaching<T> extends LocalCaching<T>
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
	interface LaunchConfig extends GlobalConfig
	{

		String LAUNCH_KEY = "launch";

		static Collection<String> launchIds( LaunchConfig config )
		{
			return ConfigUtil.enumerate( config, null, KEY_SEP + LAUNCH_KEY );
		};

		@Key( LocalConfig.ID_PREFIX + KEY_SEP + LAUNCH_KEY )
		Boolean binderLaunch();
	}

	/**
	 * {@link Config}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Config extends LocalConfig
	{

		/**
		 * @param id
		 * @param imports
		 * @return the (cached) {@link Config} instance
		 * @see ConfigCache#getOrCreate(Class, Map[])
		 */
		static Config getOrCreate( final String id, final LaunchConfig config )
		{
			return LocalConfig.getOrCreate( id, Config.class,
					ConfigUtil.export( config,
							Pattern.compile( "^" + Pattern.quote( id + KEY_SEP )
									+ "(?<sub>.*)" ),
							null /* "local.${sub}" */ ) );
		}

		String BINDER_KEY = "binder";

		String BINDER_BASE = ID_PREFIX + KEY_SEP + BINDER_KEY;

		String BINDING_KEY = "binding";

		String BINDING_PREFIX = BINDER_BASE + KEY_SEP + BINDING_KEY;

		default Collection<String> bindingIndices()
		{
			return enumerate( BINDING_PREFIX, null );
		}

		String BINDING_INDEX_KEY = "bindingIndex";

		@Key( BINDING_INDEX_KEY )
		String bindingIndex();

		String BINDING_BASE = BINDING_PREFIX + KEY_SEP + "${"
				+ BINDING_INDEX_KEY + "}";

		String MUTABLE_KEY = "mutable";

		@Key( BINDING_BASE + KEY_SEP + MUTABLE_KEY )
		@DefaultValue( "false" )
		boolean bindingMutable();

		String INITABLE_KEY = "init";

		@Key( BINDING_BASE + KEY_SEP + INITABLE_KEY )
		@DefaultValue( "false" )
		boolean bindingInitable();

		String IMPLEMENTATION_KEY = "impl";

		@Key( BINDING_BASE + KEY_SEP + IMPLEMENTATION_KEY )
		Class<?> bindingImplementation();

		String INJECTABLE_KEY = "inject";

		String INJECTABLE_PREFIX = BINDING_BASE + KEY_SEP + INJECTABLE_KEY;

		default Collection<String> injectablesIndices( final String binding )
		{
			return enumerate( INJECTABLE_PREFIX,
					Collections.singletonMap( BINDING_INDEX_KEY, binding ) );
		}

		String INJECTABLE_INDEX_KEY = "injectIndex";

		@Key( INJECTABLE_INDEX_KEY )
		String injectableIndex();

		@Key( INJECTABLE_PREFIX + KEY_SEP + "${" + INJECTABLE_INDEX_KEY + "}" )
		Class<?> bindingInjectable();
	}

	/**
	 * {@link Launcher}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Launcher
	{
		/**
		 * @param id the identifier of the {@link LocalBinder} to launch
		 */
		void launch( String id );
	}
}
