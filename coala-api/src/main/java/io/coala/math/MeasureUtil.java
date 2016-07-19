package io.coala.math;

import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.physics.amount.Amount;

import io.coala.util.Compare;
import io.coala.util.DecimalUtil;
import io.coala.util.Util;

/**
 * {@link MeasureUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MeasureUtil implements Util
{

	/**
	 * {@link MeasureUtil} inaccessible singleton constructor
	 */
	private MeasureUtil()
	{
	}

	public static <Q extends Quantity> Amount<Q> toAmount( final long value,
		final Unit<Q> unit )
	{
		return Amount.valueOf( value, unit );
	}

	public static <Q extends Quantity> Amount<Q>
		toAmount( final BigInteger value, final Unit<Q> unit )
	{
		return toAmount( value.longValueExact(), unit );
	}

	public static <Q extends Quantity> Amount<Q>
		toAmount( final BigDecimal value, final Unit<Q> unit )
	{
		return DecimalUtil.isExact( value )
				? toAmount( value.longValue(), unit )
				: Amount.valueOf( value.doubleValue(), unit );
	}

	public static <Q extends Quantity> Amount<Q> toAmount( final Number value,
		final Unit<Q> unit )
	{
		return toAmount( BigDecimal.valueOf( value.doubleValue() ), unit );
	}

	public static <Q extends Quantity> boolean
		isNegative( final Amount<Q> amount )
	{
		return !Compare.eq( amount, amount.abs() );
	}

	/**
	 * Calculates the angular distance or central angle between two points on a
	 * sphere, using the half-versed-sine or
	 * <a href="https://www.wikiwand.com/en/Haversine_formula">haversine
	 * formula</a> for great-circle distance
	 * 
	 * @param p1 a {@link LatLong}
	 * @param p2 another {@link LatLong}
	 * @return the {@link Amount} of central {@link Angle}
	 */
	public static Amount<Angle> angularDistance( final LatLong p1,
		final LatLong p2 )
	{
		final double lat1 = p1.latitudeValue( SI.RADIAN );
		final double lon1 = p1.longitudeValue( SI.RADIAN );
		final double lat2 = p2.latitudeValue( SI.RADIAN );
		final double lon2 = p2.longitudeValue( SI.RADIAN );
		return Amount
				.valueOf(
						2 * asin(
								sqrt( pow( sin( abs( lat1 - lat2 ) / 2 ), 2 )
										+ cos( lat1 ) * cos( lat2 )
												* pow( sin( abs( lon1 - lon2 )
														/ 2 ), 2 ) ) ),
						.000005, SI.RADIAN );
	}

	public static <Q extends Quantity> String
		toString( final Measure<?, Q> amount, final int scale )
	{
		return (amount.getValue() instanceof BigDecimal
				? ((BigDecimal) amount.getValue()).setScale( scale,
						RoundingMode.HALF_UP )
				: BigDecimal.valueOf( amount.doubleValue( amount.getUnit() ) )
						.setScale( scale, RoundingMode.HALF_UP ))
								.toPlainString()
				+ amount.getUnit();
	}

	public static String toString( final Amount<?> amount, final int scale )
	{
		return BigDecimal.valueOf( amount.getEstimatedValue() )
				.setScale( scale, RoundingMode.HALF_UP ).toPlainString()
				+ amount.getUnit();
	}

	public static <Q extends Quantity> Number toNumber( final Amount<Q> amount,
		final Unit<Q> unit )
	{
		final Amount<Q> result = amount.to( unit );
		return result.isExact() ? result.getExactValue()
				: result.getEstimatedValue();
	}

	public static <Q extends Quantity> BigDecimal
		toBigDecimal( final Amount<Q> amount )
	{
		return toBigDecimal( amount, amount.getUnit() );
	}

	public static <Q extends Quantity> BigDecimal
		toBigDecimal( final Amount<Q> amount, final Unit<Q> unit )
	{
		final Amount<Q> result = amount.to( unit );
		return result.isExact() ? BigDecimal.valueOf( result.getExactValue() )
				: BigDecimal.valueOf( result.getEstimatedValue() );
	}

	/**
	 * @param measure
	 * @return
	 */
	public static <Q extends Quantity> BigDecimal
		toBigDecimal( final Measure<?, Q> measure )
	{
		return toBigDecimal( measure, measure.getUnit() );
	}

	/**
	 * @param measure
	 * @return
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity> BigDecimal
		toBigDecimal( final Measure<?, Q> measure, final Unit<Q> unit )
	{
		return measure instanceof DecimalMeasure
				? ((DecimalMeasure<Q>) measure).to( unit ).getValue()
				: BigDecimal.valueOf( measure.doubleValue( unit ) );
	}
}
