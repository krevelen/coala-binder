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

	static String toString( final LocalContextual self )
	{
		return self.id().toString();
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

		String CONTEXT_BASE_KEY = "${" + ID_KEY + "}.context";

		@Key( ID_KEY )
		@DefaultValue( ID_DEFAULT )
		@ConverterClass( AnonymousConverter.class )
		String id();

		@Key( CONTEXT_BASE_KEY )
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