/* $Id: 99a48bd864ed547afbe83481715fe4f3bf3c8426 $
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
package io.coala.experimental.config;

import java.io.File;
import java.util.Properties;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.coala.config.ConfigUtil;
import io.coala.log.LogUtil;

/**
 * {@link ConfigTest}
 * 
 * @version $Id: 99a48bd864ed547afbe83481715fe4f3bf3c8426 $
 * @author Rick van Krevelen
 */
public class ConfigTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( ConfigTest.class );

	@BeforeClass
	public static void logStart()
	{
		LOG.trace( "Starting Configuration tests!" );
	}

	@AfterClass
	public static void logEnd()
	{
		LOG.trace( "Completed Configuration tests!" );
	}

	/**
	 * {@link TestConfig}
	 */
	@Sources( { "file:~/.myapp.config", "file:${user.home}/etc/myapp.config",
			"classpath:foo/bar/baz.properties" } )
	public interface TestConfig extends Config
	{
		/** @return */
		@Key( "server.http.port" )
		int port();

		/** @return */
		@Key( "server.host.name" )
		String hostname();

		/** @return */
		@Key( "server.max.threads" )
		@DefaultValue( "42" )
		int maxThreads();

		@DefaultValue( "Hello Mr. %s!" )
		String helloMr( String name );

		@DefaultValue( "Welcome: ${user.name}" )
		String welcomeString();

		@DefaultValue( "${TMPDIR}/tempFile.tmp" )
		File tempFile();
	}

	@Test
	public void testConfigCommons() throws Exception
	{
		final PropertiesConfiguration config = new PropertiesConfiguration(
				ConfigUtil.PROPERTIES_FILE );
		config.save( System.out );
	}

	@Test
	public void testOwnerAPI() throws Exception
	{
		final Properties props = new Properties()
		{
			private static final long serialVersionUID = 1L;

			{
				setProperty( "server.host.name", "localhost" );
				setProperty( "server.http.port", "80" );
			}
		};
		final TestConfig cfg1 = ConfigFactory.create( TestConfig.class, props );
		LOG.trace( "Server " + cfg1.hostname() + ":" + cfg1.port()
				+ " will run " + cfg1.maxThreads() );

		final TestConfig cfg2 = ConfigCache.getOrCreate( TestConfig.class,
				props );
		LOG.trace( "Server " + cfg2.hostname() + ":" + cfg2.port()
				+ " will run " + cfg2.maxThreads() );
	}

	@Test
	public void testConfigInjection() throws Exception
	{
		// TODO
	}
}
