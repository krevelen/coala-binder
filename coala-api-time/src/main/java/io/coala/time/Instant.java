/* $Id: 67f38cc7a8b7a6c4b7c7fc62ef53a3a464ef0d5e $
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

import java.math.BigDecimal;
import java.time.temporal.ChronoField;
import java.util.Date;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Converter;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.jscience.physics.amount.Amount;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.json.Wrapper;
import io.coala.random.ProbabilityDistribution;
import io.coala.time.TimeSpan.Prettifier;
import io.coala.util.DecimalUtil;

/**
 * {@linkplain Instant} is a {@link Wrapper} of a {@linkplain TimeSpan} value
 * measuring a duration since the EPOCH (1970-01-01T00:00:00Z) that provides a
 * {@link #valueOf(String)} method to allow loading as {@link Converter
 * configuration} value, as per
 * {@link org.aeonbits.owner.Converters#CLASS_WITH_VALUE_OF_METHOD}
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
 * <li>supports nanosecond precision,</li>s
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
 * @version $Id: 67f38cc7a8b7a6c4b7c7fc62ef53a3a464ef0d5e $
 * @author Rick van Krevelen
 */
@SuppressWarnings( { "unchecked", "rawtypes" } )
//@Wrapper.JavaPolymorph
public class Instant extends Wrapper.Simple<TimeSpan> implements Comparable<Instant>
{

	/**
	 * for {@link Config}'s "natural" value conversion for a {@link Duration}
	 * (i.e. {@link TimeSpan}).
	 * 
	 * @see org.aeonbits.owner.Converters.CLASS_WITH_VALUE_OF_METHOD
	 * @see of(String)
	 */
	public static Instant valueOf( final String value )
	{
		return of( value );
	}

	/**
	 * @param value a {@link String} representation of either:
	 *            <ul>
	 *            a duration since the EPOCH as {@link DecimalMeasure JSR-275}
	 *            measure (e.g. {@code "123 ms"}); or
	 *            <li>as ISO Period, parsed with
	 *            {@link org.threeten.bp.Duration#parse(CharSequence) JSR-310}
	 *            or (on failure) {@link Period#parse(String) Joda}. Examples of
	 *            ISO period:
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
	 *            </ul>
	 * 
	 * @see org.threeten.bp.Duration#parse(String)
	 * @see org.joda.time.format.ISOPeriodFormat#standard()
	 * @see DecimalMeasure
	 */
	public static Instant of( final String value )
	{
		return of( TimeSpan.valueOf( value ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value a{@link ReadableInstant} instant, e.g. {@link DateTime}
	 */
	public static Instant of( final ReadableInstant date,
		final ReadableInstant offset )
	{
		return of( date.getMillis() - offset.getMillis(), TimeSpan.MILLIS );
	}

	public static Instant of( final ReadableDuration joda )
	{
		return of( joda.getMillis(), TimeSpan.MILLIS );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value
	 */
	public static Instant of( final java.time.Instant value )
	{
		return of( TimeSpan.of( DecimalMeasure.valueOf(
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
	 * @param value the {@link Amount} (of
	 *            {@link javax.measure.quantity.Duration Duration} or
	 *            {@link javax.measure.unit.Unit#ONE dimensionless})
	 */
	public static Instant of( final Amount value )
	{
		return of( TimeSpan.of( value ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value the {@link Measure} (of
	 *            {@link javax.measure.quantity.Duration Duration} or
	 *            {@link javax.measure.unit.Unit#ONE dimensionless})
	 */
	public static Instant of( final Measure value )
	{
		return of( TimeSpan.of( value ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param units the amount of time units
	 * @deprecated please specify unit (of
	 *             {@link javax.measure.quantity.Duration Duration} or
	 *             {@link Dimensionless}) using {@link #of(Number, Unit)}
	 */
	@Deprecated
	public static Instant of( final Number units )
	{
		return of( TimeSpan.of( units ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param units the amount of time units
	 */
	public static <Q extends Quantity> Instant of( final Number value,
		final Unit<Q> unit )
	{
		return of( TimeSpan.of( value, unit ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value the {@link TimeSpan}
	 */
	public static Instant of( final TimeSpan value )
	{
		return Util.of( value, Instant.class );
	}

	@SuppressWarnings( "serial" )
	public static <N extends Number, Q extends Quantity>
		ProbabilityDistribution<Instant>
		of( final ProbabilityDistribution<N> dist, final Unit<Q> unit )
	{
		return new ProbabilityDistribution<Instant>()
		{
			@Override
			public Instant draw()
			{
				// FIXME use MeasureUtil?
				final Number value = dist.draw();
				return value instanceof BigDecimal
						? Instant.of( DecimalMeasure
								.valueOf( (BigDecimal) value, unit ) )
						: value instanceof Long || value instanceof Integer
								? Instant.of( DecimalMeasure
										.valueOf( value.longValue(), unit ) )
								: Instant.of( DecimalMeasure
										.valueOf( value.doubleValue(), unit ) );
			}
		};
	}

	/** the ZERO */
	public static final Instant ZERO = of( TimeSpan.ZERO );

	/** the ONE */
	public static final Instant ONE = of( TimeSpan.ONE );

	@Override
	public int compareTo( final Instant that )
	{
		return Util.compare( this, that );
	}

	public DecimalMeasure
		multiply( final Measure<?, Dimensionless> multiplicand )
	{
		return unwrap().multiply( multiplicand );
	}

	public Amount multiply( final Amount<Dimensionless> multiplicand )
	{
		return unwrap().multiply( multiplicand );
	}

	public Instant multiply( final long multiplicand )
	{
		return of( unwrap().multiply( multiplicand ) );
	}

	public Instant multiply( final Number multiplicand )
	{
		return of( unwrap().multiply( multiplicand ) );
	}

	public Instant multiply( final BigDecimal multiplicand )
	{
		return of( unwrap().multiply( multiplicand ) );
	}

	public DecimalMeasure divide( final Measure divisor )
	{
		return unwrap().divide( divisor );
	}

	public Amount divide( final Amount divisor )
	{
		return unwrap().divide( divisor );
	}

	public Instant divide( final long divisor )
	{
		return of( unwrap().divide( divisor ) );
	}

	public Instant divide( final Number divisor )
	{
		return of( unwrap().divide( divisor ) );
	}

	public Instant divide( final BigDecimal divisor )
	{
		return of( unwrap().divide( divisor ) );
	}

	public Instant add( final Duration augend )
	{
		return of( unwrap().add( augend.unwrap() ) );
	}

	public Instant add( final Measure augend )
	{
		return of( unwrap().add( augend ) );
	}

	public Instant add( final Amount augend )
	{
		return of( unwrap().add( augend ) );
	}

	public Instant add( final long augend )
	{
		return of( unwrap().add( augend ) );
	}

	public Instant add( final Number augend )
	{
		return of( unwrap().add( augend ) );
	}

	public Instant add( final BigDecimal augend )
	{
		return of( unwrap().add( augend ) );
	}

	public Instant subtract( final Duration subtrahend )
	{
		return of( unwrap().subtract( subtrahend.unwrap() ) );
	}

	public Duration subtract( final Instant subtrahend )
	{
		return Duration.of( unwrap().subtract( subtrahend.unwrap() ) );
	}

	public Instant subtract( final Amount subtrahend )
	{
		return of( unwrap().subtract( subtrahend ) );
	}

	public Instant subtract( final long subtrahend )
	{
		return of( unwrap().subtract( subtrahend ) );
	}

	public Instant subtract( final Number subtrahend )
	{
		return of( unwrap().subtract( subtrahend ) );
	}

	public Instant subtract( final BigDecimal subtrahend )
	{
		return of( unwrap().subtract( subtrahend ) );
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
	public Date toDate( final Date offset )
	{
		return new Date( offset.getTime() + toMillisLong() );
	}

	/** @return the Joda {@link ReadableInstant} implementation of an instant */
	@JsonIgnore
	public DateTime toJoda( final ReadableInstant offset )
	{
		return new DateTime( offset.getMillis() + toMillisLong(),
				offset.getZone() );
	}

	/**
	 * @return the JSR-310 back-port {@link org.threeten.bp.Instant}
	 *         implementation of an instant
	 */
	@JsonIgnore
	public java.time.Instant toJSR310()
	{
		return java.time.Instant.ofEpochMilli( toMillisLong() );
	}

	/**
	 * @return the JRE8 {@link java.time.Instant} implementation of an instant
	 */
//	@JsonIgnore
//	public java.time.Instant toJava8()
//	{
//		return java.time.Instant.ofEpochMilli( toMillisLong() );
//	}

	/** @return the JSR-275 {@link Measurable} implementation of an instant */
	@JsonIgnore
	public TimeSpan toMeasure()
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
		return Amount.valueOf( unwrap().getValue().doubleValue(),
				unwrap().getUnit() );
	}

	/**
	 * @param offset the instant that is considered the ZERO
	 * @return the (possibly negative) {@link Duration} of this instant since
	 *         specified {@code offset}
	 */
	public Duration toDuration( final Instant offset )
	{
		return Duration.of( offset == null ? unwrap().getValue()
				: unwrap().getValue()
						.subtract( offset.unwrap()
								.to( unwrap().getUnit(),
										DecimalUtil.DECIMAL_PRECISION )
								.getValue() ),
				unwrap().getUnit() );
	}

	public Prettifier prettify( final int scale )
	{
		return unwrap().prettify( scale );
	}

	public Prettifier prettify( final Unit<?> unit, final int scale )
	{
		return unwrap().prettify( unit, scale );
	}

}