package io.coala.math;

import static tec.uom.se.format.FormatBehavior.LOCALE_NEUTRAL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import javax.measure.format.UnitFormat;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Time;

import org.joda.time.Period;

import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil.Pretty;
import io.coala.util.Compare;
import io.coala.util.Util;
import tec.uom.se.AbstractUnit;
import tec.uom.se.ComparableQuantity;
import tec.uom.se.format.QuantityFormat;
import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.unit.Units;

/**
 * {@link QuantityUtil}
 * 
 * @version $Id: ebccf2937ccb8d5ea580a52d363364edc746a709 $
 * @author Rick van Krevelen
 */
@SuppressWarnings( { "rawtypes"/* , "serial" */ } )
public class QuantityUtil implements Util
{

	static
	{
		// add unit labels
		SimpleUnitFormat.getInstance().label( Units.DEGREE_ANGLE, "deg" );
		QuantityJsonModule.checkRegistered( JsonUtil.getJOM() );
	}

	/** dimension one, for pure or {@link Dimensionless} quantities */
	public static final Unit<Dimensionless> PURE = AbstractUnit.ONE;

	/** the number ZERO */
	public static final ComparableQuantity<Dimensionless> ZERO = valueOf(
			BigDecimal.ZERO, PURE );

	/** the number ONE */
	public static final ComparableQuantity<Dimensionless> ONE = valueOf(
			BigDecimal.ONE, PURE );

	/**
	 * {@link QuantityUtil} inaccessible singleton constructor
	 */
	private QuantityUtil()
	{
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

	/** TODO remove when degree/radian conversions is fixed in JSR-363 uom-se */
	@Deprecated
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		toUnit( final Quantity<Q> value, final Unit<Q> unit )
	{
		if( value.getUnit() == Units.RADIAN && unit == Units.DEGREE_ANGLE )
			return valueOf( DecimalUtil.toDegrees( value.getValue() ), unit );
		if( value.getUnit() == Units.DEGREE_ANGLE && unit == Units.RADIAN )
			return valueOf( DecimalUtil.toRadians( value.getValue() ), unit );
		return valueOf( value ).to( unit );
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
		return toUnit( (Quantity<Q>) value, unit );
	}

	public static ComparableQuantity valueOf( final CharSequence csq )
	{
//		return tec.uom.se.quantity.Quantities.getQuantity( csq );
		try
		{
			return QuantityFormat.getInstance( LOCALE_NEUTRAL ).parse( csq,
					new ParsePosition( 0 ) );
		} catch( final ParserException e )
		{
			throw new IllegalArgumentException( e.getParsedString(), e );
		}
	}

	public static ComparableQuantity valueOf( final CharSequence str,
		final UnitFormat unitFormat )
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
						: Thrower.throwNew( IllegalArgumentException::new,
								() -> "Can't determine unit for "
										+ value.getClass() );
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
	 * @param qty the {@link String} representation of a (relative) duration
	 * @return a {@linkComparableQuantity}
	 * 
	 * @see tec.uom.se.format.QuantityFormat#getInstance(tec.uom.se.format.FormatBehavior)
	 * @see java.time.Duration#parse(String)
	 * @see org.joda.time.format.ISOPeriodFormat#standard()
	 */
	public static final ComparableQuantity<?> parseDuration( final String qty )
	{
		if( qty == null ) return null;
		try
		{
			return valueOf( qty );
		} catch( final Exception e )
		{
			try
			{
				final java.time.Duration java8iso = java.time.Duration
						.parse( qty );
				return valueOf(
						BigDecimal.valueOf( java8iso.getSeconds() )
								.add( java8iso.getNano() == 0 ? BigDecimal.ZERO
										: BigDecimal
												.valueOf( java8iso.getNano() )
												.divide( BigDecimal.TEN
														.pow( 9 ) ) ),
						Units.SECOND );
			} catch( final Exception f )
			{
				try
				{
					final Period jodaIso = Period.parse( qty );
					return valueOf(
							BigDecimal
									.valueOf( jodaIso.toStandardDuration()
											.getMillis() )
									.divide( BigDecimal.TEN.pow( 3 ) ),
							Units.SECOND );
				} catch( final Exception g )
				{
					return Thrower.throwNew( IllegalArgumentException::new,
							() -> "Unable to parse '" + qty
									+ "' with JSR-363: '"
									+ parsedStringOrMessage( e )
									+ "', JSR-310: '" + f.getMessage()
									+ "', Joda: '" + g.getMessage() + "'" );
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

	public static String toString( final Quantity qty )
	{
		return decimalValue( qty ).toPlainString() + " " + qty.getUnit();
	}

	public static String toString( final Quantity qty, final int scale )
	{
		return decimalValue( qty ).setScale( scale, RoundingMode.HALF_UP )
				.toPlainString() + " " + qty.getUnit();
	}

	@SuppressWarnings( "unchecked" )
	public static Number toNumber( final Quantity qty, final Unit unit )
	{
		return qty.to( unit ).getValue();
	}

	public static BigDecimal decimalValue( final Quantity qty, final Unit unit,
		final int scale )
	{
		return DecimalUtil.toScale( toNumber( qty, unit ), scale );
	}

	public static BigDecimal decimalValue( final Quantity qty, final Unit unit )
	{
		return DecimalUtil.valueOf( toNumber( qty, unit ) );
	}

	public static BigDecimal decimalValue( final Quantity qty )
	{
		return DecimalUtil.valueOf( qty.getValue() );
	}

	public static boolean isNegative( final Quantity<?> qty )
	{
		return decimalValue( qty ).signum() < 0;
	}

	/**
	 * @see DecimalUtil#pow(double,double)
	 */
	public static Quantity<?> pow( final Quantity<?> qty, final int exponent )
	{
		return valueOf( DecimalUtil.pow( qty.getValue(), exponent ),
				qty.getUnit().pow( exponent ) );
	}

	/**
	 * @return value of undefined unit
	 * @see Math#pow(double,double)
	 */
	public static Number pow( final Quantity<?> qty, final Number exponent )
	{
		return DecimalUtil.pow( qty.getValue(), exponent );
	}

	/**
	 * applies {@link DecimalUtil#DEFAULT_CONTEXT} to avoid
	 * {@link ArithmeticException} due to non-terminating decimal expansion
	 * 
	 * @see Quantity#inverse()
	 * @see tec.uom.se.quantity.DecimalQuantity
	 */
	public static Quantity<?> inverse( final Quantity<?> value )
	{
		return valueOf( DecimalUtil.inverse( value.getValue() ),
				value.getUnit().inverse() );
	}

	/**
	 * @see Math#floor(double)
	 */
	public static <Q extends Quantity<Q>> Quantity<Q>
		floor( final Quantity<Q> qty )
	{
		return DecimalUtil.isExact( qty.getValue() ) ? qty
				: valueOf( DecimalUtil.floor( qty.getValue() ), qty.getUnit() );
	}

	/**
	 * @see Math#ceil(double)
	 */
	public static <Q extends Quantity<Q>> Quantity<Q>
		ceil( final Quantity<Q> qty )
	{
		return DecimalUtil.isExact( qty.getValue() ) ? qty
				: valueOf( DecimalUtil.ceil( qty.getValue() ), qty.getUnit() );
	}

	/**
	 */
	public static <Q extends Quantity<Q>> Quantity<Q>
		abs( final Quantity<Q> qty )
	{
		return DecimalUtil.signum( qty.getValue() ) >= 0 ? qty
				: valueOf( DecimalUtil.abs( qty.getValue() ), qty.getUnit() );
	}

	/**
	 * @param qty the {@link Quantity}
	 * @return the square root {@link Quantity} value/unit
	 * @see {@link DecimalUtil#root(Number,long)}
	 * @see {@link Unit#root(int)}
	 */
	public static Quantity<?> sqrt( final Quantity<?> quantity )
	{
		return root( quantity, 2 );
	}

	/**
	 * @param qty the {@link Quantity}
	 * @return the root {@link Quantity} value/unit
	 * @see {@link DecimalUtil#root(Number,long)}
	 * @see {@link Unit#root(int)}
	 */
	public static Quantity<?> root( final Quantity<?> qty, final int n )
	{
		return valueOf( DecimalUtil.root( qty.getValue(), n ),
				qty.getUnit().root( n ) );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated int value
	 * @see {@link DecimalUtil#intValue(Number)}
	 */
	public static <Q extends Quantity<Q>> int intValue( final Quantity<?> qty )
	{
		return DecimalUtil.intValue( qty.getValue() );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated int value
	 * @see {@link DecimalUtil#intValue(Number)}
	 */
	public static <Q extends Quantity<Q>> int intValue( final Quantity<?> qty,
		final Unit<Q> unit )
	{
		return DecimalUtil.intValue( valueOf( qty, unit ).getValue() );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated long value
	 * @see {@link DecimalUtil#longValue(Number)}
	 */
	public static <Q extends Quantity<Q>> long
		longValue( final Quantity<?> qty )
	{
		return DecimalUtil.longValue( qty.getValue() );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated long value
	 * @see {@link DecimalUtil#longValue(Number)}
	 */
	public static <Q extends Quantity<Q>> long longValue( final Quantity<?> qty,
		final Unit<Q> unit )
	{
		return DecimalUtil.longValue( valueOf( qty, unit ).getValue() );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated float value
	 * @see {@link DecimalUtil#floatValue(Number)
	 */
	public static <Q extends Quantity<Q>> float
		floatValue( final Quantity<?> qty )
	{
		return DecimalUtil.floatValue( qty.getValue() );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated float value
	 * @see {@link DecimalUtil#floatValue(Number)
	 */
	public static <Q extends Quantity<Q>> float
		floatValue( final Quantity<?> qty, final Unit<Q> unit )
	{
		return DecimalUtil.floatValue( valueOf( qty, unit ).getValue() );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated double value
	 * @see {@link DecimalUtil#doubleValue(Number)
	 */
	public static <Q extends Quantity<Q>> double
		doubleValue( final Quantity<?> qty )
	{
		return DecimalUtil.doubleValue( qty.getValue() );
	}

	/**
	 * @param qty the {@link Quantity} to truncate
	 * @return a truncated double value
	 * @see {@link DecimalUtil#doubleValue(Number)
	 */
	public static <Q extends Quantity<Q>> double
		doubleValue( final Quantity<?> qty, final Unit<Q> unit )
	{
		return DecimalUtil.doubleValue( valueOf( qty, unit ).getValue() );
	}

	public static <Q extends Quantity<Q>> Quantity<Q>
		min( final Quantity<Q> qty1, final Quantity<Q> qty2 )
	{
		return valueOf( qty1 ).compareTo( valueOf( qty2 ) ) > 0 ? qty2 : qty2;
	}

	public static <Q extends Quantity<Q>> Quantity<Q>
		max( final Quantity<Q> qty1, final Quantity<Q> qty2 )
	{
		return valueOf( qty1 ).compareTo( valueOf( qty2 ) ) > 0 ? qty1 : qty2;
	}

	public static boolean approximates( final Quantity<Angle> qty1,
		final Quantity<Angle> qty2, final int precision )
	{
		final BigDecimal v1 = DecimalUtil.toScale( qty1.getValue(),
				precision - 1 );
		final BigDecimal v2 = DecimalUtil.toScale( qty2.getValue(),
				precision - 1 );
		return Compare.eq( v1, v2 );
	}

	/**
	 * @see BigDecimal#precision()
	 */
	public static int precision( final Quantity<?> qty )
	{
		return DecimalUtil.valueOf( qty.getValue() ).precision();
	}

	/**
	 * @see BigDecimal#scale()
	 */
	public static int scale( final Quantity<?> qty )
	{
		return DecimalUtil.valueOf( qty.getValue() ).scale();
	}

	/**
	 * @see DecimalUtil#toScale(Number, int)
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		toScale( final Quantity<Q> qty, final Unit unit, final int scale )
	{
		return valueOf( decimalValue( qty, unit, scale ), unit );
	}

	/**
	 * @see DecimalUtil#toScale(Number, int)
	 */
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		toScale( final Quantity<Q> qty, final int scale )
	{
		return valueOf( DecimalUtil.toScale( qty.getValue(), scale ),
				qty.getUnit() );
	}

	/**
	 * @see BigDecimal#signum()
	 */
	public static int signum( final Quantity<?> qty )
	{
		return DecimalUtil.signum( qty.getValue() );
	}

	private static final Map<Unit, ComparableQuantity> UNIT_ZEROES = new HashMap<>(),
			UNIT_ONES = new HashMap<>();

	/**
	 * @param dimension
	 * @return zero
	 */
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		zero( final Class<Q> dimension )
	{
		return zero( Units.getInstance().getUnit( dimension ) );
	}

	/**
	 * @param unit
	 * @return zero
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		zero( final Unit<Q> unit )
	{
		return UNIT_ZEROES.computeIfAbsent( unit,
				key -> valueOf( BigDecimal.ZERO, unit ) );
	}

	/**
	 * @param dimension
	 * @return one
	 */
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		one( final Class<Q> dimension )
	{
		return one( Units.getInstance().getUnit( dimension ) );
	}

	/**
	 * @param unit
	 * @return one
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> ComparableQuantity<Q>
		one( final Unit<Q> unit )
	{
		return UNIT_ONES.computeIfAbsent( unit,
				key -> valueOf( BigDecimal.ONE, unit ) );
	}

	/** @return a {@link Pretty} wrapper for lazy {@link #toString()} */
	public static <Q extends Quantity<Q>> Pretty pretty( final Quantity<Q> qty,
		final int scale )
	{
		return Pretty.of( () -> toScale( qty, scale ) );
	}

	/** @return a {@link Pretty} wrapper for lazy {@link #toString()} */
	public static <Q extends Quantity<Q>> Pretty pretty( final Quantity<Q> qty,
		final Unit<Q> unit, final int scale )
	{
		return Pretty.of( () -> toScale( qty.to( unit ), scale ) );
	}
}
