package io.coala.random;

import org.aeonbits.owner.ConfigFactory;
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
		final Config conf = ConfigFactory.create( Config.class );
		LOG.trace( "Got config: {}; name={}; seeds={}", conf, conf.id(),
				conf.seed() );
	}
}
