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
package io.coala.eve3;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.guice4.Guice4LocalBinder;
import io.coala.inter.Exposer;
import io.coala.inter.InjectProxy;
import io.coala.inter.Invoker;
import io.coala.log.LogUtil;

/**
 * {@link Eve3Test}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Eve3Test
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( Eve3Test.class );

	public static interface MyExposableService //extends Remote
	{
		@Access( AccessType.PUBLIC )
		String myName( @Name( "millis" ) int millis )
			throws InterruptedException; //,TimeoutException
	}

	public static class MyInvoking
	{
		@InjectProxy( value = "testAgent", timeout = "P1D" )
		MyExposableService svc;
	}

	public static class MyExposing implements MyExposableService
	{
		private int counter = 0;

		@Override
		public String myName( final int millis ) throws InterruptedException
		{
			Thread.sleep( millis );
			return "myName call:" + counter++;
		}
	}

	@Test( expected = UndeclaredThrowableException.class )
	// TimeoutException undeclared in MyExposableService#myName(int)
	public void testEve3() throws InterruptedException, TimeoutException
	{
		final String agentName = "testAgent";
		final LocalConfig config = LocalConfig.builder().withId( agentName )
				.withProvider( Invoker.class, Eve3Invoker.class ).build();
		LOG.info( "Starting Eve3 test, config: {}", config.toYAML() );
		final LocalBinder binder = Guice4LocalBinder.of( config );

		LOG.trace( "Loaded Eve3 agent config for '{}': {}", agentName, binder
				.inject( Eve3Factory.class )
				.getConfig( Collections.singletonMap(
						Eve3Config.CONFIG_PATH_KEY, "eve-wrapper.yaml" ) )
				.forAgent( agentName ).toYAML() );

		final Exposer exposer = binder.inject( Eve3Exposer.class );
		LOG.trace( "Created @Singleton Exposer: {}", exposer );
		LOG.trace( "Created @Singleton Exposer: {}",
				binder.inject( Eve3Exposer.class ) );
		final List<URI> endpoints = exposer.exposeAs( MyExposableService.class,
				new MyExposing() );
		LOG.trace( "Exposed service at endpoints: {}", endpoints );

		final MyInvoking invoker = binder.inject( MyInvoking.class );
		final MyExposableService proxy = invoker.svc;
		LOG.trace( "Created @Singleton Invoker: {}",
				binder.inject( Eve3Invoker.class ) );
		LOG.trace( "Created proxy with @Singleton Invoker: {}", proxy );
		LOG.trace( "test1: {}", proxy.myName( 500 ) );
		LOG.trace( "test2: {}", proxy.myName( 900 ) );
		LOG.trace( "test3: {}", proxy.myName( 1500 ) ); // should fail
		Thread.sleep( Long.MAX_VALUE );
	}
}
