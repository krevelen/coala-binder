/* $Id: 92e818fed3349a554d6cbeb45e1ac316fd6668df $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/random/impl/RandomNumberStreamFactoryJDK.java $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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
 * 
 * Copyright (c) 2010-2014 Almende B.V. 
 */
package io.coala.math3;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.random.ISAACRandom;
import org.apache.commons.math3.random.Well1024a;
import org.apache.commons.math3.random.Well19937a;
import org.apache.commons.math3.random.Well44497a;
import org.apache.commons.math3.random.Well512a;

import io.coala.exception.CoalaExceptionFactory;
import io.coala.random.RandomNumberStream;
import io.coala.random.RandomNumberStream.AbstractRandomNumberStream;
import io.coala.util.Instantiator;

/**
 * {@link Math3RandomNumberStream} decorates several commons-math3
 * {@link RandomGenerator}s as {@link RandomNumberStream}
 * 
 * @version $Id: 92e818fed3349a554d6cbeb45e1ac316fd6668df $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class Math3RandomNumberStream extends AbstractRandomNumberStream
{

	/**
	 * @param rng the {@link RandomNumberStream} to unwrap (if possible)
	 * @return the unwrapped {@link RandomGenerator} or otherwise a decorated
	 *         {@link RandomNumberStream}
	 */
	public static RandomGenerator
		toRandomGenerator( final RandomNumberStream rng )
	{
		return rng instanceof Math3RandomNumberStream
				? ((Math3RandomNumberStream) rng).unwrap()
				: new RandomGenerator()
				{
					@Override
					public void setSeed( final int seed )
					{
						throw CoalaExceptionFactory.INVOCATION_FAILED
								.createRuntime( "Seed can be set only once" );
					}

					@Override
					public void setSeed( final int[] seed )
					{
						throw CoalaExceptionFactory.INVOCATION_FAILED
								.createRuntime( "Seed can be set only once" );
					}

					@Override
					public void setSeed( final long seed )
					{
						throw CoalaExceptionFactory.INVOCATION_FAILED
								.createRuntime( "Seed can be set only once" );
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

	private RandomGenerator rng;

	/** {@link Abstract} zero-arg bean constructor */
	protected Math3RandomNumberStream()
	{
		//
	}

	/** {@link Abstract} constructor */
	public Math3RandomNumberStream( final RandomNumberStream.ID id,
		final RandomGenerator rng )
	{
		super( id );
		this.rng = rng;
	}

	protected RandomGenerator unwrap()
	{
		return this.rng;
	}

	@Override
	public boolean nextBoolean()
	{
		return unwrap().nextBoolean();
	}

	@Override
	public void nextBytes( byte[] bytes )
	{
		unwrap().nextBytes( bytes );
	}

	@Override
	public int nextInt()
	{
		return unwrap().nextInt();
	}

	@Override
	public int nextInt( int n )
	{
		return unwrap().nextInt( n );
	}

	@Override
	public long nextLong()
	{
		return unwrap().nextLong();
	}

	@Override
	public float nextFloat()
	{
		return unwrap().nextFloat();
	}

	@Override
	public double nextDouble()
	{
		return unwrap().nextDouble();
	}

	@Override
	public double nextGaussian()
	{
		return unwrap().nextGaussian();
	}

	/**
	 * {@link Factory} of {@link Math3RandomNumberStream}s
	 * 
	 * @version $Id: 92e818fed3349a554d6cbeb45e1ac316fd6668df $
	 * @author Rick van Krevelen
	 */
	public static class Factory<T extends RandomGenerator>
		implements RandomNumberStream.Factory
	{

		/** the FACTORY_CACHE */
		private static final Map<Class<?>, Factory<?>> FACTORY_CACHE = new HashMap<>();

		/**
		 * @return the cached (new) {@link Factory} instance generating
		 *         {@link MersenneTwister} streams
		 */
		public static Factory<MersenneTwister> ofMersenneTwister()
		{
			return of( MersenneTwister.class );
		}

		/** @return a {@link Factory} of {@link ISAACRandom} streams */
		public static Factory<ISAACRandom> ISAACRandom()
		{
			return of( ISAACRandom.class );
		}

		/** @return a {@link Factory} of {@link Well19937a} streams */
		public static Factory<Well19937a> ofWell19937a()
		{
			return of( Well19937a.class );
		}

		/** @return a {@link Factory} of {@link Well19937c} streams */
		public static Factory<Well19937c> ofWell19937c()
		{
			return of( Well19937c.class );
		}

		/** @return a {@link Factory} of {@link Well44497a} streams */
		public static Factory<Well44497a> ofWell44497a()
		{
			return of( Well44497a.class );
		}

		/** @return a {@link Factory} of {@link Well512a} streams */
		public static Factory<Well512a> ofWell512a()
		{
			return of( Well512a.class );
		}

		/** @return a {@link Factory} of {@link Well1024a} streams */
		public static Factory<Well1024a> ofWell1024a()
		{
			return of( Well1024a.class );
		}

		/**
		 * @param rngType the type of {@link RandomGenerator} to create
		 * @return the cached (new) {@link Factory} instance
		 */
		public static <T extends RandomGenerator> Factory<T>
			of( final Class<T> rngType )
		{
			synchronized( FACTORY_CACHE )
			{
				@SuppressWarnings( "unchecked" )
				Factory<T> result = (Factory<T>) FACTORY_CACHE.get( rngType );
				if( result == null )
				{
					result = new Factory<T>(
							Instantiator.of( rngType, long.class ) );
					FACTORY_CACHE.put( rngType, result );
				}
				return result;
			}
		}

		private final Instantiator<T> instantiator;

		public Factory( final Instantiator<T> instantiator )
		{
			this.instantiator = instantiator;
		}

		@Override
		public Math3RandomNumberStream create( final String id,
			final Number seed )
		{
			return create( new ID( id ), seed );
		}

		@Override
		public Math3RandomNumberStream create( final ID id, final Number seed )
		{
			return new Math3RandomNumberStream( id,
					this.instantiator.instantiate( seed.longValue() ) );
		}

	}
}