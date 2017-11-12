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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import io.coala.log.LogUtil;
import io.coala.math.Range;
import io.coala.util.MapBuilder;

/**
 * {@link IndexPartition} maintains an ordered index and split points for
 * (combinations of) properties, which must have {@link Comparable} value types
 * to reproduce random picking across replications (same seed -> same pick)
 * <p>
 * TODO replace tree (duplicate end/begin indices) by flat split point array?
 * will reduce tree-search speed across siblings unless Arrays.binarysearch fits
 * 
 * @param <T> the type of {@link Tuple} used in the referent {@link Table}
 */
@SuppressWarnings( "rawtypes" )
public class IndexPartition
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( IndexPartition.class );

	private final Table<?> source;

	private final IndexPartition.PartitionNode root;

	private final List<Object> keys;

	private final List<PartitionDim> dims = new ArrayList<>();

	private boolean validation = false; // TODO from config

	@SuppressWarnings( "unchecked" )
	public IndexPartition( final Table<?> view )
	{
		this.source = view;
		this.keys = Collections.synchronizedList( this.source.keys()
				.collect( Collectors.toCollection( ArrayList::new ) ) );
		this.root = new PartitionNode( null,
				new int[]
		{ 0, this.keys.size() } );
		view.changes().subscribe( this::onChange );
	}

	@Override
	public String toString()
	{
		final int n = this.keys.size();
		return (n < 8 ? this.keys
				: LogUtil.messageOf( "[{}, {}, {}, ..., {}, {}, {}]",
						IntStream.of( 0, 1, 2, n - 3, n - 2, n - 1 )
								.mapToObj( this.keys::get ).toArray() ))
				+ " <- " + this.root;
	}

	public List<Object> keys()
	{
		return this.keys;
	}

	public <P extends Table.Property<V>, V extends Comparable> void
		groupBy( final Class<P> property )
	{
		groupBy( property, Comparator.naturalOrder(), Stream.empty() );
	}

	@SuppressWarnings( "unchecked" )
	public <P extends Table.Property<V>, V extends Comparable> void
		groupBy( final Class<P> property, final List<V> splitValues )
	{
		groupBy( property, Comparator.naturalOrder(), splitValues.stream() );
	}

	@SuppressWarnings( "unchecked" )
	public <P extends Table.Property<V>, V extends Comparable> void groupBy(
		final Class<P> property, final Comparator<? super V> valueComparator,
		final Stream<V> splitValues )
	{
		final List<V> points = splitValues.distinct().sorted( valueComparator )
				.collect( Collectors.toCollection( ArrayList::new ) );

		final PartitionDim<V> dim = new PartitionDim( property, valueComparator,
				points );
		this.dims.add( dim );

		this.root.split( dim, this::partitioner, key -> evaluator( dim, key ),
				this::nodeSplitter );
	}

	@SuppressWarnings( "unchecked" )
	public <P extends Table.Property<P>, V extends Comparable<?>> List<Object>
		nearestKeys( final BiPredicate<Class<P>, Range<V>> deviationConfirmer,
			final Comparable... valueFilter )
	{
		if( this.root.isEmpty() ) return Collections.emptyList();
		if( valueFilter == null || valueFilter.length == 0 )
			return Collections.unmodifiableList( this.keys );
		if( valueFilter.length == 1 )
		{
			final IndexPartition.PartitionNode node = this.root.children
					.floorEntry( Range.of( valueFilter[0] ) ).getValue();
			return Collections.unmodifiableList( partitioner( node.bounds ) );
		}

		IndexPartition.PartitionNode node = this.root;
		Map.Entry<Range, IndexPartition.PartitionNode> childEntry = null;
		for( Comparable value : valueFilter )
		{
			final Range valueRange;
			if( value instanceof Range )
			{
				valueRange = (Range) value;
				// TODO merge node-bins if value-range contains multiple nodes/range
				Map.Entry<Range, IndexPartition.PartitionNode> low = valueRange
						.lowerFinite()
								? node.children.ceilingEntry(
										Range.of( valueRange.lowerValue() ) )
								: node.children.firstEntry(),
						high = valueRange.upperFinite()
								? node.children.floorEntry(
										Range.of( valueRange.upperValue() ) )
								: node.children.lastEntry();
				final int start = low == null ? node.bounds[0]
						: low.getValue().bounds[0],
						end = high == null ? node.bounds[1]
								: high.getValue().bounds[1];
				if( start != end )
					// FIXME merge results from remaining filter values/ranges
					return this.keys.subList( start, end );
			} else
				valueRange = Range.of( value );

			childEntry = node.children.floorEntry( valueRange );
			if( childEntry == null || childEntry.getValue().isEmpty() )
			{
				Map.Entry<Range, IndexPartition.PartitionNode> prev = childEntry,
						next = childEntry;
				while( prev != null && prev.getValue().isEmpty() )
					prev = node.children.lowerEntry( prev.getKey() );
				while( next != null && next.getValue().isEmpty() )
					next = node.children.higherEntry( next.getKey() );
				// upper category undefined/empty, expand by 1 within bounds
				if( !deviationConfirmer.test( node.dim.property,
						(Range<V>) Range.of(
								prev == null ? null
										: prev.getKey().lowerValue(),
								next == null ? null
										: next.getKey().upperValue() ) ) )
					return Collections.emptyList();
				final int start = prev == null ? node.bounds[0]
						: prev.getValue().bounds[0],
						end = next == null ? node.bounds[1]
								: next.getValue().bounds[1];
				return this.keys.subList( start, end );
			}
			node = childEntry.getValue();
		}
		return partitioner( node.bounds );
	}

	void onChange( final Table.Change d )
	{
		switch( d.crud() )
		{
		case CREATE:
			add( this.source.get( d.sourceRef() ) );
			if( this.validation )
			{
				validate();
				if( this.keys.indexOf( d.sourceRef() ) < 0 )
					LOG.error( "failed insert of {}, not indexed: {}",
							d.sourceRef(), this );
			}
			break;
		case DELETE:
			remove( this.source.get( d.sourceRef() ) );
			if( this.validation )
			{
				validate();
				final int i = this.keys.indexOf( d.sourceRef() );
				if( i >= 0 )
					LOG.error( "failed delete of {}, still indexed at: {}: {}",
							d.sourceRef(), i, this );
			}
			break;
		case UPDATE:
			dims: for( PartitionDim dim : this.dims )
				if( dim.property == d.changedType() )
				{
					final Table.Tuple t = this.source.get( d.sourceRef() );
					remove( t.override( dim.property, d.oldValue() ) );
					add( t );
					if( this.validation )
					{
						validate();
						if( this.keys.indexOf( d.sourceRef() ) < 0 )
							LOG.error( "failed update of {}, not indexed: {}",
									d.sourceRef(), this );
					}
					break dims;
				}
			break;
		case READ:
		default:
		}
	}

	void validate()
	{
		final int[] invalid = this.root
				.invalidChildren(
						i -> this.source.select( this.keys.get( i ) ) )
				.toArray();
		if( invalid.length != 0 )
			LOG.error( "Invalid: {} of {}", invalid, this.keys );
	}

	void add( final Table.Tuple t )
	{
		this.root.resize( t, +1, ( bin, leaf ) ->
		{
			// insert halfway
			this.keys.add( (leaf.bounds[0] + leaf.bounds[1]) / 2, t.key() );
			return true;
		}, this::nodeSplitter );
	}

	void remove( final Table.Tuple t )
	{
		this.root.resize( t, -1, ( bin, leaf ) ->
		{
			final int i = partitioner( leaf.bounds ).indexOf( t.key() );
			if( i < 0 )
			{
				LOG.error( LogUtil.messageOf(
						"Remove failed, {} not in #{}, index: #{}", t.key(),
						leaf.bounds, this.keys.indexOf( t.key() ) ),
						new IllegalStateException() );
				return false;
			}
//			LOG.trace( "removing #{}={} in #{}", i + leaf.bounds[0], key,
//					leaf.bounds );
			this.keys.remove( i + leaf.bounds[0] );
			return true;
		}, this::nodeSplitter );

	}

	List<Object> partitioner( final int[] bounds )
	{
		return this.keys.subList( bounds[0], bounds[1] );
	}

	<V extends Comparable> V evaluator( final PartitionDim<V> dim,
		final Object key )
	{
		return this.source.get( key ).get( dim.property );
	}

	@SuppressWarnings( "unchecked" )
	<V extends Comparable> void split( final PartitionNode node,
		final PartitionDim<V> dim )
	{
		node.split( dim, this::partitioner, key -> evaluator( dim, key ),
				this::nodeSplitter );
	}

	@SuppressWarnings( "unchecked" )
	void nodeSplitter( final PartitionNode node )
	{
		boolean passed = false;
		for( int i = 0; i < this.dims.size() - 1; i++ )
		{
			if( !passed && this.dims.get( i ) == node.parent.dim )
				passed = true;
			if( passed ) split( node, this.dims.get( i + 1 ) );
		}
	}

	static class PartitionDim<V extends Comparable>
	{
		final Class<? extends Table.Property<V>> property;

		final Comparator<? super V> comparator;

		final List<V> splitPoints;

		final Map<V, Range<V>> groups = new HashMap<>();

		PartitionDim( final Class<? extends Table.Property<V>> property,
			final Comparator<? super V> comparator, final List<V> splitPoints )
		{
			this.property = property;
			this.comparator = comparator;
			this.splitPoints = splitPoints;
		}

		@Override
		public String toString()
		{
			return this.property.getSimpleName();
		}
	}

	/**
	 * {@link PartitionNode} helper class to build the partition-tree
	 */
	static class PartitionNode
	{
		final IndexPartition.PartitionNode parent; // null == root
		IndexPartition.PartitionDim dim; // null == leaf
		final int[] bounds;
		NavigableMap<Range, IndexPartition.PartitionNode> children; // null = leaf

		PartitionNode( final IndexPartition.PartitionNode parent,
			final int[] indexRange )
		{
			this.parent = parent;
			this.bounds = Arrays.copyOf( indexRange, indexRange.length );
		}

		@Override
		public String toString()
		{
			final int n = this.bounds[1] - this.bounds[0];
			final String name = this.dim == null ? null
					: this.dim.property.getSimpleName();
			return this.children == null ? // leaf
					(n > 2 ? n + "x" : "") + "[" + (isEmpty() ? ""
							: this.bounds[0] + (n == 1 ? ""
									: n == 2 ? "," + (this.bounds[1] - 1)
											: ".." + (this.bounds[1] - 1)))
							+ "]"
					: // branch
					"{" + name.substring( 0, Math.min( 5, name.length() ) )
							+ this.children.entrySet().stream()
									.map( e -> (e.getValue().isEmpty() ? ""
											: (this.dim.splitPoints.isEmpty()
													// category key
													? " '" + e.getKey()
															.lowerValue() + "':"
													// range key
													: e.getKey().lowerFinite()
															// intermediate range
															? " <" + e.getKey()
																	.lowerValue()
																	+ "=<"
															// lowest range			
															: "")
													// value(s): leaf or branch
													+ " " + e.getValue()) )
									.reduce( String::concat ).orElse( "" )
							+ "}";
		}

		private static int[] toBounds( final int[] bounds,
			final int[] splitKeys, final int i )
		{
			return new int[] { i == 0 ? bounds[0] : splitKeys[i - 1],
					i == splitKeys.length ? bounds[1] : splitKeys[i] };
		}

		private static <V extends Comparable> Range<V>
			toRange( final List<V> points, final int i )
		{
			final Range<V> result = Range.of(
					i == 0 ? null : (V) points.get( i - 1 ), i != 0,
					i == points.size() ? null : (V) points.get( i ), false );
//			LOG.trace( "{} {} -> {}", points, i, result );
			return result;
		}

		boolean isEmpty()
		{
			return this.bounds[0] == this.bounds[1];
		}

		IntStream keys()
		{
			return this.children == null
					? IntStream.range( this.bounds[0], this.bounds[1] )
					: this.children.values().stream()
							.flatMapToInt( n -> n.keys() );
		}

		void resize( final Table.Tuple t, final int delta,
			final BiPredicate<Range, IndexPartition.PartitionNode> leafHandler,
			final Consumer<PartitionNode> nodeSplitter )
		{
			@SuppressWarnings( "unchecked" )
			final Comparable value = (Comparable) t.get( this.dim.property );
			if( this.dim.splitPoints.isEmpty() )
			{
				// no split points: each value is a bin
				final Range bin = Range.of( value );
				final PartitionNode child = valueNode( bin, nodeSplitter );
				if( child.children != null )
				{
					// resize children/sub-ranges recursively
					child.resize( t, delta, leafHandler, nodeSplitter );
					this.bounds[1] += delta; // resize parent after child
					shift( this.children.tailMap( bin, false ), delta );
				} else if( leafHandler.test( bin, child ) )
				{
					// resize matching leaf child (end recursion)
					child.bounds[1] += delta; // resize leaf
					this.bounds[1] += delta; // resize parent after child
					shift( this.children.tailMap( bin, false ), delta );
				} else
					LOG.warn( "leaf not adjusted: " + bin );
			} else
			{
				// find appropriate range between provided split points
				final Range bin = Range.of( value );
				final Map.Entry<Range, IndexPartition.PartitionNode> entry = this.children
						.floorEntry( bin );
				this.bounds[1] += delta; // resize parent
				if( entry.getValue().children != null )
				{
					// resize children/sub-ranges recursively
					entry.getValue().resize( t, delta, leafHandler,
							nodeSplitter );
					shift( this.children.tailMap( bin, false ), delta );
				} else if( leafHandler.test( entry.getKey(),
						entry.getValue() ) )
				{
					// resize matching leaf child (end recursion)
					entry.getValue().bounds[1] += delta;
					shift( this.children.tailMap( bin, false ), delta );
				}
			}

		}

		void shift( final NavigableMap<Range, PartitionNode> map,
			final int delta )
		{
			map.values().forEach( node ->
			{
				node.bounds[0] += delta;
				node.bounds[1] += delta;
				if( node.children != null ) shift( node.children, delta );
			} );
		}

		@SuppressWarnings( "unchecked" )
		IntStream
			invalidChildren( final IntFunction<Table.Tuple> tupleSupplier )
		{
			return this.children == null ? IntStream.empty()
					: this.children.entrySet().stream().flatMapToInt( e ->
					{
						final int[] invalid = // sort once
								e.getValue().invalidChildren( tupleSupplier )
										.sorted().toArray();
						return IntStream
								.range( e.getValue().bounds[0],
										e.getValue().bounds[1] )
								// invalid: child(ren) invalid
								.filter( i -> Arrays.binarySearch( invalid,
										i ) >= 0 )
								// invalid: value out of range
								.filter( i -> !e.getKey()
										.contains( (Comparable) tupleSupplier
												.apply( i ).get(
														this.dim.property ) ) );
					} );
		}

		@SuppressWarnings( "unchecked" )
		<V extends Comparable> void split( final PartitionDim<V> dim,
			final Function<int[], List<Object>> partitioner,
			final Function<Object, V> valueSupplier,
			final Consumer<PartitionNode> nodeSplitter )
		{
			if( this.children != null ) // reached leaf node
			{
				this.children.values().forEach( child -> child.split( dim,
						partitioner, valueSupplier, nodeSplitter ) );
				return;
			}
			// provide the dimension info to each affected leaf
			this.dim = dim;
			// sort node key-partition using given property value comparator
			final List<Object> partition = partitioner.apply( this.bounds );
			Collections.sort( partition,
					( k1, k2 ) -> dim.comparator.compare(
							valueSupplier.apply( k1 ),
							valueSupplier.apply( k2 ) ) );

			if( dim.splitPoints.isEmpty() )
			{
				// split points empty? add all distinct values as split point
				this.children = MapBuilder.<Range, PartitionNode>sorted()
//						.put( Range.infinite(),
//								// infinity placeholder range
//								new PartitionNode( this, this.bounds ) )
						.build();
				if( partition.isEmpty() ) return;

				V v = valueSupplier.apply( partition.get( 0 ) );
				PartitionNode vNode = valueNode( Range.of( v ), nodeSplitter );

				int offset = this.bounds[0];
				for( int i = 1; i < partition.size(); i++ )
				{
					final V v2 = valueSupplier.apply( partition.get( i ) );
					if( dim.comparator.compare( v2, v ) != 0 )
					{
						v = v2;
						vNode.bounds[0] = offset;
						vNode.bounds[1] = this.bounds[0] + i;
						offset = vNode.bounds[1]; // intermediate
						vNode = valueNode( Range.of( v2 ), nodeSplitter );
					}
				}
				vNode.bounds[0] = offset;
				vNode.bounds[1] = this.bounds[1];
				return;
			}

			final int[] splitKeys = new int[dim.splitPoints.size()];
			for( int i = 0, k = 0; i != dim.splitPoints.size(); i++ )
			{
				while( k < partition.size() && dim.comparator.compare(
						valueSupplier.apply( partition.get( k ) ),
						dim.splitPoints.get( i ) ) < 0 )
					k++; // value[key] > point[i] : put key in next range
				splitKeys[i] = this.bounds[0] + k;
			}

			// map split points to respective sub-partition bounds
			this.children = IntStream.range( 0, dim.splitPoints.size() + 1 )
					.collect(
							// create new partition's range-bounds mapping
							() -> new TreeMap<>( ( r1, r2 ) -> Range
									.compare( r1, r2, dim.comparator ) ),
							// add split node (value range and key bounds)
							( map, i ) -> map.put(
									toRange( dim.splitPoints, i ),
									new PartitionNode( this,
											toBounds( this.bounds, splitKeys,
													i ) ) ),
							// map-reduce parallelism
							NavigableMap::putAll );
		}

		@SuppressWarnings( "unchecked" )
		<V extends Comparable> PartitionNode valueNode( final Range bin,
			final Consumer<PartitionNode> nodeSplitter )
		{
			return this.children.computeIfAbsent( bin, k ->
			{
				final Range prev = this.children.lowerKey( bin );
				final int i = prev == null ? this.bounds[0]
						: this.children.get( prev ).bounds[1];
				final int[] range = { i, i }; // initialize empty 
				final PartitionNode result = new PartitionNode( this, range );
				nodeSplitter.accept( result );
				return result;
			} );
		}
	}
}