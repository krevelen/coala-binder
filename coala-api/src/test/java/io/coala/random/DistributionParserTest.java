package io.coala.random;

import static org.junit.Assert.assertNotNull;

import java.text.ParseException;

import javax.measure.quantity.Duration;
import javax.measure.unit.NonSI;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.LocalConfig;
import io.coala.random.DistributionParsable.FromString;

/**
 * {@link DistributionParserTest}
 * 
 * @version $Id: 44fed16f2368cf0e2f826585d7b9e1902919166d $
 * @author Rick van Krevelen
 */
public class DistributionParserTest
{

	/** */
	private static final Logger LOG = LogManager
			.getLogger( DistributionParserTest.class );

	enum HML
	{
		HI, Mid, lo
	}

	interface Config extends LocalConfig
	{

		String BERNOULLI_DIST_DEFAULT = " BERNOULLI (  0.5 )";

		String GAUSS_DIST_DEFAULT = "nOrMaL (10 ;.5 ) ";

		String CATEGORICAL_DIST_DEFAULT = "eNUM (hi:4 ; miD: 3 ;LO :.91 )";

		@DefaultValue( BERNOULLI_DIST_DEFAULT )
		@ConverterClass( FromString.class )
		DistributionParsable<Boolean> bernoulli();

		@DefaultValue( GAUSS_DIST_DEFAULT )
		@ConverterClass( FromString.class )
		DistributionParsable<Double> gaussAmount();

		@DefaultValue( CATEGORICAL_DIST_DEFAULT )
		@ConverterClass( FromString.class )
		DistributionParsable<HML> categoricalEnum();
	}

	@Test
	public void testParser() throws ParseException
	{
		final Config config = ConfigCache.getOrCreate( Config.class );
		LOG.info( "Starting DistributionParser test, config: {}", config );

		final ProbabilityDistribution.Parser distParser = new DistributionParser(
				DistributionFactory.instance() );
		final ProbabilityDistribution<Boolean> bernoulli = config.bernoulli()
				.parse( distParser );

		assertNotNull( "bernoulli not set", bernoulli );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "`{}` coin toss #{}: {}", config.bernoulli(), i + 1,
					bernoulli.draw() ? "heads" : "tails" );

		final ProbabilityDistribution<HML> categoricalEnum = config
				.categoricalEnum().parse( distParser, HML.class );
		assertNotNull( "categoricalEnum not set", categoricalEnum );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "`{}` #{}: {}", config.categoricalEnum(), i + 1,
					categoricalEnum.draw() );

		final AmountDistribution<Duration> gaussAmount = config.gaussAmount()
				.parse( distParser ).toAmounts( NonSI.HOUR );
		assertNotNull( "gaussAmount not set", gaussAmount );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "`{}` gaussian Amount #{}: {}", config.gaussAmount(),
					i + 1, gaussAmount.draw() );

		LOG.info( "Completed DistributionParser test" );
	}

}
