package io.coala.dsol3;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.unit.NonSI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.exception.ExceptionFactory;
import io.coala.function.ThrowableUtil;
import io.coala.time.Duration;
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
		final Instant h5 = Instant.of( "5 h" );
		LOG.trace( "start t={}", h5 );
		final Scheduler sched = Dsol3Scheduler.of( "dsol3Test", h5,
				Duration.of( "500 day" ), s ->
				{
					// initialize the model
					s.at( h5.add( 1 ) ).call( this::logTime, s );
					s.at( h5.add( 2 ) ).call( this::logTime, s );
					s.at( h5.add( 2 ) ).call( this::logTime, s );

					final Instant throwTime = Instant.of( 200, NonSI.DAY );
					s.schedule( Timing.valueOf( "0 0 0 14 * ? *" )
							.asObservable( new Date() ), t ->
							{
								LOG.trace( "atEach handled, t={}",
										t.prettify( NonSI.DAY, 2 ) );

								if( Compare.ge( t, throwTime ) )
									ExceptionFactory.throwNew(
											IllegalStateException.class,
											"Throwing beyond t={}", throwTime );
							} ).subscribe( exp ->
							{
								LOG.trace( "atEach next: {}", exp );
							}, e ->
							{
								LOG.trace( "atEach failed, t={}",
										s.now().prettify( NonSI.DAY, 2 ), e );
								ThrowableUtil.throwAsUnchecked( e );
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
			waiter.rethrow( error );
		}, () ->
		{
			waiter.resume();
		} );
		sched.resume();
		waiter.await( 1, TimeUnit.SECONDS );
	}

}
