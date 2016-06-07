package io.coala.colt;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import cern.jet.random.tdouble.Gamma;
import cern.jet.random.tdouble.Normal;
import cern.jet.random.tdouble.engine.DoubleMersenneTwister;
import cern.jet.random.tdouble.engine.DoubleRandomEngine;

/**
 * {@link ColtProbabilityDistributionTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class ColtProbabilityDistributionTest
{
	/** */
	private static final Logger LOG = LogManager
			.getLogger( ColtProbabilityDistributionTest.class );

	/**
	 * Gibbs sampler for bivariate
	 * <code>f(x,y) = kx<sup>2</sup>exp(-xy<sup>2</sup>-y<sup>2</sup>+2y-4x)</code>
	 * with <code>k>0</code> by <a href=
	 * "https://darrenjw.wordpress.com/2011/07/16/gibbs-sampler-in-various-languages-revisited/">
	 * dr. Darren J. Wilkinson</a>
	 */
	@Test
	public void testGibbsSampling()
	{
		int N = 50000;
		int thin = 1000;
		DoubleRandomEngine rngEngine = new DoubleMersenneTwister( new Date() );
		Normal rngN = new Normal( 0.0, 1.0, rngEngine );
		Gamma rngG = new Gamma( 1.0, 1.0, rngEngine );
		double x = 0;
		double y = 0;
		LOG.trace( "Iter x y" );
		for( int i = 0; i < N; i++ )
		{
			for( int j = 0; j < thin; j++ )
			{
				x = rngG.nextDouble( 3.0, y * y + 4 );
				y = rngN.nextDouble( 1.0 / (x + 1),
						1.0 / Math.sqrt( 2 * x + 2 ) );
			}
			LOG.trace( i + " " + x + " " + y );
		}
	}
}
