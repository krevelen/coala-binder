/* $Id$
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
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;
import javax.xml.datatype.XMLGregorianCalendar;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Converter;
import org.joda.time.DateTime;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import io.coala.json.Wrapper;
import io.coala.log.LogUtil.Pretty;
import io.coala.math.DecimalUtil;
import io.coala.math.QuantityUtil;
import io.coala.xml.XmlUtil;
import tec.uom.se.ComparableQuantity;

/**
 * {@linkplain Instant} is a {@link Wrapper} of a
 * {@linkplain ComparableQuantity} value measuring a duration since the EPOCH
 * (1970-01-01T00:00:00Z) that provides a {@link #valueOf(String)} method to
 * allow loading as {@link Converter configuration} value, as per
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
 * <dt>The JSR-310 {@code javax.time} Java8 extension (or back-port from
 * <a href="http://www.threeten.org/">threeten.org</a>):</dt>
 * <dd>
 * <li>supports nanosecond precision,</li>s
 * <li>{@linkplain org.threeten.bp.OldInstant} parses strictly ISO8601 format
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
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( { "unchecked", "rawtypes" } )
public class Instant extends Wrapper.SimpleOrdinal<ComparableQuantity>
{

	/** the ZERO value in dimensionless units or {@link TimeUnits#STEPS} */
	public static final Instant ZERO = of( BigDecimal.ZERO, TimeUnits.STEPS );

//	/** the ONE */
//	public static final Instant ONE = of( BigDecimal.ONE, TimeUnits.TICK );

	/**
	 * for {@link Config}'s "natural" value conversion for an {@link Instant}
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
	 *            <li>a {@link DecimalMeasure JSR-275 Measure of duration} (e.g.
	 *            {@code "123 ms"}); or
	 *            <li>as {@code ISO 8601 Period} parsed with
	 *            {@link java.time.Duration#parse(CharSequence) JSR-310} or (on
	 *            failure)
	 *            {@link org.joda.time.format.ISOPeriodFormat#standard() Joda},
	 *            e.g.:
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
	 * 
	 * @see java.time.Duration#parse(String)
	 * @see org.joda.time.format.ISOPeriodFormat#standard()
	 * @see Quantity
	 */
	public static Instant of( final String value )
	{
		return of( QuantityUtil.parseDuration( value ) );
	}

	public static Instant of( final ReadableDuration millis )
	{
		return of( millis.getMillis(), TimeUnits.MILLIS );
	}

	public static Instant of( final ReadableInstant instant,
		final ReadableInstant offset )
	{
		return of( instant.getMillis() - offset.getMillis(), TimeUnits.MILLIS );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value a{@link ReadableInstant} instant, e.g. {@link DateTime}
	 */
	public static Instant of( final Date value, final Date offset )
	{
		return of( value.getTime() - offset.getTime(), TimeUnits.MILLIS );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value
	 */
	public static Instant of( final java.time.Instant value,
		final java.time.Instant offset )
	{
		return of( QuantityUtil.valueOf(
				BigDecimal.valueOf( value.get( ChronoField.NANO_OF_SECOND ) )
						.add( BigDecimal
								.valueOf( value
										.get( ChronoField.INSTANT_SECONDS ) )
								.multiply( BigDecimal.TEN.pow( 9 ) ) ),
				TimeUnits.NANOS ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param units the amount of {@link Dimensionless} time units
	 */
	public static Instant of( final Number units )
	{
		return of( QuantityUtil.valueOf( units ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param units the amount of time units
	 */
	public static Instant of( final Number value, final Unit<?> unit )
	{
		return of( QuantityUtil.valueOf( value, unit ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value the {@link Quantity}
	 */
	public static Instant of( final Quantity<?> value )
	{
		return of( QuantityUtil.valueOf( value ) );
	}

	/**
	 * {@link Instant} static factory method
	 * 
	 * @param value the {@link Quantity}
	 */
	public static Instant of( final ComparableQuantity<?> value )
	{
		return new Instant( value );
	}

	// for JSON deserialization
	public Instant()
	{

	}

	public Instant( final ComparableQuantity<?> value )
	{
		wrap( value );
	}

	@JsonIgnore
	public boolean isZero()
	{
		return DecimalUtil.isZero( value() );
	}

	@JsonValue
	@Override
	public String toString()
	{
		return super.toString();
	}

	@Override
	public int compareTo( final Comparable o )
	{
		final Instant that = (Instant) o;
		try
		{
			return this.unwrap().compareTo( that.unwrap() );
		} catch( final /* Incommensurable */ Exception e )
		{
			if( this.isZero() && that.isZero() ) return 0;
			throw e;
		}
	}

	public Unit<?> unit()
	{
		return unwrap().getUnit();
	}

	public Number value()
	{
		return unwrap().getValue();
	}

	public BigDecimal decimal()
	{
		return DecimalUtil.valueOf( value() );
	}

	public Instant multiply( final Quantity multiplicand )
	{
		return of( unwrap().multiply( multiplicand ) );
	}

	public Instant multiply( final Number multiplicand )
	{
		return of( unwrap().multiply( multiplicand ) );
	}

	public Instant divide( final Quantity divisor )
	{
		return of( unwrap().divide( divisor ) );
	}

	public Instant divide( final Number divisor )
	{
		return of( unwrap().divide( divisor ) );
	}

	public Instant add( final Duration augend )
	{
		if( augend.isZero() ) return this;
		return add( augend.unwrap() );
	}

	public Instant add( final Quantity augend )
	{
		if( QuantityUtil.signum( augend ) == 0 ) return this;
		return of( unwrap().add( augend ) );
	}

	public Instant add( final Number augend )
	{
		if( DecimalUtil.signum( augend ) == 0 ) return this;
		return add( QuantityUtil.valueOf( augend, unit() ) );
	}

	public Instant subtract( final Duration subtrahend )
	{
		if( subtrahend.isZero() ) return this;
		return of( unwrap().subtract( subtrahend.unwrap() ) );
	}

	public Duration subtract( final Instant subtrahend )
	{
		return Duration.of( unwrap().subtract( subtrahend.unwrap() ) );
	}

	public Instant subtract( final Quantity subtrahend )
	{
		if( QuantityUtil.signum( subtrahend ) == 0 ) return this;
		return of( unwrap().subtract( subtrahend ) );
	}

	public Instant subtract( final Number subtrahend )
	{
		if( DecimalUtil.signum( subtrahend ) == 0 ) return this;
		return subtract( QuantityUtil.valueOf( subtrahend, unit() ) );
	}

	public Instant to( final Unit unit )
	{
		return unit().equals( unit ) ? this : of( unwrap().to( unit ) );
	}

	public Instant to( final TimeUnit unit )
	{
		return to( TimeUnits.resolve( unit ) );
	}

	/** @return the total amount of (virtual) milliseconds */
	public long toMillisLong()
	{
		return to( TimeUnits.MILLIS ).value().longValue();
	}

	/** @return the total amount of (virtual) nanoseconds */
	public long toNanosLong()
	{
		return to( TimeUnits.NANOS ).value().longValue();
	}

	/** @return a posix {@link Date} */
	public Date toDate( final Date offsetUtc )
	{
		return new Date( offsetUtc.getTime() + toMillisLong() );
	}

	/** @return a {@link Calendar} */
	public Calendar toCalendar( final Calendar offset )
	{
		// TODO test if JapaneseImperialCalendar offset types are preserved
		final Calendar result = Calendar.getInstance( offset.getTimeZone() );
		result.setTimeInMillis( offset.getTimeInMillis() + toMillisLong() );
		return result;
	}

	/** @return a JSR-310 {@link LocalTime} (zone and date-less) instant */
	public LocalDate toJava8( final LocalDate offset )
	{
		return offset.plusDays(
				toQuantity( TimeUnits.DAYS ).getValue().longValue() );
	}

	/** @return a JSR-310 {@link LocalTime} (zone and date-less) instant */
	public LocalTime toJava8( final LocalTime offset )
	{
		return offset.plusNanos( toNanosLong() );
	}

	/** @return a JSR-310 UTC {@link java.time.Instant} */
	public java.time.Instant toJava8( final java.time.Instant offsetUtc )
	{
		return offsetUtc.plusNanos( toNanosLong() );
	}

	/** @return a JSR-310 {@link LocalDateTime} instant */
	public LocalDateTime toJava8( final LocalDateTime offset )
	{
		return offset.plusNanos( toNanosLong() );
	}

	/** @return a JSR-310 {@link OffsetDateTime} instant */
	public OffsetDateTime toJava8( final OffsetDateTime offset )
	{
		return offset.plusNanos( toNanosLong() );
	}

	/** @return a JSR-310 {@link ZonedDateTime} instant */
	public ZonedDateTime toJava8( final ZonedDateTime offset )
	{
		return offset.plusNanos( toNanosLong() );
	}

	/**
	 * @param days
	 * @return a JSR-363 {@link Quantity}
	 */
	public ComparableQuantity<?> toQuantity()
	{
		return unwrap();
	}

	public <Q extends Quantity<Q>> ComparableQuantity<Q>
		toQuantity( final Class<Q> unit )
	{
		return unwrap().asType( unit );
	}

	public <Q extends Quantity<Q>> ComparableQuantity<Q>
		toQuantity( final Unit<Q> unit )
	{
		return unwrap().to( unit );
	}

	/** @return a Joda {@link ReadableInstant} */
	public DateTime toJoda( final ReadableInstant offset )
	{
		return new DateTime( offset.getMillis() + toMillisLong(),
				offset.getZone() );
	}

	/** @return a JAXP {@link XMLGregorianCalendar} */
	public XMLGregorianCalendar toXML( final ZonedDateTime offset )
	{
		return XmlUtil.toXML( toJava8( offset ) );
	}

	/** @see BigDecimal.setScale(int, RoundingMode) */
	public Pretty prettify( final int scale )
	{
		return prettify( unit(), scale );
	}

	/** @see BigDecimal.setScale(int, RoundingMode) */
	public Pretty prettify( final Unit unit, final int scale )
	{
		return Pretty.of( () -> DecimalUtil
				.toScale( unwrap().to( unit ).getValue(), scale ).toString()
				+ unit );
	}

	public Pretty prettify( final Date offset )
	{
		return Pretty.of( () -> toDate( offset ).toString() );
	}

	public Pretty prettify( final Date offset, final DateFormat formatter )
	{
		return Pretty.of( () -> formatter.format( toDate( offset ) ) );
	}

	public Pretty prettify( final LocalDate offset )
	{
		return Pretty.of( () -> toJava8( offset ).toString() );
	}

	public Pretty prettify( final LocalDateTime offset )
	{
		return Pretty.of( () -> toJava8( offset ).toString() );
	}

	public Pretty prettify( final OffsetDateTime offset )
	{
		return Pretty.of( () -> toJava8( offset ).toString() );
	}

	public Pretty prettify( final ZonedDateTime offset )
	{
		return Pretty.of( () -> toJava8( offset ).toString() );
	}

	public Pretty prettify( final ZonedDateTime offset,
		final java.time.format.DateTimeFormatter formatter )
	{
		return Pretty.of( () -> formatter.format( toJava8( offset ) ) );
	}

	public Pretty prettify( final DateTime offset )
	{
		return Pretty.of( () -> toJoda( offset ).toString() );
	}

	public Object prettify( final DateTime offset,
		final DateTimeFormatter formatter )
	{
		return Pretty.of( () -> formatter.print( toJoda( offset ) ) );
	}

}