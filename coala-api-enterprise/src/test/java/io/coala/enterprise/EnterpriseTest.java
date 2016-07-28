package io.coala.enterprise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Test;

import io.coala.dsol3.Dsol3Scheduler;
import io.coala.enterprise.fact.CoordinationFactType;
import io.coala.log.LogUtil;
import io.coala.time.Duration;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.time.Timing;
import io.coala.time.Units;

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

	private static final CoordinationFact.Factory factFactory = CoordinationFact.Factory
			.ofSimpleProxy();

	/**
	 * {@link TestFact} custom fact kind
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public interface TestFact extends CoordinationFact
	{
		// empty 
	}

	/**
	 * @param scheduler
	 */
	private static void initScenario( final Scheduler scheduler )
	{
		LOG.trace( "initializing..." );

		final DateTime offset = DateTime.now().withTimeAtStartOfDay();
		scheduler.time().subscribe( t ->
		{
			LOG.trace( "t={} ({})", t.prettify( Units.DAYS, 2 ),
					t.toJoda( offset ) );
		}, e ->
		{
		} );

		// create organization
		final Organization org1 = Organization.of( scheduler, "org1",
				factFactory );
		final CompositeActor sales = org1.actor( "sales" );

		// add business rule(s)
		sales.on( TestFact.class, org1.id() ).subscribe( fact ->
		{
			final TestFact response = sales.createResponse( fact,
					CoordinationFactType.STATED, true, null,
					Collections.singletonMap( "myParam1", "myValue1" ) );
			LOG.trace( "{} responded: {} for incoming: {}", sales.id(),
					response, fact );
		} );

		// observe generated facts
		org1.outgoing().subscribe( fact ->
		{
			LOG.trace( "observed outgoing fact: {}", fact );
		} );

		// spawn initial transactions with self
		scheduler.schedule(
				Timing.of( "0 0 0 14 * ? *" ).asObservable( offset.toDate() ),
				t ->
				{
					final TestFact fact = sales.createRequest( TestFact.class,
							org1.id(), null, null, Collections
									.singletonMap( "myParam2", "myValue2" ) );

					// FIXME hold outgoing fact until this lambda returns?

					LOG.trace( "generated fact: {}", fact );
					org1.on( fact );
					throw new Exception(); // FIXME fail on error?
				} );

		// TODO test fact expiration handling

		// TODO test multilevel composition of business rules, e.g. via sub-goals?

		// TODO test performance statistics aggregation

		// TODO test on-the-fly adapting business rules
		// e.g. parametric: "reorder-level: 300->400" 
		// or compositional: "product-lines: a[demand push->pull]"

		// TODO test Jason or GOAL scripts for business rules

		// TODO test/implement JSON de/serialization (for UI interaction)

		// TODO test persistence (for database interaction)

		LOG.trace( "initialized!" );
	}

	@Test
	public void testDemo() throws InterruptedException
	{
		// initialize replication
		final Scheduler scheduler = Dsol3Scheduler.of( "demoTest",
				Instant.of( "5 h" ), Duration.of( "500 day" ),
				EnterpriseTest::initScenario );

		// track progress
		final CountDownLatch latch = new CountDownLatch( 1 );
		scheduler.time().subscribe( t ->
		{
			// virtual time passes...
		}, e ->
		{
			fail( e.getMessage() );
		}, () ->
		{
			latch.countDown();
		} );
		scheduler.resume();

		// await completion
		latch.await( 1, TimeUnit.SECONDS );
		assertEquals( "Scheduler not completed in time", 0, latch.getCount() );

		LOG.trace( "completed, t={}", scheduler.now() );
	}

}
