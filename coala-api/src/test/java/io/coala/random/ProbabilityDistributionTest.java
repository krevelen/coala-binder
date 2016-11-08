package io.coala.random;

import java.math.BigDecimal;

import javax.measure.Quantity;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;

public class ProbabilityDistributionTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( ProbabilityDistributionTest.class );

	enum MyValue
	{
		v1, v2, v3;
	}

	@SuppressWarnings( "rawtypes" )
	@Test
	public void testParser() throws Exception
	{
//		LOG.trace( "amount {}", Amount.valueOf( 3.2, Unit.ONE ) );
		final DistributionParser parser = new DistributionParser( null );

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
