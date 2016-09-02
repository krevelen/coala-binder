package io.coala.time;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDateTime;

import javax.measure.DecimalMeasure;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.exception.Thrower;
import io.coala.math.MeasureUtil;

/**
 * {@link TimingTest}
 * 
 * @version $Id: 44fed16f2368cf0e2f826585d7b9e1902919166d $
 * @author Rick van Krevelen
 */
public class TimingTest
{

	/** */
	private static final Logger LOG = LogManager.getLogger( TimingTest.class );

	@Test
	public void testInstant()
	{
		final DecimalMeasure<javax.measure.quantity.Duration> millis = DecimalMeasure
				.valueOf( BigDecimal.valueOf( System.currentTimeMillis() ),
						Units.MILLIS );
		assertThat( "should be equal",
				MeasureUtil.toUnit( MeasureUtil.toUnit( millis, Units.DAYS ),
						Units.MILLIS ),
				comparesEqualTo( millis ) );
	}

	@Test
	public void testCron() throws ParseException
	{
		final String pattern = "0 0 0 14 * ? 2016";
		final LocalDateTime offset = LocalDateTime
				.parse( "2016-08-22T11:55:12" );
		LOG.info( "Testing Timing pattern: {} with offset: {}", pattern,
				offset );
		Timing.of( pattern ).offset( offset ).stream().subscribe( t ->
		{
			LOG.trace( "pattern includes t: {} == {}",
					t.prettify( Units.DAYS, 2 ), t.prettify( offset ) );
		}, e ->
		{
			LOG.error( "Problem in pattern", e );
			Thrower.rethrowUnchecked( e );
		} );
		LOG.info( "test: done" );
	}

}
