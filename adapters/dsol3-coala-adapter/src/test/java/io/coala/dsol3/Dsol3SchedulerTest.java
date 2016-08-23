package io.coala.dsol3;

import static org.aeonbits.owner.util.Collections.entry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.unit.NonSI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.exception.Thrower;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import io.coala.time.Timing;
import io.coala.util.Compare;
import net.jodah.concurrentunit.Waiter;

/**
 * {@link Dsol3SchedulerTest}
 * 
 * @version $Id: 44fed16f2368cf0e2f826585d7b9e1902919166d $
 * @author Rick van Krevelen
 */
public class Dsol3SchedulerTest
{

	/** */
	private static final Logger LOG = LogManager
			.getLogger( Dsol3SchedulerTest.class );

	private void logTime( final Proactive t )
	{
		LOG.trace( "logging, t={}", t.now() );
	}

	@Test( expected = IllegalStateException.class )
	public void testScheduler() throws TimeoutException
	{
		final Dsol3Config config = Dsol3Config.of(
				entry( Dsol3Config.ID_KEY, "dsolTest" ),
				entry( Dsol3Config.RUN_LENGTH_KEY, "500" ) );
		LOG.info( "Starting DSOL test, config: {}", config );
		final Scheduler sched = config.create( s ->
		{
			// initialize the model
			s.at( s.now().add( 1 ) ).call( this::logTime, s );
			s.at( s.now().add( 2 ) ).call( this::logTime, s );
			s.at( s.now().add( 2 ) ).call( this::logTime, s );

			final Instant throwTime = Instant.of( 200, NonSI.DAY );
			s.schedule( Timing.stream( "0 0 0 14 * ? *" ), t ->
			{
				LOG.trace( "atEach handled, t={}", t.prettify( NonSI.DAY, 2 ) );

				// generate scheduled error
				if( Compare.ge( t, throwTime ) )
					Thrower.throwNew( IllegalStateException.class,
							"Throwing beyond t={}", throwTime );
			} ).subscribe( exp ->
			{
				LOG.trace( "atEach next: {}", exp );
			}, e ->
			{
				LOG.warn( "atEach failed, t={}",
						s.now().prettify( NonSI.DAY, 2 ) );
			}, () ->
			{
				LOG.trace( "atEach done, t={}",
						s.now().prettify( NonSI.DAY, 2 ) );
			} );

			LOG.trace( "initialized, t={}", s.now() );
		} );

		final Waiter waiter = new Waiter();
		sched.time().subscribe( time ->
		{
			LOG.trace( "t={}", time.prettify( NonSI.DAY, 2 ) );
		}, error ->
		{
			LOG.error( "error at t=" + sched.now(), error );
			waiter.rethrow( error );
		}, () ->
		{
			waiter.resume();
		} );
		sched.resume();
		waiter.await( 1, TimeUnit.SECONDS );
		LOG.error( "failed: error expected, t={}", sched.now() );
	}

}
