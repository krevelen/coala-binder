/* $Id: cf21c9c9cd4ab73588c022edc57859672280f1b9 $
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
package io.coala.math3;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;
import io.coala.random.RandomDistribution;
import io.coala.random.RandomNumberStream;

/**
 * {@link Math3RandomDistributionTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Math3RandomDistributionTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( Math3RandomDistributionTest.class );

	@Test
	public void testDist()
	{
		final RandomNumberStream rng = new Math3RandomNumberStream.Well19937cFactory()
				.create( "rng", 0L );

		final RandomDistribution<Double> dist = new Math3RandomDistribution.Factory()
				.getUniformReal( rng, 1.1, 2.1 );

		for( int i = 0; i < 100; i++ )
			LOG.trace( "Next draw " + i + ": " + dist.draw() );
	}
}
