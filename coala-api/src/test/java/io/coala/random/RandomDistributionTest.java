package io.coala.random;

import javax.measure.DecimalMeasure;

import org.apache.logging.log4j.Logger;
import org.jscience.physics.amount.Amount;
import org.junit.Test;

import io.coala.log.LogUtil;

public class RandomDistributionTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( ProbabilityDistribution.class );

	@SuppressWarnings( "rawtypes" )
	@Test
	public void testValueOf() throws Exception
	{
//		LOG.trace( "amount {}", Amount.valueOf( 3.2, Unit.ONE ) );
		final ProbabilityDistribution.Parser parser = new ProbabilityDistribution.Parser.Simple();

		final ProbabilityDistribution<Amount> dist1 = ProbabilityDistribution.Util
				.valueOf( "uniform(2 ;3 )", parser, Amount.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw amount {}: {}", i, dist1.draw() );

		final ProbabilityDistribution<DecimalMeasure> dist2 = ProbabilityDistribution.Util
				.valueOf( "const(2.01 day)", parser, DecimalMeasure.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw measure {}: {}", i, dist2.draw() );
	}

}
