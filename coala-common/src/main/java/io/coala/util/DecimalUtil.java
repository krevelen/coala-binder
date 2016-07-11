package io.coala.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * {@link DecimalUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class DecimalUtil implements Util
{

	/** TODO from config? */
	public static final MathContext DEFAULT_CONTEXT = MathContext.DECIMAL128;

	/**
	 * {@link DecimalUtil} inaccessible singleton constructor
	 */
	private DecimalUtil()
	{
	}

	/**
	 * @return {@code true} iff the {@link BigDecimal} has scale {@code <=0}
	 * @see <a href="http://stackoverflow.com/a/12748321">stackoverflow
	 *      discussion</a>
	 */
	public static boolean isExact( final BigDecimal bd )
	{
		return bd.signum() == 0 || bd.scale() <= 0
				|| bd.stripTrailingZeros().scale() <= 0;
	}

	public static String toString( final double value, final int scale )
	{
		return BigDecimal.valueOf( value )
				.setScale( scale, DEFAULT_CONTEXT.getRoundingMode() )
				.toPlainString();
	}

	public static BigDecimal valueOf( final BigDecimal value )
	{
		return value;
	}

	public static BigDecimal valueOf( final BigInteger value )
	{
		return valueOf( value.longValueExact() );
	}

	public static BigDecimal valueOf( final long value )
	{
		return BigDecimal.valueOf( value );
	}

	public static BigDecimal valueOf( final Number value )
	{
		return BigDecimal.valueOf( value.doubleValue() );
	}

}
