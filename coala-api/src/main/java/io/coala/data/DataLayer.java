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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;

import io.coala.data.Table.Change;
import io.coala.data.Table.Property;
import io.coala.data.Table.Tuple;
import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.math.DecimalUtil;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link DataLayer} provides table-like views on some data source
 * 
 * @param <ID>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface DataLayer
{
	Map<Class<?>, DataLayer> TUPLE_SOURCES = new HashMap<>();

	Subject<Change> CHANGES = PublishSubject.create();

	default Observable<Change> changes()
	{
		return CHANGES;
	}

	default Table<Tuple> createTable()
	{
		return createTable( Tuple.class );
	}

	default <T extends Tuple> Table<T> createTable( final Class<T> tupleType )
	{
		return TUPLE_SOURCES
				.computeIfAbsent( tupleType,
						k -> Thrower.throwNew( IllegalStateException::new,
								() -> "No data source for: " + k ) )
				.createTable( tupleType );
	}

	default DataLayer withSource( final Matrix data,
		final Map<Class<?>, List<Class<?>>> tupleProperties )
	{
		return withSource( tupleProperties,
				props -> new MatrixLayer( data, props ) );
	}

	@SuppressWarnings( "rawtypes" )
	default DataLayer
		withSource( final Map<Class<?>, List<Class<?>>> tupleProperties )
	{
		return withSource( new HashMap<>(), tupleProperties );
	}

	@SuppressWarnings( "rawtypes" )
	default DataLayer withSource(
		final Map<Long, Map<Class<? extends Property>, Object>> data,
		Map<Class<?>, List<Class<?>>> tupleProperties )
	{
		return withSource( data, new AtomicLong()::getAndIncrement,
				tupleProperties );
	}

	@SuppressWarnings( "rawtypes" )
	default <ID> DataLayer withSource(
		final Map<ID, Map<Class<? extends Property>, Object>> data,
		final Supplier<ID> indexer,
		Map<Class<?>, List<Class<?>>> tupleProperties )
	{
		return withSource( tupleProperties,
				props -> new MapLayer<>( data, props, indexer ) );
	}

	default DataLayer withSource( Map<Class<?>, List<Class<?>>> tupleProperties,
		Function<List<Class<?>>, DataLayer> sourceGenerator )
	{
		final List<?> replaced = tupleProperties.entrySet().stream()
				.filter( e ->
				{
					final DataLayer layer = sourceGenerator
							.apply( e.getValue() );
					layer.changes().subscribe( CHANGES );
					return TUPLE_SOURCES.put( e.getKey(), layer ) != null;
				} ).map( Map.Entry::getKey ).collect( Collectors.toList() );
		if( !replaced.isEmpty() ) LogUtil.getLogger( DataLayer.class )
				.warn( "Replaced data source for tuple types: " + replaced );
		return this;
	}

	// TODO JSON tree, JDBC, ...

	@Singleton
	class Simple implements DataLayer
	{
		// apply default implementations
	}

	/**
	 * {@link MapLayer} provides a layer for maps with structure:<br/>
	 * { tupleKey -> { propertyType -> propertyValue } }
	 */
	@SuppressWarnings( "rawtypes" )
	class MapLayer<ID> implements DataLayer
	{
		private final Map<ID, Map<Class<? extends Property>, Object>> data;

		private final List<Class<?>> properties;

		private final Supplier<ID> indexer;

		private final Subject<Change> changes = PublishSubject.create();

		public MapLayer(
			final Map<ID, Map<Class<? extends Property>, Object>> data,
			final List<Class<?>> properties, final Supplier<ID> indexer )
		{
			this.data = data;
			this.properties = properties;
			this.indexer = indexer;
		}

		private <T extends Tuple> T createTuple( final Class<T> type )
		{
			try
			{
				return (T) type.newInstance();
			} catch( final Exception e )
			{
				return Thrower.rethrowUnchecked( e );
			}
		}

		private Map<Class<? extends Property>, Object>
			validValues( final ID key, final Class<? extends Property> prop )
		{
			return this.properties != null && this.properties.contains( prop )
					? this.data.computeIfAbsent( key, k -> new HashMap<>() )
					: Thrower.throwNew( IllegalArgumentException::new,
							() -> "Illegal property: " + prop );
		}

		@SuppressWarnings( "unchecked" )
		private Object get( final ID key, final Class<? extends Property> k )
		{
			return validValues( key, k ).get( k );
		}

		@SuppressWarnings( "unchecked" )
		private Object put( final ID key, final Class<? extends Property> k,
			final Object v )
		{
			return validValues( key, k ).put( k, v );
		}

		private String stringify( final ID key )
		{
			final Map<Class<? extends Property>, Object> values = this.data
					.get( key );
			return key + (values == null ? "{}"
					: "{" + this.properties.stream()
							.map( p -> p.getSimpleName() + "="
									+ (values.containsKey( p ) ? values.get( p )
											: "") )
							.reduce( ( s1, s2 ) -> String.join( ", ", s1, s2 ) )
							+ "}");
		}

		@Override
		public Observable<Change> changes()
		{
			return this.changes;
		}

		@SuppressWarnings( { "unchecked" } )
		@Override
		public <T extends Tuple> Table<T>
			createTable( final Class<T> tupleType )
		{
			final Table<T> result = new Table.Simple<>( this.indexer::get,
					key -> this.data.remove( key ),
					() -> this.data.keySet().stream(),
					( key,
						changes ) -> (T) createTuple( tupleType ).reset( key,
								changes, k -> get( key, k ),
								( k, v ) -> put( key, k, v ),
								() -> stringify( key ) ),
					this.data::toString );
			result.changes().subscribe( this.changes );
			return result;
		}
	}

	/**
	 * {@link MatrixLayer} provides layers upon 2D matrices: <br/>
	 * { tupleKey/row -> { propertyType/column -> propertyValue } }
	 */
	@SuppressWarnings( "rawtypes" )
	class MatrixLayer implements DataLayer
	{
		private final Matrix data;

		private final List<Class<?>> columns;

		private final Map<Class<?>, Long> columnIndices; // derived

		private final LongFunction<String> rowLabeler;

		private final SortedSet<Long> rowRecycler = new TreeSet<>();

		private final AtomicLong rowMax = new AtomicLong();

		private final Subject<Change> changes = PublishSubject.create();

		public MatrixLayer( final Matrix data,
			final List<Class<?>> columnProperties )
		{
			if( columnProperties == null || columnProperties.isEmpty() )
				Thrower.throwNew( IllegalArgumentException::new,
						() -> "No columns specified" );
			this.data = data;
			this.columns = columnProperties;
			this.rowLabeler = i -> "row" + i;
			this.columnIndices = IntStream.range( 0, this.columns.size() )
					.collect( HashMap::new, ( coords, i ) -> coords
							.put( this.columns.get( i ), Long.valueOf( i ) ),
							Map::putAll );
			// set missing column labels
			IntStream.range( 0, this.columns.size() )
					.filter( i -> this.data.getColumnLabel( i ) == null )
					.forEach( i -> this.data.setColumnLabel( i,
							this.columns.get( i ).getSimpleName() ) );
		}

		private long[] coords( final Number key,
			final Class<? extends Property> propertyType )
		{
			return new long[] { key.longValue(), Objects.requireNonNull(
					this.columnIndices.get( propertyType ),
					"Column undefined for: " + propertyType.getSimpleName() ) };
		}

		private Object getValue( final Number key,
			final Class<? extends Property> propertyType )
		{
			final Class<?> returnType = Property.returnType( propertyType );
			return getValueAs( returnType, coords( key, propertyType ) );
		}

		@SuppressWarnings( "unchecked" )
		private <T> T getValueAs( final Class<T> returnType,
			final long... coords )
		{
			final Object value = this.data.getAsObject( coords );
			if( value == null )
			{
				// Matrix stores ZERO or FALSE as (Number)0 or (Object)null
				if( returnType.isEnum() )
					return returnType.getEnumConstants()[0];
				if( Boolean.class.isAssignableFrom( returnType ) )
					return (T) Boolean.FALSE;
				if( BigDecimal.class.isAssignableFrom( returnType ) )
					return (T) BigDecimal.ZERO;
				if( Long.class.isAssignableFrom( returnType ) )
					return (T) Long.valueOf( 0L );
				if( Integer.class.isAssignableFrom( returnType ) )
					return (T) Integer.valueOf( 0 );
				if( Double.class.isAssignableFrom( returnType ) )
					return (T) Double.valueOf( 0d );
				if( Float.class.isAssignableFrom( returnType ) )
					return (T) Float.valueOf( 0f );
				if( BigInteger.class.isAssignableFrom( returnType ) )
					return (T) BigInteger.ZERO;
				return null;
			}

			final Class<?> valueType = value.getClass();
			if( returnType.isAssignableFrom( valueType ) ) return (T) value;

			// check/convert number type values if necessary
			if( Number.class.isAssignableFrom( valueType ) )
			{
				final BigDecimal bd = DecimalUtil.valueOf( (Number) value );
				if( returnType.isEnum() )
					return returnType.getEnumConstants()[bd.intValue()];
				if( BigDecimal.class.isAssignableFrom( returnType ) )
					return (T) bd;
				if( Long.class.isAssignableFrom( returnType ) )
					return (T) Long.valueOf( bd.longValue() );
				if( Integer.class.isAssignableFrom( returnType ) )
					return (T) Integer.valueOf( bd.intValue() );
				if( Double.class.isAssignableFrom( returnType ) )
					return (T) Double.valueOf( bd.doubleValue() );
				if( Float.class.isAssignableFrom( returnType ) )
					return (T) Float.valueOf( bd.floatValue() );
				if( BigInteger.class.isAssignableFrom( returnType ) )
					return (T) bd.toBigInteger();
				if( Number.class.isAssignableFrom( returnType ) ) return (T) bd;
				if( Boolean.class.isAssignableFrom( returnType ) )
					return (T) Boolean.valueOf( bd.signum() == 0 );
			}
			if( Boolean.class.isAssignableFrom( returnType ) )
				return (T) Boolean.TRUE;

			return Thrower.throwNew( IllegalStateException::new,
					() -> "Expected " + returnType + ", got " + valueType );
		}

//		private List<Object> semaphores = Collections
//				.synchronizedList( new ArrayList<>() );

		private void setValue( final Number key,
			final Class<? extends Property> propertyType, final Object value )
		{
//			synchronized( this.semaphores.get( key.intValue() ) )
			{
				this.data.setAsObject(
						value != null && value.getClass().isEnum()
								? ((Enum<?>) value).ordinal() : value,
						coords( key, propertyType ) );
			}
		}

		private Tuple generate( final Class<? extends Tuple> type )
		{
			try
			{
				return type.newInstance();
			} catch( final Exception e )
			{
				return Thrower.rethrowUnchecked( e );
			}
		}

		private Long nextIndex()
		{
			if( this.rowRecycler.isEmpty() )
			{
				final long result = this.rowMax.getAndIncrement();
//				this.semaphores.add( new Object() );
				this.data.setRowLabel( result,
						this.rowLabeler.apply( result ) );
				return result;
			}
			final Long row = this.rowRecycler.first();
			this.rowRecycler.remove( row );
			this.data.setRowLabel( row, this.rowLabeler.apply( row ) );
			return row;
		}

		private void removeIndex( final Long row )
		{
			final long max = this.rowMax.get();
			if( row < 0 || row > max )
				Thrower.throwNew( IndexOutOfBoundsException::new,
						() -> "Row not in [0," + max + "]: " + row );

			this.data.selectRows( Ret.LINK, row ).clear();
			this.data.setRowLabel( row, null );
			if( row == max )
				this.rowMax.decrementAndGet();
			else
				this.rowRecycler.add( row );
		}

		private Stream<Long> indices()
		{
			if( this.rowMax.get() == 0 ) return Stream.empty();
			final List<Long> ignore = new ArrayList<>( this.rowRecycler );
			ignore.add( this.rowMax.get() );
			long first = 0;
			for( ; ignore.get( 0 ) == first; first++ )
				ignore.remove( 0 );
			final long skip = first;
			return IntStream.range( 0, ignore.size() ).mapToObj( i ->
			{
				final Long fromIncl = i == 0 ? skip : ignore.get( i - 1 ) + 1,
						toExcl = ignore.get( i );
				return fromIncl == toExcl ? Stream.<Long>empty()
						: LongStream.range( fromIncl, toExcl )
								.mapToObj( j -> j );
			} ).reduce( Stream.empty(), Stream::concat );
		}

		private boolean isIndex( final Long row )
		{
			return row > -1 && row < this.rowMax.get()
					&& !this.rowRecycler.contains( row );
		}

		private String toString( final Long row )
		{
			return "#" + row + "{" + IntStream
					.range( 0, (int) this.data.getColumnCount() ).mapToObj( i ->
					{
						final Object v = getValueAs(
								Property.returnType( this.columns.get( i )
										.asSubclass( Property.class ) ),
								row, i );
						return this.columns.get( i ).getSimpleName() + "="
								+ (v == null ? "" : v);
					} ).reduce( ( s1, s2 ) -> String.join( ", ", s1, s2 ) )
					.orElse( "" ) + "}";
		}

		@Override
		public Observable<Change> changes()
		{
			return this.changes;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <T extends Tuple> Table<T>
			createTable( final Class<T> tupleType )
		{
			final Table<T> result = new Table.Simple<>( this::nextIndex,
					this::removeIndex,
					this::indices, ( key, emitter ) -> isIndex( key )
							? (T) generate( tupleType ).reset( key, emitter,
									propertyType -> getValue( key,
											propertyType ),
									( propertyType, value ) -> setValue( key,
											propertyType, value ),
									() -> toString( key ) )
							: Thrower.throwNew( IndexOutOfBoundsException::new,
									() -> "Uncreated or removed: " + key ),
					this.data::stringValue );
			result.changes().subscribe( this.changes );
			return result;
		}
	}
}