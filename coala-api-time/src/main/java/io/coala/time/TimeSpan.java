/* $Id: 158a1548e0b5fd818f7644373f50c4a3e3b0a484 $
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.jscience.physics.amount.Amount;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.coala.math.MeasureUtil;
import io.coala.util.DecimalUtil;

/**
 * {@link TimeSpan} extends {@link DecimalMeasure} with {@link #valueOf(String)}
 * for {@link Converters#CLASS_WITH_VALUE_OF_METHOD}.
 * <p>
 * Assumes {@linkplain Double#NaN} as value for illegal/empty value types
 * 
 * Considered but rejected usingJScience {@link Amount} as super type rather
 * than {@link DecimalMeasure}, providing (exact) arithmetic operations by
 * default, e.g. {@link Amount#plus(Amount)}, a la
 * <a href="https://www.jcp.org/en/jsr/detail?id=363">JSR 363</a> (see
 * <a href="https://github.com/unitsofmeasurement/uom-se">Java8 reference
 * implementation</a>) due to {@link Amount}s incompatibility with
 * {@link BigDecimal} values
 * 
 * @version $Id: 158a1548e0b5fd818f7644373f50c4a3e3b0a484 $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
@JsonSerialize( using = TimeSpan.JsonSerializer.class )
@JsonDeserialize( using = TimeSpan.JsonDeserializer.class )
public class TimeSpan extends DecimalMeasure
{

	/** */
	private static final long serialVersionUID = 1L;

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
	public static TimeSpan valueOf( final String value )
	{
		return new TimeSpan( value );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param temporal a JSR-310 {@link TemporalAmount}
	 */
	public static TimeSpan of( final TemporalAmount temporal )
	{
		return new TimeSpan(
				BigDecimal.valueOf( temporal.get( ChronoUnit.NANOS ) )
						.add( BigDecimal
								.valueOf( temporal.get( ChronoUnit.MILLIS ) )
								.multiply( BigDecimal.TEN.pow( 6 ) ) ),
				NANOS );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param value a Joda {@link ReadableDuration}, e.g.
	 *            {@link org.joda.time.Duration}
	 */
	public static TimeSpan of( final ReadableDuration value )
	{
		return new TimeSpan( BigDecimal.valueOf( value.getMillis() ), MILLIS );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param value a {@link Measure} of some {@link Duration} or
	 *            {@link Dimensionless} {@link Quantity}
	 */
	public static TimeSpan of( final Measure<?, ?> value )
	{
		return value instanceof TimeSpan ? (TimeSpan) value
				: new TimeSpan( value );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param value an {@link Amount} of {@link Duration} or
	 *            {@link Dimensionless} units
	 */
	public static TimeSpan of( final Amount value )
	{
		return new TimeSpan( value );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param units the amount of time units
	 * @return a {@link Dimensionless} {@link TimeSpan}
	 */
	public static TimeSpan of( final Number units )
	{
		return of( units, Unit.ONE );
	}

	/**
	 * Returns the decimal measure for the specified number stated in the
	 * specified unit.
	 * 
	 * @param decimal the measurement value
	 * @param unit the measurement {@link Unit}
	 * @see DecimalMeasure#valueOf(Number, Unit)
	 */
	public static TimeSpan of( final Number decimal, final Unit<?> unit )
	{
		return new TimeSpan( decimal, unit );
	}

	/** */
	// private static final Logger LOG = LogManager.getLogger(TimeSpan.class);

	/** */
	public static final Unit<Duration> MILLIS = SI.MILLI( SI.SECOND );

	/** */
	public static final Unit<Duration> NANOS = SI.NANO( SI.SECOND );

	/** the ZERO */
	public static final TimeSpan ZERO = of( 0, Unit.ONE );

	/** the ONE */
	public static final TimeSpan ONE = of( 1, Unit.ONE );

	/**
	 * Parse duration as {@link DecimalMeasure JSR-275} measure (e.g.
	 * {@code "123 ms"}) or as ISO Period with
	 * {@link org.threeten.bp.Duration#parse(CharSequence) JSR-310} or
	 * {@link Period#parse(String) Joda}.
	 * 
	 * Examples of ISO period:
	 * 
	 * <pre>
	 *    "PT20.345S" -> parses as "20.345 seconds"
	 *    "PT15M"     -> parses as "15 minutes" (where a minute is 60 seconds)
	 *    "PT10H"     -> parses as "10 hours" (where an hour is 3600 seconds)
	 *    "P2D"       -> parses as "2 days" (where a day is 24 hours or 86400 seconds)
	 *    "P2DT3H4M"  -> parses as "2 days, 3 hours and 4 minutes"
	 *    "P-6H3M"    -> parses as "-6 hours and +3 minutes"
	 *    "-P6H3M"    -> parses as "-6 hours and -3 minutes"
	 *    "-P-6H+3M"  -> parses as "+6 hours and -3 minutes"
	 * </pre>
	 * 
	 * @param measure the {@link String} representation of a duration
	 * @return
	 * 
	 * @see org.threeten.bp.Duration#parse(String)
	 * @see org.joda.time.format.ISOPeriodFormat#standard()
	 * @see DecimalMeasure
	 */
	protected static final Measure<BigDecimal, Duration>
		parsePeriodOrMeasure( final String measure )
	{
		if( measure == null ) throw new NullPointerException();
		DecimalMeasure<Duration> result;
		try
		{
			result = DecimalMeasure.valueOf( measure );
			// LOG.trace("Parsed '{}' as JSR-275 measure/unit: {}", measure,
			// result);
			return result;
		} catch( final Exception a )
		{
			// LOG.trace("JSR-275 failed, try JSR-310", e);
			try
			{
				// final long millis = Period.parse(measure).getMillis();
				// return DecimalMeasure.valueOf(BigDecimal.valueOf(millis),
				// SI.MILLI(SI.SECOND));
				final java.time.Duration temp = java.time.Duration
						.parse( measure );
				result = temp.getNano() == 0 ? DecimalMeasure.valueOf(
						BigDecimal.valueOf( temp.getSeconds() ), SI.SECOND )
						: DecimalMeasure
								.valueOf(
										BigDecimal.valueOf( temp.getSeconds() )
												.multiply( BigDecimal.TEN
														.pow( 9 ) )
												.add( BigDecimal.valueOf(
														temp.getNano() ) ),
										NANOS );
				// LOG.trace(
				// "Parsed '{}' using JSR-310 to JSR-275 measure/unit: {}",
				// measure, result);
				return result;
			} catch( final Exception e )
			{
				// LOG.trace("JSR-275 and JSR-310 failed, try Joda", e);
				final Period joda = Period.parse( measure );
				result = DecimalMeasure.valueOf(
						BigDecimal.valueOf(
								joda.toStandardDuration().getMillis() ),
						MILLIS );
				// LOG.trace(
				// "Parsed '{}' using Joda to JSR-275 measure/unit: {}",
				// measure, result);
				return result;
			}
		}
	}

	/**
	 * {@link TimeSpan} constructor for "natural" polymorphic Jackson bean
	 * deserialization
	 * 
	 * @see com.fasterxml.jackson.databind.deser.BeanDeserializer
	 */
	public TimeSpan( final String measure )
	{
		this( parsePeriodOrMeasure( measure ) );
	}

	/**
	 * {@link TimeSpan} constructor for "natural" polymorphic Jackson bean
	 * deserialization
	 * 
	 * @see com.fasterxml.jackson.databind.deser.BeanDeserializer
	 */
	public TimeSpan( final double millis )
	{
		this( millis, MILLIS );
	}

	/**
	 * {@link TimeSpan} constructor for "natural" polymorphic Jackson bean
	 * deserialization
	 * 
	 * @see com.fasterxml.jackson.databind.deser.BeanDeserializer
	 */
	public TimeSpan( final int millis )
	{
		this( millis, MILLIS );
	}

	/**
	 * {@link TimeSpan} constructor
	 * 
	 * @param measure
	 * @param unit
	 */
	public <Q extends Quantity> TimeSpan( final Measure<?, Q> measure )
	{
		this( MeasureUtil.toBigDecimal( measure ), measure.getUnit() );
	}

	/**
	 * {@link TimeSpan} constructor
	 * 
	 * @param amount
	 * @param unit
	 */
	public TimeSpan( final Amount<?> amount )
	{
		this( MeasureUtil.toBigDecimal( amount ), amount.getUnit() );
	}

	/**
	 * {@link TimeSpan} main constructor for (inexact) {@link Number}s
	 * 
	 * @param value
	 * @param unit
	 */
	@SuppressWarnings( "unchecked" )
	public TimeSpan( final Number value, final Unit unit )
	{
		super( DecimalUtil.valueOf( value ), unit );
	}

	/**
	 * @param augend the {@link Measurable}, e.g. another {@link TimeSpan},
	 *            {@link Measure} or {@link Amount}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public TimeSpan add( final Measurable<?> augend )
	{
		return augend instanceof DecimalMeasure
				? add( (BigDecimal) ((DecimalMeasure) augend)
						.to( getUnit(), DecimalUtil.DEFAULT_CONTEXT )
						.getValue() )
				: add( augend instanceof Amount && ((Amount) augend).isExact()
						? augend.longValue( getUnit() )
						: augend.doubleValue( getUnit() ) );
	}

	/**
	 * @param augend
	 * @return a new {@link TimeSpan}
	 */
	public TimeSpan add( final Number augend )
	{
		return of( getValue().add( DecimalUtil.valueOf( augend ) ), getUnit() );
	}

	/**
	 * @param subtrahend the {@link Measure}, e.g. another {@link TimeSpan}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public TimeSpan subtract( final Measurable<?> subtrahend )
	{
		return subtrahend instanceof DecimalMeasure
				? subtract( (BigDecimal) ((DecimalMeasure) subtrahend)
						.to( getUnit(), DecimalUtil.DEFAULT_CONTEXT )
						.getValue() )
				: subtrahend instanceof Amount
						&& ((Amount) subtrahend).isExact()
								? subtract( subtrahend.longValue( getUnit() ) )
								: subtract(
										subtrahend.doubleValue( getUnit() ) );
	}

	/**
	 * @param subtrahend
	 * @return a new {@link TimeSpan}
	 */
	public TimeSpan subtract( final Number subtrahend )
	{
		return of( getValue().subtract( DecimalUtil.valueOf( subtrahend ) ),
				getUnit() );
	}

	/**
	 * @param divisor the {@link Dimensionless} {@link Measure}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public TimeSpan divide( final Measurable<Dimensionless> divisor )
	{
		// FIXME generate more exact Measure for discrete divisor values?
		return divide( divisor instanceof DecimalMeasure
				? ((DecimalMeasure) divisor)
						.to( Unit.ONE, DecimalUtil.DEFAULT_CONTEXT ).getValue()
				: BigDecimal.valueOf( divisor.doubleValue( Unit.ONE ) ) );
	}

	/**
	 * @param divisor the {@link Measure}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public <Q extends Quantity> TimeSpan
		divide( final Measure<? extends Number, Q> divisor )
	{
		return of(
				getValue().divide( DecimalUtil.valueOf( divisor.getValue() ) ),
				getUnit().divide( divisor.getUnit() ) );
	}

	/**
	 * @param divisor the {@link Amount}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public <Q extends Quantity> TimeSpan divide( final Amount<Q> divisor )
	{
		return of( getValue().divide( MeasureUtil.toBigDecimal( divisor ) ),
				getUnit().divide( divisor.getUnit() ) );
	}

	/**
	 * @param divisor
	 * @return a new {@link TimeSpan}
	 */
	public TimeSpan divide( final Number divisor )
	{
		return of( getValue().divide( DecimalUtil.valueOf( divisor ),
				DecimalUtil.DEFAULT_CONTEXT ), getUnit() );
	}

	/**
	 * @param multiplier the {@link Dimensionless} multiplier {@link Measure}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public TimeSpan multiply( final Measurable<Dimensionless> multiplier )
	{
		return multiply( multiplier instanceof DecimalMeasure
				? ((DecimalMeasure) multiplier)
						.to( Unit.ONE, DecimalUtil.DEFAULT_CONTEXT ).getValue()
				: BigDecimal.valueOf( multiplier.doubleValue( Unit.ONE ) ) );
	}

	/**
	 * @param multiplier the {@link Measure}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public <Q extends Quantity> TimeSpan
		multiply( final Measure<? extends Number, Q> multiplier )
	{
		return of(
				getValue().multiply(
						DecimalUtil.valueOf( multiplier.getValue() ) ),
				getUnit().times( multiplier.getUnit() ) );
	}

	/**
	 * @param multiplier the {@link Amount}
	 * @return a new {@link TimeSpan}
	 */
	@SuppressWarnings( "unchecked" )
	public <Q extends Quantity> TimeSpan multiply( final Amount<Q> multiplier )
	{
		return of(
				getValue().multiply( MeasureUtil.toBigDecimal( multiplier ) ),
				getUnit().times( multiplier.getUnit() ) );
	}

	/**
	 * @param multiplicand the {@link Number} to multiply with
	 * @return a new {@link TimeSpan}
	 */
	public TimeSpan multiply( final Number multiplicand )
	{
		return of( getValue().multiply( DecimalUtil.valueOf( multiplicand ),
				DecimalUtil.DEFAULT_CONTEXT ), getUnit() );
	}

	public Prettifier prettify( final int scale )
	{
		return Prettifier.of( this, getUnit(), scale );
	}

	public Prettifier prettify( final Unit<?> unit, final int scale )
	{
		return Prettifier.of( this, unit, scale );
	}

	public static class JsonSerializer
		extends com.fasterxml.jackson.databind.JsonSerializer<TimeSpan>
	{
		public JsonSerializer()
		{
			// LOG.trace("Created " + getClass().getName());
		}

		@Override
		public void serialize( final TimeSpan value, final JsonGenerator gen,
			final SerializerProvider serializers )
			throws IOException, JsonProcessingException
		{
			final String result = value.toString();
			// if (value.getUnit().getClass() == ProductUnit.class)
			// LOG.trace("Serialized {} {} to: {}", value.getUnit().getClass()
			// .getSimpleName(), value, result, new RuntimeException());
			gen.writeString( result );
		}
	}

	public static class JsonDeserializer
		extends com.fasterxml.jackson.databind.JsonDeserializer<TimeSpan>
	{
		public JsonDeserializer()
		{
			// LOG.trace("Created " + getClass().getName());
		}

		@Override
		public TimeSpan deserialize( final JsonParser p,
			final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			final TimeSpan result = p.getCurrentToken().isNumeric()
					? TimeSpan.of( p.getNumberValue() )
					: TimeSpan.valueOf( p.getText() );
			// LOG.trace("Deserialized {} {} to: {}", p.getCurrentToken(),
			// p.getText(), result);
			return result;
		}
	}

	public static class Prettifier
	{
		public static Prettifier of( final TimeSpan span, final Unit<?> unit,
			final int scale )
		{
			return new Prettifier( span, unit, scale );
		}

		private final TimeSpan span;
		private final Unit<?> unit;
		private final int scale;
		private String result = null;

		protected Prettifier( final TimeSpan span, final Unit<?> unit,
			final int scale )
		{
			this.span = span;
			this.unit = unit;
			this.scale = scale;
		}

		@SuppressWarnings( "unchecked" )
		public String toString()
		{
			return this.result != null ? this.result
					: (this.result = this.span
							.to( this.unit, DecimalUtil.DEFAULT_CONTEXT )
							.getValue()
							.setScale( this.scale, RoundingMode.HALF_UP )
							.toString() + this.unit);
		}
	}

}