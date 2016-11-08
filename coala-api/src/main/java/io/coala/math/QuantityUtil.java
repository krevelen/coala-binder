package io.coala.math;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Time;

import org.joda.time.Period;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.coala.exception.Thrower;
import io.coala.util.Util;
import tec.uom.se.AbstractUnit;
import tec.uom.se.ComparableQuantity;
import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.unit.Units;

/**
 * {@link QuantityUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( { "rawtypes", "serial" } )
public class QuantityUtil implements Util
{

	public static final SimpleModule JSON_MODULE = new SimpleModule()
			.addKeyDeserializer( Quantity.class, new KeyDeserializer()
			{
				@Override
				public Object deserializeKey( final String key,
					final DeserializationContext ctxt )
				{
					return QuantityUtil.valueOf( key );
				}
			} )
			.addDeserializer( Quantity.class, new JsonDeserializer<Quantity>()
			{
				@Override
				public Quantity deserialize( final JsonParser p,
					final DeserializationContext ctxt ) throws IOException
				{
					if( p.getCurrentToken().isNumeric() )
						return QuantityUtil.valueOf( p.getNumberValue() );

					if( p.getCurrentToken().isScalarValue() )
						return QuantityUtil.valueOf( p.getValueAsString() );

					final TreeNode tree = p.readValueAsTree();
					if( tree.size() == 0 ) return null;

					return Thrower.throwNew( IOException.class,
							"Problem parsing {} from {}",
							Quantity.class.getSimpleName(), tree );
				}
			} ).addSerializer( new StdSerializer<Quantity>( Quantity.class )
			{
				@Override
				public void serialize( final Quantity value,
					final JsonGenerator gen,
					final SerializerProvider serializers ) throws IOException
				{
					gen.writeString( QuantityUtil.toString( value ) );
				}
			} );

	/** dimension one, for pure or {@link Dimensionless} quantities */
	public static final Unit<Dimensionless> PURE = AbstractUnit.ONE;

	public static final ComparableQuantity<Dimensionless> ZERO = valueOf(
			BigDecimal.ZERO, PURE );

	public static final ComparableQuantity<Dimensionless> ONE = valueOf(
			BigDecimal.ONE, PURE );

	/**
	 * {@link QuantityUtil} inaccessible singleton constructor
	 */
	private QuantityUtil()
	{
	}

	private static final Set<ObjectMapper> JSON_REGISTERED = new HashSet<>();

	public synchronized static void checkRegistered( final ObjectMapper om )
	{
		if( !JSON_REGISTERED.contains( om ) )
			JSON_REGISTERED.add( om.registerModule( JSON_MODULE ) );
	}

	/**
	 * @param value
	 * @return
	 */
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		valueOf( final Quantity<Q> value )
	{
		return value instanceof ComparableQuantity
				? (ComparableQuantity<Q>) value
				: valueOf( value.getValue(), value.getUnit() );
	}

	/**
	 * @param value
	 * @param unit
	 * @return
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		valueOf( final Quantity<?> value, final Unit<Q> unit )
	{
		if( value.getUnit() == Units.RADIAN && unit == Units.DEGREE_ANGLE )
			return valueOf( DecimalUtil.toDegrees( value.getValue() ), unit );
		if( value.getUnit() == Units.DEGREE_ANGLE && unit == Units.RADIAN )
			return valueOf( DecimalUtil.toRadians( value.getValue() ), unit );
		return valueOf( (Quantity<Q>) value ).to( unit );
	}

	@SuppressWarnings( "rawtypes" )
	public static ComparableQuantity valueOf( final CharSequence str )
	{
		return tec.uom.se.quantity.Quantities.getQuantity( str );
	}

	/**
	 * @param input
	 * @param unitFormat
	 * @return
	 */
	public static ComparableQuantity valueOf( final CharSequence str,
		final SimpleUnitFormat unitFormat )
	{
		final String[] split = str.toString().split( "\\s+" );
		return split.length < 2 ? valueOf( DecimalUtil.valueOf( str ) )
				: valueOf( DecimalUtil.valueOf( split[0] ),
						unitFormat.parse( split[1] ) );
	}

	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		valueOf( final Number value, final Unit<Q> unit )
	{
		return tec.uom.se.quantity.Quantities.getQuantity(
				value.getClass().getPackage() == Number.class.getPackage()
						? value : DecimalUtil.valueOf( value ),
				unit );
	}

	public static ComparableQuantity<Dimensionless>
		valueOf( final Number value )
	{
		return valueOf( value, AbstractUnit.ONE );
	}

	public static Unit<?> unitOf( final Object value )
	{
		return value instanceof Number ? AbstractUnit.ONE
				: value instanceof Quantity ? ((Quantity<?>) value).getUnit()
						: Thrower.throwNew( IllegalArgumentException.class,
								"Can't determine unit for {}",
								value.getClass() );
	}

	/**
	 * Attempt parsing a {@link Quantity JSR-363} time measurement, expecting
	 * either:
	 * <ul>
	 * <li>{@link Time} units (e.g. {@code "123 ms"});</li>
	 * <li>{@link Dimensionless} units (e.g. {@code "123 "}); or</li>
	 * <li>ISO 8601 time period, parsed using
	 * {@link java.time.Duration#parse(CharSequence) JSR-310 Duration} or
	 * {@link Period#parse(String) Joda Period}.</li>
	 * </ul>
	 * 
	 * Examples of ISO 8601 time period:
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
	 * @return a {@linkComparableQuantity}
	 * 
	 * @see tec.uom.se.format.QuantityFormat#getInstance(tec.uom.se.format.FormatBehavior)
	 * @see java.time.Duration#parse(String)
	 * @see org.joda.time.format.ISOPeriodFormat#standard()
	 */
	public static final ComparableQuantity<?>
		parseDuration( final String measure )
	{
		if( measure == null ) return null;
		try
		{
			return valueOf( measure );
		} catch( final Exception e )
		{
			try
			{
				final java.time.Duration temp = java.time.Duration
						.parse( measure );
				return valueOf(
						BigDecimal.valueOf( temp.getSeconds() )
								.add( temp.getNano() == 0 ? BigDecimal.ZERO
										: BigDecimal.valueOf( temp.getNano() )
												.divide( BigDecimal.TEN
														.pow( 9 ) ) ),
						Units.SECOND );
			} catch( final Exception f )
			{
				try
				{
					final Period joda = Period.parse( measure );
					return valueOf(
							BigDecimal
									.valueOf( joda.toStandardDuration()
											.getMillis() )
									.divide( BigDecimal.TEN.pow( 3 ) ),
							Units.SECOND );
				} catch( final Exception g )
				{
					return Thrower.throwNew( IllegalArgumentException.class,
							"Unable to parse '{}' with JSR-363: '{}'"
									+ ", JSR-310: '{}', Joda: '{}'",
							measure, parsedStringOrMessage( e ), f.getMessage(),
							g.getMessage() );
				}
			}
		}
	}

	/** TODO remove when bug is fixed in uom-se */
	@Deprecated
	public static String parsedStringOrMessage( final Throwable e )
	{
		return e.getCause() instanceof ParserException
				? ((ParserException) e.getCause()).getParsedString()
				: e instanceof ParserException
						? ((ParserException) e).getParsedString()
						: e.getMessage();
	}

	public static String toString( final Quantity<?> amount )
	{
		return toBigDecimal( amount ).toPlainString() + " " + amount.getUnit();
	}

	public static String toString( final Quantity<?> amount, final int scale )
	{
		return toBigDecimal( amount ).setScale( scale, RoundingMode.HALF_UP )
				.toPlainString() + " " + amount.getUnit();
	}

	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> Number
		toNumber( final Quantity<?> amount, final Unit<Q> unit )
	{
		return ((Quantity<Q>) amount).to( unit ).getValue();
	}

	public static BigDecimal toBigDecimal( final Quantity<?> amount )
	{
		return toBigDecimal( amount, amount.getUnit() );
	}

	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> BigDecimal
		toBigDecimal( final Quantity<?> amount, final Unit<Q> unit )
	{
		return DecimalUtil
				.valueOf( ((Quantity<Q>) amount).to( unit ).getValue() );
	}

	public static boolean isNegative( final Quantity<?> amount )
	{
		return toBigDecimal( amount ).signum() < 0;
	}

	/**
	 * @param amount
	 * @param exponent
	 * @return
	 * @see DecimalUtil#pow(double,double)
	 */
	public static Quantity<?> pow( final Quantity<?> amount,
		final int exponent )
	{
		return valueOf( DecimalUtil.pow( amount.getValue(), exponent ),
				amount.getUnit().pow( exponent ) );
	}

	/**
	 * @param measure
	 * @param exponent
	 * @return value of undefined unit
	 * @see Math#pow(double,double)
	 */
	public static Number pow( final Quantity<?> measure, final Number exponent )
	{
		return DecimalUtil.pow( measure.getValue(), exponent );
	}

	/**
	 * @param measure
	 * @return
	 * @see Math#floor(double)
	 */
	public static <Q extends Quantity<Q>> Quantity<Q>
		floor( final Quantity<Q> measure )
	{
		return DecimalUtil.isExact( measure.getValue() ) ? measure
				: valueOf( DecimalUtil.floor( measure.getValue() ),
						measure.getUnit() );
	}

	/**
	 * @param measure
	 * @return
	 * @see Math#ceil(double)
	 */
	public static <Q extends Quantity<Q>> Quantity<Q>
		ceil( final Quantity<Q> measure )
	{
		return DecimalUtil.isExact( measure.getValue() ) ? measure
				: valueOf( DecimalUtil.ceil( measure.getValue() ),
						measure.getUnit() );
	}

	/**
	 * @param t
	 * @return
	 */
	public static <Q extends Quantity<Q>> Quantity<Q>
		abs( final Quantity<Q> measure )
	{
		return DecimalUtil.signum( measure.getValue() ) >= 0 ? measure
				: valueOf( DecimalUtil.abs( measure.getValue() ),
						measure.getUnit() );
	}

	public static Quantity<?> sqrt( final Quantity<?> measure )
	{
		return root( measure, 2 );
	}

	public static Quantity<?> root( final Quantity<?> measure, final int n )
	{
		return valueOf( DecimalUtil.root( measure.getValue(), n ),
				measure.getUnit().root( n ) );
	}

	/**
	 * @param qty
	 * @return
	 */
	public static <Q extends Quantity<Q>> int intValue( final Quantity<?> qty )
	{
		return DecimalUtil.intValue( qty.getValue() );
	}

	/**
	 * @param qty
	 * @param unit
	 * @return
	 */
	public static <Q extends Quantity<Q>> int intValue( final Quantity<?> qty,
		final Unit<Q> unit )
	{
		return DecimalUtil.intValue( valueOf( qty, unit ).getValue() );
	}

	/**
	 * @param qty
	 * @return
	 */
	public static <Q extends Quantity<Q>> long
		longValue( final Quantity<?> qty )
	{
		return DecimalUtil.longValue( qty.getValue() );
	}

	/**
	 * @param qty
	 * @param unit
	 * @return
	 */
	public static <Q extends Quantity<Q>> long longValue( final Quantity<?> qty,
		final Unit<Q> unit )
	{
		return DecimalUtil.longValue( valueOf( qty, unit ).getValue() );
	}

	/**
	 * @param qty
	 * @return
	 */
	public static <Q extends Quantity<Q>> float
		floatValue( final Quantity<?> qty )
	{
		return DecimalUtil.floatValue( qty.getValue() );
	}

	/**
	 * @param qty
	 * @param unit
	 * @return
	 */
	public static <Q extends Quantity<Q>> float
		floatValue( final Quantity<?> qty, final Unit<Q> unit )
	{
		return DecimalUtil.floatValue( valueOf( qty, unit ).getValue() );
	}

	/**
	 * @param qty
	 * @return
	 */
	public static <Q extends Quantity<Q>> double
		doubleValue( final Quantity<?> qty )
	{
		return DecimalUtil.doubleValue( qty.getValue() );
	}

	/**
	 * @param qty
	 * @param unit
	 * @return
	 */
	public static <Q extends Quantity<Q>> double
		doubleValue( final Quantity<?> qty, final Unit<Q> unit )
	{
		return DecimalUtil.doubleValue( valueOf( qty, unit ).getValue() );
	}
}
