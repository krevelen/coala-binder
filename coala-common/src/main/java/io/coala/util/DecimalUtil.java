package io.coala.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * {@link DecimalUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class DecimalUtil implements Util
{

	/** TODO from config? */
	public static final MathContext DECIMAL_PRECISION = MathContext.DECIMAL128;

	/**
	 * {@link DecimalUtil} inaccessible singleton constructor
	 */
	private DecimalUtil()
	{
	}

	/**
	 * see <a href="http://stackoverflow.com/a/12748321">stackoverflow
	 * discussion</a>
	 */
	public static boolean isExact( final BigDecimal bd )
	{
		return bd.signum() == 0 || bd.scale() <= 0
				|| bd.stripTrailingZeros().scale() <= 0;
	}

	public static String toString( final double value, final int scale )
	{
		return BigDecimal.valueOf( value )
				.setScale( scale, RoundingMode.HALF_UP ).toPlainString();
	}

	public static BigDecimal valueOf( final Number value )
	{
		return value instanceof BigDecimal ? (BigDecimal) value
				: value instanceof Long || long.class.isInstance( value )
						|| value instanceof Integer
						|| int.class.isInstance( value )
						|| value instanceof Short
						|| short.class.isInstance( value )
						|| value instanceof BigInteger
								? BigDecimal.valueOf( value.longValue() )
								: BigDecimal.valueOf( value.doubleValue() );
	}

}
