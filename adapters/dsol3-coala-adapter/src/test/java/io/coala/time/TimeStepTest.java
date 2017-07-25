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
package io.coala.time;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import io.coala.dsol3.Dsol3Config;
import io.coala.util.MapBuilder;

/**
 * {@link ProactiveTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class TimeStepTest
{

	/** */
	static final Logger LOG = LogManager.getLogger( TimeStepTest.class );

	@Test
	@Ignore
	public void testTimeStep() throws TimeoutException
	{
		final Dsol3Config config = Dsol3Config
				.of( MapBuilder.<String, Object>unordered()
						.put( Dsol3Config.ID_KEY, "timeStepTest" )
						.put( Dsol3Config.START_TIME_KEY, "0" )
						.put( Dsol3Config.RUN_LENGTH_KEY, "10" ).build() );
		LOG.info( "Starting timeStep test, config: {}", config.toYAML() );
		final Scheduler scheduler = config.create( s ->
		{
			assertThat( "default start time is zero", s.now(),
					comparesEqualTo( Instant.ZERO ) );
			LOG.trace( "Scheduler initialized, t={}", s.now().prettify( 3 ) );
		} );

		scheduler.run();
		assertThat( "end time is ten", scheduler.now(),
				comparesEqualTo( Instant.of( 10 ) ) );
		LOG.info( "Time step test complete, t={}", scheduler.now() );
	}

}
