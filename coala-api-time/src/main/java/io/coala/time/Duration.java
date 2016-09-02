/* $Id: a4d1db0cef5ed1fc88522da2c69aa49907d8de7d $
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

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.jscience.physics.amount.Amount;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.json.Wrapper;
import io.coala.math.MeasureUtil;

/**
 * {@linkplain Duration} wraps an {@linkplain TimeSpan} that is
 * {@linkplain JavaPolymorph} and provides a {@link #valueOf(String)} method for
 * loading as configured value {@link Converters#CLASS_WITH_VALUE_OF_METHOD}
 * <p>
 * We considered various temporal measure implementations, including
 * <dl>
 * <dt>Java's native utilities</dt>
 * <dd>Offers no standard tuple combining a {@link java.lang.Number} and
 * {@link java.util.concurrent.TimeUnit}</dd>
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
 * <li>{@linkplain org.threeten.bp.Duration} parses strictly 'PTx.xS' (upper
 * case) ISO8601 format only</li>
 * <li>{@linkplain org.threeten.bp.temporal.TemporalAmount} does not align with
 * earlier JSR-275 {@link javax.measure.quantity.Duration}</li></dd>
 * <dt>Joda's time API from <a href="http://www.joda.org/">joda.org</a></dt>
 * <dd>
 * <li>Allows lenient (lower and upper case) ISO8601 format strings</li>
 * <li>{@link org.joda.time.Duration} implements {@link Comparable} whereas
 * {@link org.joda.time.Period} does not.</li>
 * <li>Joda time offers this
 * <a href="https://github.com/FasterXML/jackson-datatype-joda">datatype
 * extension for Jackson</a>.</li>
 * <li>offers many nice calendar and formatter implementations</li>
 * <li>will <a href="https://github.com/JodaOrg/joda-time/issues/52">not support
 * microsecond or nanosecond precision</a></li></dd>
 * <dt>Apache {@code commons-lang3} Date Utilities</dt>
 * <dd>limitations similar to Joda's time API (millisecond precision only)</dd>
 * <dt>Guava in the Google Web Toolkit from
 * <a href="https://github.com/google/guava">github.com/google/guava</a></dt>
 * <dd>extends relevant Java types only with a (time-line offset) interval (
 * {@code Range}) API, not a (free floating) duration quantity</dd>
 * <dt>DESMO-J's TimeSpan API from
 * <a href="http://desmoj.sf.net/">desmoj.sf.net</a></dt>
 * <dd>limited to Java's standard TimeUnit</dd>
 * <dt>DSOL3's UnitTime API from
 * <a href="http://simulation.tudelft.nl/">simulation.tudelft.nl</a></dt>
 * <dd>no Javadoc available</dd>
 * </dl>
 * 
 * @date $Date$
 * @version $Id: a4d1db0cef5ed1fc88522da2c69aa49907d8de7d $
 */
@SuppressWarnings( { "rawtypes", "unchecked" } )
public class Duration extends Wrapper.Simple<TimeSpan>
	implements Comparable<Duration>
{

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
	 * @see org.threeten.bp.Duration#parse(String)
	 * @see org.joda.time.format.ISOPeriodFormat#standard()
	 * @see DecimalMeasure
	 */
	public static Duration of( final String value )
	{
		return of( TimeSpan.valueOf( value ) );
	}

	/**
	 * @param value
	 * @return
	 * @see org.aeonbits.owner.Converters.CLASS_WITH_VALUE_OF_METHOD
	 */
	public static Duration valueOf( final String value )
	{
		return of( value );
	}

	/**
	 * {@link Duration} static factory method
	 * 
	 * @param value
	 */
	public static Duration of( final ReadableDuration value )
	{
		return of( TimeSpan.of( value ) );
	}

	/**
	 * {@link Duration} static factory method
	 * 
	 * @param value
	 */
	public static Duration of( final Measure value )
	{
		return of( TimeSpan.of( value ) );
	}

	/**
	 * {@link Duration} static factory method
	 * 
	 * @param value
	 */
	public static Duration of( final Amount value )
	{
		return of( TimeSpan.of( value ) );
	}

	/**
	 * {@link Duration} static factory method
	 * 
	 * @param units the number of time steps
	 */
	public static Duration of( final Number units )
	{
		return of( TimeSpan.of( units ) );
	}

	/**
	 * {@link Duration} static factory method
	 * 
	 * @param value the number of milliseconds
	 * @param unit {@link javax.measure.quantity.Duration} or
	 *            {@link Dimensionless}
	 */
	public static Duration of( final Number value, final Unit<?> unit )
	{
		return of( TimeSpan.of( value, unit ) );
	}

	/**
	 * {@link Duration} static factory method
	 * 
	 * @param value the {@link TimeSpan}
	 */
	public static Duration of( final TimeSpan value )
	{
		return Util.of( value, new Duration() );
	}

	/**
	 * @param i1
	 * @param i2
	 * @return the absolute distance/duration between two {@link Instant}s
	 */
	public static Duration between( final Instant i1, final Instant i2 )
	{
		return i1.compareTo( i2 ) > 0 ? i1.subtract( i2 ) : i2.subtract( i1 );
	}

	/** */
	public static final Duration ZERO = Duration.of( Amount.ZERO );

	@Override
	public int compareTo( final Duration that )
	{
		return Util.compare( this, that );
	}

	public Unit<?> unit()
	{
		return unwrap().getUnit();
	}

	public Duration to( final Unit unit )
	{
		return of( unwrap().to( unit ) );
	}

	public int intValue( final Unit unit )
	{
		return unwrap().intValue( unit );
	}

	public long longValue( final Unit unit )
	{
		return unwrap().longValue( unit );
	}

	public float floatValue( final Unit unit )
	{
		return unwrap().floatValue( unit );
	}

	public double doubleValue( final Unit unit )
	{
		return unwrap().doubleValue( unit );
	}

	public Duration pow( final double exponent )
	{
		return of( MeasureUtil.pow( unwrap(), exponent ) );
	}

	public Duration floor()
	{
		return of( MeasureUtil.floor( unwrap() ) );
	}

	public Duration ceil()
	{
		return of( MeasureUtil.ceil( unwrap() ) );
	}

	@JsonIgnore
	public long toMillisLong()
	{
		return longValue( Units.MILLIS );
	}

	@JsonIgnore
	public long toNanosLong()
	{
		return longValue( Units.NANOS );
	}

	/**
	 * @return the Joda {@link ReadableDuration} implementation of a time span
	 */
	@JsonIgnore
	public ReadableDuration toJoda()
	{
		return org.joda.time.Duration.millis( toMillisLong() );
	}

	/**
	 * @return a JRE8 {@link java.time.Duration} implementation of a time span
	 */
	@JsonIgnore
	public java.time.Duration toJSR310()
	{
		return java.time.Duration.ofNanos( toNanosLong() );
	}

	/** @return the JScience {@link Amount} implementation of a time span */
	@JsonIgnore
	public Amount toAmount()
	{
		return MeasureUtil.toAmount( unwrap() );
	}

	/** @return the JSR-275 {@link Measure} implementation of a time span */
	@JsonIgnore
	public Measure toMeasure()
	{
		return unwrap();
	}

	public Object prettify( final int scale )
	{
		return unwrap().prettify( scale );
	}

	public Object prettify( final Unit<?> unit, final int scale )
	{
		return unwrap().prettify( unit, scale );
	}
}