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
package io.coala.name;

import java.util.function.Supplier;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigCache;

import io.coala.config.YamlConfig;

/**
 * {@link JndiUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class JndiUtil
{

	/**
	 * {@link JndiUtil} constructor
	 */
	private JndiUtil()
	{
		// singleton
	}

	/**
	 * {@link JndiContextConfig}
	 * 
	 * Adapted from Randy Carver's example available at
	 * https://blogs.oracle.com/randystuph/entry/injecting_jndi_datasources_for_junit
	 * Requires dependency org.apache.tomcat:catalina:6.x (as per
	 * http://stackoverflow.com/a/37463150/1418999) or
	 * org.apache.tomcat:tomcat-catalina:7/8/9 (see
	 * https://mvnrepository.com/artifact/org.apache.tomcat/tomcat-catalina)
	 */
	@Sources( "classpath:jndi.properties" )
	public interface JndiContextConfig extends YamlConfig
	{
		@Key( Context.INITIAL_CONTEXT_FACTORY )
		@DefaultValue( "org.apache.naming.java.javaURLContextFactory" )
		Class<? extends InitialContextFactory> initialContextFactory();

		@Key( Context.URL_PKG_PREFIXES )
		@DefaultValue( "org.apache.naming" )
		Package packagePrefix();
	}

	/**
	 * 
	 * @param jndiName the JNDI context path to bind the {@link Object}
	 * @param sep the context path separator used in the {@code dsJndiName}
	 * @param dsFactory the {@link Supplier} of a {@link Object} to bind
	 * @throws NamingException
	 */
	public static void bindLocally( final String jndiName, final char sep,
		final Supplier<?> supplier ) throws NamingException
	{
		ConfigCache
				.getOrCreate( JndiContextConfig.class, System.getProperties() )
				.export()
				.forEach( ( k, v ) -> System.setProperty( k, v.toString() ) );
		final Context root = new InitialContext();
		try
		{
			// create required sub-contexts
			int lastIndex = jndiName.lastIndexOf( sep );
			if( lastIndex >= 0 )
			{
				Context sub = root;
				for( String name : jndiName.substring( 0, lastIndex )
						.split( "" + sep ) )
					sub = sub.createSubcontext( name );
			}

			root.bind( jndiName, supplier.get() );
		} finally
		{
			root.close();
		}
	}

}
