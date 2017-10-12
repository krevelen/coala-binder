/* $Id: 041401a75f0ee60ad4d19456e8746914450e821e $
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
package io.coala.math;

import java.util.function.BiFunction;

import javax.measure.Quantity;

import io.coala.util.Comparison;

/**
 * {@link Bin}
 * 
 * @param <Q> the {@link Quantity} of extreme values
 * @version $Id: 041401a75f0ee60ad4d19456e8746914450e821e $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public class Bin<V extends Comparable> extends Range<V>
{

	public static <V extends Comparable<?>> Bin<V> of( final V unit )
	{
		return new Bin<>( Extreme.lower( unit, true ),
				Extreme.upper( unit, true ), unit );
	}

	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q> & Comparable<Q>> Bin<Q>
		of( final Q minIncl, final Q maxExcl )
	{
		return of( minIncl, maxExcl, (BiFunction<Q, Q, Q>)
		// disambiguated
		( a, b ) -> (Q) a.add( b ).divide( 2 ) );
	}

	public static <V extends Comparable<?>> Bin<V> of( final V minIncl,
		final V maxExcl, final BiFunction<V, V, V> averager )
	{
		return of( minIncl, maxExcl,
				minIncl == null ? (maxExcl == null ? null : maxExcl)
						: maxExcl == null ? minIncl
								: averager.apply( minIncl, maxExcl ) );
	}

	public static <V extends Comparable<?>> Bin<V> of( final V minIncl,
		final V maxExcl, final V average )
	{
		final Extreme<V> minimum = Extreme.lower( minIncl, minIncl != null );
		final Extreme<V> maximum = Extreme.upper( maxExcl, false );
		return new Bin<>( minimum, maximum, average );
	}

	private V kernel;

	protected Bin()
	{
		super();
	}

	public Bin( final V unit )
	{
		super( Extreme.lower( unit, true ), Extreme.upper( unit, true ) );
	}

	public Bin( final Extreme<V> minimum, final Extreme<V> maximum,
		final V kernel )
	{
		super( minimum, maximum );
		this.kernel = kernel;
	}

	@SuppressWarnings( { /*"rawtypes",*/ "unchecked" } )
	@Override
	public int compareTo( final Range<V> o )
	{
		return Comparison.compare( (Comparable) getKernel(),
				((Bin) o).getKernel() );
	}

	public V getKernel()
	{
		return this.kernel;
	}
}