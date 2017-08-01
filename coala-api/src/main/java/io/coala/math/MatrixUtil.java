/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance compute the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, computeOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;

import io.coala.exception.Thrower;
import io.coala.log.LogUtil;

/**
 * {@link MatrixUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MatrixUtil
{
	// TODO read default behavior from config
	private static boolean PARALLEL = true;

	/**
	 * FIXME work-around, see <a
	 * href=https://github.com/ujmp/universal-java-matrix-package/issues/22>ujmp
	 * issue 22</a>
	 * 
	 * @param source the matrix to traverse
	 * @param all whether to return all coordinates, or just the sparse ones
	 * @return a {@link Iterable} coordinate supplier for parallel traversal
	 */
	public static Iterable<long[]> coordinates( final Matrix source,
		final boolean all )
	{
		return () -> new Iterator<long[]>()
		{
			final long[] n = source.getSize(), x = Arrays.copyOf( n, n.length );
			Matrix row = null;

			@Override
			public long[] next()
			{
				return Arrays.copyOf( x, x.length ); // must copy to parallelize
			}

			@Override
			public boolean hasNext()
			{
				while( x[0] != -1 )
				{
					while( row == null && --x[0] != -1 )
					{
						row = source.selectRows( Ret.LINK, x[0] );
						x[1] = n[1];
					}
					if( row == null ) return false;

					while( --x[1] != -1 )
						if( all || row.containsCoordinates( 0, x[1] ) )
							return true; // lacks index[], may add log(n)*k ops

					row = null;
				}
				return false;
			}
		};
	}

	/** */
	public static Stream<long[]> stream( final Iterable<long[]> coords,
		final boolean parallel )
	{
		return StreamSupport.stream( coords.spliterator(), parallel );
	}

	/**
	 * @param source
	 * @return
	 */
	public static Stream<long[]>
		streamAvailableCoordinates( final Matrix source )
	{
		return streamAvailableCoordinates( source, PARALLEL );
	}

	/**
	 * @param source
	 * @return
	 */
	public static Stream<long[]> streamAvailableCoordinates(
		final Matrix source, final boolean parallel )
	{
		return stream( coordinates( source, false ), parallel );
	}

	/**
	 * @param source
	 * @return
	 */
	public static Stream<long[]> streamAllCoordinates( final Matrix source )
	{
		return streamAllCoordinates( source, PARALLEL );
	}

	/**
	 * @param source
	 * @return
	 */
	public static Stream<long[]> streamAllCoordinates( final Matrix source,
		final boolean parallel )
	{
		return stream( coordinates( source, true ), parallel );
	}

	// TODO for each 'primitive' type
	public static Stream<BigDecimal> streamBigDecimal( final Matrix m )
	{
		return streamAvailableCoordinates( m ).map( m::getAsBigDecimal );
	}

	// TODO for each 'primitive' type
	public static void forEachBigDecimal( final Matrix m,
		final BiConsumer<BigDecimal, long[]> consumer )
	{
		forEach( streamAvailableCoordinates( m ), m::getAsBigDecimal,
				consumer );
	}

	// TODO for each 'primitive' type
	public static <T> Stream<T> mapBigDecimal( final Matrix m,
		final BiFunction<BigDecimal, long[], T> mapper )
	{
		return map( streamAvailableCoordinates( m ), m::getAsBigDecimal,
				mapper );
	}

	// TODO for each 'primitive' type
	public static void insertObject( final boolean parallel,
		final Matrix target, final Matrix source, final long... targetOffset )
	{
		insert( streamAvailableCoordinates( source, parallel ),
				source::getAsObject, target::setAsObject, targetOffset );
	}

	// TODO for each 'primitive' type
	public static void insertBigDecimal( final Matrix target,
		final Matrix source, final long... targetOffset )
	{
		insert( streamAvailableCoordinates( source ), source::getAsBigDecimal,
				target::setAsBigDecimal, targetOffset );
	}

	public static Matrix computeBigDecimal( final Matrix m,
		final Function<BigDecimal, BigDecimal> func )
	{
		return compute( m, func, m::getAsBigDecimal, m::setAsBigDecimal );
	}

	public static Matrix computeBigInteger( final Matrix m,
		final Function<BigInteger, BigInteger> func )
	{
		return compute( m, func, m::getAsBigInteger, m::setAsBigInteger );
	}

	public static Matrix computeBoolean( final Matrix m,
		final Function<Boolean, Boolean> func )
	{
		return compute( m, func, m::getAsBoolean, m::setAsBoolean );
	}

	public static Matrix computeByte( final Matrix m,
		final Function<Byte, Byte> func )
	{
		return compute( m, func, m::getAsByte, m::setAsByte );
	}

	public static Matrix computeByteArray( final Matrix m,
		final Function<byte[], byte[]> func )
	{
		return compute( m, func, m::getAsByteArray, m::setAsByteArray );
	}

	public static Matrix computeChar( final Matrix m,
		final Function<Character, Character> func )
	{
		return compute( m, func, m::getAsChar, m::setAsChar );
	}

	public static Matrix computeDate( final Matrix m,
		final Function<Date, Date> func )
	{
		return compute( m, func, m::getAsDate, m::setAsDate );
	}

	public static Matrix computeDouble( final Matrix m,
		final Function<Double, Double> func )
	{
		return compute( m, func, m::getAsDouble, m::setAsDouble );
	}

	public static Matrix computeFloat( final Matrix m,
		final Function<Float, Float> func )
	{
		return compute( m, func, m::getAsFloat, m::setAsFloat );
	}

	public static Matrix computeInt( final Matrix m,
		final Function<Integer, Integer> func )
	{
		return compute( m, func, m::getAsInt, m::setAsInt );
	}

	public static Matrix computeLong( final Matrix m,
		final Function<Long, Long> func )
	{
		return compute( m, func, m::getAsLong, m::setAsLong );
	}

	public static Matrix computeMatrix( final Matrix m,
		final Function<Matrix, Matrix> func )
	{
		return compute( m, func, m::getAsMatrix, m::setAsMatrix );
	}

	public static Matrix computeObject( final Matrix m,
		final Function<Object, Object> func )
	{
		return compute( m, func, m::getAsObject, m::setAsObject );
	}

	public static Matrix computeShort( final Matrix m,
		final Function<Short, Short> func )
	{
		return compute( m, func, m::getAsShort, m::setAsShort );
	}

	public static Matrix computeString( final Matrix m,
		final Function<String, String> func )
	{
		return compute( m, func, m::getAsString, m::setAsString );
	}

	public static <T> void forEach( final Stream<long[]> coords,
		final Function<long[], T> getter, final BiConsumer<T, long[]> consumer )
	{
		coords.forEach(
				coord -> consumer.accept( getter.apply( coord ), coord ) );
	}

	public static <T, R> Stream<R> map( final Stream<long[]> coords,
		final Function<long[], T> getter,
		final BiFunction<T, long[], R> mapper )
	{
		return coords
				.map( coord -> mapper.apply( getter.apply( coord ), coord ) );
	}

	public static <T> Matrix compute( final Matrix m, final Function<T, T> func,
		final Function<long[], T> getter, final BiConsumer<T, long[]> setter )
	{
		streamAvailableCoordinates( m ).forEach( coord -> setter
				.accept( func.apply( getter.apply( coord ) ), coord ) );
		return m;
	}

	public static long[] add( final long[] coords, final long[] addendum )
	{
		if( addendum == null ) return coords;
		if( coords.length != addendum.length )
			Thrower.throwNew( IllegalArgumentException::new,
					() -> "Can't add " + Arrays.asList( addendum ) + " to "
							+ Arrays.asList( coords ) );
		return IntStream.range( 0, coords.length )
				.mapToLong( i -> coords[i] + addendum[i] ).toArray();
	}

	public static <T> void insert( final Stream<long[]> sourceCoords,
		final Function<long[], T> sourceGetter,
		final BiConsumer<T, long[]> targetSetter, final long... targetOffset )
	{
		sourceCoords.forEach( sourceCoord ->
		{
			try
			{
				final T v = sourceGetter.apply( sourceCoord );
				final long[] c = add( sourceCoord, targetOffset );
				targetSetter.accept( v, c );
			} catch( final Throwable e )
			{
				LogUtil.getLogger( MatrixUtil.class ).error(
						"Problem getting {} + {} -> {}", sourceCoord,
						targetOffset, add( sourceCoord, targetOffset ) );
				e.printStackTrace();
			}
		} );
	}
}