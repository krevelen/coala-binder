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
	/** @return the signal {@link Differentiater} */
	Differentiater<Q, K> differentiater();

	/** @return the signal {@link Intercepter} */
	Intercepter<Q, K> intercepter();

	/**
	 * {@link Differentiater} derives the {@link Quantity} difference after
	 * applying constant k to q_0 over period dt starting from t_0:
	 * <blockquote>dq = &part;q/&part;t &larr;
	 * <sub>t<sub>0</sub></sub>&int;<sup>t<sub>0</sub>+dt</sup>
	 * f(q<sub>t</sub>,k)</blockquote>
	 * 
	 * @param t_0 signal offset {@link Instant} (for periodic effects)
	 * @param dt the signal duration {@link Quantity} of {@link Time}
	 * @param q_0 the {@link Quantity} at t_0
	 * @param k the condition {@link K} during period [t<sub>0</sub>,
	 *            t<sub>0</sub>+dt]
	 */
	@FunctionalInterface
	interface Differentiater<Q extends Quantity<?>, K>
		extends SignalQuantifier.Differentiater<Q>
	{
		Q dq( Instant t_0, Quantity<Time> dt, Q q_0, K k );

		@Override
		default Q dq( Instant t_0, Quantity<Time> dt, Q q_0 )
		{
			return dq( t_0, dt, q_0, null );
		}
	}

	/**
	 * {@link Intercepter} solves the smallest duration dt>0 starting from
	 * t_0 where q_t is first reached, or {@code null} if this never occurs in
	 * state k: <blockquote>min(dt) such that f(q<sub>t<sub>0</sub>+dt</sub>,k)
	 * = q<sub>t</sub></blockquote>
	 * 
	 * @param t_0 the signal offset {@link Instant}, for periodic effects
	 * @param q_0 the current signal {@link Quantity} at t_0
	 * @param q_t the signal target {@link Quantity}
	 * @param k the condition {@link K} during period [t<sub>0</sub>,t]
	 */
	@FunctionalInterface
	interface Intercepter<Q extends Quantity<?>, K>
		extends SignalQuantifier.Intercepter<Q>
	{
		Quantity<Time> dt( Instant t_0, Q q_0, Q q_t, K k );

		@Override
		default Quantity<Time> dt( Instant t_0, Q q_0, Q q_t )
		{
			return dt( t_0, q_0, q_t, null );
		}
	}
}