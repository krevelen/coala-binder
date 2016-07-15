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

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import io.coala.math.MeasureUtil;
import rx.functions.Func1;

/**
 * {@link AmountDistribution} is a {@link ProbabilityDistribution} for
 * {@link Amount}s of some {@link Quantity}, decorated with arithmetic
 * transforms of e.g. {@link Amount#times(double)} etc.
 * 
 * @param <Q> the type of {@link Quantity} for produced {@link Amount}s
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface AmountDistribution<Q extends Quantity>
	extends ProbabilityDistribution<Amount<Q>>
{
	static <Q extends Quantity> AmountDistribution<Q>
		of( final ProbabilityDistribution<Amount<Q>> dist )
	{
		return () ->
		{
			return dist.draw();
		};
	}

	/**
	 * @param <N> the measurement value {@link Number} type
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @param dist the {@link ProbabilityDistribution} to wrap
	 * @param unit the {@link Unit} of measurement to assign
	 * @return an {@link AmountDistribution} for {@link Amount}s from drawn
	 *         {@link Number}s, with an attempt to maintain exactness
	 */
	public static <N extends Number, Q extends Quantity> AmountDistribution<Q>
		of( final ProbabilityDistribution<N> dist, final Unit<Q> unit )
	{
		return of( () ->
		{
			return MeasureUtil.toAmount( dist.draw(), unit );
		} );
	}

	/**
	 * @param <N> the type of {@link Number} value
	 * @param value the constant to be returned on each draw
	 * @return a degenerate or deterministic {@link AmountDistribution}
	 */
	public static <N extends Number, Q extends Quantity> AmountDistribution<Q>
		of( final N value, final Unit<Q> unit )
	{
		final Amount<Q> constant = MeasureUtil.toAmount( value, unit );
		return of( ProbabilityDistribution.createDeterministic( constant ) );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param transform a unary {@link Func1} to transform {@link Amount}s
	 * @return a chained {@link AmountDistribution}
	 */
	default <R extends Quantity> AmountDistribution<R>
		transform( final Func1<Amount<Q>, Amount<R>> transform )
	{
		final AmountDistribution<Q> self = (AmountDistribution<Q>) this;
		return of( new ProbabilityDistribution<Amount<R>>()
		{
			@Override
			public Amount<R> draw()
			{
				return transform.call( self.draw() );
			}
		} );
	}

	/**
	 * @param unit the {@link Unit} to convert to
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#to(Unit)
	 */
	default AmountDistribution<Q> to( final Unit<Q> unit )
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.to( unit );
			}
		} );
	}

	/**
	 * @param that the {@link Amount} to be added
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#plus(Amount)
	 */
	default AmountDistribution<Q> plus( final Amount<?> that )
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.plus( that );
			}
		} );
	}

	/**
	 * @param that the {@link Amount} to be subtracted
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#minus(Amount)
	 */
	default AmountDistribution<Q> minus( final Amount<?> that )
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.minus( that );
			}
		} );
	}

	/**
	 * @param factor the exact scaling factor
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#times(long)
	 */
	default AmountDistribution<Q> times( final long factor )
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.times( factor );
			}
		} );
	}

	/**
	 * @param factor the approximate scaling factor
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#times(double)
	 */
	default AmountDistribution<Q> times( final double factor )
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.times( factor );
			}
		} );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param factor the measure multiplier {@link Amount}
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#times(Amount)
	 */
	default <R extends Quantity> AmountDistribution<R>
		times( final Amount<?> factor, final Unit<R> unit )
	{
		return transform( new Func1<Amount<Q>, Amount<R>>()
		{
			@Override
			public Amount<R> call( final Amount<Q> t )
			{
				return t.times( factor ).to( unit );
			}
		} );
	}

	/**
	 * @param divisor the exact divisor
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#divide(long)
	 */
	default AmountDistribution<Q> divide( final long divisor )
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.divide( divisor );
			}
		} );
	}

	/**
	 * @param divisor the approximate divisor
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#divide(double)
	 */
	default AmountDistribution<Q> divide( final double divisor )
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.divide( divisor );
			}
		} );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param divisor the divisor {@link Amount}
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#divide(Amount)
	 */
	default <R extends Quantity> AmountDistribution<R>
		divide( final Amount<?> divisor, final Unit<R> unit )
	{
		return transform( new Func1<Amount<Q>, Amount<R>>()
		{
			@Override
			public Amount<R> call( final Amount<Q> t )
			{
				return t.divide( divisor ).to( unit );
			}
		} );
	}

	/**
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#inverse()
	 */
	default <R extends Quantity> AmountDistribution<R> inverse()
	{
		return transform( new Func1<Amount<Q>, Amount<R>>()
		{
			@SuppressWarnings( "unchecked" )
			@Override
			public Amount<R> call( final Amount<Q> t )
			{
				return (Amount<R>) t.inverse();
			}
		} );
	}

	/**
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#abs()
	 */
	default AmountDistribution<Q> abs()
	{
		return transform( new Func1<Amount<Q>, Amount<Q>>()
		{
			@Override
			public Amount<Q> call( final Amount<Q> t )
			{
				return t.abs();
			}
		} );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#sqrt()
	 */
	default <R extends Quantity> AmountDistribution<R> sqrt()
	{
		return transform( new Func1<Amount<Q>, Amount<R>>()
		{
			@SuppressWarnings( "unchecked" )
			@Override
			public Amount<R> call( final Amount<Q> t )
			{
				return (Amount<R>) t.sqrt();
			}
		} );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param n the root's order (n != 0)
	 * @return a chained {@link AmountDistribution}
	 * @see Amount#root(int)
	 */
	default <R extends Quantity> AmountDistribution<R> root( final int n )
	{
		return transform( new Func1<Amount<Q>, Amount<R>>()
		{
			@SuppressWarnings( "unchecked" )
			@Override
			public Amount<R> call( final Amount<Q> t )
			{
				return (Amount<R>) t.root( n );
			}
		} );
	}

	/**
	 * @param <R> the new type of {@link Quantity} after transformation
	 * @param exp the exponent
	 * @return <code>this<sup>exp</sup></code>
	 * @see Amount#pow(int)
	 */
	default <R extends Quantity> AmountDistribution<R> pow( final int exp )
	{
		return transform( new Func1<Amount<Q>, Amount<R>>()
		{
			@SuppressWarnings( "unchecked" )
			@Override
			public Amount<R> call( final Amount<Q> t )
			{
				return (Amount<R>) t.pow( exp );
			}
		} );
	}
}