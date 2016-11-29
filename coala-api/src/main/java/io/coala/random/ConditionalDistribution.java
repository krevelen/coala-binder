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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@link ConditionalDistribution}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 * 
 * @param <T> the type of value to draw from the conditional distributions
 * @param <C> the type of condition for selecting a distribution
 */
@FunctionalInterface
public interface ConditionalDistribution<T, C>
{

	T draw( C condition );

	/**
	 * Simple example usage for Bernoulli distributions
	 * 
	 * @param <C> the type of condition for selecting a distribution
	 * @param rng a {@link PseudoRandom} number generator
	 * @param probGen the probability generator, e.g. a {@link Map::get}
	 * @return a {@link ConditionalDistribution}
	 */
	static <C> ConditionalDistribution<Boolean, C>
		ofBernoulli( final PseudoRandom rng, final Function<C, Number> probGen )
	{
		return of( p -> ProbabilityDistribution.createBernoulli( rng, p ),
				probGen );
	}

	/**
	 * @param <T> the type of value drawn by the conditional distributions
	 * @param <C> the type of condition for selecting a distribution
	 * @param <X> the type of parameter for generating a distribution
	 * @param distGen distribution generator taking one parameter
	 * @param param1Gen generator of the first parameter given the condition
	 * @return a {@link ConditionalDistribution}
	 */
	static <T, C, X> ConditionalDistribution<T, C> of(
		final Function<X, ProbabilityDistribution<T>> distGen,
		final Function<C, X> param1Gen )
	{
		return of( distGen, param1Gen, new HashMap<>() );
	}

	/**
	 * @param <T> the type of value drawn by the conditional distributions
	 * @param <C> the type of condition for selecting a distribution
	 * @param <X> the type of parameter for generating a distribution
	 * @param distGen distribution generator taking one parameter
	 * @param param1Gen generator of the first parameter given the condition
	 * @param distCache caches previously generated distributions
	 * @return a {@link ConditionalDistribution}
	 */
	static <T, C, X> ConditionalDistribution<T, C> of(
		final Function<X, ProbabilityDistribution<T>> distGen,
		final Function<C, X> param1Gen,
		final Map<C, ProbabilityDistribution<T>> distCache )
	{
		return c -> distCache.computeIfAbsent( c,
				t -> distGen.apply( param1Gen.apply( t ) ) ).draw();
	}

	/**
	 * @param <T> the type of value drawn by the conditional distributions
	 * @param <C> the type of condition for selecting a distribution
	 * @param <X> the type of the first distribution parameter
	 * @param <Y> the type of the second distribution parameter
	 * @param distGen distribution generator taking one parameter
	 * @param param1Gen generator of the first parameter for some condition
	 * @param param2Gen generator of the second parameter for some condition
	 * @return a {@link ConditionalDistribution}
	 */
	static <T, C, X, Y> ConditionalDistribution<T, C> of(
		final BiFunction<X, Y, ProbabilityDistribution<T>> distGen,
		final Function<C, X> param1Gen, final Function<C, Y> param2Gen )
	{
		return of( distGen, param1Gen, param2Gen, new HashMap<>() );
	}

	/**
	 * @param <T> the type of value drawn by the conditional distributions
	 * @param <C> the type of condition for selecting a distribution
	 * @param <X> the type of the first distribution parameter
	 * @param <Y> the type of the second distribution parameter
	 * @param distGen distribution generator taking one parameter
	 * @param param1Gen generator of the first parameter for some condition
	 * @param param2Gen generator of the second parameter for some condition
	 * @param distCache caches previously generated distributions
	 * @return a {@link ConditionalDistribution}
	 */
	static <T, C, X, Y> ConditionalDistribution<T, C> of(
		final BiFunction<X, Y, ProbabilityDistribution<T>> distGen,
		final Function<C, X> param1Gen, final Function<C, Y> param2Gen,
		final Map<C, ProbabilityDistribution<T>> distCache )
	{
		return c -> distCache.computeIfAbsent( c, t -> distGen
				.apply( param1Gen.apply( t ), param2Gen.apply( t ) ) ).draw();
	}
}
