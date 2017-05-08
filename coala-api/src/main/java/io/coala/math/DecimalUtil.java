package io.coala.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.aeonbits.owner.ConfigCache;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;
import org.apfloat.Apint;

import io.coala.util.Util;

/**
 * {@link DecimalUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class DecimalUtil implements Util
{

	/** */
	public static final MathContext DEFAULT_CONTEXT = ConfigCache
			.getOrCreate( DecimalConfig.class ).createMathContext();

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

	/**
	 * @return {@code true} iff the {@link BigDecimal} has scale {@code <=0}
	 * @see <a href="http://stackoverflow.com/a/12748321">stackoverflow
	 *      discussion</a>
	 */
	public static boolean isExact( final Apfloat value )
	{
		return value.signum() == 0 || value.truncate().equals( value );
	}

	/**
	 * @param value
	 * @return
	 */
	public static boolean isExact( final Number value )
	{
		// TODO Auto-generated method stub
		return value instanceof Long || value instanceof Integer
				|| value instanceof Short || value instanceof Byte
				|| value instanceof BigInteger || value instanceof Apint ? true
						: value instanceof Apfloat ? isExact( (Apfloat) value )
								: isExact( valueOf( value ) );
	}

	public static String toString( final double value, final int scale )
	{
		return BigDecimal.valueOf( value )
				.setScale( scale, DEFAULT_CONTEXT.getRoundingMode() )
				.toPlainString();
	}

	/**
	 * adopted from <a href=
	 * "https://forum.processing.org/two/discussion/10384/bigdecimal-to-byte-array">
	 * orthoptera (apr 2015)</a>
	 * 
	 * @param num the {@link BigDecimal} to encode
	 * @return raw big-endian two's-complement binary representation with first
	 *         4 bytes representing the scale
	 * @see BigInteger#toByteArray()
	 * @see BigDecimal#unscaledValue()
	 */
	public static byte[] toByteArray( final Number value )
	{
		final BigDecimal num = valueOf( value );
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

	public static Apfloat toApfloat( final Number value )
	{
		return value instanceof BigDecimal ? new Apfloat( (BigDecimal) value )
				: value instanceof Long || value instanceof Integer
						|| value instanceof Short || value instanceof Byte
								? new Apfloat( value.longValue() )
								: value instanceof BigInteger
										? new Apfloat( (BigInteger) value )
										: new Apfloat( valueOf( value ) );
	}

	public static BigDecimal valueOf( final Apfloat value )
	{
		return new BigDecimal( value.toString() );
	}

	public static BigDecimal valueOf( final CharSequence value )
	{
		return new BigDecimal( value.toString() );
	}

	// 
	/**
	 * adopted from <a href=
	 * "https://forum.processing.org/two/discussion/10384/bigdecimal-to-byte-array">
	 * orthoptera (apr 2015)</a>
	 * 
	 * @param raw big-endian two's-complement binary representation with first 4
	 *            bytes representing the scale. The value of the
	 *            {@code BigDecimal} is
	 *            <tt>(unscaledVal &times; 10<sup>-scale</sup>)</tt>.
	 * @return a {@link BigDecimal}
	 * @see BigInteger#BigInteger(byte[])
	 * @see BigDecimal#BigDecimal(BigInteger,int)
	 */
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
		return value instanceof BigDecimal ? (BigDecimal) value
				: value instanceof Long || value instanceof Integer
						|| value instanceof Short || value instanceof Byte
								? valueOf( value.longValue() )
								: value instanceof Apfloat
										? valueOf( (Apfloat) value )
										: value instanceof BigInteger
												? valueOf( (BigInteger) value )
												: BigDecimal.valueOf(
														value.doubleValue() );
	}

	/**
	 * for difference between scale (decimals) and precision (significance), see
	 * e.g. http://stackoverflow.com/a/13461270/1418999
	 * 
	 * @param value the {@link Number} to scale
	 * @param scale the number of decimals
	 * @see BigDecimal.setScale(int, RoundingMode)
	 */
	public static BigDecimal toScale( final Number value, final int scale )
	{
		return valueOf( value ).setScale( scale,
				DEFAULT_CONTEXT.getRoundingMode() );
	}

	public static BigDecimal round( final Number value )
	{
		return toScale( value, 0 );
	}

	/**
	 * @param value the {@link BigDecimal} to round
	 * @return the rounded int value
	 * @see BigDecimal.setScale(int, RoundingMode)
	 */
	public static int toInt( final Number value )
	{
		return value instanceof Integer ? (Integer) value
				: round( value ).intValue();
	}

	/**
	 * @param value the {@link BigDecimal} to round
	 * @return the rounded long value
	 * @see BigDecimal.setScale(int, RoundingMode)
	 */
	public static long toLong( final Number value )
	{
		return value instanceof Long ? (Long) value
				: round( value ).longValue();
	}

	/**
	 * @param value the {@link Number} to truncate
	 * @return a truncated int value
	 * @see BigDecimal#intValue()
	 */
	public static int intValue( final Number value )
	{
		return value instanceof Integer ? (Integer) value
				: valueOf( value ).intValue();
	}

	/**
	 * @param value the {@link Number} to truncate
	 * @return a truncated long value
	 * @see BigDecimal#longValue()
	 */
	public static long longValue( final Number value )
	{
		return value instanceof Long ? (Long) value
				: valueOf( value ).longValue();
	}

	/**
	 * @param value the {@link Number} to truncate
	 * @return a truncated float value
	 * @see BigDecimal#floatValue()
	 */
	public static float floatValue( final Number value )
	{
		return value instanceof Float ? (Float) value
				: valueOf( value ).floatValue();
	}

	/**
	 * @param value the {@link Number} to truncate
	 * @return a truncated double value
	 * @see BigDecimal#doubleValue()
	 */
	public static double doubleValue( final Number value )
	{
		return value instanceof Double ? (Double) value
				: valueOf( value ).doubleValue();
	}

	private static final Apfloat TWO = new Apint( 2 );

	/**
	 * Binary (information) entropy
	 * 
	 * @param p(x)
	 * @return H(X) = -SUM_x p(x) * log_2 p(x)
	 * @see https://www.wikiwand.com/en/Binary_entropy_function
	 */
	public static Apfloat binaryEntropy( final Apfloat... probabilities )
	{
		Apfloat result = Apfloat.ZERO;
		for( Apfloat p : probabilities )
			result = result.subtract( p.equals( Apint.ZERO ) ? Apint.ZERO
					: p.multiply( ApfloatMath.log( p, TWO ) ) );
		return result;
	}

	/**
	 * Binary (information) entropy of bernoulli process (coin flip: p v. 1-p)
	 * 
	 * @param p(x)
	 * @return H(X) = -SUM_x p(x) * log_2 p(x)
	 * @see https://www.wikiwand.com/en/Binary_entropy_function
	 */
	public static Apfloat binaryEntropy( final Apfloat p )
	{
		return binaryEntropy( p, Apfloat.ONE.subtract( p ) );
	}

	/**
	 * Binary (information) entropy of bernoulli process (coin flip: p v. 1-p)
	 * 
	 * @param p(x)
	 * @return H(X) = -SUM_x p(x) * log_2 p(x)
	 * @see https://www.wikiwand.com/en/Binary_entropy_function
	 */
	public static BigDecimal binaryEntropy( final Number prob )
	{
		return valueOf( binaryEntropy( toApfloat( prob ) ) );
	}

	/**
	 * @param degrees
	 * @return
	 */
	public static BigDecimal toRadians( final Number degrees )
	{
		return valueOf( ApfloatMath.toRadians( toApfloat( degrees ) ) );
	}

	/**
	 * @param radians
	 * @return
	 */
	public static BigDecimal toDegrees( final Number radians )
	{
		return valueOf( ApfloatMath.toDegrees( toApfloat( radians ) ) );
	}

	/**
	 * @param value
	 * @param subtrahend
	 * @return the {@link BigDecimal} subtraction
	 */
	public static BigDecimal subtract( final Number value,
		final Number subtrahend )
	{
		return valueOf( value ).subtract( valueOf( subtrahend ),
				DEFAULT_CONTEXT );
	}

	/**
	 * @param value
	 * @param augend
	 * @return the {@link BigDecimal} addition
	 */
	public static BigDecimal add( final Number value, final Number augend )
	{
		return valueOf( value ).add( valueOf( augend ), DEFAULT_CONTEXT );
	}

	/**
	 * @param value
	 * @param multiplicand
	 * @return the {@link BigDecimal} multiplication
	 */
	public static BigDecimal multiply( final Number value,
		final Number multiplicand )
	{
		return valueOf( value ).multiply( valueOf( multiplicand ),
				DEFAULT_CONTEXT );
	}

	/**
	 * @param dividend the numerator
	 * @param divisor the denominator
	 * @return the {@link BigDecimal} division
	 */
	public static BigDecimal divide( final Number dividend,
		final Number divisor )
	{
		return valueOf( dividend ).divide( valueOf( divisor ),
				DEFAULT_CONTEXT );
	}

	public static Number floor( final Number value )
	{
		return isExact( value ) ? value
				: value instanceof Apfloat ? ((Apfloat) value).floor()
						: valueOf( value ).setScale( 0, RoundingMode.FLOOR );
	}

	public static Number ceil( final Number value )
	{
		return isExact( value ) ? value
				: value instanceof Apfloat ? ((Apfloat) value).ceil()
						: valueOf( value ).setScale( 0, RoundingMode.CEILING );
	}

	/**
	 * @param value
	 * @return
	 * @see Apfloat#signum()
	 * @see BigDecimal#signum()
	 */
	public static int signum( final Number value )
	{
		return value instanceof Apfloat ? ((Apfloat) value).signum()
//				: value instanceof Double ? Math.signum( (Double) value )
//						: value instanceof Float ? Math.signum( (Float) value )
				: valueOf( value ).signum();
	}

	/**
	 * @param value
	 * @return
	 * @see Math#abs(int)
	 * @see Math#abs(long)
	 * @see Math#abs(float)
	 * @see Math#abs(double)
	 * @see ApfloatMath#abs(Apfloat)
	 * @see BigDecimal#abs(MathContext)
	 */
	public static Number abs( final Number value )
	{
		return value instanceof Apfloat ? ApfloatMath.abs( (Apfloat) value )
				: value instanceof Integer ? Math.abs( (Integer) value )
						: value instanceof Long ? Math.abs( (Long) value )
								: value instanceof Double
										? Math.abs( (Double) value )
										: value instanceof Float
												? Math.abs( (Float) value )
												: valueOf( value )
														.abs( DEFAULT_CONTEXT );
	}

	public static Apfloat sqrt( final Number value )
	{
		return root( value, 2 );
	}

	public static Apfloat root( final Number value, final long n )
	{
		return ApfloatMath.root( toApfloat( value ), n );
	}

	public static Number pow( final Number value, final Number exponent )
	{
		return exponent instanceof Integer || exponent instanceof Short
				|| exponent instanceof Byte
						? pow( value, exponent.intValue() )
						: exponent instanceof Long
								? ApfloatMath.pow( toApfloat( value ),
										exponent.longValue() )
								: ApfloatMath.pow( toApfloat( value ),
										toApfloat( exponent ) );
	}

	public static Number pow( final Number value, final int exponent )
	{
		return value instanceof Apfloat
				? pow( (Apfloat) value, (long) exponent )
				: valueOf( value ).pow( exponent, DEFAULT_CONTEXT );
	}

	public static final BigDecimal KILO = BigDecimal.TEN.pow( 3 );

	public static final BigDecimal MEGA = BigDecimal.TEN.pow( 6 );

	/**
	 * @param posix the POSIX {@link ZonedDateTime} time stamp (seconds + nanos)
	 * @return the rounded milliseconds
	 */
	public static BigInteger toMillis( final ZonedDateTime posix )
	{
		return posix == null ? null
				: round( valueOf( posix.toEpochSecond() ).multiply( KILO )
						.add( valueOf( posix.getNano() ).divide( MEGA ) ) )
								.toBigIntegerExact();
	}

	/**
	 * @param value
	 * @return
	 */
	public static boolean isZero( final Number value )
	{
		return signum( value ) == 0;
	}
}
