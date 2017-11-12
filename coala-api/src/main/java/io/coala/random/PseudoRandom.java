/* $Id$
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.coala.random;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.SortedMap;

import javax.inject.Singleton;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Converter;

import com.eaio.uuid.UUID;

import io.coala.config.GlobalConfig;
import io.coala.exception.Thrower;
import io.coala.json.Wrapper.Util;
import io.coala.math.DecimalUtil;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.util.ArrayUtil;
import io.reactivex.Observable;

/**
 * {@link PseudoRandom} generates a stream of pseudo-random numbers, with an API
 * similar to the standard Java {@link Random} generator (which is wrapped
 * accordingly in the {@link JavaRandom} decorator)
 * <p>
 * <b>NOTE</b> Implement a thread-safe/multi-threaded default, e.g. <a href=
 * "https://gist.github.com/dhadka/f5a3adc36894cc6aebcaf3dc1bbcef9f">ThreadLocal</a>
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface PseudoRandom extends Identified<PseudoRandom.Name>
{
	Number seed();

	/** @see Random#nextBoolean() */
	boolean nextBoolean();

	/** @see Random#nextBytes(byte[]) */
	void nextBytes( byte[] bytes );

	// adopted from BigInteger#randomBits(int, Random)
	default byte[] nextBits( final int numBits )
	{
		if( numBits < 0 ) throw new IllegalArgumentException(
				"numBits must be non-negative" );
		int numBytes = (int) (((long) numBits + 7) / 8); // avoid overflow
		byte[] randomBits = new byte[numBytes];

		// Generate random bytes and mask out any excess bits
		if( numBytes > 0 )
		{
			nextBytes( randomBits );
			int excessBits = 8 * numBytes - numBits;
			randomBits[0] &= (1 << (8 - excessBits)) - 1;
		}
		return randomBits;
	}

	/** @see Random#nextInt() */
	int nextInt();

	/** @see Random#nextInt(int) */
	int nextInt( int bound );

	/** @see Random#nextLong() */
	long nextLong();

	/**
	 * @param bound > 0 (exclusive)
	 * @return 0 <= x < bound
	 * @see Random#nextInt(int)
	 */
	default long nextLong( final long bound )
	{
		if( bound < 0 ) return Thrower.throwNew( IllegalArgumentException::new,
				() -> "bound < 0" );

		// skip 2^n matching, as per http://stackoverflow.com/a/2546186/1418999
		long bits, val;
		do
		{
			bits = (nextLong() << 1) >>> 1;
			val = bits % bound;
		} while( bits - val + (bound - 1) < 0L );

		return val;
	}

	/** @see Random#nextFloat() */
	float nextFloat();

	/**
	 * @return next {@link Double} (i.e. 64-bit precision) floating-point value
	 *         &isin; [0,1]
	 * 
	 * @see Random#nextDouble()
	 */
	double nextDouble();

	/** @see Random#nextGaussian() */
	double nextGaussian();

	/** @return next 128-bits positive {@link BigInteger} */
	default BigInteger nextBigInteger()
	{
		return nextBigInteger( 1, 128 );
	}

	/** @return next numBits-precision {@link BigInteger} with given signum */
	default BigInteger nextBigInteger( final int signum, final int numBits )
	{
		return new BigInteger( signum, nextBits( numBits ) );
	}

	/** @return next 64-bit/double precision {@link BigDecimal} &isin; [0,1] */
	default BigDecimal nextBigDecimal()
	{
		return DecimalUtil.valueOf( nextDouble() );
	}

	/**
	 * @return next <em>n</em>-bit precision {@link BigDecimal} &isin; [0,1],
	 *         e.g. {@code numBits=128} for {@link MathContext#DECIMAL128}
	 */
	default BigDecimal nextBigDecimal( int numBits )
	{
		return DecimalUtil.valueOf( nextBits( numBits ) );
	}

	/**
	 * @param elements an ordered collection
	 * @return next element drawn with uniform probability
	 */
	default <E> E nextElement( final List<E> elements )
	{
		if( Objects.requireNonNull( elements, "Missing values" ).isEmpty() )
			return Thrower.throwNew( IllegalArgumentException::new,
					() -> "Nothing to pick from" );
		if( elements.size() == 1 ) return elements.get( 0 );
		return nextElement( elements, 0, elements.size() );
	}

	/**
	 * 0 =< min =< max =< (n - 1)
	 * 
	 * @param elements non-empty ordered set
	 * @param min lower index bound (inclusive) 0 =< min =< max
	 * @param max upper index bound (exclusive) max > 0
	 * @return element at the next index drawn with uniform probability
	 * @see Random#nextInt(int)
	 */
	default <E> E nextElement( final List<E> elements, final int min,
		final int max )
	{
		// sanity check
		if( Objects.requireNonNull( elements ).isEmpty() ) return Thrower
				.throwNew( IllegalArgumentException::new, () -> "empty" );
		if( min < 0 ) return Thrower.throwNew( IllegalArgumentException::new,
				() -> "min < 0" );
		if( min >= elements.size() )
			return Thrower.throwNew( ArrayIndexOutOfBoundsException::new,
					() -> "min > size" );
		if( max < min ) return Thrower.throwNew( IllegalArgumentException::new,
				() -> "max < min" );
		if( max > elements.size() )
			return Thrower.throwNew( ArrayIndexOutOfBoundsException::new,
					() -> "max > size" );
		if( elements.size() == 1 ) return elements.get( 0 );
		return elements.get( min + nextInt( max - min ) );
	}

	/**
	 * <b>NOTE</b> if the elements are not ordered, e.g. a {@link HashSet}, then
	 * results are not guaranteed to be reproducible
	 * 
	 * @param elements the {@link Collection}
	 * @return element at the next index drawn with uniform probability
	 */
	default <E> E nextElement( final Collection<E> elements )
	{
		if( elements instanceof List ) return nextElement( (List<E>) elements );
		if( Objects.requireNonNull( elements ).isEmpty() ) return Thrower
				.throwNew( IllegalArgumentException::new, () -> "empty" );
		return nextElement( elements, elements.size() );
	}

	/**
	 * <b>NOTE</b> if the elements are not ordered, e.g. a {@link HashSet}, then
	 * results are not guaranteed to be reproducible
	 * 
	 * @param elements the {@link Collection}
	 * @param max bound > 0 (exclusive)
	 * @return element at the next index drawn with uniform probability
	 * @see Random#nextInt(int)
	 */
	default <E> E nextElement( final Collection<E> elements, final long max )
	{
		if( elements instanceof List )
			return nextElement( (List<E>) elements, 0, (int) max );
		if( Objects.requireNonNull( elements ).isEmpty() ) return Thrower
				.throwNew( IllegalArgumentException::new, () -> "empty" );
		return nextElement( (Iterable<E>) elements, max );
	}

	/**
	 * <b>NOTE</b> if the elements are not ordered, e.g. a {@link HashSet}, then
	 * results are not guaranteed to be reproducible
	 * 
	 * @param elements the {@link Iterable}
	 * @param max bound > 0 (exclusive)
	 * @return the next random element
	 * @see Random#nextInt(int)
	 */
	default <E> E nextElement( final Iterable<E> elements, final long max )
	{
		if( elements instanceof List )
			return nextElement( (List<E>) elements, 0, (int) max );

		final Iterator<E> it = Objects.requireNonNull( elements ).iterator();

		final long n = nextLong( max );
		for( long i = 0; it.hasNext(); it.next() )
			if( n == i++ ) return it.next();

		return Thrower.throwNew( IndexOutOfBoundsException::new,
				() -> "Out of bounds: " + max + " > size ("
						+ (elements instanceof Collection
								? ((Collection<E>) elements).size()
								: elements.getClass().getSimpleName())
						+ ")" );
	}

	/**
	 * <b>NOTE</b> if the elements are not {@link Observable#sorted()}, then
	 * results are not guaranteed to be reproducible
	 * 
	 * @param elements the {@link Observable} stream
	 * @param max bound > 0 (exclusive)
	 * @return the next random element
	 * @see Random#nextInt(int)
	 */
	default <E> E nextElement( final Observable<E> elements, final long max )
	{
		return elements.elementAt( nextLong( max - 1 ) ).blockingGet();
	}

	/**
	 * @param elements the {@link SortedMap}
	 * @return the next random element
	 */
	default <K, V> Map.Entry<K, V> nextEntry( final SortedMap<K, V> elements )
	{
		return nextElement( elements.entrySet(), elements.size() );
	}

	/**
	 * <b>NOTE</b> if the elements are not ordered, e.g. a {@link HashMap}, then
	 * results are not guaranteed to be reproducible
	 * 
	 * @param elements the {@link Map}
	 * @return the next random element
	 */
	default <K, V> Map.Entry<K, V> nextEntry( final Map<K, V> elements )
	{
		return nextElement( elements.entrySet(), elements.size() );
	}

	/**
	 * {@link Name}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Name extends Id.Ordinal<String>
	{
		/**
		 * @param value the {@link String} value
		 * @return the new {@link Name}
		 */
		public static Name of( final CharSequence value )
		{
			return Util.of( value.toString(), new Name() );
		}
	}

	/**
	 * {@link Config}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Config extends GlobalConfig
	{
		String NAME_KEY = "random.id";

		String NAME_DEFAULT = "RNG";

		String SEED_KEY = "random.seed";

		String SEED_DEFAULT = "NaN";

		@Key( NAME_KEY )
		@DefaultValue( NAME_DEFAULT )
		@ConverterClass( NameConverter.class )
		Name id();

		@Key( SEED_KEY )
		@DefaultValue( SEED_DEFAULT )
		@ConverterClass( SystemMillisConverter.class )
		BigInteger seed();

		class NameConverter implements Converter<Name>
		{
			@Override
			public Name convert( final Method method, final String input )
			{
				return Util.of( input == null || input.isEmpty()
						|| input.equalsIgnoreCase( "null" ) ? NAME_DEFAULT
								: input,
						Name.class );
			}
		}

		class SystemMillisConverter implements Converter<BigInteger>
		{
			private static final ByteBuffer BYTE_BUFFER = ByteBuffer
					.allocate( 2 * Long.BYTES );

			@Override
			public BigInteger convert( final Method method, final String input )
			{
				try
				{
					return new BigInteger( input );
				} catch( final Throwable t )
				{
					final UUID uuid = new UUID();
					// with help from http://stackoverflow.com/a/4485196
					BYTE_BUFFER.putLong( 0, uuid.getTime() );
					BYTE_BUFFER.putLong( 8, uuid.getClockSeqAndNode() );
					// for int/long conversion: most dynamic bytes last
					ArrayUtil.reverse( BYTE_BUFFER.array() );
					return new BigInteger( BYTE_BUFFER.array() );
				}
			}
		}
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Factory
	{
		/**
		 * @return a {@link PseudoRandom}
		 */
		default PseudoRandom create()
		{
			return create( ConfigCache.getOrCreate( Config.class ) );
		}

		/**
		 * @param config the {@link Config}
		 * @return a {@link PseudoRandom}
		 */
		default PseudoRandom create( final Config config )
		{
			return create( config.id(), config.seed() );
		}

		/**
		 * @return a {@link PseudoRandom}
		 */
		default PseudoRandom create( final CharSequence id )
		{
			return create( id, id.hashCode() );
		}

		/**
		 * @return a {@link PseudoRandom}
		 */
		default PseudoRandom create( final CharSequence id, final Number seed )
		{
			return create( Name.of( id.toString() ), seed );
		}

		/**
		 * @return a {@link PseudoRandom}
		 */
		PseudoRandom create( Name id, Number seed );
	}

	/**
	 * {@link JavaRandom} decorates a standard Java {@link Random} generator as
	 * {@link PseudoRandom}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public class JavaRandom extends Random implements PseudoRandom
	{
		/** the serialVersionUID */
		private static final long serialVersionUID = 1L;

		public static JavaRandom of( final Name id, final long seed )
		{
			final JavaRandom result = new JavaRandom();
			result.id = id;
			result.setSeed( seed );
			return result;
		}

		/** the id */
		private Name id;

		/** the seed */
		private Long seed;

		@Override
		public void setSeed( final long seed )
		{
			super.setSeed( seed );
			this.seed = seed;
		}

		@Override
		public Name id()
		{
			return this.id;
		}

		@Override
		public Long seed()
		{
			return this.seed;
		}

		@Singleton
		public static class Factory implements PseudoRandom.Factory
		{
			private final static Factory INSTANCE = new Factory();

			public static final Factory instance()
			{
				return INSTANCE;
			}

			@Override
			public JavaRandom create( final Name id, final Number seed )
			{
				return JavaRandom.of( id, seed.longValue() );
			}
		}
	}
}