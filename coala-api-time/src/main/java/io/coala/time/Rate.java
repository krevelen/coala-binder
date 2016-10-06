/* $Id: bfdc8d8bb88fbee407229d510a7619daeed3b992 $
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

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Frequency;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * {@link Rate} extends {@link DecimalMeasure} with {@link #valueOf(String)} for
 * {@link Converters#CLASS_WITH_VALUE_OF_METHOD}.
 * <p>
 * Assumes {@linkplain Double#NaN} as value for illegal/empty value types
 * 
 * @version $Id: bfdc8d8bb88fbee407229d510a7619daeed3b992 $
 * @author Rick van Krevelen
 */
@JsonSerialize( using = Rate.ToJsonString.class )
@JsonDeserialize( using = Rate.FromJsonString.class )
public class Rate extends DecimalMeasure<Frequency>
{

	/** */
	// private static final Logger LOG = LogManager.getLogger(Rate.class);

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link Rate} constructor for "natural" polymorphic Jackson bean
	 * deserialization
	 * 
	 * TODO handle {@link Double#NaN} etc.
	 * 
	 * @see com.fasterxml.jackson.databind.deser.BeanDeserializer
	 */
	public Rate( final String measure )
	{
		this( DecimalMeasure.valueOf( measure ) );
	}

	/**
	 * {@link Rate} constructor for "natural" polymorphic Jackson bean
	 * deserialization
	 * 
	 * @see com.fasterxml.jackson.databind.deser.BeanDeserializer
	 */
	public Rate( final double rate )
	{
		this( BigDecimal.valueOf( rate ), SI.HERTZ );
	}

	/**
	 * {@link Rate} constructor for "natural" polymorphic Jackson bean
	 * deserialization
	 * 
	 * @see com.fasterxml.jackson.databind.deser.BeanDeserializer
	 */
	public Rate( final int rate )
	{
		this( BigDecimal.valueOf( rate ), SI.HERTZ );
	}

	/**
	 * {@link Rate} constructor
	 * 
	 * @param measure
	 */
	public Rate( final Measure<BigDecimal, ?> measure )
	{
		this( measure.getValue(), measure.getUnit().asType( Frequency.class ) );
	}

	/**
	 * {@link Rate} constructor
	 * 
	 * @param value
	 * @param unit
	 */
	public Rate( final BigDecimal value, final Unit<Frequency> unit )
	{
		super( value, unit );
	}

	/**
	 * {@link Rate} static factory method
	 */
	public static Rate of( final long value, final Unit<Frequency> amount )
	{
		return of( BigDecimal.valueOf( value ), amount );
	}

	/**
	 * {@link Rate} static factory method
	 */
	public static Rate of( final Number value, final Unit<Frequency> amount )
	{
		return of( BigDecimal.valueOf( value.doubleValue() ), amount );
	}

	/**
	 * {@link Rate} static factory method
	 */
	public static Rate of( final BigDecimal value,
		final Unit<Frequency> amount )
	{
		return new Rate( value, amount );
	}

	/**
	 * {@link Rate} static factory method
	 * 
	 * @param measure
	 */
	public static Rate of( final Measure<? extends Number, Frequency> measure )
	{
		return measure.getValue() instanceof BigDecimal
				? of( (BigDecimal) measure.getValue(), measure.getUnit() )
				: of( measure.getValue(), measure.getUnit() );
	}

	/**
	 * {@link Rate} static factory method
	 * 
	 * @param amount
	 */
	public static Rate of( final Amount<Frequency> amount )
	{
		return amount.isExact() ? of( amount.getExactValue(), amount.getUnit() )
				: of( amount.getEstimatedValue(), amount.getUnit() );
	}

	/**
	 * {@link Rate} static factory method
	 * 
	 * @see Converters.CLASS_WITH_VALUE_OF_METHOD
	 */
	public static Rate valueOf( final String value )
	{
		return new Rate( value );
	}

	public static class ToJsonString extends JsonSerializer<Rate>
	{
		@Override
		public void serialize( final Rate value, final JsonGenerator gen,
			final SerializerProvider serializers )
			throws IOException, JsonProcessingException
		{
			gen.writeString( value.toString() );
		}
	}

	public static class FromJsonString extends JsonDeserializer<Rate>
	{
		@Override
		public Rate deserialize( final JsonParser p,
			final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			return Rate.valueOf( p.getText() );
		}
	}
}