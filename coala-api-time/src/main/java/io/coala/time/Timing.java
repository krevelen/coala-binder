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

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.TriggerBuilder;

import com.google.ical.compat.jodatime.DateTimeIteratorFactory;

import io.coala.exception.Thrower;
import io.coala.json.Wrapper;
import rx.Observable;

/**
 * {@link Timing} wraps a {@link String} representation of some calendar-based
 * recurrence pattern or duration, and streams {@link Instant}s relative to some
 * calendar-based offset. Pattern syntax compatibility includes:
 * <ul>
 * <li>a {@link CronExpression} (e.g. {@code "0 0 0 14 * ? *"} for
 * <em>&ldquo;midnight of every 14th day of the month&rdquo;</em> or
 * {@code "0 30 9,12,15 * * ?"} for <em>&ldquo;every day at 9:30am, 12:30pm and
 * 3:30pm&rdquo;</em>);</li>
 * <li>a {@link DateTimeIteratorFactory iCal RRULE or RDATE} pattern (e.g.
 * {@code "DTSTART;TZID=US-Eastern:19970902T090000\r\nRRULE:FREQ=DAILY;UNTIL=20130430T083000Z;INTERVAL=1;"}
 * );</li>
 * <li>a relative {@linkplain Instant#of(String) ISO8601 period or duration}
 * (e.g. {@code "P2DT3H4M"});</li>
 * <li>a relative {@linkplain Amount#valueOf(CharSequence) scientific amount}
 * (e.g. units {@code "3 "} or duration {@code "27.5 µs"} );</li>
 * </ul>
 * 
 * @version $Id: e10547851c245342c4636ba562faddb4efc7f5e5 $
 * @author Rick van Krevelen
 */
public interface Timing extends Wrapper<String>
{

	/**
	 * @param offset the absolute epoch {@link java.time.Instant} start date
	 * @param max the maximum number of iterations to generate (if possible)
	 * @return an {@link Iterable} stream of {@link Instant}s following this
	 *         {@link Timing} pattern calculated from given offset
	 * @throws Exception for instance a {@link ParseException}
	 */
	Iterable<Instant> iterate( java.time.Instant offset, Long max )
		throws ParseException;

	/**
	 * @return an {@link Iterable} stream of {@link Instant}s calculated from
	 *         {@link #unwrap() pattern}, {@link #offset()} and {@link #max()}
	 * @throws ParseException
	 */
	default Iterable<Instant> iterate() throws ParseException
	{
		return iterate( offset(), max() );
	}

	/**
	 * @param offset the {@link LocalDateTime} start date in default time zone
	 * @return this {@link Timing} to allow chaining
	 */
	default Timing offset( final LocalDateTime offset )
	{
		return offset( offset.atZone( ZoneId.systemDefault() ).toInstant() );
	}

	/**
	 * @param offset the absolute epoch {@link java.time.Instant} start date
	 * @return this {@link Timing} to allow chaining
	 */
	default Timing offset( final Date offset )
	{
		return offset( offset.toInstant() );
	}

	/**
	 * @param offset the absolute epoch {@link ReadableInstant} start date
	 * @return this {@link Timing} to allow chaining
	 */
	default Timing offset( final ReadableInstant offset )
	{
		return offset( new Date( offset.getMillis() ) );
	}

	default Timing offset( final java.time.Instant offset )
	{
		final Timing self = this;
		return new Timing()
		{
			@Override
			public String unwrap()
			{
				return self.unwrap();
			}

			@Override
			public Wrapper<String> wrap( final String value )
			{
				return self.wrap( value );
			}

			@Override
			public java.time.Instant offset()
			{
				return offset;
			}

			@Override
			public Iterable<Instant> iterate( final java.time.Instant offset,
				final Long max ) throws ParseException
			{
				return self.iterate( offset, max );
			}
		};
	}

	/**
	 * @return the offset {@link Date}
	 */
	default java.time.Instant offset()
	{
		return java.time.Instant.now();
	}

	default Timing max( final Long max )
	{
		final Timing self = this;
		return new Timing()
		{
			@Override
			public String unwrap()
			{
				return self.unwrap();
			}

			@Override
			public Wrapper<String> wrap( final String value )
			{
				return self.wrap( value );
			}

			@Override
			public Long max()
			{
				return max;
			}

			@Override
			public Iterable<Instant> iterate( final java.time.Instant offset,
				final Long max ) throws ParseException
			{
				return self.iterate( offset, max );
			}
		};
	}

	/**
	 * @return the maximum total iterations
	 */
	default Long max()
	{
		return null;
	}

	/**
	 * @return an {@link Observable} stream of {@link Instant}s calculated from
	 *         {@link #unwrap() pattern}, {@link #offset()} and {@link #max()}
	 */
	default Observable<Instant> stream()
	{
		try
		{
			return Observable.from( iterate() );
		} catch( final Throwable e )
		{
			return Observable.error( e );
		}
	}

	/**
	 * natural converter
	 * 
	 * @param pattern a formatted string, either {@link CronExpression CRON},
	 *            {@link DateTimeIteratorFactory#createDateTimeIterator(String, org.joda.time.ReadableDateTime, DateTimeZone, boolean)
	 *            iCal}, {@link DateTimeFormatter ISO8601} or {@link Measurable}
	 * @return the new {@link Timing} pattern wrapper
	 */
	static Timing valueOf( final String pattern )
	{
		try
		{
			return of( pattern, CronTiming.class );
		} catch( final Exception e )
		{
			try
			{
				return of( pattern, ICalTiming.class );
			} catch( final Exception e1 )
			{
				try
				{
					return of( pattern, InstantTiming.class );
				} catch( final Exception e2 )
				{
					return Thrower.throwNew( IllegalArgumentException.class,
							"Problem parsing `{}`, errors:"
									+ "\n\tCron: {}\n\tiCal: {}\n\t Instant: {}",
							pattern, e.getMessage(), e1.getMessage(),
							e2.getMessage() );
				}
			}
		}
	}

	/**
	 * @param pattern a formatted string, either {@link CronExpression CRON},
	 *            {@link DateTimeIteratorFactory#createDateTimeIterator(String, org.joda.time.ReadableDateTime, DateTimeZone, boolean)
	 *            iCal}, {@link DateTimeFormatter ISO8601} or {@link Measurable}
	 * @return the new {@link Timing} pattern wrapper
	 */
	static Timing of( final String pattern )
	{
		return valueOf( pattern );
	}

	/**
	 * @param pattern a formatted string, either {@link CronExpression CRON},
	 *            {@link DateTimeIteratorFactory#createDateTimeIterator(String, org.joda.time.ReadableDateTime, DateTimeZone, boolean)
	 *            iCal}, {@link DateTimeFormatter ISO8601} or {@link Measurable}
	 * @param type the type of {@link Timing} wrapper/streamer
	 * @return the new {@link Timing} pattern wrapper
	 */
	static <T extends Timing> T of( final String pattern, final Class<T> type )
	{
		return Util.valueOf( pattern, type );
	}

	/**
	 * {@link CronTiming}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class CronTiming extends Simple<String> implements Timing
	{

		private final static Map<String, CronTrigger> TRIGGERS = new HashMap<>();

		@Override
		public Iterable<Instant> iterate( final java.time.Instant offset,
			final Long max )
		{
			final CronTrigger trigger = TRIGGERS.computeIfAbsent( unwrap(),
					pattern ->
					{
						return TriggerBuilder.newTrigger().withSchedule(
								CronScheduleBuilder.cronSchedule( pattern ) )
								.build();
					} );
			return () ->
			{
				return new Iterator<Instant>()
				{
					private Date current = trigger
							.getFireTimeAfter( Date.from( offset ) );
					private long count = 0;

					@Override
					public boolean hasNext()
					{
						return this.current != null
								&& (max == null || this.count < max);
					}

					@Override
					public Instant next()
					{
						final Date next = this.current;
						this.current = trigger.getFireTimeAfter( this.current );
						this.count++;
						return Instant.of(
								next.getTime() - offset.toEpochMilli(),
								TimeUnits.MILLIS );
					}
				};
			};
		}
	}

	/**
	 * {@link ICalTiming} handles
	 * {@link DateTimeIteratorFactory#createDateTimeIterator(String, org.joda.time.ReadableDateTime, DateTimeZone, boolean)
	 * iCal RDATEs, EXDATEs, RRULEs or EXRULEs}, e.g.
	 * 
	 * <pre>
	 * {@code 
	DTSTART;TZID=US-Eastern:19970902T090000
	RRULE:FREQ=DAILY;UNTIL=20130430T083000Z;INTERVAL=1;
	 * }
	 * </pre>
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 * @see http://www.kanzaki.com/docs/ical/rrule.html
	 */
	class ICalTiming extends Simple<String> implements Timing
	{
		/** */
		private static final Pattern dtStartTimePattern = Pattern
				.compile( "DTSTART[.]*:(.*)" );

		/** */
		private static final Pattern dtStartZonePattern = Pattern
				.compile( "TZID=([^:;]*)" );

		@Override
		public Iterable<Instant> iterate( final java.time.Instant offset,
			final Long max ) throws ParseException
		{
			return () ->
			{
				final String ical = unwrap();
				try
				{
					final DateTimeZone zone = DateTimeZone.forID(
							dtStartZonePattern.matcher( ical ).group() );
					final DateTime start = DateTime
							.parse( dtStartTimePattern.matcher( ical ).group() )
							.withZone( zone );
					return new Iterator<Instant>()
					{
						private final Iterator<DateTime> it = DateTimeIteratorFactory
								.createDateTimeIterable( ical, start, zone,
										true )
								.iterator();
						private long count = 0;

						@Override
						public boolean hasNext()
						{
							return this.it.hasNext()
									&& (max == null || this.count < max);
						}

						@Override
						public Instant next()
						{
							this.count++;
							return Instant.of(
									this.it.next().getMillis()
											- offset.toEpochMilli(),
									TimeUnits.MILLIS );
						}
					};
				} catch( final Exception e )
				{
					return Thrower.rethrowUnchecked( e );
				}
			};
		}
	}

	/**
	 * {@link InstantTiming} for pattern formats {@link DateTimeFormatter
	 * ISO8601} or {@link Measurable}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class InstantTiming extends Simple<String> implements Timing
	{
		@Override
		public Iterable<Instant> iterate( final java.time.Instant offset,
			final Long max )
		{
			final long absMax = max == null ? 1L : Math.max( max, 1L );
			return () ->
			{
				return new Iterator<Instant>()
				{
					private long count = 0;

					@Override
					public boolean hasNext()
					{
						return this.count < absMax;
					}

					@Override
					public Instant next()
					{
						this.count++;
						return Instant.valueOf( unwrap() );
					}
				};
			};
		}
	}
}