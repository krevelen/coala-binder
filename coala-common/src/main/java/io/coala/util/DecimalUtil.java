package io.coala.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;

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

	// adopted from https://forum.processing.org/two/discussion/10384/bigdecimal-to-byte-array
	public static byte[] toByte( final BigDecimal num )
	{
		// write scale to first 4 bytes
		final int scale = num.scale();
		final byte[] scaleBytes = new byte[] { (byte) (scale >>> 24),
				(byte) (scale >>> 16), (byte) (scale >>> 8), (byte) (scale) };
		final BigInteger unscaled = new BigInteger(
				num.unscaledValue().toString() );
		final byte[] unscaledBytes = unscaled.toByteArray();
		final byte[] concat = Arrays.copyOf( scaleBytes,
				unscaledBytes.length + scaleBytes.length );
		System.arraycopy( unscaledBytes, 0, concat, scaleBytes.length,
				unscaledBytes.length );
		return concat;
	}

	// adopted from https://forum.processing.org/two/discussion/10384/bigdecimal-to-byte-array
	public static BigDecimal valueOf( final byte[] raw )
	{
		// read scale from first 4 bytes
		final int scale = (raw[0] & 0xFF) << 24 | (raw[1] & 0xFF) << 16
				| (raw[2] & 0xFF) << 8 | (raw[3] & 0xFF);
		final byte[] subset = Arrays.copyOfRange( raw, 4, raw.length );
		final BigInteger unscaled = new BigInteger( subset );
		return new BigDecimal( unscaled, scale );
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
		return value instanceof BigDecimal ? valueOf( (BigDecimal) value )
				: value instanceof Long || value instanceof Integer
						? valueOf( value.longValue() )
						: value instanceof BigInteger
								? valueOf( (BigInteger) value )
								: BigDecimal.valueOf( value.doubleValue() );
	}
}
