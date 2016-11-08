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

import javax.measure.Quantity;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.guice4.Guice4LocalBinder;
import io.coala.log.LogUtil;
import io.coala.random.DistributionParser;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.PseudoRandom;

/**
 * {@link Math3ProbabilityDistributionTest}
 * 
 * @version $Id: bfadf655bd8008aafea9b9abf73eab34d7701718 $
 * @author Rick van Krevelen
 */
public class Math3ProbabilityDistributionTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( Math3ProbabilityDistributionTest.class );

	enum MyValue
	{
		v1, v2, v3;
	}

	@SuppressWarnings( "rawtypes" )
	@Test
	public void testParser() throws Exception
	{
		final LocalBinder binder = Guice4LocalBinder.of( LocalConfig.builder()
				.withId( "testMath3" ) // use fixed id for reproducible seeding
				.withProvider( PseudoRandom.Factory.class,
						Math3PseudoRandom.MersenneTwisterFactory.class )
				.withProvider( ProbabilityDistribution.Factory.class,
						Math3ProbabilityDistribution.Factory.class )
				.build() );
		LOG.info( "Starting Math3 parser test, binder: {}", binder );
		final DistributionParser parser = binder
				.inject( DistributionParser.class );

		final ProbabilityDistribution<Quantity> dist2 = parser
				.parse( "const(2.01 day)", Quantity.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw constant {}: {}", i, dist2.draw() );

		final ProbabilityDistribution<BigDecimal> dist1 = parser
				.parse( "uniform(5.1;6.2)", BigDecimal.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw BigDecimal {}: {}", i, dist1.draw() );

//		LOG.trace( "amount {}", Amount.valueOf( 3.2, Unit.ONE ) );
		final ProbabilityDistribution<Quantity> dist0 = parser
				.parse( "uniform-enum(2 ;3 )", Quantity.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw Object {}: {}", i, dist0.draw() );

		final ProbabilityDistribution<MyValue> dist3 = parser
				.parse( "uniform-enum()", MyValue.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw MyValue {}: {}", i, dist3.draw() );

		final ProbabilityDistribution<MyValue> dist4 = parser
				.parse( "uniform-enum( v1; v3 )", MyValue.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw MyValue subset {}: {}", i, dist4.draw() );
	}
}
