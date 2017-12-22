/* $Id: 35899cea600f2163e82d1bced04ff39ab24d5ed9 $
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
package io.coala.math3;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;

import org.apache.commons.math3.random.ISAACRandom;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well1024a;
import org.apache.commons.math3.random.Well19937a;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.random.Well44497a;
import org.apache.commons.math3.random.Well512a;

import io.coala.exception.ExceptionFactory;
import io.coala.random.PseudoRandom;
import io.coala.util.Instantiator;

/**
 * {@link Math3PseudoRandom} decorates a [@code commons-math3}
 * {@link RandomGenerator} as {@link ThreadSafe} {@link PseudoRandom}
 * 
 * @version $Id: 35899cea600f2163e82d1bced04ff39ab24d5ed9 $
 * @author Rick van Krevelen
 */
@ThreadSafe
public class Math3PseudoRandom implements PseudoRandom
{

	/**
	 * @param rng the {@link PseudoRandom} to unwrap (if possible)
	 * @return the unwrapped {@link RandomGenerator} or otherwise a decorated
	 *         {@link PseudoRandom}
	 */
	public static RandomGenerator toRandomGenerator( final PseudoRandom rng )
	{
		return rng instanceof Math3PseudoRandom
				? ((Math3PseudoRandom) rng).unwrap() : new RandomGenerator()
				{
					@Override
					public void setSeed( final int seed )
					{
						throw ExceptionFactory
								.createUnchecked( "Seed can be set only once" );
					}

					@Override
					public void setSeed( final int[] seed )
					{
						throw ExceptionFactory
								.createUnchecked( "Seed can be set only once" );
					}

					@Override
					public void setSeed( final long seed )
					{
						throw ExceptionFactory
								.createUnchecked( "Seed can be set only once" );
					}

					@Override
					public void nextBytes( final byte[] bytes )
					{
						rng.nextBytes( bytes );
					}

					@Override
					public int nextInt()
					{
						return rng.nextInt();
					}

					@Override
					public int nextInt( int n )
					{
						return rng.nextInt( n );
					}

					@Override
					public long nextLong()
					{
						return rng.nextInt();
					}

					@Override
					public boolean nextBoolean()
					{
						return rng.nextBoolean();
					}

					@Override
					public float nextFloat()
					{
						return rng.nextFloat();
					}

					@Override
					public double nextDouble()
					{
						return rng.nextDouble();
					}

					@Override
					public double nextGaussian()
					{
						return rng.nextGaussian();
					}
				};
	}

	/** constructor */
	public static Math3PseudoRandom of( final Name id, final Long seed,
		final RandomGenerator rng )
	{
		final Math3PseudoRandom result = new Math3PseudoRandom();
		result.id = id;
		result.seed = seed;
		result.rng = rng;
		return result;
	}

	private Name id;

	private Long seed;

	private RandomGenerator rng;

	/** {@link Abstract} zero-arg bean constructor */
	protected Math3PseudoRandom()
	{
		//
	}

	@Override
	public Long seed()
	{
		return this.seed;
	}

	@Override
	public Name id()
	{
		return this.id;
	}

	protected RandomGenerator unwrap()
	{
		return this.rng;
	}

	@Override
	public synchronized boolean nextBoolean()
	{
		return unwrap().nextBoolean();
	}

	@Override
	public synchronized void nextBytes( byte[] bytes )
	{
		unwrap().nextBytes( bytes );
	}

	@Override
	public synchronized int nextInt()
	{
		return unwrap().nextInt();
	}

	@Override
	public synchronized int nextInt( int n )
	{
		return unwrap().nextInt( n );
	}

	@Override
	public synchronized long nextLong()
	{
		return unwrap().nextLong();
	}

	@Override
	public synchronized float nextFloat()
	{
		return unwrap().nextFloat();
	}

	@Override
	public synchronized double nextDouble()
	{
		return unwrap().nextDouble();
	}

	@Override
	public synchronized double nextGaussian()
	{
		return unwrap().nextGaussian();
	}

	/**
	 * {@link Factory} of {@link Math3PseudoRandom}s
	 * 
	 * @version $Id: 35899cea600f2163e82d1bced04ff39ab24d5ed9 $
	 * @author Rick van Krevelen
	 */
	public static class Factory implements PseudoRandom.Factory
	{

		/**
		 * @return the cached (new) {@link Factory} instance generating
		 *         {@link MersenneTwister} streams
		 */
		public static Factory ofMersenneTwister()
		{
			return of( MersenneTwister.class );
		}

		/** @return a {@link Factory} of {@link ISAACRandom} streams */
		public static Factory ISAACRandom()
		{
			return of( ISAACRandom.class );
		}

		/** @return a {@link Factory} of {@link Well19937a} streams */
		public static Factory ofWell19937a()
		{
			return of( Well19937a.class );
		}

		/** @return a {@link Factory} of {@link Well19937c} streams */
		public static Factory ofWell19937c()
		{
			return of( Well19937c.class );
		}

		/** @return a {@link Factory} of {@link Well44497a} streams */
		public static Factory ofWell44497a()
		{
			return of( Well44497a.class );
		}

		/** @return a {@link Factory} of {@link Well512a} streams */
		public static Factory ofWell512a()
		{
			return of( Well512a.class );
		}

		/** @return a {@link Factory} of {@link Well1024a} streams */
		public static Factory ofWell1024a()
		{
			return of( Well1024a.class );
		}

		/** the FACTORY_CACHE */
		private static final Map<Class<?>, Factory> FACTORY_CACHE = new HashMap<>();

		/**
		 * @param rngType the type of {@link RandomGenerator} to create
		 * @return the cached (new) {@link Factory} instance
		 */
		public synchronized static Factory
			of( final Class<? extends RandomGenerator> rngType )
		{
			return FACTORY_CACHE.computeIfAbsent( rngType, key ->
			{
				return new Factory( Instantiator.of( rngType, long.class ) );
			} );
		}

		private final Instantiator<? extends RandomGenerator> instantiator;

		public Factory(
			final Instantiator<? extends RandomGenerator> instantiator )
		{
			this.instantiator = instantiator;
		}

		@Override
		public Math3PseudoRandom create( final Name id, final Number seed )
		{
			return Math3PseudoRandom.of( id, seed.longValue(),
					this.instantiator.instantiate( seed.longValue() ) );
		}

	}

	/**
	 * {@link MersenneTwisterFactory} implements a {@link Factory} for
	 * {@link MersenneTwister} instances decorated as {@link PseudoRandom}
	 * 
	 * @version $Id: 35899cea600f2163e82d1bced04ff39ab24d5ed9 $
	 * @author Rick van Krevelen
	 */
	@Singleton
	public static class MersenneTwisterFactory extends Factory
	{

		/**
		 * {@link MersenneTwisterFactory} constructor
		 * 
		 * @param instantiator
		 */
		public MersenneTwisterFactory()
		{
			super( Instantiator.of( MersenneTwister.class, long.class ) );
		}

	}
}