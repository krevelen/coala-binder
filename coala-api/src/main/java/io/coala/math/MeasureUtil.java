package io.coala.math;

import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
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

	public static Amount<Dimensionless> toAmount( final long value )
	{
		return Amount.valueOf( value, Unit.ONE );
	}

	public static <Q extends Quantity> Amount<Q> toAmount( final long value,
		final Unit<Q> unit )
	{
		return Amount.valueOf( value, unit );
	}

	public static Amount<Dimensionless> toAmount( final BigInteger value )
	{
		return toAmount( value, Unit.ONE );
	}

	public static <Q extends Quantity> Amount<Q>
		toAmount( final BigInteger value, final Unit<Q> unit )
	{
		return toAmount( value.longValueExact(), unit );
	}

	public static Amount<Dimensionless> toAmount( final byte[] value )
	{
		return toAmount( value, Unit.ONE );
	}

	public static <Q extends Quantity> Amount<Q> toAmount( final byte[] value,
		final Unit<Q> unit )
	{
		return toAmount( DecimalUtil.valueOf( value ), unit );
	}

	public static Amount<Dimensionless> toAmount( final BigDecimal value )
	{
		return toAmount( value, Unit.ONE );
	}

	public static <Q extends Quantity> Amount<Q>
		toAmount( final BigDecimal value, final Unit<Q> unit )
	{
		return DecimalUtil.isExact( value )
				? toAmount( value.longValue(), unit )
				: Amount.valueOf( value.doubleValue(), unit );
	}

	public static Amount<Dimensionless> toAmount( final Number value )
	{
		return toAmount( value, Unit.ONE );
	}

	public static <Q extends Quantity> Amount<Q> toAmount( final Number value,
		final Unit<Q> unit )
	{
		if( value instanceof BigDecimal )
			return toAmount( (BigDecimal) value, unit );
		if( value instanceof BigInteger )
			return toAmount( (BigInteger) value, unit );
		if( value instanceof Long
				|| long.class.isAssignableFrom( value.getClass() ) )
			return toAmount( (long) value, unit );
		if( value instanceof Integer
				|| int.class.isAssignableFrom( value.getClass() ) )
			return toAmount( (int) value, unit );
		if( value instanceof Short
				|| short.class.isAssignableFrom( value.getClass() ) )
			return toAmount( (short) value, unit );
		if( value instanceof Byte
				|| byte.class.isAssignableFrom( value.getClass() ) )
			return toAmount( (byte) value, unit );
		return toAmount( BigDecimal.valueOf( value.doubleValue() ), unit );
	}

	public static <Q extends Quantity> Amount<Q>
		toAmount( final Measure<?, Q> prob )
	{
		final Unit<Q> unit = prob.getUnit();
		return prob.getValue() instanceof BigDecimal
				? toAmount( (BigDecimal) prob.getValue(), unit )
				: toAmount( prob.doubleValue( unit ), unit );
	}

	/**
	 * @param prob
	 * @return
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity> Amount<Q>
		toAmount( final Measurable<Q> prob )
	{
		if( prob instanceof Amount ) return (Amount<Q>) prob;
		if( prob instanceof Measure ) return toAmount( (Measure<?, Q>) prob );
		throw new IllegalArgumentException(
				"Unsupported: Amount <- " + prob.getClass().getName() );
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
						2 * asin( sqrt( Math.pow( sin( abs( lat1 - lat2 ) / 2 ),
								2 ) + cos( lat1 ) * cos( lat2 ) * Math.pow(
										sin( abs( lon1 - lon2 ) / 2 ), 2 ) ) ),
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

	public static <Q extends Quantity> Measure<?, Q>
		toMeasure( final Amount<Q> amount )
	{
		return DecimalMeasure.valueOf( toBigDecimal( amount ),
				amount.getUnit() );
	}

	public static <Q extends Quantity> BigDecimal
		toBigDecimal( final Amount<Q> amount )
	{
		return toBigDecimal( amount, amount.getUnit() );
	}

	public static <Q extends Quantity> BigDecimal
		toBigDecimal( final Amount<Q> amount, final Unit<Q> unit )
	{
		final Amount<Q> value = amount.to( unit );
		if( value.isExact() )
			return BigDecimal.valueOf( value.getExactValue() );
		final BigDecimal result = BigDecimal
				.valueOf( value.getEstimatedValue() );
//		LOG.trace( "Conversion ignores error {} => {}", amount, result );
		return result;
	}

	/**
	 * @param measure
	 * @return
	 */
	public static BigDecimal toBigDecimal( final Measure<?, ?> measure )
	{
		return toBigDecimal( measure, measure.getUnit() );
	}

	/**
	 * @param measure
	 * @return
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity> BigDecimal
		toBigDecimal( final Measure<?, Q> measure, final Unit<?> unit )
	{
		return measure instanceof DecimalMeasure
				? ((DecimalMeasure<Q>) measure).to( (Unit<Q>) unit ).getValue()
				: BigDecimal.valueOf( measure.doubleValue( (Unit<Q>) unit ) );
	}

	/**
	 * @param amount
	 * @param exponent
	 * @return
	 * @see Math#pow(double,double)
	 */
	public static <Q extends Quantity> Amount<?> pow( final Amount<Q> amount,
		final int exponent )
	{
		return amount.pow( exponent );
	}

	/**
	 * @param amount
	 * @param exponent
	 * @return value of undefined unit
	 */
	public static <Q extends Quantity> double pow( final Amount<Q> amount,
		final double exponent )
	{
		return Math.pow( amount.doubleValue( amount.getUnit() ), exponent );
	}

	/**
	 * @param measure
	 * @param exponent
	 * @return
	 * @see Math#pow(double,double)
	 */
	public static <Q extends Quantity> Measure<?, ?>
		pow( final Measure<?, Q> measure, final int exponent )
	{
		return DecimalMeasure.valueOf(
				BigDecimal.valueOf( Math.pow(
						measure.doubleValue( measure.getUnit() ), exponent ) ),
				measure.getUnit().pow( exponent ) );
	}

	/**
	 * @param measure
	 * @param exponent
	 * @return value of undefined unit
	 * @see Math#pow(double,double)
	 */
	public static <Q extends Quantity> double pow( final Measure<?, Q> measure,
		final double exponent )
	{
		return Math.pow( measure.doubleValue( measure.getUnit() ), exponent );
	}

	/**
	 * @param amount
	 * @return
	 * @see Math#floor(double)
	 */
	public static <Q extends Quantity> Amount<Q> floor( final Amount<Q> amount )
	{
		return amount.isExact() ? amount
				: Amount.valueOf(
						Math.floor( amount.doubleValue( amount.getUnit() ) ),
						amount.getUnit() );
	}

	/**
	 * @param measure
	 * @return
	 * @see Math#floor(double)
	 */
	public static <Q extends Quantity> Measure<?, Q>
		floor( final Measure<?, Q> measure )
	{
		return measure instanceof DecimalMeasure && DecimalUtil
				.isExact( ((DecimalMeasure<?>) measure).getValue() )
						? measure
						: DecimalMeasure.valueOf(
								BigDecimal.valueOf( Math.floor( measure
										.doubleValue( measure.getUnit() ) ) ),
								measure.getUnit() );
	}

	/**
	 * @param amount
	 * @return
	 * @see Math#ceil(double)
	 */
	public static <Q extends Quantity> Amount<Q> ceil( final Amount<Q> amount )
	{
		return amount.isExact() ? amount
				: Amount.valueOf(
						Math.ceil( amount.doubleValue( amount.getUnit() ) ),
						amount.getUnit() );
	}

	/**
	 * @param measure
	 * @return
	 * @see Math#ceil(double)
	 */
	public static <Q extends Quantity> Measure<?, Q>
		ceil( final Measure<?, Q> measure )
	{
		return measure instanceof DecimalMeasure && DecimalUtil
				.isExact( ((DecimalMeasure<?>) measure).getValue() )
						? measure
						: DecimalMeasure.valueOf(
								BigDecimal.valueOf( Math.ceil( measure
										.doubleValue( measure.getUnit() ) ) ),
								measure.getUnit() );
	}

	/**
	 * @param measure
	 * @param unit
	 * @return
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity> DecimalMeasure<Q>
		toUnit( final DecimalMeasure<Q> measure, final Unit<?> unit )
	{
		return measure.to( (Unit<Q>) unit, DecimalUtil.DEFAULT_CONTEXT );
	}

	/**
	 * apply {@link BigDecimal#compareTo(BigDecimal)} in stead of default
	 * {@link Double#compareTo(Double)} of {@link Measure#compareTo(Measurable)}
	 * or {@link Amount#compareTo(Measurable)}
	 * 
	 * @param timeSpan
	 * @param that
	 * @return
	 */
//	@SuppressWarnings( { "unchecked", "rawtypes" } )
//	public static int compareTo( final DecimalMeasure self,
//		final Measurable that )
//	{
//		LogUtil.getLogger( MeasureUtil.class ).trace( "Compare {} with {}",
//				self, that );
//		if( that instanceof DecimalMeasure ) return self.getValue().compareTo(
//				toUnit( (DecimalMeasure<?>) that, self.getUnit() ).getValue() );
//		return self.compareTo( that );
//	}
}
