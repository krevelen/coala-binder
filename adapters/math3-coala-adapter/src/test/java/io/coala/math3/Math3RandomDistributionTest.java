/* $Id: bfadf655bd8008aafea9b9abf73eab34d7701718 $
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

import java.math.BigDecimal;

import javax.measure.DecimalMeasure;

import org.apache.logging.log4j.Logger;
import org.jscience.physics.amount.Amount;
import org.junit.Test;

import io.coala.log.LogUtil;
import io.coala.random.RandomDistribution;

/**
 * {@link Math3RandomDistributionTest}
 * 
 * @version $Id: bfadf655bd8008aafea9b9abf73eab34d7701718 $
 * @author Rick van Krevelen
 */
public class Math3RandomDistributionTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( Math3RandomDistributionTest.class );

	enum MyValue
	{
		v1, v2, v3;
	}

	@SuppressWarnings( "rawtypes" )
	@Test
	public void testParser()
	{
		final RandomDistribution.Parser parser = new RandomDistribution.Parser.Simple(
				new Math3RandomDistribution.Factory(),
				new Math3RandomNumberStream.MersenneFactory().create( "rng",
						0L ) );

//		LOG.trace( "amount {}", Amount.valueOf( 3.2, Unit.ONE ) );
		final RandomDistribution<Amount> dist0 = RandomDistribution.Util
				.valueOf( "uniform(2 ;3 )", parser, Amount.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw amount {}: {}", i, dist0.draw() );

		final RandomDistribution<BigDecimal> dist1 = RandomDistribution.Util
				.valueOf( "uniform(5.1;6.2)", parser, BigDecimal.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw decimal {}: {}", i, dist1.draw() );

		final RandomDistribution<DecimalMeasure> dist2 = RandomDistribution.Util
				.valueOf( "const(2.01 day)", parser, DecimalMeasure.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw measure {}: {}", i, dist2.draw() );

		final RandomDistribution<MyValue> dist3 = RandomDistribution.Util
				.valueOf( "uniform()", parser, MyValue.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw enum {}: {}", i, dist3.draw() );

		final RandomDistribution<MyValue> dist4 = RandomDistribution.Util
				.valueOf( "uniform( v1; v3 )", parser, MyValue.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw enum subset {}: {}", i, dist4.draw() );
	}
}
