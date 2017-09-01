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
package io.coala.math;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.Logger;
import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation.Ret;
import org.ujmp.core.enums.ValueType;

import io.coala.log.LogUtil;
import io.coala.math.DecimalUtil;
import io.coala.math.MatrixUtil;

/**
 * {@link MatrixBuilder}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MatrixBuilder
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( MatrixBuilder.class );

	private static long[] verifySize( final long... size )
	{
		if( size == null || size.length == 0 )
			throw new IllegalArgumentException( "Must have >0 dimensions" );

		// vectors default to columns
		return size.length == 1 ? new long[] { size[0], 1 } : size;
	}

	public static MatrixBuilder zeros( final long... size )
	{
		return zeros( ValueType.DOUBLE, size );
	}

	public static MatrixBuilder zeros( final ValueType valueType,
		final long... size )
	{
		if( size.length > 2 && valueType != ValueType.DOUBLE )
			LOG.warn( "Ignoring extra dimensions: {}>2", size.length );
		return of( Matrix.Factory.zeros( valueType, verifySize( size ) ) );
	}

	public static MatrixBuilder sparse( final long... size )
	{
		return sparse( ValueType.BIGDECIMAL, size );
	}

	public static MatrixBuilder sparse( final ValueType valueType,
		final long... size )
	{
		return of( Matrix.Factory.sparse( valueType, verifySize( size ) ) );
	}

	public static MatrixBuilder scalar( final Object value )
	{
		Objects.requireNonNull( value, "Value missing" );
		if( value instanceof CharSequence )
			return of( Matrix.Factory.linkToValue( value.toString() ) );
		if( value instanceof char[] ) return of( Matrix.Factory
				.linkToValue( String.valueOf( (char[]) value ) ) );
		try
		{
			return of( (Matrix) Matrix.Factory.getClass()
					.getMethod( "linkToValue", value.getClass() )
					.invoke( Matrix.Factory, value ) );
		} catch( final Exception e )
		{
			LOG.warn( "Value type not declared: "
					+ value.getClass().getSimpleName(), e );
			return of( Matrix.Factory.linkToValue( value ) );
		}
	}

	public static MatrixBuilder collection( final Object coll )
	{
		Objects.requireNonNull( coll, "Collection missing" );
		if( coll instanceof Set )
			return of( Matrix.Factory.linkToSet( (Set<?>) coll ) );
		if( coll instanceof List )
			return of( Matrix.Factory.linkToList( (List<?>) coll ) );
		if( coll instanceof Map )
			return of( Matrix.Factory.linkToMap( (Map<?, ?>) coll ) );
		throw new IllegalArgumentException(
				"Unsupported collection type: " + coll.getClass() );
	}

	public static MatrixBuilder array( final Object array )
	{
		Objects.requireNonNull( array, "Array missing" );
		if( !array.getClass().isArray() ) throw new IllegalArgumentException(
				"Not an array: " + array.getClass().getName() );
		try
		{
			return of( (Matrix) Matrix.Factory.getClass()
					.getMethod( "linkToArray", array.getClass() )
					.invoke( Matrix.Factory, array ) );
		} catch( final Exception e )
		{
			LOG.warn( "Array type not declared: "
					+ array.getClass().getSimpleName(), e );
			return of( Matrix.Factory.linkToValue( array ) );
		}
	}

	public static MatrixBuilder of( final Matrix m )
	{
		return new MatrixBuilder( m );
	}

	public static long[] verifyBounds( final Matrix m, final long... coords )
	{
		return verifyBounds( m, coords, false );
	}

	public static long[] verifyBounds( final Matrix m, final long[] coords,
		final boolean orOrigin )
	{
		final int dimCount = m.getDimensionCount();

		// assume null -> origin (eg. scalar)
		if( coords == null || coords.length == 0 )
			return orOrigin ? new long[dimCount] : null;

		if( coords.length != dimCount )
		{
			if( coords.length == 1 ) // convert vector: 1D -> 2D or multiD 
			{
				// try vertical 
				if( m.isRowVector() ) return verifyBounds( m, coords[0], 0 );
				// try horizontal
				if( m.isColumnVector() ) return verifyBounds( m, 0, coords[0] );
				// try diagonal
				final long[] diagonal = new long[dimCount];
				Arrays.fill( diagonal, coords[0] );
				return verifyBounds( m, diagonal );
			}
			throw new IndexOutOfBoundsException( "Dimensions "
					+ Arrays.toString( coords ) + ".length <> " + dimCount );
		}

		final long[] size = m.getSize();
		for( int dim = 0; dim < dimCount; dim++ )
			if( coords[dim] >= size[dim] ) throw new IndexOutOfBoundsException(
					"Coordinates " + Arrays.toString( coords )
							+ " out of bounds: " + Arrays.toString( size ) );
		return coords;
	}

	private final Matrix m;

	private boolean parallel;

	private MatrixBuilder( final Matrix m )
	{
		this.m = m;
		this.parallel = true;
	}

	public Matrix build()
	{
		return this.m;
	}

	@Override
	public String toString()
	{
		return this.m.toString();
	}

	private void setNumber( final Number value, final long[] coords )
	{
		// TODO lock row-array, just to be sure?
		this.m.setAsBigDecimal( DecimalUtil.valueOf( value ), coords );
	}

	public BigDecimal getNumber( final long... coords )
	{
		return this.m.getAsBigDecimal( verifyBounds( this.m, coords ) );
	}

	public BigDecimal calcSum()
	{
		return mapAvailableNumbers().reduce( BigDecimal::add )
				.orElse( BigDecimal.ZERO );
	}

	public BigDecimal calcProduct()
	{
		return mapAvailableNumbers().reduce( DecimalUtil::multiply )
				.orElse( BigDecimal.ZERO );
	}

	public MatrixBuilder parallel( final boolean parallel )
	{
		this.parallel = parallel;
		return this;
	}

	public MatrixBuilder eye()
	{
		this.m.eye( Ret.ORIG );
		return this;
	}

	public MatrixBuilder zeros()
	{
		this.m.zeros( Ret.ORIG );
		return this;
	}

	public MatrixBuilder ones()
	{
		this.m.ones( Ret.ORIG );
		return this;
	}

	public MatrixBuilder sum()
	{
		return scalar( calcSum() );
	}

	public MatrixBuilder product()
	{
		return scalar( calcProduct() );
	}

	public MatrixBuilder mtimes( final MatrixBuilder builder )
	{
		return mtimes( builder.m );
	}

	public MatrixBuilder mtimes( final Matrix m )
	{
		if( this.m.getColumnCount() != m.getRowCount() )
			throw new IllegalArgumentException(
					"Incompatible c1<>r2 ([r1,c1] x [r2,c2] -> [r1,c2]): "
							+ Arrays.toString( this.m.getSize() ) + " x "
							+ Arrays.toString( m.getSize() ) );
		return of( this.m.mtimes( m ) );
	}

	public MatrixBuilder label( final String label, final Object... dimLabels )
	{
		this.m.setLabel( label );
		if( dimLabels != null ) for( int i = 0; i < dimLabels.length; i++ )
			labelDimension( i, dimLabels[i] );
		return this;
	}

	public MatrixBuilder label( final long i, final Object... labels )
	{
		if( labels == null || labels.length == 0 )
			throw new IllegalArgumentException( "No labels" );

		if( this.m.isRowVector() ) return labelRow( i, labels[0] );
		if( this.m.isColumnVector() ) return labelColumn( i, labels[0] );
		if( i < this.m.getRowCount() ) labelRow( i, labels[0] );
		if( i < this.m.getColumnCount() )
			labelColumn( i, labels[labels.length - 1] );
		return this;
	}

	public MatrixBuilder labelRow( final long row, final Object label )
	{
		this.m.setRowLabel( row, label );
		return this;
	}

	public <T> MatrixBuilder labelRows( final T[] labels )
	{
		if( labels != null ) for( int i = labels.length; i-- != 0; )
			labelRow( i, labels[i] );
		return this;
	}

	public MatrixBuilder labelColumn( final long col, final Object label )
	{
		this.m.setColumnLabel( col, label );
		return this;
	}

	public <T> MatrixBuilder labelColumns( final T[] labels )
	{
		if( labels != null ) for( int i = labels.length; i-- != 0; )
			labelColumn( i, labels[i] );
		return this;
	}

	public MatrixBuilder labelDimension( final int dim, final Object label )
	{
		this.m.setDimensionLabel( dim, label );
		return this;
	}

	public MatrixBuilder with( final Object value, final long... coords )
	{
		this.m.setAsObject( value, verifyBounds( this.m, coords ) );
		return this;
	}

	public MatrixBuilder with( final Object value, final long i )
	{
		this.m.setAsObject( value, verifyBounds( this.m, i ) );
		return this;
	}

	public MatrixBuilder with( final Object value, final long i,
		final CharSequence label )
	{
		return with( value, i ).label( i, label );
	}

	public MatrixBuilder with( final Number value, final long... coords )
	{
		setNumber( value, verifyBounds( this.m, coords ) );
		return this;
	}

	public MatrixBuilder with( final Number value, final long i )
	{
		setNumber( value, verifyBounds( this.m, i ) );
		return this;
	}

	public MatrixBuilder with( final Number value, long row,
		final CharSequence label )
	{
		return with( value, row ).label( row, label );
	}

	public MatrixBuilder withContent( final MatrixBuilder builder,
		final long... offset )
	{
		return withContent( builder.m, offset );
	}

	public MatrixBuilder withContent( final Matrix values,
		final long... offset )
	{
		this.m.setContent( Ret.ORIG, values,
				verifyBounds( this.m, offset, true ) );
		return this;
	}

	public <K extends Enum<K>, V> MatrixBuilder
		withContent( final Map<K, V> map )
	{
		return withContent( map, 0L );
	}

	public <K extends Enum<K>, V> MatrixBuilder
		withContent( final Map<K, V> map, final long offset )
	{
		map.forEach( ( k, v ) -> with( v, offset + k.ordinal(), k.name() ) );
		return this;
	}

	public <V> MatrixBuilder withContent( final Iterable<V> values )
	{
		return withContent( values, 0L );
	}

	public <V> MatrixBuilder withContent( final Iterable<V> values,
		final long offset )
	{
		return withContent(
				StreamSupport.stream( values.spliterator(), true ) );
	}

	public <V> MatrixBuilder withContent( final Stream<V> values )
	{
		return withContent( values, 0L );
	}

	private <T> Stream<T> checkParallel( final Stream<T> stream )
	{
		if( stream.isParallel() != this.parallel ) if( this.parallel )
			stream.parallel();
		else
			stream.sequential();
		return stream;
	}

	private LongStream checkParallel( final LongStream stream )
	{
		if( stream.isParallel() != this.parallel ) if( this.parallel )
			stream.parallel();
		else
			stream.sequential();
		return stream;
	}

	public <V> MatrixBuilder withContent( final Stream<V> values,
		final long offset )
	{
		final AtomicLong i = new AtomicLong( offset );
		checkParallel( values ).forEach( v -> with( v, i.getAndIncrement() ) );
		return this;
	}

	public MatrixBuilder fill( final Object value )
	{
		MatrixUtil.streamAllCoordinates( this.m, this.parallel )
				.forEach( x -> this.m.setAsObject( value, x ) );
		return this;
	}

	public MatrixBuilder fill( final Object value, final Stream<long[]> coords )
	{
		checkParallel( coords ).forEach( x -> with( value, x ) );
		return this;
	}

	public MatrixBuilder fill( final Object value, final LongStream coords )
	{
		checkParallel( coords ).forEach( i -> with( value, i ) );
		return this;
	}

	public MatrixBuilder fill( final Number value, final Stream<long[]> coords )
	{
		checkParallel( coords ).forEach( x -> with( value, x ) );
		return this;
	}

	public MatrixBuilder apply( final UnaryOperator<Number> operator,
		final long... coords )
	{
		final long[] x = verifyBounds( this.m, coords );
		setNumber( operator.apply( getNumber( x ) ), x );
		return this;
	}

	public MatrixBuilder apply( final BinaryOperator<Number> function,
		final Number operand, final long... coords )
	{
		return apply( v -> function.apply( v, operand ), coords );
	}

	public MatrixBuilder add( final Number augend, final long... coords )
	{
		if( coords == null || coords.length == 0 ) return withAllNumbers(
				( x, value ) -> DecimalUtil.add( (Number) value, augend ) );

		return apply( DecimalUtil::add, augend, coords );
	}

	public MatrixBuilder subtract( final Number subtrahend,
		final long... coords )
	{
		if( coords == null || coords.length == 0 ) return withAllNumbers( ( x,
			value ) -> DecimalUtil.subtract( (Number) value, subtrahend ) );

		return apply( DecimalUtil::subtract, subtrahend, coords );
	}

	public MatrixBuilder multiply( final Number multiplicand,
		final long... coords )
	{
		if( coords == null || coords.length == 0 )
			return withAvailableNumbers( ( x, value ) -> value == null ? null
					: DecimalUtil.multiply( (Number) value, multiplicand ) );

		return apply( DecimalUtil::multiply, multiplicand, coords );
	}

	public MatrixBuilder divide( final Number divisor, final long... coords )
	{
		Objects.requireNonNull( divisor, "no divisor" );
		if( coords == null || coords.length == 0 )
			return withAvailableNumbers( ( x, value ) -> value == null ? null
					: DecimalUtil.divide( (Number) value, divisor ) );

		return apply( DecimalUtil::divide, divisor, coords );
	}

	public MatrixBuilder
		withAllValues( final BiFunction<long[], Object, Object> visitor )
	{
		MatrixUtil.streamAllCoordinates( this.m, this.parallel )
				.forEach( x -> this.m.setAsObject(
						visitor.apply( x, this.m.getAsObject( x ) ), x ) );
		return this;
	}

	public MatrixBuilder withAllNumbers(
		final BiFunction<long[], BigDecimal, BigDecimal> visitor )
	{
		MatrixUtil.streamAllCoordinates( this.m, this.parallel )
				.forEach( x -> this.m.setAsBigDecimal(
						visitor.apply( x, this.m.getAsBigDecimal( x ) ), x ) );
		return this;
	}

	/** stream non-null values (returns 0) */
	public MatrixBuilder
		withAvailableValues( final BiFunction<long[], Object, Object> visitor )
	{
		MatrixUtil.streamAvailableCoordinates( this.m, this.parallel )
				.forEach( x ->
				{
					final Object v = this.m.getAsObject( x );
					if( v != null )
						this.m.setAsObject( visitor.apply( x, v ), x );
				} );
		return this;
	}

	/** robust to null values (returns 0) */
	public MatrixBuilder withAvailableNumbers(
		final BiFunction<long[], BigDecimal, BigDecimal> visitor )
	{
		return withAvailableValues( ( x, v ) -> visitor.apply( x,
				DecimalUtil.valueOf( (Number) v ) ) );
	}

	public void
		forEachAvailableValue( final BiConsumer<long[], Object> visitor )
	{
		MatrixUtil.streamAvailableCoordinates( this.m, this.parallel )
				.forEach( x -> visitor.accept( x, this.m.getAsObject( x ) ) );
	}

	public Stream<Object> mapAvailableValues()
	{
		return MatrixUtil.streamAvailableCoordinates( this.m, this.parallel )
				.map( this.m::getAsObject );
	}

	public Stream<BigDecimal> mapAvailableNumbers()
	{
		return MatrixUtil.streamAvailableCoordinates( this.m, this.parallel )
				.map( this.m::getAsBigDecimal );
	}

	public void forEachValue( final BiConsumer<long[], Object> visitor )
	{
		MatrixUtil.streamAllCoordinates( this.m, this.parallel )
				.forEach( x -> visitor.accept( x, this.m.getAsObject( x ) ) );
	}

	public <R> Stream<R>
		mapValues( final BiFunction<long[], Object, R> visitor )
	{
		return MatrixUtil.streamAllCoordinates( this.m, this.parallel )
				.map( x -> visitor.apply( x, this.m.getAsObject( x ) ) );
	}

	public MatrixBuilder add( final MatrixBuilder builder )
	{
		return add( builder.m );
	}

	public MatrixBuilder add( final Matrix m )
	{

		if( !Arrays.equals( this.m.getSize(), m.getSize() ) )
			throw new IllegalArgumentException(
					"Unequal sizes: " + Arrays.toString( this.m.getSize() )
							+ " + " + Arrays.toString( m.getSize() ) );
		MatrixUtil.streamAvailableCoordinates( m, this.parallel )
				.forEach( x -> add( m.getAsBigDecimal( x ), x ) );
		return this;
	}
}