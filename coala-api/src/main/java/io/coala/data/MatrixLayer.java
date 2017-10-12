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
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;

import io.coala.data.Table.Change;
import io.coala.data.Table.Property;
import io.coala.data.Table.Tuple;
import io.coala.exception.Thrower;
import io.coala.math.DecimalUtil;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link MatrixLayer} provides layers upon 2D matrices: <br/>
 * { tupleKey/row -> { propertyType/column -> propertyValue } }
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public class MatrixLayer implements DataLayer
{
	private final Matrix data;

	private final List<Class<? extends Property>> columns;

	private final Map<Class<?>, Long> columnIndices; // derived

	private final LongFunction<String> rowLabeler;

	private final SortedSet<Long> rowRecycler = new TreeSet<>();

	private final AtomicLong rowMax = new AtomicLong();

	private final Subject<Change> changes = PublishSubject.create();

	public MatrixLayer( final Matrix data,
		final List<Class<? extends Property>> columnProperties )
	{
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
	private <T> T getValueAs( final Class<T> returnType, final long... coords )
	{
		final Object value = this.data.getAsObject( coords );
		if( value == null )
		{
			// Matrix stores ZERO or FALSE as (Number)0 or (Object)null
			if( returnType.isEnum() ) return returnType.getEnumConstants()[0];
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
			if( BigDecimal.class.isAssignableFrom( returnType ) ) return (T) bd;
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
			this.data.setRowLabel( result, this.rowLabeler.apply( result ) );
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
					: LongStream.range( fromIncl, toExcl ).mapToObj( j -> j );
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
	public <T extends Tuple> Table<T> createTable( final Class<T> tupleType )
	{
		final Table<T> result = new Table.Simple<>( this.columns::stream,
				this::nextIndex, this::removeIndex,
				this::indices, ( key, emitter ) -> isIndex( key )
						? (T) generate( tupleType ).reset( key, emitter,
								propertyType -> getValue( key, propertyType ),
								( propertyType, value ) -> setValue( key,
										propertyType, value ),
								() -> toString( key ) )
						: Thrower.throwNew( IndexOutOfBoundsException::new,
								() -> "Uncreated or removed: " + key ),
				() -> (int) this.rowMax.get() - this.rowRecycler.size(),
				this.data::stringValue, this.data::clear );
		this.changes.mergeWith( result.changes() );
		return result;
	}
}