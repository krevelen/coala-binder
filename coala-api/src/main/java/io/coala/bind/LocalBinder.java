/* $Id: b238149c5603c3fd5aff586654ed6c0144c8fdfd $
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

import javax.inject.Provider;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;

import io.coala.name.Identified;
import io.coala.util.Instantiator;
import io.reactivex.Observable;

/**
 * {@link LocalBinder} maintains {@link LocalProvider} bindings for providing
 * objects {@link LocalContextual}, i.e. within its own locally
 * {@link Identified} {@link Context}
 * 
 * @version $Id: b238149c5603c3fd5aff586654ed6c0144c8fdfd $
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
	 * @param type the expected type of object
	 * @param config the {@link JsonNode} for {@link InjectConfig @InjectConfig}
	 * @return an instance as provided by a {@link LocalProvider}
	 */
	<T> T inject( Class<T> type, JsonNode config );

	/**
	 * @param encloser the enclosing object to inject members into
	 * @return an instance as provided by a {@link LocalProvider}
	 */
	<T> T injectMembers( T encloser );

	/**
	 * @param type the factory type to (re)bind
	 * @param provider the replacement {@link Provider}
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
		return reset( type, (Provider<T>) // must cast to avoid deploy errors 
		LocalProvider.of( this,
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
	 * @version $Id: b238149c5603c3fd5aff586654ed6c0144c8fdfd $
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

			final LocalWrapped<T> result = caching ? new LocalCaching<>()
					: new LocalWrapped<>();
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
			public LocalId id()
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
		 * @version $Id: b238149c5603c3fd5aff586654ed6c0144c8fdfd $
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
	 * @version $Id: b238149c5603c3fd5aff586654ed6c0144c8fdfd $
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
		 * @version $Id: b238149c5603c3fd5aff586654ed6c0144c8fdfd $
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
}
