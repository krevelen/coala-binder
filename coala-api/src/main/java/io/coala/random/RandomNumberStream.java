/* $Id: 8b9d9fe183b232dd2059419030b815bfed544b9e $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/random/RandomNumberStream.java $
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
package io.coala.random;

import java.util.Random;

import io.coala.json.x.Wrapper;
import io.coala.name.AbstractIdentifiable;
import io.coala.name.AbstractIdentifier;
import io.coala.name.Identifiable;

/**
 * {@link RandomNumberStream} provides a stream of pseudo-random numbers, with
 * an API similar to the standard Java {@link Random} generator (which is
 * wrapped accordingly in the {@link RandomStream} decorator)
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface RandomNumberStream extends Identifiable<RandomNumberStream.ID>
{

	boolean nextBoolean();

	void nextBytes( byte[] bytes );

	/**
	 * @return the next pseudo-random int
	 */
	int nextInt();

	/**
	 * @param n
	 * @return the next pseudo-random {@link int} between 0 and {@code n}
	 */
	int nextInt( int n );

	/**
	 * @return the next pseudo-random int
	 */
	// int nextInt(int min, int max);

	long nextLong();

	float nextFloat();

	double nextDouble();

	double nextGaussian();

	/**
	 * {@link RandomNumberStreamID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class ID extends AbstractIdentifier<String>
	{

		/** */
		private static final long serialVersionUID = 1L;

		/** {@link AbstractIdentifier} zero-arg bean constructor */
		protected ID()
		{
		}

		/**
		 * {@link ID} constructor
		 * 
		 * @param value the (unique) {@link T} value
		 */
		public ID( final String value )
		{
			super( value );
		}
	}

	/**
	 * {@link RandomNumberStreamFactory}
	 * 
	 * @version $Revision: 324 $
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 *
	 */
	interface Factory
	{
		/**
		 * @param id
		 * @param seed
		 * @return
		 */
		RandomNumberStream create( String id, Number seed );

		/**
		 * @param id
		 * @param seed
		 * @return
		 */
		RandomNumberStream create( ID id, Number seed );

	}

	/**
	 * {@link AbstractRandomNumberStream}
	 * 
	 * TODO deprecate using {@link Wrapper}s
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "serial" )
	abstract class AbstractRandomNumberStream
		extends AbstractIdentifiable<RandomNumberStream.ID>
		implements RandomNumberStream
	{
		/** {@link AbstractRandomNumberStream} zero-arg bean constructor */
		public AbstractRandomNumberStream()
		{
			//
		}

		/** {@link AbstractRandomNumberStream} constructor */
		public AbstractRandomNumberStream( final RandomNumberStream.ID id )
		{
			super( id );
		}
	}

	/**
	 * {@link RandomStream} decorates a standard Java {@link Random} generator
	 * as {@link RandomNumberStream}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "serial" )
	class RandomStream extends AbstractRandomNumberStream
	{
		/** the {@link Random} generator */
		private Random random;

		/**
		 * {@link RandomStream} zero-arg bean constructor
		 */
		protected RandomStream()
		{
		}

		/**
		 * {@link RandomStream} constructor
		 * 
		 * @param id
		 * @param seed
		 */
		public RandomStream( final ID id, final Number seed )
		{
			super( id );
			this.random = new Random( seed.longValue() );
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

	/**
	 * {@link RandomFactory} creates {@link RandomStream} instances
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class RandomFactory implements Factory
	{
		@Override
		public RandomStream create( final String id, final Number seed )
		{
			return create( new ID( id ), seed );
		}

		@Override
		public RandomStream create( final ID id, final Number seed )
		{
			return new RandomStream( id, seed );
		};
	}
}