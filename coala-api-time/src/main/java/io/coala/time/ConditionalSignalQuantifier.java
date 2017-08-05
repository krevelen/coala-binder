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
 * {@link ConditionalSignalQuantifier}
 * 
 * @param <Q> the type of signal {@link Quantity}
 * @param <K> the type of condition affecting the signal
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface ConditionalSignalQuantifier<Q extends Quantity<?>, K>
	extends SignalQuantifier<Q>
{
	@Override
	Integrator<Q, K> integrator();

	@Override
	Interceptor<Q, K> interceptor();

	/**
	 * {@link Integrator} derives the {@link Quantity} difference after applying
	 * constant {@code k} to {@code q_0} over period {@code dt} starting from
	 * {@code t_0}
	 */
	@FunctionalInterface
	interface Integrator<Q extends Quantity<?>, K>
		extends SignalQuantifier.Integrator<Q>
	{
		/**
		 * @param t_0 signal offset {@link Instant} (for periodic effects)
		 * @param dt the signal duration {@link Quantity} of {@link Time}
		 * @param q_0 the {@link Quantity} at {@code t_0}
		 * @param k the condition {@link K} during period [t<sub>0</sub>,
		 *            t<sub>0</sub>+dt]
		 * @return dq = <sub>t<sub>0</sub></sub>&int;<sup>t<sub>0</sub>+dt</sup>
		 *         q(t,k) &middot; &part;t
		 */
		Q dq( Instant t_0, Quantity<Time> dt, Q q_0, K k );

		@Override
		default Q dq( Instant t_0, Quantity<Time> dt, Q q_0 )
		{
			return dq( t_0, dt, q_0, null );
		}
	}

	/**
	 * {@link Interceptor} solves the smallest duration {@code dt>0} starting
	 * from {@code t_0} where {@code q_t} is first reached, or {@code null} if
	 * this never occurs in state {@code k}
	 */
	@FunctionalInterface
	interface Interceptor<Q extends Quantity<?>, K>
		extends SignalQuantifier.Interceptor<Q>
	{
		/**
		 * @param t_0 the signal offset {@link Instant}, for periodic effects
		 * @param q_0 the current signal {@link Quantity} at {@code t_0}
		 * @param q_t the signal target {@link Quantity}
		 * @param k the condition {@link K} during period
		 *            [t<sub>0</sub>,t<sub>0</sub>+dt]
		 * @return min(dt) such that q(t<sub>0</sub>+dt,k) = q<sub>t</sub>, or
		 *         &bottom;
		 */
		Quantity<Time> dtMin( Instant t_0, Q q_0, Q q_t, K k );

		@Override
		default Quantity<Time> dtMin( Instant t_0, Q q_0, Q q_t )
		{
			return dtMin( t_0, q_0, q_t, null );
		}
	}
}