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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.ujmp.core.Matrix;

import io.coala.data.Table.Change;
import io.coala.data.Table.Operation;
import io.coala.data.Table.Property;
import io.coala.data.Table.Simple;
import io.coala.data.Table.Tuple;
import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.util.MapBuilder;
import io.reactivex.Observable;

/**
 * {@link DataLayer} provides {@link Table}-indirection on some data source with
 * the basic persistence {@link Operation}s
 * <p>
 * TODO JSON/XML, JDBC/JPA, HTTP/REST, ...
 * <p>
 * TODO n-by-m relations a/symmetric, e.g. matrix sparsely linking sources n, m
 * 
 * @param <ID>
 * @version $Id$
 * @author Rick van Krevelen
 * @version beta release, still undergoing changes
 */
public interface DataLayer
{
	default Observable<Change> changes()
	{
		return Observable.fromIterable( StaticCaching.LAYER_CHANGES )
				.flatMap( rx -> rx );
	}

	default <T extends Tuple> Table<T> getTable( final Class<T> tupleType )
	{
		return StaticCaching.SOURCE_CACHE.compute( tupleType, ( k,
			v ) -> v == null ? Thrower.throwNew( IllegalStateException::new,
					() -> "Data source not set for: " + k ) : v )
				.getTable( tupleType );
	}

	default DataLayer withSource( final PropertyMapper propertyMapper,
		final Matrix data )
	{
		return withSource( propertyMapper, props ->
		{
			final MatrixLayer result = new MatrixLayer( data, props );
			StaticCaching.LAYER_CHANGES.add( result.changes() );
			return result;
		} );
	}

	default DataLayer withSource( final PropertyMapper propertyMapper,
		final MapFactory<Long> mapFactory )
	{
		return withSource( propertyMapper, mapFactory,
				// start at 1, Matrix transforms 0 to 'null' 
				new AtomicLong( 1L )::getAndIncrement );
	}

	default <PK> DataLayer withSource( final PropertyMapper propertyMapper,
		final MapFactory<PK> mapFactory, final Supplier<PK> indexer )
	{
		return withSource( propertyMapper, props ->
		{
			final MapLayer<PK> result = new MapLayer<>( mapFactory.get(), props,
					indexer );
			StaticCaching.LAYER_CHANGES.add( result.changes() );
			return result;
		} );
	}

	@SuppressWarnings( "rawtypes" )
	default DataLayer withSource( final PropertyMapper propertyMapper,
		Function<List<Class<? extends Property>>, DataLayer> layerGenerator )
	{
		final List<?> replaced = propertyMapper.map( MapBuilder.unordered() )
				.build().entrySet().stream()
				// for each tuple, create a new layer for their property subset
				.filter( e -> StaticCaching.SOURCE_CACHE.put( e.getKey(),
						layerGenerator.apply( e.getValue() ) ) != null )
				// collect list of replaced tuple/layer combinations
				.map( Map.Entry::getKey ).collect( Collectors.toList() );
		if( !replaced.isEmpty() ) LogUtil.getLogger( DataLayer.class )
				.warn( "Replaced layer/source for tuple types: " + replaced );
		return this;
	}

	static Table<Tuple> defaultTable()
	{
		return StaticCaching.SOURCE_CACHE
				.computeIfAbsent( Tuple.class,
						k -> Thrower.throwNew( IllegalStateException::new,
								() -> "Default table not set" ) )
				.getTable( Tuple.class );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	static DataLayer
		resetDefault( final Class<? extends Property>... properties )
	{
		return resetDefault( HashMap::new, properties );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	static DataLayer resetDefault( final MapFactory<Long> mapFactory,
		final Class<? extends Property>... properties )
	{
		final DataLayer result = new MapLayer<>( mapFactory.get(),
				Arrays.asList( properties ),
				new AtomicLong()::getAndIncrement );
		StaticCaching.SOURCE_CACHE.put( Tuple.class, result );
		return result;
	}

	/**
	 * {@link PropertyMapper} removes painstaking type argument definitions
	 */
	@FunctionalInterface
	interface PropertyMapper
	{
		@SuppressWarnings( "rawtypes" )
		MapBuilder<Class<? extends Tuple>, List<Class<? extends Property>>, ?>
			map( MapBuilder<Class<? extends Tuple>, List<Class<? extends Property>>, ?> mapBuilder );
	}

	/**
	 * {@link MapFactory} removes painstaking type argument definitions
	 */
	@FunctionalInterface
	interface MapFactory<PK>
	{
		@SuppressWarnings( "rawtypes" )
		Map<PK, Map<Class<? extends Property>, Object>> get();
	}

	/**
	 * {@link Simple} provides the default {@link DataLayer} method
	 * implementations
	 */
	@Singleton
	class StaticCaching implements DataLayer
	{

		static final Map<Class<?>, DataLayer> SOURCE_CACHE = new HashMap<>();

		static final List<Observable<Change>> LAYER_CHANGES = new ArrayList<>();
	}
}