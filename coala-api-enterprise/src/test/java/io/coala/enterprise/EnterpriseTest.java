package io.coala.enterprise;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.unit.NonSI;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Test;

import io.coala.dsol3.Dsol3Scheduler;
import io.coala.log.LogUtil;
import io.coala.time.Duration;
import io.coala.time.Instant;
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
			LOG.trace( "t={}, date: {}", t.prettify( Units.DAYS, 2 ),
					t.toDate( offset ) );
		} );

		// create organization
		final Organization org1 = Organization.of( scheduler, "org1",
				factFactory );
		final CompositeActor sales = org1.actor( "sales" );

		// add business rule(s)
		sales.on( TestFact.class, org1.id(), fact ->
		{
			sales.after( Duration.of( 1, NonSI.DAY ) ).call( t ->
			{
				final TestFact response = sales.createResponse( fact,
						CoordinationFactType.STATED, true, null,
						Collections.singletonMap( "myParam1", "myValue1" ) );
				LOG.trace( "t={}, {} responded: {} for incoming: {}", t,
						sales.id(), response, fact );

			} );
		} );

		// observe generated facts
		org1.outgoing().subscribe( fact ->
		{
			LOG.trace( "t={}, outgoing: {}", org1.now(), fact );
		} );

		org1.outgoing( TestFact.class, CoordinationFactType.REQUESTED )
				.doOnNext( f ->
				{
					org1.consume( f );
				} ).subscribe();

		// spawn initial transactions with self
		scheduler.schedule(
				Timing.of( "0 0 0 14 * ? *" ).asObservable( offset.toDate() ),
				t ->
				{
					sales.createRequest( TestFact.class, org1.id(), null,
							t.add( 1 ), Collections.singletonMap( "myParam2",
									"myValue2" ) );
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
	public void testDemo() throws TimeoutException
	{
		// initialize replication
		final Scheduler scheduler = Dsol3Scheduler.of( "demoTest",
				Instant.of( "5 h" ), Duration.of( "500 day" ),
				EnterpriseTest::initScenario );

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
		waiter.await( 1, TimeUnit.SECONDS );

		LOG.trace( "completed, t={}", scheduler.now() );
	}

}
