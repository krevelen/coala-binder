/* $Id: 6e7cdc7950694a38c5d755cb565960e03c007784 $
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
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;

import java.text.ParseException;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import io.coala.json.JsonUtil;
import io.coala.json.Wrapper;
import io.coala.log.LogUtil;

/**
 * {@link InstantTest}
 * 
 * @version $Id: 6e7cdc7950694a38c5d755cb565960e03c007784 $
 * @author Rick van Krevelen
 */
public class InstantTest
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( InstantTest.class );

//	@SuppressWarnings( "rawtypes" )
	@Test
	public void testInstant()
	{
		final Instant millis = Instant.of( System.currentTimeMillis(),
				TimeUnits.MILLIS );
		assertThat( "should be equal",
				millis.to( TimeUnits.DAYS ).to( TimeUnits.MILLIS ),
				comparesEqualTo( millis ) );
		LOG.info( "millis = {} as years = {}", millis,
				millis.to( TimeUnits.ANNUM ) );
	}

	@Ignore // FIXME Instant#equals() doesn't match correctly
	@Test
	public void testJson() throws ParseException
	{
		LOG.info( "Started InstantTest#testJson() test" );

		final Instant zeroTick = Instant.ZERO;
		final String zeroTickJson = JsonUtil.toJSON( zeroTick );
		LOG.trace( "test '{}' to/from JSON: {}", zeroTick, zeroTickJson );
		assertThat( "Should be equal", zeroTick,
				equalTo( JsonUtil.valueOf( zeroTickJson, Instant.class ) ) );

		final Instant zeroDays = Instant.of( 0, TimeUnits.DAYS );
		final String zeroDaysJson = JsonUtil.toJSON( zeroDays );
		LOG.trace( "test '{}' to/from JSON: {}", zeroDays, zeroDaysJson );
		assertThat( "Should be equal", zeroDays,
				equalTo( JsonUtil.valueOf( zeroDaysJson, Instant.class ) ) );

//		final Instant oneTick = Instant.of( 1, TimeUnits.TICK );

		LOG.info( "Completed InstantTest#testJson() test" );
	}

	@Test
	public void testCompareZero() throws ParseException
	{
		LOG.info( "Started InstantTest#testCompareZero() test" );

		final Instant zeroDays = Instant.of( 0, TimeUnits.DAYS );
		LOG.trace( "test '{}' \u2248 '{}'", Instant.ZERO, zeroDays );
		assertThat( "Should compare equal", Instant.ZERO,
				comparesEqualTo( zeroDays ) );

		final Instant t1 = Instant.of( 1.2, TimeUnits.ANNUM ),
				t2 = Instant.of( 1.1, TimeUnits.ANNUM );
		final long start1 = System.currentTimeMillis(), n = 10000000;
		for( long i = 0; i < n; i++ )
			Wrapper.Util.compare( t1, t2 );
		final long start2 = System.currentTimeMillis(), dur1 = start2 - start1;
		for( long i = 0; i < n; i++ )
			t1.compareTo( t2 );
		final long dur2 = System.currentTimeMillis() - start2;
		LOG.trace( "compare: {}ms vs {}ms", dur1, dur2 );

		LOG.info( "Completed InstantTest#testCompareZero() test" );
	}
}
