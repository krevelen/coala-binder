package io.coala.math;

import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

	public static <Q extends Quantity> Amount<Q> toAmount( final Number value,
		final Unit<Q> unit )
	{
		if( value instanceof Long || value instanceof Integer
				|| (value instanceof BigDecimal
						&& DecimalUtil.isExact( (BigDecimal) value )) )
			return Amount.valueOf( value.longValue(), unit );
		return Amount.valueOf( value.doubleValue(), unit );
	}

	public static boolean isNegative( final Amount<?> amount )
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

	public static <Q extends Quantity, U extends Unit<Q>> String
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
}
