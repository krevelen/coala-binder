package io.coala.random;

import java.math.BigInteger;

import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Mutable;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;
import io.coala.random.PseudoRandom.JavaRandom;

/**
 * {@link PseudoRandomTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class PseudoRandomTest
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( PseudoRandomTest.class );

	private interface Config extends PseudoRandom.Config, Mutable
	{

	}

	@Test
	public void testConfig()
	{
		final Config conf = ConfigFactory.create( Config.class );
		LOG.info( "Testing {}, defaults: {}", Config.class.getSimpleName(),
				conf );
		for( int i = 0; i < 10; i++ )
		{
			final BigInteger seed = conf.seed();
			LOG.trace( "Got seed={}, long: {}, int: {}", seed, seed.longValue(),
					seed.intValue() );
		}
		final long seed = 3L;
		conf.setProperty( Config.SEED_KEY, Long.toString( seed ) );
		LOG.trace( "Got seed={}", conf.seed() );
		LOG.trace( "Got seed={}", conf.seed() );
		LOG.trace( "Got seed={}", conf.seed() );
		LOG.trace( "Got seed={}", conf.seed() );
		final PseudoRandom rnd = JavaRandom.Factory.instance().create( conf );
		LOG.trace( "Next BigInteger: {}", rnd.nextBigInteger() );
		LOG.trace( "Next BigDecimal: {}", rnd.nextBigDecimal() );
	}
}
