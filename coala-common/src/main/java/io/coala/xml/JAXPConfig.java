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
package io.coala.xml;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilderFactory;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigCache;

import io.coala.config.GlobalConfig;
import io.coala.config.YamlConfig;
import io.coala.exception.Thrower;

/**
 * {@link JAXPConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@LoadPolicy( LoadType.MERGE )
@Sources( { "classpath:" + JAXPConfig.JAXP_CONFIG_FILE_LOCATION } )
public interface JAXPConfig extends GlobalConfig, YamlConfig
{

	/** default as per {@link DocumentBuilderFactory} javadoc */
	String JAXP_CONFIG_FILE_LOCATION = "jaxp.properties";

	/** @return a cached JAXPConfig instance */
	static JAXPConfig getOrCreate()
	{
		return ConfigCache.getOrCreate( JAXPConfig.class,
				System.getProperties(), System.getenv() );
	}

	/**
	 * @return the JAXP implementation type of {@link DatatypeFactory}
	 * @see DatatypeFactory#DATATYPEFACTORY_IMPLEMENTATION_CLASS
	 */
	@Key( DatatypeFactory.DATATYPEFACTORY_PROPERTY )
	Class<? extends DatatypeFactory> datatypeFactoryType();

	@DefaultValue( "" + true )
	boolean isNamespaceAware();

	/**
	 * @return a new XMLInputFactory instance
	 * @see javax.xml.parsers.DocumentBuilderFactory#newInstance()
	 */
	default DatatypeFactory newDatatypeFactory()
	{
		try
		{
			return DatatypeFactory.newInstance( datatypeFactoryType().getName(),
					Thread.currentThread().getContextClassLoader() );
		} catch( final DatatypeConfigurationException e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @return a new XMLInputFactory instance
	 * @see javax.xml.parsers.DocumentBuilderFactory#newInstance()
	 */
	default DocumentBuilderFactory newDocumentBuilderFactory()
	{
		final DocumentBuilderFactory result = DocumentBuilderFactory
				.newInstance();
		result.setNamespaceAware( isNamespaceAware() );
		return result;
	}
}