package io.coala.dsol3;

import static org.aeonbits.owner.util.Collections.entry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.guice4.Guice4LocalBinder;
import io.coala.math3.Math3ProbabilityDistribution;
import io.coala.math3.Math3PseudoRandom;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.PseudoRandom;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import io.coala.time.Units;
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

	public static interface Model extends Proactive
	{
		void init( Scheduler scheduler );
	}

	public static class ModelImpl implements Model
	{
		@Inject
		private Scheduler scheduler;

		@Inject
		@Named
		private PseudoRandom rng;

		@Override
		public void init( final Scheduler scheduler )
		{
			this.scheduler = scheduler;
		}

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}
	}

	@Test //( expected = IllegalStateException.class )
	public void testScheduler() throws TimeoutException
	{
		final LocalBinder binder = Guice4LocalBinder.of( LocalConfig.builder()
				.withId( "dsolTest" )
				.withProvider( Scheduler.class, Dsol3Scheduler.class )
				.withProvider( PseudoRandom.Factory.class,
						Math3PseudoRandom.MersenneTwisterFactory.class )
				.withProvider( ProbabilityDistribution.Factory.class,
						Math3ProbabilityDistribution.Factory.class )
				.build() );
		final Dsol3Config config = Dsol3Config.of(
				entry( Dsol3Config.ID_KEY, "dsolTest" ),
				entry( Dsol3Config.RUN_LENGTH_KEY, "500" ) );
		LOG.info( "Starting DSOL test, config: {}", config );
		final Scheduler sched = binder.inject( Model.class ).scheduler();

		final Waiter waiter = new Waiter();
		sched.time().subscribe( time ->
		{
			LOG.trace( "t={}", time.prettify( Units.DAYS, 2 ) );
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
