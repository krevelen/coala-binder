package io.coala.time;

import static org.aeonbits.owner.util.Collections.entry;

import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Test;

import io.coala.dsol3.Dsol3Config;
import io.coala.log.LogUtil;

public class SignalTest
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( SignalTest.class );

	@Test
	public void testSignal() throws TimeoutException
	{
		final Dsol3Config config = Dsol3Config
				.of( entry( Dsol3Config.ID_KEY, "signalTest" ) );
		LOG.info( "Starting signal test, config: {}", config.toYAML() );
		final Scheduler scheduler = config.create( s ->
		{
			final DateTime offset = DateTime.now().withTimeAtStartOfDay();
			final Signal<BigDecimal> signal = Signal.Simple.of( s,
					BigDecimal.valueOf( 1.2 ) );
			signal.atEach(
					Timing.of( "0 0 13 * * ?" ).offset( offset ).iterate(), t ->
					{
						LOG.trace( "t={}, dt={} (1pm), const={}",
								t.prettify( TimeUnits.DAYS, 2 ), t.toDate( offset ),
								signal.current() );
					} );
			LOG.trace( "scheduler initialized" );
		} );

		scheduler.run();
		LOG.info( "Signal test complete, t={}", scheduler.now() );
	}

}
