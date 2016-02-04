/* $Id$
 * $URL$
 * 
 * Part of the EU project Inertia, see http://www.inertia-project.eu/
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
 * 
 * Copyright (c) 2014 Almende B.V. 
 */
package io.coala.time.x;

import java.math.BigDecimal;
import java.util.Date;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.unit.SI;

import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.jscience.physics.amount.Amount;
import org.threeten.bp.temporal.ChronoField;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.json.x.Wrapper;

/**
 * {@linkplain Instant} wraps a {@linkplain TimeSpan} that is
 * {@linkplain Polymorph} (measuring the duration since the EPOCH,
 * 1970-01-01T00:00:00Z) and provides a {@link #valueOf(String)} method for
 * loading as configured value {@link Converters#CLASS_WITH_VALUE_OF_METHOD}
 * <p>
 * We considered various temporal measure implementations, including
 * <dl>
 * <dt>Java's native utilities</dt>
 * <dd>{@link java.util.Date} and {@link java.util.GregorianCalendar}</dd>
 * <dt></dt>
 * <dt>The JSR-275 {@code javax.measure} reference implementation v4.3.1 from
 * <a href="http://jscience.org/">jscience.org</a></dt>
 * <dd>
 * <li>takes any value type (e.g. {@linkplain Number}) or granularity (e.g.
 * {@link SI#NANO(javax.measure.unit.Unit nano)} or
 * {@link SI#PICO(javax.measure.unit.Unit) pico})</li></dd>
 * <dt>The JSR-310 {@code javax.time} Java8 extension back-port from
 * <a href="http://www.threeten.org/">threeten.org</a>:</dt>
 * <dd>
 * <li>supports nanosecond precision,</li>
 * <li>{@linkplain org.threeten.bp.Instant} parses strictly ISO8601 format
 * (millis/nanos) only</li>
 * <dt>Joda's time API from <a href="http://www.joda.org/">joda.org</a></dt>
 * <dd>
 * <li>Allows lenient (lower and upper case) ISO8601 format strings</li>
 * <li>Joda time offers this
 * <a href="https://github.com/FasterXML/jackson-datatype-joda">datatype
 * extension for Jackson</a>.</li>
 * <li>offers many nice calendar and formatter implementations</li>
 * <li>will <a href="https://github.com/JodaOrg/joda-time/issues/52">not support
 * microsecond or nanosecond precision</a></li></dd>
 * <dt>Apache {@code commons-lang3} Date Utilities</dt>
 * <dd>limitations similar to Joda's time API (millisecond precision only)</dd>
 * </dl>
 * 
 * @date $Date$
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( { "unchecked", "rawtypes" } )
@Wrapper.Polymorph
public class Instant implements Wrapper<TimeSpan>, Comparable<Instant>
{

	private TimeSpan value;

	@Override
	public TimeSpan unwrap()
	{
		return this.value;
	}

	@Override
	public void wrap( final TimeSpan value )
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return unwrap().toString();
	}

	@Override
	public int hashCode()
	{
		return unwrap().hashCode();
	}

	@Override
	public int compareTo( final Instant that )
	{
		return unwrap().compareTo( that.unwrap() );
	}

	@JsonIgnore
	public long toMillisLong()
	{
		return unwrap().longValue( TimeSpan.MILLIS );
	}

	@JsonIgnore
	public long toNanosLong()
	{
		return unwrap().longValue( TimeSpan.NANOS );
	}

	@JsonIgnore
	public Date toDate()
	{
		return new Date( toMillisLong() );
	}

	/** @return the Joda {@link ReadableInstant} implementation of an instant */
	@JsonIgnore
	public ReadableInstant toJoda()
	{
		return new org.joda.time.Instant( toMillisLong() );
	}

	/**
	 * @return the JSR-310 {@link org.threeten.bp.Instant} implementation of an
	 *         instant
	 */
	@JsonIgnore
	public org.threeten.bp.Instant toJava8()
	{
		return org.threeten.bp.Instant.ofEpochMilli( toMillisLong() );
	}

	/** @return the JSR-275 {@link Measurable} implementation of an instant */
	@JsonIgnore
	public Measurable toMeasure()
	{
		return unwrap();
	}

	/**
	 * @return the JScience {@link Amount} precision implementation of an
	 *         instant
	 */
	@JsonIgnore
	public Amount toAmount()
	{
		return Amount.valueOf( unwrap().toString() ).to( unwrap().getUnit() );
	}

	/**
	 * @param offset the instant that is considered the ZERO
	 * @return the (possibly negative) {@link Duration} of this instant since
	 *         specified {@code offset}
	 */
	public Duration toDuration( final Instant offset )
	{
		return Duration.valueOf( offset == null ? unwrap().getValue()
				: unwrap().getValue().subtract(
						offset.unwrap().to( unwrap().getUnit() ).getValue() ) );
	}

	/**
	 * for "natural" Config value conversion for a {@link Duration} (i.e.
	 * {@link TimeSpan}).
	 * 
	 * @param value a duration as {@link DecimalMeasure JSR-275} measure (e.g.
	 *            {@code "123 ms"}) or as ISO Period, parsed with
	 *            {@link org.threeten.bp.Duration#parse(CharSequence) JSR-310}
	 *            or {@link Period#parse(String) Joda}.
	 * 
	 *            Examples of ISO period:
	 * 
	 *            <pre>
	 *    "PT20.345S" -> parses as "20.345 seconds"
	 *    "PT15M"     -> parses as "15 minutes" (where a minute is 60 seconds)
	 *    "PT10H"     -> parses as "10 hours" (where an hour is 3600 seconds)
	 *    "P2D"       -> parses as "2 days" (where a day is 24 hours or 86400 seconds)
	 *    "P2DT3H4M"  -> parses as "2 days, 3 hours and 4 minutes"
	 *    "P-6H3M"    -> parses as "-6 hours and +3 minutes"
	 *    "-P6H3M"    -> parses as "-6 hours and -3 minutes"
	 *    "-P-6H+3M"  -> parses as "+6 hours and -3 minutes"
	 *            </pre>
	 * 
	 * @see org.aeonbits.owner.Converters.CLASS_WITH_VALUE_OF_METHOD
	 * @see org.threeten.bp.Duration#parse(String)
	 * @see org.joda.time.format.ISOPeriodFormat#standard()
	 * @see DecimalMeasure
	 */
	public static Instant valueOf( final String value )
	{
		return valueOf( TimeSpan.valueOf( value ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value
	 */
	public static Instant valueOf( final ReadableInstant joda )
	{
		return valueOf( joda.getMillis() );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value
	 */
	public static Instant valueOf( final org.threeten.bp.Instant value )
	{
		return valueOf( TimeSpan.valueOf( DecimalMeasure.valueOf(
				BigDecimal.valueOf( value.get( ChronoField.NANO_OF_SECOND ) )
						.add( BigDecimal
								.valueOf( value
										.get( ChronoField.INSTANT_SECONDS ) )
								.multiply( BigDecimal.TEN.pow( 9 ) ) ),
				TimeSpan.NANOS ) ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value the {@link Measure} (of
	 *            {@link javax.measure.quantity.Duration Duration} or
	 *            {@link javax.measure.unit.Unit#ONE dimensionless})
	 */
	public static Instant valueOf( final Measure value )
	{
		return valueOf( TimeSpan.valueOf( value ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param units the amount of time units
	 */
	public static Instant valueOf( final Number units )
	{
		return valueOf( TimeSpan.valueOf( units ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value the {@link TimeSpan}
	 */
	public static Instant valueOf( final TimeSpan value )
	{
		return new Instant()
		{
			{
				wrap( value );
			}
		};
	}

}