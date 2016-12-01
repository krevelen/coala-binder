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

import java.util.function.Function;

import javax.measure.Quantity;
import javax.measure.Unit;

import io.coala.math.QuantityUtil;
import io.coala.util.Compare;
import tec.uom.se.ComparableQuantity;

/**
 * {@link QuantityDistribution} is a {@link ProbabilityDistribution} for
 * {@link Quantity}s of some {@link Quantity}, decorated with arithmetic
 * transforms of e.g. {@link Quantity#times(double)} etc.
 * 
 * @param <Q> the type of {@link Quantity} for produced {@link Quantity}s
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface QuantityDistribution<Q extends Quantity<Q>>
	extends ProbabilityDistribution<Quantity<Q>>
{
	static <Q extends Quantity<Q>> QuantityDistribution<Q>
		of( final ProbabilityDistribution<Quantity<Q>> dist )
	{
		return () -> dist.draw();
	}

	/**
	 * @param <N> the measurement value {@link Number} type
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @param dist the {@link ProbabilityDistribution} to wrap
	 * @param unit the {@link Unit} of measurement to assign
	 * @return an {@link QuantityDistribution} for {@link Quantity}s from drawn
	 *         {@link Number}s, with an attempt to maintain exactness
	 */
	public static <N extends Number, Q extends Quantity<Q>>
		QuantityDistribution<Q>
		of( final ProbabilityDistribution<N> dist, final Unit<Q> unit )
	{
		return of( () -> QuantityUtil.valueOf( dist.draw(), unit ) );
	}

	/**
	 * @param <N> the type of {@link Number} value
	 * @param value the constant to be returned on each draw
	 * @return a degenerate or deterministic {@link QuantityDistribution}
	 */
	public static <N extends Number, Q extends Quantity<Q>>
		QuantityDistribution<Q> of( final N value, final Unit<Q> unit )
	{
		final Quantity<Q> constant = QuantityUtil.valueOf( value, unit );
		return of( ProbabilityDistribution.createDeterministic( constant ) );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param transform a unary {@link Function} to transform {@link Quantity}s
	 * @return a chained {@link QuantityDistribution}
	 */
	default <R extends Quantity<R>> QuantityDistribution<R>
		transform( final Function<Quantity<Q>, Quantity<R>> transform )
	{
		return of( () -> transform.apply( draw() ) );
	}

	/**
	 * @param qty
	 * @return
	 */
	default <R extends Quantity<R>> QuantityDistribution<R>
		asType( final Class<R> qty )
	{
		return transform( q -> q.asType( qty ) );
	}

	/**
	 * @param unit the {@link Unit} to convert to
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#to(Unit)
	 */
	default QuantityDistribution<Q> to( final Unit<Q> unit )
	{
		return transform( t -> t.to( unit ) );
	}

	/**
	 * @param augend the {@link Quantity} to be added
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#plus(Quantity)
	 */
	default QuantityDistribution<Q> add( final Quantity<Q> augend )
	{
		return transform( t -> t.add( augend ) );
	}

	/**
	 * @param subtrahend the {@link Quantity} to be subtracted
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#minus(Quantity)
	 */
	default QuantityDistribution<Q> subtract( final Quantity<Q> subtrahend )
	{
		return transform( t -> t.subtract( subtrahend ) );
	}

	/**
	 * @param multiplier the scaling factor
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#multiply(Number)
	 */
	default QuantityDistribution<Q> multiply( final Number multiplier )
	{
		return transform( t -> t.multiply( multiplier ) );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param multiplier the measure multiplier {@link Quantity}
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#multiply(Quantity)
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	default <R extends Quantity<R>> QuantityDistribution<R>
		multiply( final Quantity<?> multiplier, final Unit unit )
	{
		return transform( t -> t.multiply( multiplier ).to( unit ) );
	}

	/**
	 * @param divisor the exact divisor
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#divide(Number)
	 */
	default QuantityDistribution<Q> divide( final Number divisor )
	{
		return transform( t -> t.divide( divisor ) );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param divisor the divisor {@link Quantity}
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#divide(Quantity)
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	default <R extends Quantity<R>> QuantityDistribution<R>
		divide( final Quantity<?> divisor, final Unit unit )
	{
		return transform( t -> t.divide( divisor ).to( unit ) );
	}

	/**
	 * @return a chained {@link QuantityDistribution}
	 * @see Quantity#inverse()
	 */
	default QuantityDistribution<?> inverse()
	{
		return transform( Quantity::inverse );
	}

	default QuantityDistribution<Q> min(final ComparableQuantity<Q> qty2 )
	{
		return transform(qty1-> Compare.min( (ComparableQuantity<Q>)qty1, qty2 ));
	}

	default QuantityDistribution<Q> max(final ComparableQuantity<Q> qty2 )
	{
		return transform(qty1-> Compare.max( (ComparableQuantity<Q>)qty1, qty2 ));
	}

	/**
	 * @return a chained {@link QuantityDistribution}
	 * @see QuantityUtil#abs(Quantity)
	 */
	default QuantityDistribution<Q> abs()
	{
		return transform( QuantityUtil::abs );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @return a chained {@link QuantityDistribution}
	 * @see QuantityUtil#sqrt(Quantity)
	 */
	default QuantityDistribution<?> sqrt()
	{
		return transform( QuantityUtil::sqrt );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param n the root's order (n != 0)
	 * @return a chained {@link QuantityDistribution}
	 * @see QuantityUtil#root(Quantity,int)
	 */
	default QuantityDistribution<?> root( final int n )
	{
		return transform( t -> QuantityUtil.root( t, n ) );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param exp the exponent
	 * @return <code>this<sup>exp</sup></code>
	 * @see QuantityUtil#pow(Quantity,int)
	 */
	default QuantityDistribution<?> pow( final int exp )
	{
		return transform( t -> QuantityUtil.pow( t, exp ) );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param exp the exponent
	 * @return <code>this<sup>exp</sup></code>
	 * @see QuantityUtil#pow(Quantity,int)
	 */
	default ProbabilityDistribution<Number> pow( final Number exp )
	{
		return () -> QuantityUtil.pow( draw(), exp );
	}
}