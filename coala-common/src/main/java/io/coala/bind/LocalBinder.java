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

import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;

import io.coala.config.GlobalConfig;
import io.coala.name.x.Id;
import rx.Observable;

/**
 * {@link LocalBinder} maintains {@link LocalProvider} bindings for providing
 * objects within its own local {@link Context}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface LocalBinder
{

	/**
	 * @return the {@link Config} used for this {@link LocalBinder}
	 */
	Config config();

	/** */
	Context context();

	/**
	 * @param type the type to bind as query
	 * @param instance the constant or singleton instance to bind as result
	 * @return the new {@link Provider}
	 */
	<T, S extends T> LocalProvider<T> bind( Class<T> type, S instance );

	/**
	 * @param type the factory type to bind as singleton instance
	 * @return the new {@link Provider}
	 */
	<T, S extends T> LocalProvider<T> bind( Class<T> type,
		Provider<S> provider );

	/**
	 * @param type the {@link Singleton} type to instantiate on each binding
	 * @param args the parameter constants to use for each instantiation
	 * @return the new {@link Provider}
	 */
	<T> LocalProvider<T> bind( Class<T> type, Object... args );

	/**
	 * @return an {@link Observable} stream of all binding {@link Provider}s
	 */
	Observable<LocalProvider<?>> emitBindings();

	/**
	 * @param type the expected type of object
	 * @return an instance as provided by a {@link LocalProvider}
	 */
	<T> T inject( Class<T> type );

	/**
	 * {@link Context}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Context extends Id.Ordinal<String>
	{
		/**
		 * @param value the {@link String} value
		 * @return the new {@link Context}
		 */
		public static Context valueOf( final String value )
		{
			return Util.of( value, new Context() );
		}
	}

	/**
	 * {@link LocalProvider} is a contextual or local {@link Provider}
	 * 
	 * @param <T>
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface LocalProvider<T> extends Provider<T>
	{
		Context context();
	}

	/**
	 * {@link Config}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 * @see ConfigFactory#create(Class, Map[])
	 * @see ConfigCache#getOrCreate(Class, Map[])
	 */
	interface Config extends GlobalConfig
	{
		String context();

		Map<Class<?>, Class<?>> initialBindings();
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Factory extends io.coala.factory.Factory
	{

		FactoryConfig config();

		void bootInitial();

		void boot( Config config );

		/**
		 * {@link FactoryConfig}
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 * @see ConfigFactory#create(Class, Map[])
		 * @see ConfigCache#getOrCreate(Class, Map[])
		 */
		interface FactoryConfig extends GlobalConfig
		{
			List<String> initialContexts();

			Class<? extends Factory> factoryType();
		}
	}
}
