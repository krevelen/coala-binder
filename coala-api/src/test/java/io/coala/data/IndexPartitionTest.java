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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.ujmp.core.Matrix;

import io.coala.data.Picker.Groups;
import io.coala.data.Picker.Root;
import io.coala.data.Table.Property;
import io.coala.data.Table.Tuple;
import io.coala.log.LogUtil;
import io.coala.math.Range;
import io.coala.random.PseudoRandom;

/**
 * {@link IndexPartitionTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class IndexPartitionTest
{
	/** */
	static final Logger LOG = LogUtil.getLogger( IndexPartitionTest.class );

	class Prop1 extends AtomicReference<Float> implements Table.Property<Float>
	{
	}

	class Prop2 extends AtomicReference<Double>
		implements Table.Property<Double>
	{
	}

	class Prop3 extends AtomicReference<Double>
		implements Table.Property<Double>
	{
	}

	@Test
	public void testPartitions()
	{
		LOG.info( "Test partitions (static)" );

		final int n = 10;
		@SuppressWarnings( "rawtypes" )
		final List<Class<? extends Property>> props = Arrays
				.asList( Prop1.class, Prop2.class, Prop3.class );
		final Matrix m = Matrix.Factory.rand( 2 * n, props.size() );
		final Table<Tuple> t = new MatrixLayer( m, props )
				.getTable( Tuple.class );
		final List<Object> removable = IntStream.range( 0, n / 2 )
				.mapToObj( i ->
				{
					m.setAsInt( 1 + i / 3, i, 0 );
					return t.insert().key();
				} ).collect( Collectors.toCollection( ArrayList::new ) );
//		LOG.trace( "table before: \n{}", m );
		final IndexPartition p = new IndexPartition( t,
				Throwable::printStackTrace );
		LOG.trace( "partition all: {}", p );
		p.groupBy( Prop1.class /* ,Arrays.asList( .8 ) */ ); // use value as bin
		LOG.trace( "partition col1-1: {}", p );
		p.groupBy( Prop2.class, Stream.of( .8 ) );
		LOG.trace( "partition col1-2: {}", p );
		p.groupBy( Prop3.class, Stream.of( .8 ) );
		LOG.trace( "partition col1-3: {}", p );
		final PseudoRandom rng = PseudoRandom.JavaRandom.of( "rng", 1L );
		final Groups<Double, Groups<Double, Groups<Float, Root<Tuple>>>> picker = Picker
				.of( t, rng, Throwable::printStackTrace ).splitBy( Prop1.class )
				.thenBy( Prop2.class, Stream.of( .8 ) )
				.thenBy( Prop3.class, Stream.of( .8 ) );
		LOG.trace( "   picker col1-3: {}", picker.index() );
		LOG.trace( "Picks, 10x in prop1=1: {}",
				IntStream.range( 0, 10 )
						.mapToObj( i -> picker.any().any()
								.match( Range.of( 1f, 3f ) ).draw() )
						.collect( Collectors.toList() ) );
		IntStream.range( n / 2, n ).forEach( i ->
		{
			m.setAsInt( 1 + i / 3, i, 0 );
			final Table.Tuple r = t.insert();
			LOG.trace( "insert #{}: {} -> {}", r.key(), r, p );
			removable.add( r.key() );
		} );
		final Table.Tuple n1 = t.insert();
		LOG.trace( "insert #{}: {} -> {}", n1.key(), n1, p );
//		@SuppressWarnings( "deprecation" )
//		final Table.Tuple n2 = t.remove( Long.valueOf( 1 ) );
//		LOG.trace( "remove #{}: {} -> {}", n2.key(), n2, p );
		LOG.trace( "matrix pre-deletion: {}\n{}", m );
		Collections.reverse( removable );
		removable.forEach( i ->
		{
			t.delete( i );
			LOG.trace( "deleted #{}: {}", i, p );
		} );
		final long[] keys = p.keys().stream().mapToLong( i -> (Long) i )
				.toArray();
		LOG.trace( "matrix after: {}\n{}", keys, m );
	}
}
