package io.coala.random;

import static org.junit.Assert.assertNotNull;

import java.text.ParseException;

import javax.measure.Quantity;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.LocalConfig;
import io.coala.random.DistributionParsable.FromString;

/**
 * {@link DistributionParserTest}
 * 
 * @version $Id: 1a5d7307585db8807c284829be756b25800e2760 $
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

		String GAUSS_DIST_DEFAULT = "nOrMaL (10 day;.5 h ) ";

		String CATEGORICAL_DIST_DEFAULT = "eNUM (hi:4 ; miD: 3 ;LO :.91 )";

		@DefaultValue( BERNOULLI_DIST_DEFAULT )
		@ConverterClass( FromString.class )
		DistributionParsable bernoulliBoolean();

		@DefaultValue( GAUSS_DIST_DEFAULT )
		@ConverterClass( FromString.class )
		DistributionParsable gaussQuantity();

		@DefaultValue( CATEGORICAL_DIST_DEFAULT )
		@ConverterClass( FromString.class )
		DistributionParsable categoricalEnum();
	}

	@Test
	public void testParser() throws ParseException
	{
		final Config config = ConfigCache.getOrCreate( Config.class );
		LOG.info( "Starting DistributionParser test, config: {}", config );

		final ProbabilityDistribution<Boolean> bernoulli = config
				.bernoulliBoolean().parse().ofType( Boolean.class );

		assertNotNull( "bernoulli not set", bernoulli );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "`{}` coin toss #{}: {}", config.bernoulliBoolean(),
					i + 1, bernoulli.draw() ? "heads" : "tails" );

		final ProbabilityDistribution<HML> categoricalEnum = config
				.categoricalEnum().parseAndDraw( HML.class );
		assertNotNull( "categoricalEnum not set", categoricalEnum );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "`{}` #{}: {}", config.categoricalEnum(), i + 1,
					categoricalEnum.draw() );

		@SuppressWarnings( "unchecked" )
		final QuantityDistribution<?> gaussAmount = config.gaussQuantity()
				.parseQuantity().asType( Quantity.class );
		assertNotNull( "gaussQuantity not set", gaussAmount );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "`{}` gaussian Quantity #{}: {}", config.gaussQuantity(),
					i + 1, gaussAmount.draw() );

		LOG.info( "Completed DistributionParser test" );
	}

}
