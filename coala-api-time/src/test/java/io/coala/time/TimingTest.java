package io.coala.time;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.json.JsonUtil;

/**
 * {@link TimingTest}
 * 
 * @version $Id: ee4603c0f87c6f272e782d4892f92806db9f327c $
 * @author Rick van Krevelen
 */
public class TimingTest
{

	/** */
	private static final Logger LOG = LogManager.getLogger( TimingTest.class );

	@Test
	public void testTimingMap()
	{
		final Instant t1 = Instant.of( 1 ), t2 = Instant.of( 2 ),
				t3 = Instant.of( 3 ), t4 = Instant.of( 4 ),
				t5 = Instant.of( 5 );
		final TimingMap<String> schedule = TimingMap.Simple
				.of( Arrays.asList( t2, t4 ).stream().collect(
						Collectors.toMap( t -> t, t -> "v" + t.value() ) ) );

		assertThat( "t1 < schedule{t2,t4}: null", schedule.current( t1 ),
				nullValue() );
		LOG.info( "schedule {} yields at {}: {}", schedule, t1,
				schedule.current( t1 ) );

		assertThat( "t3 in schedule{t2,t4}: t2", schedule.current( t3 ),
				equalTo( schedule.unwrap().get( t2 ) ) );
		LOG.info( "schedule {} yields at {}: {}", schedule, t3,
				schedule.current( t3 ) );

		assertThat( "t4 > schedule{t2,t4}: >0", schedule.current( t5 ),
				equalTo( schedule.unwrap().get( t4 ) ) );
		LOG.info( "schedule {} yields at {}: {}", schedule, t5,
				schedule.current( t5 ) );
	}

	@Test
	public void testTimingMapJson() throws ParseException
	{
		final TimingMap<String> schedule = TimingMap.Simple.of( Arrays
				.asList( Instant.of( 1 ), Instant.of( 2 ) ).stream()
				.collect( Collectors.toMap( t -> t, t -> "v" + t.value() ) ) );

		// throws NullPointerException during key serialization in MapSerializer 
		final String json = JsonUtil.toJSON( schedule );
		final TimingMap<?> deser = JsonUtil.valueOf( json, TimingMap.class );
		// FIXME the toString() method on deserialized TimingMap fails somehow
		LOG.trace( "{} -ser-> {} -deser-> {}", schedule, json, deser );
		assertThat( "deser{} == unser{}", deser.unwrap(),
				equalTo( schedule.unwrap() ) );
		assertThat( "deser == unser", deser.unwrap().getClass(),
				equalTo( schedule.unwrap().getClass() ) );
	}

	@Test
	public void testCron() throws ParseException
	{
		// FIXME offsets in past are not handled (at all?)
		final String pattern = "0 0 0 14 * ? 2017";
		final OffsetDateTime offset = OffsetDateTime
				.parse( "2017-08-22T11:55:12+02:00" );
		LOG.info( "Testing Timing pattern: {} with offset: {}", pattern,
				offset );
		for( Instant t : Timing.of( pattern ).offset( offset )
				.iterate( Instant.ZERO ) )
			LOG.trace( "pattern includes t: {} == {}",
					t.prettify( TimeUnits.DAYS, 2 ), t.prettify( offset ) );
		LOG.info( "test: done" );
	}

}
