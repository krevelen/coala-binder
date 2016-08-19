package io.coala.time;

import static org.aeonbits.owner.util.Collections.entry;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Test;

import io.coala.dsol3.Dsol3Config;
import io.coala.log.LogUtil;
import net.jodah.concurrentunit.Waiter;

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
			signal.atEach( Timing.of( "0 0 13 * * ?" ).asObservable( offset ),
					t ->
					{
						LOG.trace( "t={}, dt={}, v={}",
								t.prettify( Units.DAYS, 2 ), t.toDate( offset ),
								signal.current() );
					} );
			LOG.trace( "scheduler initialized" );
		} );

		final Waiter waiter = new Waiter();
		scheduler.time().subscribe( time ->
		{
		}, error ->
		{
			LOG.error( "error at t=" + scheduler.now(), error );
			waiter.rethrow( error );
		}, () ->
		{
			waiter.resume();
		} );
		scheduler.resume();
		waiter.await( 1, TimeUnit.SECONDS );
		LOG.info( "Signal test complete, t={}", scheduler.now() );
	}

}
