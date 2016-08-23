package io.coala.enterprise;

import static org.aeonbits.owner.util.Collections.entry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.unit.NonSI;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.dsol3.Dsol3Config;
import io.coala.log.LogUtil;
import io.coala.time.Duration;
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

	private static final CoordinationFact.Factory factFactory = new CoordinationFact.SimpleFactory();

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
	 * @throws Exception
	 */
	private static void initScenario( final Scheduler scheduler )
		throws Exception
	{
		LOG.trace( "initializing..." );

		final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME
				.withZone( ZoneId.systemDefault() );
		final Instant offset = java.time.Instant.now()
				.truncatedTo( ChronoUnit.DAYS );
//		final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
		scheduler.time().subscribe( t ->
		{
			LOG.trace( "t={}, date: {}", t.prettify( Units.DAYS, 2 ),
					formatter.format( t.toDate( offset ) ) );
		}, e ->
		{
			LOG.error( "Time logging problem", e );
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
			LOG.trace( "t={}, outgoing: {}", org1.now().prettify( offset ),
					fact );
		} );

		org1.outgoing( TestFact.class, CoordinationFactType.REQUESTED )
				.subscribe( f ->
				{
					org1.consume( f );
				}, e ->
				{
					LOG.error( "Problem redirecting TestFact", e );
				} );

		// spawn initial transactions with self
		scheduler.schedule(
				Timing.valueOf( "0 0 0 14 * ? *" ).offset( offset ).iterate(),
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
	public void testEnterpriseOntology() throws TimeoutException
	{
		// initialize replication
		final Dsol3Config config = Dsol3Config.of(
				entry( Dsol3Config.ID_KEY, "eoTest" ),
				entry( Dsol3Config.START_TIME_KEY, "0 day" ),
				entry( Dsol3Config.RUN_LENGTH_KEY, "500" ) );
		LOG.info( "Starting signal test, config: {}", config.toYAML() );
		final Scheduler scheduler = config
				.create( EnterpriseTest::initScenario );

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

		LOG.info( "completed, t={}", scheduler.now() );
	}

}
