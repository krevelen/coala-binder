package io.coala.enterprise;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;
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
import io.coala.log.LogUtil;
import io.coala.time.Duration;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.ReplicateConfig;
import io.coala.time.Scheduler;
import io.coala.time.Timing;
import io.coala.time.Units;
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
	public static class World implements Proactive
	{
		public interface Sales extends Actor<Sale>
		{
			Integer getTotalValue();

			void setTotalValue( Integer totalValue );

			default void addToTotal( Integer increment )
			{
				setTotalValue( getTotalValue() + increment );
			}
		}

		public interface Procurement extends Actor<Sale>
		{
			Integer getTotalValue();

			void setTotalValue( Integer totalValue );

			default void addToTotal( Integer increment )
			{
				setTotalValue( getTotalValue() + increment );
			}
		}

		/**
		 * {@link Sale} custom fact kind
		 */
		public interface Sale extends Fact
		{
			Instant getRqParam(); // get "rqParam" bean property

			void setRqParam( Instant value ); // set "rqParam" bean property

			default Sale withRqParam( Instant value ) // test default method
			{
				setRqParam( value );
				return this;
			}

			String getStParam(); // get "stParam" bean property

			static Sale fromJSON( final String json ) // test de/serialization
			{
				return Fact.fromJSON( json, Sale.class );
			}
		}

		private final Scheduler scheduler;

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
					this.actors.offset().toEpochMilli() );
			LOG.trace( "initialized occurred and expired fact sniffing" );

			org1.emit().subscribe( fact ->
			{
				LOG.trace( "t={}, occurred: {}", now().prettify( offset ),
						fact );
			}, e -> LOG.error( "Problem", e ) );

			final AtomicInteger counter = new AtomicInteger( 0 );
			final Procurement proc = org1.specialist( Procurement.class );
			final Sales sales = org1.specialist( Sales.class );
			sales.setTotalValue( 0 );
			sales.emit( FactKind.REQUESTED ).subscribe(
					rq -> after( Duration.of( 1, Units.DAYS ) ).call( t ->
					{
						final Sale st = sales.respond( rq, FactKind.STATED )
								.with( "stParam",
										"stValue" + counter.getAndIncrement() );
						sales.addToTotal( 1 );
						LOG.trace( "{} responds: {} <- {}, total now: {}",
								sales.id(), st.causeRef().prettyHash(),
								st.getStParam(), sales.getTotalValue() );
						st.commit( true );
					} ), e -> LOG.error( "Problem", e ),
					() -> LOG.trace( "sales/rq completed?" ) );
			LOG.trace( "initialized business rule(s)" );

			atEach( Timing.valueOf( "0 0 0 30 * ? *" ).offset( offset )
					.iterate(), t ->
					{
						// spawn initial transactions from/with self
						final Sale rq = proc.initiate( sales.id(), t.add( 1 ) )
								.withRqParam( t );

						// de/serialization test
						final String json = rq.toJSON();
						final String fact = Sale.fromJSON( json ).toString();
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

	@Test
	public void testEnterpriseOntology()
		throws TimeoutException, IOException, InterruptedException
	{
		LOG.trace( "Deser: ",
				Fact.fromJSON( "{"
						+ "\"id\":\"1a990581-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"occurrence\":{},\"transaction\":{"
						+ "\"kind\":\"io.coala.enterprise.EnterpriseTest$World$Sale\""
						+ ",\"id\":\"1a990580-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"initiatorRef\":\"eoSim-org1-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"executorRef\":\"eoSim-org2-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ "}" + ",\"kind\":\"REQUESTED\",\"expiration\":{}"
						+ ",\"rqParam\":\"123 ms\"" + "}", World.Sale.class ) );

		// configure replication FIXME via LocalConfig?
		ConfigCache.getOrCreate( ReplicateConfig.class, Collections
				.singletonMap( ReplicateConfig.DURATION_KEY, "" + 200 ) );

		// configure tooling
		final LocalBinder binder = LocalConfig.builder().withId( "world1" )
				.withProvider( Scheduler.class, Dsol3Scheduler.class )
				.withProvider( Actor.Factory.class,
						Actor.Factory.LocalCaching.class )
				.withProvider( Transaction.Factory.class,
						Transaction.Factory.LocalCaching.class )
				.withProvider( Fact.Factory.class,
						Fact.Factory.SimpleProxies.class )
				.withProvider( FactBank.Factory.class,
						FactBank.Factory.LocalJPA.class )
				.build()
//		final LocalBinder binder = LocalConfig
//				.openYAML( "world1.yaml", "my-world" )
				.create( Collections.singletonMap( EntityManagerFactory.class,
						HibHikHypConfig.createEMF() ) );

		LOG.info( "Starting EO test, config: {}", binder );
		final Scheduler scheduler = binder.inject( World.class ).scheduler();

		final Waiter waiter = new Waiter();
		scheduler.time().subscribe( time ->
		{
			// virtual time passes...
		}, waiter::rethrow, waiter::resume );
		scheduler.resume();
		waiter.await( 15, TimeUnit.SECONDS );

//		CountDownLatch latch = new CountDownLatch( 1 );
//		World world = binder.inject( World.class );
//		world.scheduler().time().subscribe(
//				t -> System.out
//						.println( "t=" + t.prettify( world.actors.offset() ) ),
//				Thrower::rethrowUnchecked, latch::countDown );
//		world.scheduler().resume();
//		latch.await();
//		System.out.println( "End reached!" );

		for( Object f : binder.inject( FactBank.Factory.class ).create().find()
				.toBlocking().toIterable() )
			LOG.trace( "Fetched fact: {}, rqParam: {}", f,
					((World.Sale) f).getRqParam() );

		LOG.info( "completed, t={}", scheduler.now() );
	}

}
