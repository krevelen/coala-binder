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

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import io.coala.exception.CoalaExceptionFactory;
import io.coala.random.RandomNumberStream;

/**
 * {@link Math3RandomNumberStream} decorates several commons-math3
 * {@link RandomGenerator}s as {@link RandomNumberStream}
 * 
 * @version $Id: 92e818fed3349a554d6cbeb45e1ac316fd6668df $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class Math3RandomNumberStream
	extends RandomNumberStream.AbstractRandomNumberStream
{

	/**
	 * @param rng the {@link RandomNumberStream} to unwrap (if possible)
	 * @return the unwrapped {@link RandomGenerator} or otherwise a decorated
	 *         {@link RandomNumberStream}
	 */
	public static RandomGenerator unwrap( final RandomNumberStream rng )
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

	public static Math3RandomNumberStream of( final RandomNumberStream.ID id,
		final RandomGenerator rng )
	{
		return new Math3RandomNumberStream( id, rng );
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
	 * {@link MersenneFactory} of {@link Math3RandomNumberStream}s wrapping
	 * Apache common-math3's {@link MersenneTwister} implementation
	 * 
	 * @version $Id: 92e818fed3349a554d6cbeb45e1ac316fd6668df $
	 * @author Rick van Krevelen
	 */
	public static class MersenneFactory implements RandomNumberStream.Factory
	{
		@Override
		public Math3RandomNumberStream create( final String id,
			final Number seed )
		{
			return create( new RandomNumberStream.ID( id ), seed );
		}

		@Override
		public Math3RandomNumberStream create( final RandomNumberStream.ID id,
			final Number seed )
		{
			return of( id, new MersenneTwister( seed.longValue() ) );
		}
	}

	/**
	 * {@link MersenneFactory} of {@link Math3RandomNumberStream}s wrapping
	 * Apache common-math3's {@link Well19937c} implementation
	 * 
	 * @version $Id: 92e818fed3349a554d6cbeb45e1ac316fd6668df $
	 * @author Rick van Krevelen
	 */
	public static class Well19937cFactory implements RandomNumberStream.Factory
	{
		@Override
		public Math3RandomNumberStream create( final String id,
			final Number seed )
		{
			return create( new RandomNumberStream.ID( id ), seed );
		}

		@Override
		public Math3RandomNumberStream create( final RandomNumberStream.ID id,
			final Number seed )
		{
			return of( id, new Well19937c( seed.longValue() ) );
		}
	}
}