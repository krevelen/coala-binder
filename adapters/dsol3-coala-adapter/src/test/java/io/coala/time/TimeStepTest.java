/* $Id: ded717d12ec0e332fc68d22797ac203f45f182d6 $
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
package io.coala.time;

import static org.aeonbits.owner.util.Collections.entry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.dsol3.Dsol3Config;
import net.jodah.concurrentunit.Waiter;

/**
 * {@link ProactiveTest}
 * 
 * @version $Id: ded717d12ec0e332fc68d22797ac203f45f182d6 $
 * @author Rick van Krevelen
 */
public class TimeStepTest
{

	/** */
	static final Logger LOG = LogManager.getLogger( TimeStepTest.class );

	@Test
	public void testTimeStep() throws TimeoutException
	{
		final Dsol3Config config = Dsol3Config.of(
				entry( Dsol3Config.ID_KEY, "timeStepTest" ),
				entry( Dsol3Config.START_TIME_KEY, "0" ),
				entry( Dsol3Config.RUN_LENGTH_KEY, "10" ) );
		LOG.info( "Starting timeStep test, config: {}", config.toYAML() );
		final Scheduler scheduler = config.create( s ->
		{
			assertThat( "default start time is zero", s.now(),
					comparesEqualTo( Instant.ZERO ) );
			LOG.trace( "Scheduler initialized, t={}", s.now().prettify( 3 ) );
		} );

		final Waiter waiter = new Waiter();
		scheduler.time().subscribe( time ->
		{
		}, error ->
		{
			LOG.error( "error at t=" + scheduler.now(), error );
			waiter.rethrow( error );
		}, () ->
		{
			waiter.resume();
		} );
		scheduler.resume();
		waiter.await( 1, TimeUnit.SECONDS );
		assertThat( "end time is ten", scheduler.now(),
				comparesEqualTo( Instant.of( 10 ) ) );
		LOG.info( "Time step test complete, t={}", scheduler.now() );
	}

}
