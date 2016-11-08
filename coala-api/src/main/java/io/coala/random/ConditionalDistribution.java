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
 */
public interface ConditionalDistribution<T, C>
{

	T draw( C condition );

	/**
	 * @param <T> the type of value drawn by the conditional distributions
	 * @param <C> the type of condition for selecting a distribution
	 * @param <X> the type of parameter for generating a distribution
	 * @param distGen
	 * @param distParameter1
	 * @return
	 */
	static <T, C, X> ConditionalDistribution<T, C> of(
		final Function<X, ProbabilityDistribution<T>> distGen,
		final Function<C, X> distParameter1 )
	{
		final Map<C, ProbabilityDistribution<T>> distCache = new HashMap<>();
		return c -> distCache
				.computeIfAbsent( c,
						t -> distGen.apply( distParameter1.apply( t ) ) )
				.draw();
	}

	static <T, C, X, Y> ConditionalDistribution<T, C> of(
		final BiFunction<X, Y, ProbabilityDistribution<T>> distGen,
		final Function<C, X> distParameter1,
		final Function<C, Y> distParameter2 )
	{
		final Map<C, ProbabilityDistribution<T>> distCache = new HashMap<>();
		return c -> distCache.computeIfAbsent( c, t -> distGen
				.apply( distParameter1.apply( t ), distParameter2.apply( t ) ) )
				.draw();
	}

	static <T, C, X> ConditionalDistribution<T, C> of(
		final Function<X, ProbabilityDistribution<T>> distGen,
		final Map<C, X> conditionalParam1cache, final X defaultParam1 )
	{
		return of( distGen, c -> conditionalParam1cache.computeIfAbsent( c,
				key -> defaultParam1 ) );
	}

	static <T, C, X, Y> ConditionalDistribution<T, C> of(
		final BiFunction<X, Y, ProbabilityDistribution<T>> distGen,
		final Map<C, X> conditionalParam1cache, final X defaultParam1,
		final Map<C, Y> conditionalParam2cache, final Y defaultParam2 )
	{
		return of( distGen,
				c -> conditionalParam1cache.computeIfAbsent( c,
						key -> defaultParam1 ),
				c -> conditionalParam2cache.computeIfAbsent( c,
						key -> defaultParam2 ) );
	}
}
