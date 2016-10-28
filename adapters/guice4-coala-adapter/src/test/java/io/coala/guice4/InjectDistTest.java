package io.coala.dsol3;

import java.text.ParseException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.quantity.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.LocalConfig;
import io.coala.config.InjectConfig;
import io.coala.guice4.Guice4LocalBinder;
import io.coala.math3.Math3ProbabilityDistribution;
import io.coala.math3.Math3PseudoRandom;
import io.coala.random.AmountDistribution;
import io.coala.random.DistributionConverter;
import io.coala.random.DistributionParsable;
import io.coala.random.DistributionParser;
import io.coala.random.InjectDist;
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

	public static interface Model extends Proactive//, Dsol3Config.Initer
	{
		void init() throws Exception;

		enum HML
		{
			HI, Mid, lo
		}

		String BERNOULLI_DIST_DEFAULT = " BERNOULLI (  0.5 )";

		String GAUSS_DIST_DEFAULT = " nOrMaL (10 ;.5 )";

		String CATEGORICAL_DIST_DEFAULT = " eNUM (hi:4 ; miD: 3 ;LO :.91 )";

		interface Config extends LocalConfig
		{
			@DefaultValue( BERNOULLI_DIST_DEFAULT )
			@ConverterClass( DistributionConverter.class )
			DistributionParsable<Boolean> bernoulli();

			@DefaultValue( GAUSS_DIST_DEFAULT )
			@ConverterClass( DistributionConverter.class )
			DistributionParsable<Double> gaussAmount();

			@DefaultValue( CATEGORICAL_DIST_DEFAULT )
			@ConverterClass( DistributionConverter.class )
			DistributionParsable<HML> categoricalEnum();
		}
	}

	@Singleton
	public static class ModelImpl implements Model
	{
		@InjectConfig
		private Config config;

		private final Scheduler scheduler;

		@Inject
		private ProbabilityDistribution.Parser distParser;

//		@InjectConfig( configType = Config.class, methodName = "bernoulli" )
		@InjectDist( BERNOULLI_DIST_DEFAULT )
		private ProbabilityDistribution<Boolean> bernoulli;

		@InjectDist( value = GAUSS_DIST_DEFAULT, unit = "h" )
		private AmountDistribution<Duration> gaussAmount;

		@InjectDist( value = CATEGORICAL_DIST_DEFAULT, paramType = HML.class )
		private ProbabilityDistribution<HML> categoricalEnum;

		@Inject
		public ModelImpl( final Scheduler scheduler )
		{
			this.scheduler = scheduler;
			scheduler().onReset( this::init );
		}

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}

		@Override
		public void init() throws ParseException
		{
			if( this.bernoulli == null )
			{
				LOG.warn( "bernoulli not injected!" );
				this.bernoulli = this.config.bernoulli()
						.parse( this.distParser );
			}
			for( int i = 0; i < 10; i++ )
				LOG.trace( "coin toss #{}: {}", i + 1,
						this.bernoulli.draw() ? "heads" : "tails" );

			if( this.gaussAmount == null )
			{
				LOG.warn( "gaussAmount not injected!" );
				this.gaussAmount = this.config.gaussAmount()
						.parse( this.distParser ).toAmounts();
			}
			for( int i = 0; i < 10; i++ )
				LOG.trace( "gauss Amount #{}: {}", i + 1,
						this.gaussAmount.draw() );

			if( this.categoricalEnum == null )
			{
				LOG.warn( "categoricalEnum not injected!" );
				this.categoricalEnum = this.config.categoricalEnum()
						.parse( this.distParser );
			}
			for( int i = 0; i < 10; i++ )
				LOG.trace( "cat enum #{}: {}", i + 1,
						this.categoricalEnum.draw() );
		}
	}

	@Test //( expected = IllegalStateException.class )
	public void testScheduler() throws TimeoutException
	{
		final LocalConfig config = LocalConfig.builder().withId( "dsolTest" )
				.withProvider( Scheduler.class, Dsol3Scheduler.class )
				.withProvider( PseudoRandom.Factory.class,
						Math3PseudoRandom.MersenneTwisterFactory.class )
				.withProvider( ProbabilityDistribution.Factory.class,
						Math3ProbabilityDistribution.Factory.class )
				.withProvider( ProbabilityDistribution.Parser.class,
						DistributionParser.class )
				.build();
		LOG.info( "Starting DSOL test, config: {}", config );
		final Scheduler sched = Guice4LocalBinder.of( config )
				.inject( ModelImpl.class ).scheduler();

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
//		LOG.error( "failed: error expected, t={}", sched.now() );
	}

}
