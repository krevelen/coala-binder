package io.coala.enterprise;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;
import org.hibernate.cfg.AvailableSettings;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.dsol3.Dsol3Scheduler;
import io.coala.exception.ExceptionStream;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.persist.HikariHibernateJPAConfig;
import io.coala.time.Duration;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import io.coala.time.SchedulerConfig;
import io.coala.time.TimeUnits;
import io.coala.time.Timing;
import io.coala.util.ReflectUtil;

/**
 * {@link EnterpriseTest}
 * 
 * TODO specialized logging adding e.g. Timed#now() and Identified#id()
 * 
 * @version $Id: 51002a696ffbf1ab847b1ea28bd553c408d26a02 $
 * @author Rick van Krevelen
 */
public class EnterpriseTest
{
	private static final Logger LOG = LogUtil.getLogger( EnterpriseTest.class );

	public interface MyJPAConfig extends HikariHibernateJPAConfig
	{
		@DefaultValue( "fact_test_pu" ) // match persistence.xml
		@Key( JPA_UNIT_NAMES_KEY )
		String[] jpaUnitNames();

//		@DefaultValue( "jdbc:mysql://localhost/testdb" )
//		@DefaultValue( "jdbc:neo4j:bolt://192.168.99.100:7687/db/data" )
//		@DefaultValue( "jdbc:hsqldb:file:target/testdb" )
		@DefaultValue( "jdbc:hsqldb:mem:mymemdb" )
		@Key( AvailableSettings.URL )
		URI jdbcUrl();
	}

	/**
	 * {@link Valuable} super-interface to test {@link ReflectUtil#invokeAsBean}
	 * coping with http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4275879
	 */
	public interface Valuable
	{
		/**
		 * test {@link ReflectUtil#invokeAsBean} called by {@link Actor#proxyAs}
		 */
		Integer getTotalValue();

		/**
		 * test {@link ReflectUtil#invokeAsBean} called by {@link Actor#proxyAs}
		 */
		void setTotalValue( Integer totalValue );

		/**
		 * test {@link ReflectUtil#invokeDefaultMethod} called by
		 * {@link Actor#proxyAs}
		 */
		default void addToTotal( Integer increment )
		{
			setTotalValue( getTotalValue() + increment );
		}
	}

//	@SuppressWarnings( "serial" )
	@Singleton
	public static class World implements Proactive
	{
		public interface Sales extends Actor<Sale>, Valuable
		{
		}

		public interface Procurement extends Actor<Sale>, Valuable
		{
		}

		/**
		 * {@link Sale} custom fact kind
		 */
		public interface Sale extends Fact
		{
			@JsonIgnore
			Instant getRqParam(); // get "rqParam" bean property

			void setRqParam( Instant value ); // set "rqParam" bean property

			default Sale withRqParam( Instant value ) // test default method
			{
				setRqParam( value );
				return this;
			}

			@JsonIgnore
			String getStParam(); // get "stParam" bean property

			static Sale fromJSON( final String json ) // test de/serialization
			{
				return Fact.fromJSON( json, Sale.class );
			}
		}

		private final Scheduler scheduler;

		@Inject
		private LocalBinder binder;

		@Inject
		private Actor.Factory actors;

		@Inject
		public World( final Scheduler scheduler )
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

			final Actor<Fact> org1 = this.actors.create( "org1" );
			LOG.trace( "initialized organization" );

			final DateTime offset = new DateTime(
					scheduler().offset().toInstant().toEpochMilli() );
			LOG.trace( "initialized occurred and expired fact sniffing" );

			org1.emitFacts().subscribe( fact ->
			{
				LOG.trace( "t={}, occurred: {}", now().prettify( offset ),
						fact );
			}, e -> LOG.error( "Problem", e ) );

			final AtomicInteger counter = new AtomicInteger( 0 );
			final Procurement proc = org1.subRole( Procurement.class );
			final Sales sales = org1.subRole( Sales.class );
			sales.setTotalValue( 0 );
			sales.emit( FactKind.REQUESTED ).subscribe(
					rq -> after( Duration.of( 1, TimeUnits.DAYS ) ).call( t ->
					{
						final Sale st = sales.respond( rq, FactKind.STATED )
								.with( "stParam",
										"stValue" + counter.getAndIncrement() )
								.typed();
						sales.addToTotal( 1 );
						LOG.trace( "{} responds: {} <- {}, total now: {}",
								sales.id(), st.causeRef().prettyHash(),
								st.getStParam(), sales.getTotalValue() );
						st.commit( true );
					} ), e -> LOG.error( "Problem", e ),
					() -> LOG.trace( "sales/rq completed?" ) );
			LOG.trace( "initialized business rule(s)" );

			atEach( Timing.valueOf( "0 0 0 30 * ? *" ).iterate( scheduler() ),
					t ->
					{
						// spawn initial transactions from/with self
						final Sale rq = proc.initiate( sales.id(), t.add( 1 ) )
								.withRqParam( t );

						// de/serialization test
						final String json = rq.toJSON();
						LOG.trace( "de/serializing: {} as {} in {}", t,
								JsonUtil.stringify( t ), json );
						final String fact = this.binder
								.injectMembers( // FIXME 
										Sale.fromJSON( json ).transaction() )
								.toString();
						LOG.trace( "{} initiates: {} => {}", proc.id(), json,
								fact );
						rq.commit();
					} );
			LOG.trace( "intialized TestFact initiation" );

			// TODO test fact expiration handling

			// TODO test multilevel composition of business rules, e.g. via sub-goals?

			// TODO test performance statistics aggregation

			// TODO test on-the-fly adapting business rules
			// e.g. parametric: "reorder-level: 300->400" 
			// or compositional: "product-lines: a[demand push->pull]"

			// TODO test Jason or GOAL scripts for business rules

			LOG.trace( "initialization complete!" );
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

	@Ignore // FIXME inject tx.scheduler with offset for occured() in toString()
	@Test
	public void testFactDeser()
	{
		LOG.trace( "Deser: {}",
				World.Sale.fromJSON( "{"
						+ "\"id\":\"1a990581-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"occurrence\":{},\"transaction\":{"
						+ "\"kind\":\"io.coala.enterprise.EnterpriseTest$World$Sale\""
						+ ",\"id\":\"1a990580-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"initiatorRef\":\"eoSim-org1-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"executorRef\":\"eoSim-org2-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ "}" + ",\"kind\":\"REQUESTED\",\"expiration\":{}"
						+ ",\"rqParam\":\"123 ms\"" + "}" ) );
	}

	@Test
	public void testEnterpriseOntology()
		throws TimeoutException, IOException, InterruptedException
	{
		// configure replication FIXME via LocalConfig?
		ConfigCache.getOrCreate( SchedulerConfig.class, Collections
				.singletonMap( SchedulerConfig.DURATION_KEY, "" + 200 ) );

		// configure tooling
		final LocalBinder binder = LocalConfig.builder().withId( "world1" )
				.withProvider( Scheduler.class, Dsol3Scheduler.class )
				.withProvider( Actor.Factory.class,
						Actor.Factory.LocalCaching.class )
				.withProvider( Transaction.Factory.class,
						Transaction.Factory.LocalCaching.class )
				.withProvider( Fact.Factory.class,
						Fact.Factory.SimpleProxies.class )
				.withProvider( FactBank.class, FactBank.SimpleDrain.class )
				.withProvider( FactExchange.class,
						FactExchange.SimpleBus.class )
				.build()
//		final LocalBinder binder = LocalConfig
//				.openYAML( "world1.yaml", "my-world" )
				.createBinder(
//						Collections.singletonMap( EntityManagerFactory.class, 
//								ConfigFactory.create( MyJPAConfig.class ).createEMF() )
		);

		LOG.info( "Starting EO test, config: {}", binder );
		final Scheduler scheduler = binder.inject( World.class ).scheduler();

		scheduler.run();

//		CountDownLatch latch = new CountDownLatch( 1 );
//		World world = binder.inject( World.class );
//		world.scheduler().time().subscribe(
//				t -> System.out
//						.println( "t=" + t.prettify( world.actors.offset() ) ),
//				Thrower::rethrowUnchecked, latch::countDown );
//		world.scheduler().resume();
//		latch.await();
//		System.out.println( "End reached!" );

		for( Object f : binder.inject( FactBank.class ).find()
				.blockingIterable() )
			LOG.trace( "Fetched fact: {}, rqParam: {}", f,
					((World.Sale) f).getRqParam() );

		LOG.info( "completed, t={}", scheduler.now() );
	}

}