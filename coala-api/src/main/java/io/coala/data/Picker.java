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
package io.coala.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import io.coala.log.LogUtil;
import io.coala.math.Range;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.PseudoRandom;

/**
 * 
 * {@link Picker} recursive selection builder API
 * 
 * @param <P>
 * @param <T>
 * @version $Id$ gamma
 * @author Rick van Krevelen
 */
public interface Picker<P extends Picker<?, ?>, T extends Table.Tuple>
	extends ProbabilityDistribution<T>
{
	P parent();

	@SuppressWarnings( "unchecked" )
	@Override
	default T draw()
	{
		return (T) parent().draw();
	}

	default List<Comparable<?>> filter()
	{
		return parent().filter();
	}

	default IndexPartition index()
	{
		return parent().index();
	}

	interface Root<T extends Table.Tuple> extends Picker<Root<T>, T>
	{
		@Override
		default Root<T> parent()
		{
			return this;
		}

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		default <THIS extends Root<T>, K extends Table.Property<V>, V extends Comparable>
			Groups<V, THIS>
			splitBy( final Class<K> property, final V... splitValues )
		{
			final Stream<V> values = splitValues == null
					|| splitValues.length == 0 || splitValues[0] == null
							? Stream.empty() : Arrays.stream( splitValues );
			return splitBy( property, Comparator.naturalOrder(), values );
		}

		@SuppressWarnings( "rawtypes" )
		default <THIS extends Root<T>, K extends Table.Property<V>, V extends Comparable>
			Groups<V, THIS>
			splitBy( final Class<K> property, final Stream<V> splitValues )
		{
			return splitBy( property, Comparator.naturalOrder(), splitValues );
		}

		@SuppressWarnings( "rawtypes" )
		default <THIS extends Root<T>, K extends Table.Property<V>, V extends Comparable>
			Groups<V, THIS>
			splitBy( final Class<K> property, final Collection<V> splitValues )
		{
			return splitBy( property, Comparator.naturalOrder(),
					splitValues.stream() );
		}

		@SuppressWarnings( "rawtypes" )
		default <THIS extends Root<T>, K extends Table.Property<V>, V extends Comparable>
			Groups<V, THIS> splitBy( Class<K> property,
				Comparator<? super V> valueComparator, Stream<V> splitValues )
		{
			index().groupBy( property, valueComparator, splitValues );
			@SuppressWarnings( "unchecked" )
			final THIS parent = (THIS) this;
			return Groups.of( parent );
		}
	}

	static <T extends Table.Tuple> Root<T> of( final Table<T> source,
		final PseudoRandom rng, final Consumer<Throwable> onError )
	{
		final Logger log = LogUtil.getLogger( Picker.class );
		return of( source, rng, ( filter, k, v ) ->
		{
			log.trace( "Pick [{};{}] deviates: {} in {}", filter,
					k.getSimpleName(), v );
			return true;
		}, e -> log.error( "Problem while re-indexing", e ) );
	}

	@FunctionalInterface
	interface DeviationConfirmer
	{
		boolean confirm( Comparable<?>[] filter, Class<?> property,
			Range<?> bin );

	}

	static <T extends Table.Tuple> Root<T> of( final Table<T> source,
		final PseudoRandom rng, final DeviationConfirmer onDeviation,
		final Consumer<Throwable> onError )
	{
		final IndexPartition index = new IndexPartition( source, onError );
		return new Root<T>()
		{
			final List<Comparable<?>> filter = new ArrayList<>();

			@Override
			public T draw()
			{
				@SuppressWarnings( "rawtypes" )
				final Comparable[] filterArgs = this.filter
						.toArray( new Comparable[this.filter.size()] );
				this.filter.clear();
				final List<Object> selection = index.nearestKeys(
						( k, v ) -> onDeviation.confirm( filterArgs, k, v ),
						filterArgs );
				return source.select( rng.nextElement( selection ) );
			}

			@Override
			public List<Comparable<?>> filter()
			{
				return this.filter;
			}

			@Override
			public IndexPartition index()
			{
				return index;
			}
		};
	}

	interface Groups<V extends Comparable<?>, P extends Picker<?, ?>>
		extends Picker<P, Table.Tuple>
	{
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		default <THIS extends Groups<V, P>, K extends Table.Property<W>, W extends Comparable>
			Groups<W, THIS>
			thenBy( final Class<K> property, final W... splitValues )
		{
			final Stream<W> values = splitValues == null
					|| splitValues.length == 0 || splitValues[0] == null
							? Stream.empty() : Arrays.stream( splitValues );
			return thenBy( property, Comparator.naturalOrder(), values );
		}

		@SuppressWarnings( { "rawtypes", "unchecked" } )
		default <THIS extends Groups<V, P>, K extends Table.Property<W>, W extends Comparable>
			Groups<W, THIS>
			thenBy( final Class<K> property, final Stream<W> values )
		{
			return thenBy( property, Comparator.naturalOrder(), values );
		}

		@SuppressWarnings( "rawtypes" )
		default <THIS extends Groups<V, P>, K extends Table.Property<W>, W extends Comparable>
			Groups<W, THIS>
			thenBy( final Class<K> property, final Collection<W> splitValues )
		{
			return thenBy( property, Comparator.naturalOrder(),
					splitValues.stream() );
		}

		@SuppressWarnings( "rawtypes" )
		default <THIS extends Groups<V, P>, K extends Table.Property<W>, W extends Comparable>
			Groups<W, THIS> thenBy( final Class<K> property,
				final Comparator<? super W> valueComparator,
				final Stream<W> splitValues )
		{
			index().groupBy( property, valueComparator, splitValues );
			@SuppressWarnings( "unchecked" )
			final THIS parent = (THIS) this;
			return Groups.of( parent );
		}

		default P any()
		{
			filter().add( 0, null );
			return parent();
		}

		default P match( final V valueFilter )
		{
			filter().add( 0, valueFilter );
			return parent();
		}

		default P match( final Range<V> rangeFilter )
		{
			filter().add( 0, rangeFilter );
			return parent();
		}

		static <V extends Comparable<?>, P extends Picker<?, ?>> Groups<V, P>
			of( final P parent )
		{
			return new Groups<V, P>()
			{
				@Override
				public P parent()
				{
					return parent;
				}
			};
		}
	}
}