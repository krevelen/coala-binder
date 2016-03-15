package io.coala.random;

import java.math.BigDecimal;

import io.coala.util.Util;

/**
 * {@link NumberUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class NumberUtil implements Util
{

	private NumberUtil()
	{
		// inaccessible singleton constructor
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

}
