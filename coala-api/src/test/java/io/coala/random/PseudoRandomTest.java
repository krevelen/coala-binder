package io.coala.random;

import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.util.Collections;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;
import io.coala.random.PseudoRandom.Config;

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

	@Test
	public void testConfig()
	{
		LOG.trace( "Testing {}", Config.class );
		final long seed = 3L;
		final Config conf = ConfigFactory.create( Config.class,
				Collections.map( Config.SEED_KEY, Long.toString( seed ) ) );
		LOG.trace( "Got config: {}; name={}; seed={}", conf, conf.id(),
				conf.seed() );
		final PseudoRandom rnd = JavaRandom.Factory.instance().create( conf );
		LOG.trace( "Next BigInteger: {}", rnd.nextBigInteger() );
		LOG.trace( "Next BigDecimal: {}", rnd.nextBigDecimal() );
	}
}
