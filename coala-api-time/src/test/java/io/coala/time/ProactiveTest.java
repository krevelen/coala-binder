/* $Id: 6277d09a29f8b5f28a3451ad37cefebb42e54efd $
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * {@link ProactiveTest}
 * 
 * @version $Id: 6277d09a29f8b5f28a3451ad37cefebb42e54efd $
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

		LOG.trace( "testing t+1 == " + Instant.of( 1 ) );
		assertThat( "FutureSelf#after(t) time is added to Timed#now()",
				model.after( Duration.ONE ).now(),
				comparesEqualTo( Instant.of( 1 ) ) );

		LOG.trace( "testing t+3 != " + Instant.of( 2 ) );
		assertThat( "FutureSelf#after(t) time is added to Timed#now()",
				model.after( Duration.of( 3 ) ).now(),
				not( comparesEqualTo( Instant.of( 2 ) ) ) );
	}

}
