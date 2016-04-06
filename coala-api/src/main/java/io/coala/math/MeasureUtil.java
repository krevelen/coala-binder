package io.coala.math;

import java.math.BigDecimal;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

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
}
