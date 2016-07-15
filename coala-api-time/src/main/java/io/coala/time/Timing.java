/* $Id: e10547851c245342c4636ba562faddb4efc7f5e5 $
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.coala.time;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.measure.DecimalMeasure;
import javax.measure.unit.SI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.TriggerBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.ical.compat.jodatime.DateTimeIteratorFactory;

import io.coala.exception.ExceptionFactory;
import io.coala.json.Wrapper;
import rx.Observable;
import rx.functions.Func1;

/**
 * {@link Timing} wraps a {@link String} representation of some calendar-based
 * pattern or period since the EPOCH, like:
 * <ul>
 * <li>a {@link CronExpression} (e.g. {@code "0 0 0 14 * ? *"} for
 * <em>&ldquo;midnight of every 14th day
 *            of the month&rdquo;</em> );</li>
 * <li>a {@link DateTimeIteratorFactory iCal RRULE or RDATE} pattern (e.g.
 * {@code "DTSTART;TZID=US-Eastern:19970902T090000\r\nRRULE:FREQ=DAILY;UNTIL=20130430T083000Z;INTERVAL=1;"}
 * );</li>
 * <li>a relative {@link Instant#of(String) period or duration} (e.g.
 * {@code "P2DT3H4M"} or {@code "27.5 µs"} );</li>
 * </ul>
 * 
 * @version $Id: e10547851c245342c4636ba562faddb4efc7f5e5 $
 * @author Rick van Krevelen
 */
// FIXME use generic Wrapper de/serializers ?
@JsonSerialize( using = Timing.JsonSerializer.class )
@JsonDeserialize( using = Timing.JsonDeserializer.class )
public class Timing extends Wrapper.Simple<String>
{

	/** */
	private static final Logger LOG = LogManager.getLogger( Timing.class );

	/** */
	private static final Pattern dtStartTimePattern = Pattern
			.compile( "DTSTART[.]*:(.*)" );

	/** */
	private static final Pattern dtStartZonePattern = Pattern
			.compile( "TZID=([^:;]*)" );

	/**
	 * @return
	 */
	@JsonIgnore
	public Observable<Instant> asObservable( final Date offset )
	{
		final String pattern = unwrap();
		try
		{
//			final MutableTrigger trigger = CronScheduleBuilder.cronSchedule( pattern ).build();

			// example: "0/20 * * * * ?"
			final CronTrigger trigger = TriggerBuilder.newTrigger()
					.withSchedule( CronScheduleBuilder.cronSchedule( pattern ) )
					.build();
			return Observable.from( new Iterable<Instant>()
			{
				@Override
				public Iterator<Instant> iterator()
				{
					return new Iterator<Instant>()
					{
						private Date current = trigger
								.getFireTimeAfter( offset );

						@Override
						public boolean hasNext()
						{
							return this.current != null;
						}

						@Override
						public Instant next()
						{
							final long millis = this.current.getTime()
									- offset.getTime();

							this.current = trigger
									.getFireTimeAfter( this.current );
							return Instant.of( DecimalMeasure
									.valueOf( millis, SI.MILLI( SI.SECOND ) )
									.to( Units.DAYS ) );
						}
					};
				}
			} );
		} catch( final Exception e )
		{
			try
			{
				final boolean strict = false;
				// String ical =
				// "DTSTART;TZID=US-Eastern:19970902T090000\r\n"
				// + "RRULE:FREQ=DAILY;"
				// + "UNTIL=20130430T083000Z;"
				// + "INTERVAL=1;";

				// parse DTSTART (see
				// http://www.kanzaki.com/docs/ical/rrule.html)
				final DateTimeZone zone = DateTimeZone
						.forID( dtStartZonePattern.matcher( pattern ).group() );
				final DateTime start = DateTime
						.parse( dtStartTimePattern.matcher( pattern ).group() )
						.withZone( zone );

				// convert DateTime to Instant
				return Observable
						.from( DateTimeIteratorFactory.createDateTimeIterable(
								pattern, start, zone, strict ) )
						.map( new Func1<DateTime, Instant>()
						{
							@Override
							public Instant call( final DateTime dt )
							{
								final Instant t = Instant
										.of( org.joda.time.Duration
												.millis( dt.getMillis()
														- offset.getTime() ) );
								LOG.trace( "ical {}: {}: {}", pattern, dt, t );
								return t;
							}
						} );
			} catch( final Exception e1 )
			{
				try
				{
					// ISO date/time
					return Observable.just( Instant.of( org.joda.time.Duration
							.millis( DateTime.parse( pattern ).getMillis()
									- offset.getTime() ) ) );
				} catch( final Exception e2 )
				{
					try
					{
						// ISO period
						return Observable.just( Instant.valueOf( pattern ) );
					} catch( final Exception e3 )
					{

						throw ExceptionFactory.createUnchecked( e,
								"Problem parsing `{0}`, errors: \n\t{1}\n\t{2}\n\t{3}",
								pattern, e.getMessage(), e1.getMessage(),
								e2.getMessage() );
					}
				}
			}
		}
	}

	/*
	 * public static Iterable<Instant> createIterableInstant( final CronTrigger
	 * trigger) { return new Iterable<Instant>() {
	 * 
	 * @Override public Iterator<Instant> iterator() { return new
	 * Iterator<Instant>() {
	 *//** *//*
			 * private Date current = trigger.getStartTime();
			 * 
			 * @Override public boolean hasNext() { return this.current != null;
			 * }
			 * 
			 * @Override public Instant next() { final Date result =
			 * this.current; this.current =
			 * trigger.getFireTimeAfter(this.current); return
			 * Instant.valueOf(result.getTime()); }
			 * 
			 * @Override public void remove() { throw
			 * ExceptionBuilder.unchecked("NOT SUPPORTED") .build(); } }; } }; }
			 */

	/**
	 * @param pattern a {@link String} representation of some calendar-based
	 *            period or pattern
	 * @return the new {@link Timing} pattern wrapper
	 */
	public static Timing of( final String pattern )
	{
		return valueOf( pattern );
	}

	/**
	 * @param pattern a {@link String} representation of some calendar-based
	 *            period or pattern
	 * @return the new {@link Timing} pattern wrapper
	 */
	public static Timing valueOf( final String pattern )
	{
		return Util.valueOf( pattern, Timing.class );
	}

	public static class JsonSerializer
		extends com.fasterxml.jackson.databind.JsonSerializer<Timing>
	{
		public JsonSerializer()
		{
			LOG.trace( "Created " + getClass().getName() );
		}

		@Override
		public void serialize( final Timing value, final JsonGenerator gen,
			final SerializerProvider serializers )
			throws IOException, JsonProcessingException
		{
			// LOG.trace("Serializing " + value);
			gen.writeString( Util.toString( value ) );
		}
	}

	public static class JsonDeserializer
		extends com.fasterxml.jackson.databind.JsonDeserializer<Timing>
	{
		public JsonDeserializer()
		{
			LOG.trace( "Created " + getClass().getName() );
		}

		@Override
		public Timing deserialize( final JsonParser p,
			final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			return Util.valueOf( p.getText(), Timing.class );
		}
	}

}