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
package io.coala.time;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

/**
 * {@link SignalQuantifier} represents some (differentiable) function over time
 * <p>
 * TODO add some default time-transformation-subject
 * 
 * @param <Q> the type of signal {@link Quantity}
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface SignalQuantifier<Q extends Quantity<?>>
{
	/** @return the signal {@link Integrator} for &part;q/&part;t */
	Integrator<Q> integrator();

	/** @return the signal {@link Interceptor} for &part;t/&part;q */
	Interceptor<Q> interceptor();

	/**
	 * {@link Integrator} derives the {@link Quantity} difference after applying
	 * constant {@code k} to {@code q_0} over period {@code dt} starting from
	 * {@code t_0}
	 */
	@FunctionalInterface
	interface Integrator<Q extends Quantity<?>>
	{
		/**
		 * @param t_0 signal offset {@link Instant} (for periodic effects)
		 * @param dt the signal duration {@link Quantity} of {@link Time}
		 * @param q_0 the {@link Quantity} at {@code t_0}
		 * @return dq = <sub>t<sub>0</sub></sub>&int;<sup>t<sub>0</sub>+dt</sup>
		 *         q(t) &middot; &part;t
		 */
		Q dq( Instant t_0, Quantity<Time> dt, Q q_0 );
	}

	/**
	 * {@link Interceptor} solves the smallest duration {@code dt>0} starting
	 * from {@code t_0} where {@code q_t} is first reached, or {@code null} if
	 * this never occurs in state {@code k}
	 */
	@FunctionalInterface
	interface Interceptor<Q extends Quantity<?>>
	{
		/**
		 * @param t_0 the signal offset {@link Instant}, for periodic effects
		 * @param q_0 the current signal {@link Quantity} at {@code t_0}
		 * @param q_t the signal target {@link Quantity}
		 * @return min(dt) such that q(t<sub>0</sub>+dt) = q<sub>t</sub> or
		 *         &bottom;
		 */
		Quantity<Time> dtMin( Instant t_0, Q q_0, Q q_t );
	}
}