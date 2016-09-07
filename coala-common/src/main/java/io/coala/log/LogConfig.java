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
package io.coala.log;

import static io.coala.config.ConfigUtil.join;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Converter;

import io.coala.config.GlobalConfig;

/**
 * {@link LogConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface LogConfig extends GlobalConfig
{

	/** */
	String LOCALE_KEY = "locale";

	/** */
	String LOCALE_DEFAULT = "en";

	/** */
	String JUL_MANAGER_KEY = "java.util.logging.manager";

	/** */
	String JUL_MANAGER_DEFAULT = "org.apache.logging.log4j.jul.LogManager";

	@Key( LOCALE_KEY )
	@DefaultValue( LOCALE_DEFAULT )
	@ConverterClass( LocaleConverter.class )
	Locale locale();

	@Key( JUL_MANAGER_KEY )
	@DefaultValue( JUL_MANAGER_DEFAULT )
	Class<? extends java.util.logging.LogManager> julManagerType();

	class LocaleConverter implements Converter<Locale>
	{
		@Override
		public Locale convert( final Method method, final String input )
		{
			return Locale.forLanguageTag( input );
		}
	}

	static LogConfig getOrCreate( final Map<?, ?>... imports )
	{
		return ConfigCache.getOrCreate( LogConfig.class, join( System.getenv(),
				join( System.getProperties(), imports ) ) );
	}
}