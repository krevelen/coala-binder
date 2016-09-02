/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.random;

import java.util.Random;

import javax.inject.Singleton;

/**
 * {@link JavaRandom} decorates a standard Java {@link Random} generator as
 * {@link PseudoRandom}
 * 
 * @version $Id: 1af879e91e793fc6b991cfc2da7cb93928527b4b $
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