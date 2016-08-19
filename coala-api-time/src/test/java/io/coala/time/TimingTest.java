package io.coala.time;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;

import javax.measure.DecimalMeasure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.TriggerBuilder;

import io.coala.math.MeasureUtil;
import rx.Observable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

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
	public void testCron()
	{
		final String pattern = "0 0 0 14 * ? 2016";
		final Date offset = new Date();
		final CronTrigger trigger = TriggerBuilder.newTrigger()
				.withSchedule( CronScheduleBuilder.cronSchedule( pattern ) )
				.build();
		final Iterator<Instant> it = new Iterator<Instant>()
		{
			private Date current = trigger.getFireTimeAfter( offset );

			@Override
			public boolean hasNext()
			{
				return this.current != null;
			}

			@Override
			public Instant next()
			{
				final Instant next = //this.current == null ? null :
						Instant.of( this.current.getTime() - offset.getTime(),
								SI.MILLI( SI.SECOND ) );
				this.current = trigger.getFireTimeAfter( this.current );
				return next;
			}
		};
//		while( it.hasNext() )
//			LOG.trace( "it: {}", it.next() );
		final Observable<Instant> obs = Observable.from( new Iterable<Instant>()
		{
			@Override
			public Iterator<Instant> iterator()
			{
				return it;
			}
		} );
		obs.subscribe( t ->
		{
			LOG.trace( "obs t: {}", t.prettify( NonSI.HOUR, 2 ) );
		}, e ->
		{
			LOG.trace( "obs t", e );
		}, () ->
		{
			LOG.trace( "obs t: done" );
		} );
	}

}
