package io.coala.guice4;

import javax.inject.Singleton;
import javax.measure.quantity.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.InjectDist;
import io.coala.bind.LocalConfig;
import io.coala.random.AmountDistribution;
import io.coala.random.DistributionFactory;
import io.coala.random.DistributionParser;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.PseudoRandom;

/**
 * {@link InjectDistTest}
 * 
 * @version $Id: 44fed16f2368cf0e2f826585d7b9e1902919166d $
 * @author Rick van Krevelen
 */
public class InjectDistTest
{

	/** */
	private static final Logger LOG = LogManager
			.getLogger( InjectDistTest.class );

	enum HML
	{
		HI, Mid, lo
	}

	@Singleton
	public static class Model
	{
		@InjectDist( " BERNOULLI (  0.5 )" )
		private ProbabilityDistribution<Boolean> bernoulli;

		@InjectDist( value = " nOrMaL (10 ;.5 )", unit = "h" )
		private AmountDistribution<Duration> gaussAmount;

		@InjectDist( value = " eNUM (hi:4 ; miD: 3 ;LO :.91 )",
			paramType = HML.class )
		private ProbabilityDistribution<HML> categoricalEnum;
	}

	@Test
	public void testInjectDist()
	{
		final LocalConfig config = LocalConfig.builder().withId( "dsolTest" )
				.withProvider( PseudoRandom.Factory.class,
						PseudoRandom.JavaRandom.Factory.class )
				.withProvider( ProbabilityDistribution.Factory.class,
						DistributionFactory.class )
				.withProvider( ProbabilityDistribution.Parser.class,
						DistributionParser.class )
				.build();

		LOG.info( "Starting InjectDist test, config: {}", config );
		final Model model = config.createBinder().inject( Model.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "coin toss #{}: {}", i + 1,
					model.bernoulli.draw() ? "heads" : "tails" );

		for( int i = 0; i < 10; i++ )
			LOG.trace( "gauss Amount #{}: {}", i + 1,
					model.gaussAmount.draw() );

		for( int i = 0; i < 10; i++ )
			LOG.trace( "cat enum #{}: {}", i + 1,
					model.categoricalEnum.draw() );

		LOG.info( "Starting InjectDist test, config: {}", config );
	}

}
