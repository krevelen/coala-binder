package io.coala.enterprise;

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
import io.coala.guice4.Guice4LocalBinder;
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
	public static class EnterpriseModel implements Proactive
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
		private Actor.Factory actorFactory;

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

			final Actor<Fact> org1 = this.actorFactory.create( "org1" );
			LOG.trace( "initialized organization" );

			final DateTime offset = new DateTime(
					this.actorFactory.offset().toEpochMilli() );
			LOG.trace( "initialized occurred and expired fact sniffing" );

			org1.commits().subscribe( fact ->
			{
				LOG.trace( "t={}, occurred: {}", now().prettify( offset ),
						fact );
			}, e -> LOG.error( "Problem", e ) );

			final AtomicInteger counter = new AtomicInteger( 0 );
			final Procurement proc = org1.initiator( Sale.class,
					Procurement.class );
			final Sales sales = org1.executor( Sale.class, Sales.class );
			sales.setTotalValue( 0 );
			sales.commits( FactKind.REQUESTED ).subscribe(
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
						final Sale rq = proc
								.initiate( Sale.class, sales.id(), t.add( 1 ) )
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
	public void testEnterpriseOntology() throws TimeoutException
	{
		LOG.trace( "Deser: ", Fact.fromJSON(
				"{" + "\"id\":\"1a990581-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"occurrence\":{},\"transaction\":{"
						+ "\"kind\":\"io.coala.enterprise.EnterpriseTest$EnterpriseModel$Sale\""
						+ ",\"id\":\"1a990580-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"initiatorRef\":\"eoSim-org1-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ ",\"executorRef\":\"eoSim-org2-sales@17351a00-863a-11e6-8b9d-c47d461717bb\""
						+ "}" + ",\"kind\":\"REQUESTED\",\"expiration\":{}"
						+ ",\"rqParam\":\"123 ms\"" + "}",
				EnterpriseModel.Sale.class ) );

		// configure replication FIXME via LocalConfig?
		ConfigCache.getOrCreate( ReplicateConfig.class, Collections
				.singletonMap( ReplicateConfig.DURATION_KEY, "" + 200 ) );

		// configure tooling
		final LocalConfig config = LocalConfig.builder().withId( "world1" )
				.withProvider( Scheduler.class, Dsol3Scheduler.class )
				.withProvider( Actor.Factory.class,
						Actor.Factory.LocalCaching.class )
				.withProvider( Transaction.Factory.class,
						Transaction.Factory.LocalCaching.class )
				.withProvider( Fact.Factory.class,
						Fact.Factory.SimpleProxies.class )
				.withProvider( FactBank.Factory.class,
						FactBank.Factory.LocalJPA.class )
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
		waiter.await( 15, TimeUnit.SECONDS );

		for( Object f : binder.inject( FactBank.Factory.class ).create().find()
				.toBlocking().toIterable() )
			LOG.trace( "Fetched fact: {}, rqParam: {}", f,
					((EnterpriseModel.Sale) f).getRqParam() );

		LOG.info( "completed, t={}", scheduler.now() );
	}

}
