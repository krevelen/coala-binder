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
package io.coala.math;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.math.BigDecimal;
import java.text.ParseException;

/**
 * {@link RangeTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class RangeTest
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( RangeTest.class );

	@Test
	public void testParse() throws ParseException
	{
		LOG.info( "Started Range#parse() test" );
		final String inf = " \u3008 \u2190 ; +inf >", eights = " [ 8 : 8 ] ";
		final BigDecimal eight = BigDecimal.valueOf( 8 );
		LOG.trace( "test `{}` equals `{}`", inf, Range.infinite() );
		assertThat( "Should be equal", Range.parse( inf, Integer.class ),
				equalTo( Range.infinite() ) );
		LOG.trace( "test `{}` = `{}`", eights, Range.of( 8 ) );
		assertThat( "Should be equal", Range.parse( eights ),
				comparesEqualTo( Range.of( eight ) ) );
		LOG.trace( "test `{}`.crop(10) = 8", eights, Range.of( 8 ) );
		assertThat( "Should be equal",
				Range.parse( eights ).crop( BigDecimal.TEN ),
				comparesEqualTo( eight ) );
		LOG.info( "Completed Range#parse() test" );
	}
}
