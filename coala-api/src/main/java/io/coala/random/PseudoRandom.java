/* $Id: 1af879e91e793fc6b991cfc2da7cb93928527b4b $
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Converter;

import com.eaio.uuid.UUID;

import io.coala.config.GlobalConfig;
import io.coala.json.Wrapper.Util;
import io.coala.name.Id;
import io.coala.name.Identified;

/**
 * {@link PseudoRandom} generates a stream of pseudo-random numbers, with an API
 * similar to the standard Java {@link Random} generator (which is wrapped
 * accordingly in the {@link JavaRandom} decorator)
 * 
 * @version $Id: 1af879e91e793fc6b991cfc2da7cb93928527b4b $
 * @author Rick van Krevelen
 */
public interface PseudoRandom extends Identified<PseudoRandom.Name>
{
	Number seed();

	/** @see Random#nextBoolean() */
	boolean nextBoolean();

	/** @see Random#nextBytes(byte[]) */
	void nextBytes( byte[] bytes );

	/** @see Random#nextInt() */
	int nextInt();

	/** @see Random#nextInt(int) */
	int nextInt( int bound );

	/** @see Random#nextLong() */
	long nextLong();

	/** @see Random#nextFloat() */
	float nextFloat();

	/** @see Random#nextDouble() */
	double nextDouble();

	/** @see Random#nextGaussian() */
	double nextGaussian();

	/**
	 * @param elements
	 * @return
	 */
	default <E> E nextElement( final List<E> elements )
	{
		if( elements == null || elements.isEmpty() ) return null;
		return elements.get( nextInt( elements.size() ) );
	}

	/**
	 * NOTE that if the {@link Collection} is not ordered, e.g. a
	 * {@link HashSet}, then results are not guaranteed reproducible
	 * 
	 * @param elements the {@link Collection}
	 * @return the next random element
	 */
	default <E> E nextElement( final Collection<E> elements )
	{
		if( elements == null || elements.isEmpty() ) return null;
		return nextElement( elements, elements.size() );
	}

	/**
	 * NOTE that if the {@link Map} is not ordered, e.g. a {@link HashMap}, then
	 * results are not guaranteed reproducible
	 * 
	 * @param elements the {@link Collection}
	 * @return the next random element
	 */
	default <K, V> Map.Entry<K, V> nextEntry( final Map<K, V> elements )
	{
		if( elements == null || elements.isEmpty() ) return null;
		return nextElement( elements.entrySet(), elements.size() );
	}

	/**
	 * NOTE that if the {@link Iterable} is not ordered, e.g. a {@link HashSet},
	 * then results are not guaranteed reproducible
	 * 
	 * @param elements the {@link Iterable}
	 * @param size the n
	 * @return the next random element
	 */
	default <E> E nextElement( final Iterable<E> elements, final int size )
	{
		if( elements == null || size < 1 ) return null;
		final Iterator<E> it = elements.iterator();
		for( int i = 0, n = nextInt( size ); it.hasNext(); i++ )
			if( i == n )
				return it.next();
			else
				it.next();
		throw new IllegalStateException();
	}

	/**
	 * {@link Name}
	 * 
	 * @version $Id: 1af879e91e793fc6b991cfc2da7cb93928527b4b $
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
		BigDecimal seed();

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

		class SystemMillisConverter implements Converter<BigDecimal>
		{
			private static Long DEFAULT = null;

			@Override
			public BigDecimal convert( final Method method, final String input )
			{
				try
				{
					final BigDecimal result = new BigDecimal( input );
					DEFAULT = result.longValue();
					return result;
				} catch( final Throwable t )
				{
					return BigDecimal.valueOf( DEFAULT == null
							? new UUID().getTime() : ++DEFAULT );
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
			return create( ConfigFactory.create( Config.class ) );
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
	 * @version $Id: 1af879e91e793fc6b991cfc2da7cb93928527b4b $
	 * @author Rick van Krevelen
	 */
	class JavaRandom extends Random implements PseudoRandom
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

		public static class Factory implements PseudoRandom.Factory
		{
			@Override
			public JavaRandom create( final Name id, final Number seed )
			{
				return JavaRandom.of( id, seed.longValue() );
			}
		}
	}
}