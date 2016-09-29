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
import org.junit.BeforeClass;
import org.junit.Test;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.dsol3.Dsol3Scheduler;
import io.coala.exception.ExceptionStream;
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

	@SuppressWarnings( "serial" )
	@Singleton
	public static class EnterpriseModel implements Proactive
	{
//		static
//		{
//			JsonUtil.getJOM().registerModule( new SimpleModule()
//			{
//				{
//					this.addDeserializer( TestFact.class, null );
//				}
//			} );
//		}

		/**
		 * {@link TestFact} custom fact kind
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
//		@JsonDeserialize( using = TestFactDeserializer.class )
		interface TestFact extends CoordinationFact
		{
			// empty 
			static TestFact fromJSON( final String json )
			{
				return CoordinationFact.fromJSON( json, TestFact.class );
			}
		}

//		public static class TestFactDeserializer
//			extends JsonDeserializer<TestFact>
//		{
//			@Override
//			public TestFact deserialize( final JsonParser p,
//				final DeserializationContext ctxt )
//				throws IOException, JsonProcessingException
//			{
//				return CoordinationFact.fromJSON( p.readValueAsTree(),
//						TestFact.class );
//			}
//		}

		private final Scheduler scheduler;

		@Inject
		private CompositeActor.Factory organizations;

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
			final CompositeActor org1 = this.organizations.create( "org1" );
			final CompositeActor sales = org1.actor( "sales" );

			LOG.trace( "initialize business rule(s)" );
			sales.on( TestFact.class, CoordinationFactKind.REQUESTED,
					sales.id() ).subscribe( cause ->
					{
						sales.after( Duration.of( 1, NonSI.DAY ) ).call( t ->
						{
							final TestFact response = sales.respond( cause,
									CoordinationFactKind.STATED, true, null,
									Collections.singletonMap( "stParam",
											"stValue" ) );
							LOG.trace( "t={}, responded: {}", t, response );
						} );
					}, e -> LOG.error( "Problem", e ) );

//			LOG.trace( "initialize TestFact[RQ] redirect to self" );
//			org1.outgoing( TestFact.class, CoordinationFactKind.REQUESTED )
//					.subscribe( f ->
//					{
//						org1.consume( f );
//					}, e ->
//					{
//						LOG.error( "Problem redirecting TestFact", e );
//					} );

			LOG.trace( "intialize TestFact initiation" );
			atEach( Timing.valueOf( "0 0 0 14 * ? *" ).offset( offset )
					.iterate(), t ->
					{
						// spawn initial transactions with self
						final TestFact request = sales.initiate( TestFact.class,
								sales.id(), null, t.add( 1 ), Collections
										.singletonMap( "rqParam", "rqValue" ) );
						final String json = request.toJSON();
						final String fact = TestFact.fromJSON( json )
								/*
								 * .transaction()
								 */.toString();
						LOG.trace( "initiated: {} => {}", json, fact );
					} );

			LOG.trace( "initialize incoming fact sniffing" );
			org1.facts().subscribe( fact ->
			{
				LOG.trace( "t={}, fact: {}", org1.now().prettify( offset ),
						fact );
			} );

			LOG.trace( "initialize outgoing fact persistence" );
			org1.facts().subscribe( CoordinationFact::save,
					e -> LOG.error( "Problem while saving fact", e ) );

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

	@BeforeClass
	public static void listenExceptions()
	{
		ExceptionStream.asObservable().subscribe(
				t -> LOG.error( "Intercept " + t.getClass().getSimpleName() ),
				e -> LOG.error( "ExceptionStream failed", e ),
				() -> LOG.trace( "JUnit test completed" ) );
	}

	@Test
	public void testEnterpriseOntology() throws TimeoutException
	{
		LOG.trace( "Deser: ", CoordinationFact.fromJSON(
				"{" + "\"id\":\"1a990581-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"occurrence\":{},\"transaction\":{"
						+ "\"kind\":\"io.coala.enterprise.EnterpriseTest$EnterpriseModel$TestFact\""
						+ ",\"id\":\"1a990580-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"initiatorRef\":\"eoSim-org1-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"executorRef\":\"eoSim-org2-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ "}" + ",\"kind\":\"REQUESTED\",\"expiration\":{}"
						+ ",\"rqParam\":\"rqValue\"" + "}",
				EnterpriseModel.TestFact.class ) );

		// configure replication FIXME via LocalConfig?
		ConfigCache.getOrCreate( ReplicateConfig.class, Collections
				.singletonMap( ReplicateConfig.DURATION_KEY, "" + 500 ) );

		// configure tooling
		final LocalConfig config = LocalConfig.builder().withId( "eoSim" )
				.withProvider( Scheduler.class, Dsol3Scheduler.class )
				.withProvider( CompositeActor.Factory.class,
						CompositeActor.Factory.LocalCaching.class )
				.withProvider( Transaction.Factory.class,
						Transaction.Factory.LocalCaching.class )
				.withProvider( CoordinationFact.Factory.class,
						CoordinationFact.Factory.SimpleProxies.class )
				.withProvider( CoordinationFactBank.Factory.class,
						CoordinationFactBank.Factory.LocalJPA.class )
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

		for( Object f : binder.inject( CoordinationFactBank.Factory.class )
				.create().find().toBlocking().toIterable() )
			LOG.trace( "Got fact: {}", f );

		LOG.info( "completed, t={}", scheduler.now() );
	}

}
