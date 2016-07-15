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

import io.coala.math.Tuple;

/**
 * {@link MultivariateDistribution} restricts sampling to <em>n</em>-
 * {@link Tuple}s based respectively on <em>n</em> independent
 * {@link ProbabilityDistribution}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface MultivariateDistribution extends ProbabilityDistribution<Tuple>
{

	static MultivariateDistribution
		of( final ProbabilityDistribution<Tuple> dist )
	{
		return () ->
		{
			return dist.draw();
		};
	}
//	static <T extends Tuple> MultivariateDistribution<T> of(
//		final PseudoRandom stream, final Collection<T> items,
//		final List<WeightedValue<BinTuple>> frequencies )
//	{
//		final Map<?, Set<Object>> ranges = new HashMap<>();
//		items.forEach( t ->
//		{
//			addValues( t, ranges );
//		} );
//
//		final MultivariateDistribution<T> result = new MultivariateDistribution<T>()
//		{
//			@Override
//			public T draw()
//			{
//				if( items.isEmpty() ) return null;
//				if( items.size() == 1 ) return items.iterator().next();
//
//				final List<T> candidates = new ArrayList<>();
//				final Map<K, Range<?>> filter = new HashMap<>();
//				variates.entrySet().forEach( e ->
//				{
//					filter.put( e.getKey(), e.getValue().draw() );
//				} );
//				items.forEach( t ->
//				{
//					if( match( t, filter ) ) candidates.add( t );
//				} );
//				return candidates.get( stream.nextInt( candidates.size() ) );
//			}
//
//			@Override
//			public void register( final T item )
//			{
//				items.add( item );
//				addValues( item, ranges );
//			}
//
//			@Override
//			public void unregister( final T item )
//			{
//				items.remove( item );
//				checkRemovableValues( item, ranges, items );
//			}
//		};
//		return result;
//	}
//
//	void register( T item );
//
//	void unregister( T item );
//
//	@SuppressWarnings( "unchecked" )
//	default void register( final T... items )
//	{
//		if( items != null ) for( T item : items )
//			register( item );
//	}
//
//	default void register( final Iterable<T> items )
//	{
//		if( items != null ) for( T item : items )
//			register( item );
//	}
//
//	@SuppressWarnings( "unchecked" )
//	default void unregister( final T... items )
//	{
//		if( items != null ) for( T item : items )
//			unregister( item );
//	}
//
//	default void unregister( final Iterable<T> items )
//	{
//		if( items != null ) for( T item : items )
//			unregister( item );
//	}
//
//	@SuppressWarnings( "unchecked" )
//	static <V> boolean match( final Range<?> range, final V value )
//	{
//		return ((Range<? super V>) range).contains( value );
//	}
//
//	static <K> boolean match( final Multivariate<K> multi,
//		final Map<K, Range<?>> filter )
//	{
//		for( Map.Entry<K, Range<?>> entry : filter.entrySet() )
//			if( !match( entry.getValue(),
//					multi.values().get( entry.getKey() ) ) )
//				return false;
//		return true;
//	}
//
//	static <K> void addValues( final Multivariate<K> multi,
//		final Map<K, Set<Object>> ranges )
//	{
//		multi.values().forEach( ( key, value ) ->
//		{
//			ranges.computeIfAbsent( key, t ->
//			{
//				return new HashSet<Object>();
//			} ).add( value );
//		} );
//	}
//
//	static <K, T extends Multivariate<K>> void checkRemovableValues(
//		final Multivariate<K> multi, final Map<K, Set<Object>> ranges,
//		final Iterable<T> others )
//	{
//		// only keep key/value pairs if absent in remaining/others
//		final Map<K, Object> removable = new HashMap<>( multi.values() );
//		for( T other : others )
//		{
//			removable.entrySet().removeIf( e ->
//			{
//				return e.getValue().equals( other.values().get( e.getKey() ) );
//			} );
//			if( removable.isEmpty() ) return;
//		}
//		removable.forEach( ( key, value ) ->
//		{
//			ranges.remove( key, value );
//		} );
//	}
}