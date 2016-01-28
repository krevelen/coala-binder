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

import java.io.IOException;
import java.math.BigDecimal;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.jscience.physics.amount.Amount;
import org.threeten.bp.temporal.ChronoUnit;
import org.threeten.bp.temporal.TemporalAmount;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * {@link TimeSpan} extends {@link DecimalMeasure} with {@link #valueOf(String)}
 * for {@link Converters#CLASS_WITH_VALUE_OF_METHOD}.
 * <p>
 * Assumes {@linkplain Double#NaN} as value for illegal/empty value types
 * 
 * TODO consider using more complex JScience {@link Amount} as super type rather
 * than {@link DecimalMeasure}, providing more (exact) operations by default,
 * e.g. {@link Amount#plus(Amount)}, a la
 * <a href="https://www.jcp.org/en/jsr/detail?id=363">JSR 363</a> (see
 * <a href="https://github.com/unitsofmeasurement/uom-se">Java8 reference
 * implementation</a>)
 * 
 * @date $Date$
 * @version $Id$
 * @author <a href="mailto:rick@almende.org">Rick</a>
 */
@SuppressWarnings( "rawtypes" )
@JsonSerialize( using = TimeSpan.JsonSerializer.class )
@JsonDeserialize( using = TimeSpan.JsonDeserializer.class )
public class TimeSpan extends DecimalMeasure
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	// private static final Logger LOG = LogManager.getLogger(TimeSpan.class);

	/** */
	public static final Unit<Duration> MILLIS = SI.MILLI( SI.SECOND );

	/** */
	public static final Unit<Duration> NANOS = SI.NANO( SI.SECOND );

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
	public static final Measure<BigDecimal, Duration> parsePeriodOrMeasure( final String measure )
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
				final org.threeten.bp.Duration temp = org.threeten.bp.Duration.parse( measure );
				result = temp.getNano() == 0
						? DecimalMeasure.valueOf( BigDecimal.valueOf( temp.getSeconds() ), SI.SECOND )
						: DecimalMeasure.valueOf( BigDecimal.valueOf( temp.getSeconds() )
								.multiply( BigDecimal.TEN.pow( 9 ) ).add( BigDecimal.valueOf( temp.getNano() ) ),
								NANOS );
				// LOG.trace(
				// "Parsed '{}' using JSR-310 to JSR-275 measure/unit: {}",
				// measure, result);
				return result;
			} catch( final Exception e )
			{
				// LOG.trace("JSR-275 and JSR-310 failed, try Joda", e);
				final Period joda = Period.parse( measure );
				result = DecimalMeasure.valueOf( BigDecimal.valueOf( joda.toStandardDuration().getMillis() ), MILLIS );
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
	public TimeSpan( final Measure<? extends Number, ?> measure )
	{
		this( measure.getValue(), measure.getUnit() );
	}

	/**
	 * {@link TimeSpan} constructor
	 * 
	 * @param value
	 * @param unit
	 */
	@SuppressWarnings( "unchecked" )
	public TimeSpan( final Number value, final Unit unit )
	{
		super( value instanceof BigDecimal ? (BigDecimal) value : BigDecimal.valueOf( value.doubleValue() ), unit );
	}

	/**
	 * added for erasure-compatibility with
	 * {@link DecimalMeasure#valueOf(CharSequence)}
	 * 
	 * @see Converters.CLASS_WITH_VALUE_OF_METHOD
	 */
	// @SuppressWarnings("unchecked")
	// public static MeasurableDuration valueOf(final CharSequence value)
	// {
	// return valueOf(value.toString());
	// }

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param temporal
	 */
	public static Measurable valueOf( final TemporalAmount temporal )
	{
		return new TimeSpan(
				BigDecimal.valueOf( temporal.get( ChronoUnit.NANOS ) ).add(
						BigDecimal.valueOf( temporal.get( ChronoUnit.MILLIS ) ).multiply( BigDecimal.TEN.pow( 6 ) ) ),
				NANOS );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param value
	 */
	public static TimeSpan valueOf( final ReadableDuration value )
	{
		return new TimeSpan( BigDecimal.valueOf( value.getMillis() ), MILLIS );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param value
	 */
	public static TimeSpan valueOf( final Measure<? extends Number, Duration> value )
	{
		return new TimeSpan( value );
	}

	/**
	 * {@link TimeSpan} static factory method
	 * 
	 * @param value
	 */
	public static TimeSpan valueOf( final Amount<Duration> value )
	{
		return new TimeSpan( BigDecimal.valueOf( value.getEstimatedValue() ), value.getUnit() );
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
	public static TimeSpan valueOf( final String value )
	{
		return new TimeSpan( value );
	}

	/**
	 */
	public static TimeSpan valueOf( final Number millis )
	{
		return new TimeSpan( millis, MILLIS );
	}

	public static class JsonSerializer extends com.fasterxml.jackson.databind.JsonSerializer<TimeSpan>
	{
		public JsonSerializer()
		{
			// LOG.trace("Created " + getClass().getName());
		}

		@Override
		public void serialize( final TimeSpan value, final JsonGenerator gen, final SerializerProvider serializers )
			throws IOException, JsonProcessingException
		{
			final String result = value.toString();
			// if (value.getUnit().getClass() == ProductUnit.class)
			// LOG.trace("Serialized {} {} to: {}", value.getUnit().getClass()
			// .getSimpleName(), value, result, new RuntimeException());
			gen.writeString( result );
		}
	}

	public static class JsonDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<TimeSpan>
	{
		public JsonDeserializer()
		{
			// LOG.trace("Created " + getClass().getName());
		}

		@Override
		public TimeSpan deserialize( final JsonParser p, final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			final TimeSpan result = p.getCurrentToken().isNumeric() ? TimeSpan.valueOf( p.getNumberValue() )
					: TimeSpan.valueOf( p.getText() );
			// LOG.trace("Deserialized {} {} to: {}", p.getCurrentToken(),
			// p.getText(), result);
			return result;
		}
	}

}