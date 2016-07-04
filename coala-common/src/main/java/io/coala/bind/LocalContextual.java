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
import java.util.Map;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Converter;

import com.eaio.uuid.UUID;

import io.coala.bind.LocalBinder.BinderConfig;
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
	interface LocalConfig extends GlobalConfig
	{

		String ID_KEY = "id";

		String ID_DEFAULT = "";

		String ID_PREFIX = "${" + ID_KEY + "}";

		@Key( ID_KEY )
		@DefaultValue( ID_DEFAULT )
		@ConverterClass( AnonymousConverter.class )
		String id();

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

		// TODO add 'extends' key to inherit/import from other contexts
	}
}