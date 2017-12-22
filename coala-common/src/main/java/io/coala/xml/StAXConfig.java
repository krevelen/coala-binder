/* $Id: b8d074110a052aa4099af0f651d94410ffe8f661 $
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
package io.coala.xml;

import javax.xml.stream.XMLInputFactory;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigCache;

import io.coala.config.GlobalConfig;
import io.coala.config.YamlConfig;
import io.coala.log.LogUtil;

/**
 * {@link StAXConfig}
 * 
 * @version $Id: b8d074110a052aa4099af0f651d94410ffe8f661 $
 * @author Rick van Krevelen
 */
@LoadPolicy( LoadType.MERGE )
@Sources( { "classpath:" + StAXConfig.STAX_CONFIG_FILE_LOCATION } )
public interface StAXConfig extends GlobalConfig, YamlConfig
{
	String STAX_CONFIG_FILE_LOCATION = "stax.properties";

	/**
	 * @return the system property for {@code javax.xml.stream.XMLInputFactory}
	 *         as per the javadoc of {@link XMLInputFactory#newFactory()}
	 */
	@Key( "javax.xml.stream.XMLInputFactory" )
//	@DefaultValue( "com.ctc.wstx.stax.WstxInputFactory" )
//	@DefaultValue( "com.sun.xml.internal.stream.XMLInputFactoryImpl" )
	Class<? extends XMLInputFactory> xmlInputFactoryImpl();

	@Key( XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES )
	@DefaultValue( "" + false )
	Boolean isReplacingEntityReferences();

	@Key( XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES )
	@DefaultValue( "" + false )
	Boolean isSupportingExternalEntities();

	@Key( XMLInputFactory.IS_COALESCING )
	@DefaultValue( "" + false )
	Boolean isCoalescing();

	/** @return a cached StAXConfig instance */
	static StAXConfig getOrCreate()
	{
		return ConfigCache.getOrCreate( StAXConfig.class,
				System.getProperties(), System.getenv() );
	}

	/**
	 * @return a new (StAX) XMLInputFactory instance
	 * @see javax.xml.stream.XMLInputFactory#newFactory()
	 */
	default XMLInputFactory newXMLInputFactory()
	{
//		@SuppressWarnings( "deprecation" )
		final XMLInputFactory result = XMLInputFactory.newInstance(
//				xmlInputFactoryImpl().getName(),
//				Thread.currentThread().getContextClassLoader() 
				);
		LogUtil.getLogger( StAXConfig.class ).trace( "Using StAX {}: {}",
				XMLInputFactory.class.getSimpleName(),
				result.getClass().getName() );
		result.setProperty( XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
				isReplacingEntityReferences() );
		result.setProperty( XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
				isSupportingExternalEntities() );
		result.setProperty( XMLInputFactory.IS_COALESCING, isCoalescing() );
		return result;
	}
}