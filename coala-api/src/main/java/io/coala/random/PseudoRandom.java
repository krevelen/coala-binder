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

import static org.aeonbits.owner.util.Collections.entry;
import static org.aeonbits.owner.util.Collections.map;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Converter;

import com.eaio.uuid.UUID;

import io.coala.config.GlobalConfig;
import io.coala.json.Wrapper.Util;
import io.coala.name.x.Id;

/**
 * {@link PseudoRandom} generates a stream of pseudo-random numbers, with an API
 * similar to the standard Java {@link Random} generator (which is wrapped
 * accordingly in the {@link JavaPseudoRandom} decorator)
 * 
 * @version $Id: 1af879e91e793fc6b991cfc2da7cb93928527b4b $
 * @author Rick van Krevelen
 */
public interface PseudoRandom
{
	Number getSeed();

	Name getId();

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
		public static Name of( final String value )
		{
			return Util.of( value, new Name() );
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
				return Util
						.valueOf(
								input == null || input.isEmpty()
										|| input.equalsIgnoreCase( "null" )
												? NAME_DEFAULT : input,
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
		PseudoRandom create();

		/**
		 * @return a {@link PseudoRandom}
		 */
		PseudoRandom create( CharSequence id, Number seed );

		/**
		 * @param config the {@link Config}
		 * @return a {@link PseudoRandom}
		 */
		PseudoRandom create( Config config );

	}

	/**
	 * {@link JavaPseudoRandom} decorates a standard Java {@link Random}
	 * generator as {@link PseudoRandom}
	 * 
	 * @version $Id: 1af879e91e793fc6b991cfc2da7cb93928527b4b $
	 * @author Rick van Krevelen
	 */
	class JavaPseudoRandom implements PseudoRandom
	{
		public static class Factory implements PseudoRandom.Factory
		{
			@Override
			public JavaPseudoRandom create()
			{
				return create( ConfigFactory.create( Config.class ) );
			}

			@Override
			public JavaPseudoRandom create( final Config config )
			{
				return JavaPseudoRandom.of( config );
			}

			@SuppressWarnings( "unchecked" )
			@Override
			public PseudoRandom create( final CharSequence id,
				final Number seed )
			{
				return create( ConfigFactory.create( Config.class,
						map( new Map.Entry[]
				{ entry( Config.NAME_KEY, id.toString() ),
						entry( Config.SEED_KEY, seed.toString() ) } ) ) );
			}
		}

		public static JavaPseudoRandom of( final Config config )
		{
			final JavaPseudoRandom result = new JavaPseudoRandom();
			result.id = config.id();
			result.seed = config.seed().longValue();
			result.random = new Random( result.seed );
			return result;
		}

		/** the id */
		private Name id;

		/** the seed */
		private Long seed;

		/** the {@link Random} generator */
		private Random random;

		@Override
		public Name getId()
		{
			return this.id;
		}

		@Override
		public Number getSeed()
		{
			return this.seed;
		}

		@Override
		public boolean nextBoolean()
		{
			return this.random.nextBoolean();
		}

		@Override
		public void nextBytes( final byte[] bytes )
		{
			this.random.nextBytes( bytes );
		}

		@Override
		public int nextInt()
		{
			return this.random.nextInt();
		}

		@Override
		public int nextInt( final int n )
		{
			return this.random.nextInt( n );
		}

		@Override
		public long nextLong()
		{
			return this.random.nextLong();
		}

		@Override
		public float nextFloat()
		{
			return this.random.nextFloat();
		}

		@Override
		public double nextDouble()
		{
			return this.random.nextDouble();
		}

		@Override
		public double nextGaussian()
		{
			return this.random.nextGaussian();
		}
	}
}