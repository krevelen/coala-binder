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
package io.coala.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link MapBuilder} inspired by http://stackoverflow.com/a/32879629/1418999
 * and Guava's Collection Utilities
 * https://github.com/google/guava/wiki/CollectionUtilitiesExplained
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MapBuilder<K, V, M extends Map<K, V>>
{

	public static <K, V> MapBuilder<K, V, Map<K, V>> unordered()
	{
		return new MapBuilder<>( HashMap::new, Collections::unmodifiableMap,
				Collections::synchronizedMap );
	}

	public static <K, V> MapBuilder<K, V, Map<K, V>> ordered()
	{
		return new MapBuilder<>( LinkedHashMap::new,
				Collections::unmodifiableMap, Collections::synchronizedMap );
	}

	public static <K extends Enum<K>, V> MapBuilder<K, V, Map<K, V>>
		ordered( final Class<K> enumType )
	{
		return new MapBuilder<>( () -> new EnumMap<>( enumType ),
				Collections::unmodifiableMap, Collections::synchronizedMap );
	}

	// help compiler determine run-time type arguments at compile time
	public static <K extends Enum<K>, V> MapBuilder<K, V, Map<K, V>>
		ordered( final Class<K> enumType, final Class<V> valueType )
	{
		return ordered( enumType );
	}

	public static <K, V> MapBuilder<K, V, NavigableMap<K, V>> sorted()
	{
		return new MapBuilder<>( TreeMap::new,
				Collections::unmodifiableNavigableMap,
				Collections::synchronizedNavigableMap );
	}

	public static <K, V> MapBuilder<K, V, NavigableMap<K, V>>
		sorted( final Comparator<K> comparator )
	{
		return new MapBuilder<>( () -> new TreeMap<>( comparator ),
				Collections::unmodifiableNavigableMap,
				Collections::synchronizedNavigableMap );
	}

	private final M map;
	private final Function<M, M> fixer;
	private final Function<M, M> syncher;

	private MapBuilder( final Supplier<M> mapFactory,
		final Function<M, M> fixer, final Function<M, M> syncher )
	{
		this.map = mapFactory.get();
		this.fixer = fixer;
		this.syncher = syncher;
	}

	public M build()
	{
		return this.map;
	}

	public M fixed()
	{
		return this.fixer.apply( this.map );
	}

	public M synched()
	{
		return this.syncher.apply( this.map );
	}

	public MapBuilder<K, V, M> put( final K key, final V value )
	{
		this.map.put( Objects.requireNonNull( key, "Missing key" ), value );
		return this;
	}

	public MapBuilder<K, V, M>
		putAll( final Map<? extends K, ? extends V> other )
	{
		this.map.putAll( Objects.requireNonNull( other, "Missing other map" ) );
		return this;
	}

	public MapBuilder<K, V, M> fill( final Stream<K> keys, final V value )
	{
		keys.parallel().forEach( k -> put( k, value ) );
		return this;
	}

	public MapBuilder<K, V, M> fill( final Iterable<K> keys, final V value )
	{
		return fill( StreamSupport.stream( keys.spliterator(), false ), value );
	}

	public MapBuilder<K, V, M> fill( final K[] keys, final V value )
	{
		return fill( keys == null ? Stream.empty() : Arrays.stream( keys ),
				value );
	}
}
