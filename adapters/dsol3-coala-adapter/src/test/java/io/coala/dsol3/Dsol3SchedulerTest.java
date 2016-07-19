package io.coala.dsol3;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.measure.unit.NonSI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.exception.ExceptionFactory;
import io.coala.time.Duration;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.time.Timed;
import io.coala.time.Timing;

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

	private void logTime( final Timed t )
	{
		LOG.trace( "logging, t={}", t.now() );
	}

	@Test
	public void testScheduler() throws InterruptedException
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

					s.schedule( Timing.valueOf( "0 0 0 14 * ? *" )// + DateTime.now().getYear() ) // 0 30 9,12,15 * * ?
							.asObservable( new Date() ), () ->
							{
								LOG.trace( "atEach handled, t={}",
										s.now().prettify( NonSI.DAY, 2 ) );
								// FIXME: should halt sim
								throw new RuntimeException();
							} ).subscribe( exp ->
							{
								LOG.trace( "atEach next: {}", exp );
							}, e ->
							{
								LOG.trace( "atEach failed, t={}",
										s.now().prettify( NonSI.DAY, 2 ), e );
								throw ExceptionFactory
										.createUnchecked( "<kill app>", e );
							}, () ->
							{
								LOG.trace( "atEach done, t={}",
										s.now().prettify( NonSI.DAY, 2 ) );
							} );

					LOG.trace( "initialized, t={}", s.now() );
				} );

		final CountDownLatch latch = new CountDownLatch( 1 );
		sched.time().subscribe( t ->
		{
			LOG.trace( "t={}", t.prettify( NonSI.DAY, 2 ) );
		}, e ->
		{
			LOG.trace( "problem, t=" + sched.now().prettify( NonSI.DAY, 2 ),
					e );
			latch.countDown();
		}, () ->
		{
			LOG.trace( "completed, t={}", sched.now() );
			latch.countDown();
		} );
		sched.resume();

		latch.await( 1, TimeUnit.SECONDS );
		assertEquals( "Scheduler not completed in time", 0, latch.getCount() );
	}

}
