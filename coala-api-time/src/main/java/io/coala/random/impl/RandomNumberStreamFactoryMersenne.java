/* $Id: 330dbddef8cb039f3950162c937de930c4198964 $
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
package io.coala.random.impl;

import io.coala.random.RandomNumberStream;
import io.coala.random.RandomNumberStreamID;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * {@link RandomNumberStreamFactoryMersenne}for {@link RandomNumberStream}s
 * wrapping the {@link MersenneTwister} implementation of a
 * {@link RandomGenerator}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class RandomNumberStreamFactoryMersenne
	implements RandomNumberStream.Factory
{

	@Override
	public RandomNumberStream create( final String id, final Number seed )
	{
		return create( new RandomNumberStreamID( id ), seed );
	}

	@Override
	public RandomNumberStream create( final RandomNumberStreamID id,
		final Number seed )
	{
		final RandomGenerator rng = new MersenneTwister( seed.longValue() );
		return RandomNumberStream.Util.asStream( id, rng );
	}
}