package io.coala.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.format.ParserException;
import javax.measure.format.UnitFormat;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Time;

import org.joda.time.Period;

import io.coala.exception.Thrower;
import io.coala.util.Compare;
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

	static
	{
		// add unit labels
		SimpleUnitFormat.getInstance().label( Units.DEGREE_ANGLE, "deg" );
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
					return Thrower.throwNew( IllegalArgumentException.class,
							"Unable to parse '{}' with JSR-363: '{}'"
									+ ", JSR-310: '{}', Joda: '{}'",
							qty, parsedStringOrMessage( e ), f.getMessage(),
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

	public static String toString( final Quantity<?> qty )
	{
		return toBigDecimal( qty ).toPlainString() + " " + qty.getUnit();
	}

	public static String toString( final Quantity<?> qty, final int scale )
	{
		return toBigDecimal( qty ).setScale( scale, RoundingMode.HALF_UP )
				.toPlainString() + " " + qty.getUnit();
	}

	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> Number
		toNumber( final Quantity<?> qty, final Unit<Q> unit )
	{
		return ((Quantity<Q>) qty).to( unit ).getValue();
	}

	public static BigDecimal toBigDecimal( final Quantity<?> amount )
	{
		return toBigDecimal( amount, amount.getUnit() );
	}

	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> BigDecimal
		toBigDecimal( final Quantity<?> qty, final Unit<Q> unit )
	{
		return DecimalUtil.valueOf( ((Quantity<Q>) qty).to( unit ).getValue() );
	}

	public static boolean isNegative( final Quantity<?> amount )
	{
		return toBigDecimal( amount ).signum() < 0;
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

	public static Quantity<?> sqrt( final Quantity<?> quantity )
	{
		return root( quantity, 2 );
	}

	public static Quantity<?> root( final Quantity<?> qty, final int n )
	{
		return valueOf( DecimalUtil.root( qty.getValue(), n ),
				qty.getUnit().root( n ) );
	}

	public static <Q extends Quantity<Q>> int intValue( final Quantity<?> qty )
	{
		return DecimalUtil.intValue( qty.getValue() );
	}

	public static <Q extends Quantity<Q>> int intValue( final Quantity<?> qty,
		final Unit<Q> unit )
	{
		return DecimalUtil.intValue( valueOf( qty, unit ).getValue() );
	}

	public static <Q extends Quantity<Q>> long
		longValue( final Quantity<?> qty )
	{
		return DecimalUtil.longValue( qty.getValue() );
	}

	public static <Q extends Quantity<Q>> long longValue( final Quantity<?> qty,
		final Unit<Q> unit )
	{
		return DecimalUtil.longValue( valueOf( qty, unit ).getValue() );
	}

	public static <Q extends Quantity<Q>> float
		floatValue( final Quantity<?> qty )
	{
		return DecimalUtil.floatValue( qty.getValue() );
	}

	public static <Q extends Quantity<Q>> float
		floatValue( final Quantity<?> qty, final Unit<Q> unit )
	{
		return DecimalUtil.floatValue( valueOf( qty, unit ).getValue() );
	}

	public static <Q extends Quantity<Q>> double
		doubleValue( final Quantity<?> qty )
	{
		return DecimalUtil.doubleValue( qty.getValue() );
	}

	public static <Q extends Quantity<Q>> double
		doubleValue( final Quantity<?> qty, final Unit<Q> unit )
	{
		return DecimalUtil.doubleValue( valueOf( qty, unit ).getValue() );
	}

	public <Q extends Quantity<Q>> Quantity<Q> min(
		final ComparableQuantity<Q> qty1, final ComparableQuantity<Q> qty2 )
	{
		return Compare.min( qty1, qty2 );
	}

	public <Q extends Quantity<Q>> Quantity<Q> max(
		final ComparableQuantity<Q> qty1, final ComparableQuantity<Q> qty2 )
	{
		return Compare.max( qty1, qty2 );
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
	public static int precision( final Quantity<Angle> qty )
	{
		return DecimalUtil.valueOf( qty.getValue() ).precision();
	}

	/**
	 * @see BigDecimal#scale()
	 */
	public static int scale( final Quantity<Angle> qty )
	{
		return DecimalUtil.valueOf( qty.getValue() ).scale();
	}

	/**
	 * @see BigDecimal#signum()
	 */
	public static int signum( final Quantity<?> qty )
	{
		return DecimalUtil.signum( qty.getValue() );
	}
}
