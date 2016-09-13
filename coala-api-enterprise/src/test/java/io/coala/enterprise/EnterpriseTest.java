package io.coala.enterprise;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.unit.NonSI;
import javax.persistence.EntityManagerFactory;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Test;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.dsol3.Dsol3Scheduler;
import io.coala.guice4.Guice4LocalBinder;
import io.coala.log.LogUtil;
import io.coala.time.Duration;
import io.coala.time.Proactive;
import io.coala.time.ReplicateConfig;
import io.coala.time.Scheduler;
import io.coala.time.Timing;
import net.jodah.concurrentunit.Waiter;

/**
 * {@link EnterpriseTest}
 * 
 * @version $Id: 44fed16f2368cf0e2f826585d7b9e1902919166d $
 * @author Rick van Krevelen
 */
public class EnterpriseTest
{

	/** TODO specialized logging adding e.g. Timed#now() and Identified#id() */
	private static final Logger LOG = LogUtil.getLogger( EnterpriseTest.class );

	@Singleton
	public static class EnterpriseModel implements Proactive
	{

		/**
		 * {@link TestFact} custom fact kind
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		interface TestFact extends CoordinationFact
		{
			// empty 
		}

		private final Scheduler scheduler;

		@Inject
		private Organization.Factory organizations;

		@Inject
		private CoordinationFact.Persister persister;

		@Inject
		public EnterpriseModel( final Scheduler scheduler )
		{
			this.scheduler = scheduler;
			scheduler.onReset( this::initScenario );
		}

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}

		/**
		 * @param scheduler
		 * @throws Exception
		 */
		public void initScenario() throws Exception
		{
			LOG.trace( "initializing..." );

			final DateTime offset = DateTime.now().withTimeAtStartOfDay()
					.withDayOfMonth( 1 ).withMonthOfYear( 1 );

			LOG.trace( "initialize organization" );
			final Organization org1 = this.organizations.create( "org1" );
			final CompositeActor sales = org1.actor( "sales" );

			LOG.trace( "initialize business rule(s)" );
			sales.on( TestFact.class, sales.id(), fact ->
			{
				sales.after( Duration.of( 1, NonSI.DAY ) ).call( t ->
				{
					final TestFact response = sales.createResponse( fact,
							CoordinationFactType.STATED, true, null, Collections
									.singletonMap( "myParam1", "myValue1" ) );
					LOG.trace( "t={}, {} responded: {} for incoming: {}", t,
							sales.id(), response, fact );
				} );
			} );

			LOG.trace( "initialize TestFact[RQ] redirect to self" );
			org1.outgoing( TestFact.class, CoordinationFactType.REQUESTED )
					.subscribe( f ->
					{
						org1.consume( f );
					}, e ->
					{
						LOG.error( "Problem redirecting TestFact", e );
					} );

			LOG.trace( "intializeg TestFact initiation" );
			atEach( Timing.valueOf( "0 0 0 14 * ? *" ).offset( offset )
					.iterate(), t ->
					{
						// spawn initial transactions with self
						LOG.trace( "initiating TestFact" );
						sales.createRequest( TestFact.class, sales.id(), null,
								t.add( 1 ), Collections.singletonMap(
										"myParam2", "myValue2" ) );
					} );

			LOG.trace( "initialize incoming fact sniffing" );
			org1.incoming().subscribe( fact ->
			{
				LOG.trace( "t={}, incoming: {}", org1.now().prettify( offset ),
						fact );
			} );

			LOG.trace( "initialize outgoing fact persistence" );
			org1.outgoing().subscribe( fact ->
			{
				try
				{
					this.persister.save( fact );
				} catch( final Exception e )
				{
					e.printStackTrace();
				}
			} );

			// TODO test fact expiration handling

			// TODO test multilevel composition of business rules, e.g. via sub-goals?

			// TODO test performance statistics aggregation

			// TODO test on-the-fly adapting business rules
			// e.g. parametric: "reorder-level: 300->400" 
			// or compositional: "product-lines: a[demand push->pull]"

			// TODO test Jason or GOAL scripts for business rules

			// TODO test/implement JSON de/serialization (for UI interaction)

			LOG.trace( "initialized!" );
		}
	}

	@Test
	public void testEnterpriseOntology() throws TimeoutException
	{
		// configure replication FIXME via LocalConfig?
		ConfigCache.getOrCreate( ReplicateConfig.class, Collections
				.singletonMap( ReplicateConfig.DURATION_KEY, "" + 500 ) );

		// configure tooling
		final LocalConfig config = LocalConfig.builder().withId( "eoSim" )
				.withProvider( Scheduler.class, Dsol3Scheduler.class )
				.withProvider( Organization.Factory.class,
						Organization.Factory.LocalCaching.class )
				.withProvider( Transaction.Factory.class,
						Transaction.Factory.LocalCaching.class )
				.withProvider( CoordinationFact.Factory.class,
						CoordinationFact.Factory.Simple.class )
				.withProvider( CoordinationFact.Persister.class,
						CoordinationFact.Persister.SimpleJPA.class )
				.build();

		LOG.info( "Starting EO test, config: {}", config.toYAML() );
		final LocalBinder binder = Guice4LocalBinder.of( config,
				Collections.singletonMap( EntityManagerFactory.class,
						HibHikHypConfig.createEMF() ) );
		final Scheduler scheduler = binder.inject( EnterpriseModel.class )
				.scheduler();

		final Waiter waiter = new Waiter();
		scheduler.time().subscribe( time ->
		{
			// virtual time passes...
		}, error ->
		{
			waiter.rethrow( error );
		}, () ->
		{
			waiter.resume();
		} );
		scheduler.resume();
		waiter.await( 10, TimeUnit.SECONDS );

		for( Object f : binder.inject( CoordinationFact.Persister.class )
				.findAll() )
			LOG.trace( "Got fact: {}", f );

		LOG.info( "completed, t={}", scheduler.now() );
	}

}
