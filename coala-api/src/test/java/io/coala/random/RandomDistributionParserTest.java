package io.coala.random;

import javax.measure.DecimalMeasure;

import org.apache.logging.log4j.Logger;
import org.jscience.physics.amount.Amount;
import org.junit.Test;

import io.coala.log.LogUtil;

public class RandomDistributionParserTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( ProbabilityDistribution.class );

	@SuppressWarnings( "rawtypes" )
	@Test
	public void testValueOf() throws Exception
	{
//		LOG.trace( "amount {}", Amount.valueOf( 3.2, Unit.ONE ) );
		final ProbabilityDistribution.Parser parser = new ProbabilityDistribution.Parser(
				null );

		final ProbabilityDistribution<Amount> dist1 = parser
				.parse( "uniform(2 ;3 )", Amount.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw amount {}: {}", i, dist1.draw() );

		final ProbabilityDistribution<DecimalMeasure> dist2 = parser
				.parse( "const(2.01 day)", DecimalMeasure.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw measure {}: {}", i, dist2.draw() );
	}

}