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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

import java.util.concurrent.TimeoutException;

import javax.measure.unit.Unit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * {@link ProactiveTest}
 * 
 * @version $Id: ded717d12ec0e332fc68d22797ac203f45f182d6 $
 * @author Rick van Krevelen
 */
public class ProactiveTest
{

	/** */
	static final Logger LOG = LogManager.getLogger( ProactiveTest.class );

	@Test
	public void testFutureSelf() throws TimeoutException
	{
		final Proactive model = new Proactive()
		{
			@Override
			public Scheduler scheduler()
			{
				return null;
			}

			@Override
			public Instant now()
			{
				return Instant.of( 0 );
			}
		};

		LOG.trace( "testing t+1 == " + Instant.ONE );
		assertThat( "FutureSelf#after(t) time is added to Timed#now()",
				model.after( TimeSpan.ONE ).now(),
				comparesEqualTo( Instant.ONE ) );

		LOG.trace( "testing t+3 != " + Instant.of( 2, Unit.ONE ) );
		assertThat( "FutureSelf#after(t) time is added to Timed#now()",
				model.after( TimeSpan.of( 3, Unit.ONE ) ).now(),
				not( comparesEqualTo( Instant.of( 2, Unit.ONE ) ) ) );
	}

}
