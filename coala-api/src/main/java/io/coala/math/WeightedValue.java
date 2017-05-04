package io.coala.math;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.coala.json.Wrapper;

/**
 * {@link WeightedValue}
 * 
 * @param <V> the concrete type of value
 * @param <W> the concrete type of weight {@link Number}
 * @version $Id: a7842c5dc1c8963fe6c9721cdcda6c3b21980bb0 $
 * @author Rick van Krevelen
 */
public class WeightedValue<V> extends Wrapper.Simple<V> implements Serializable
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * @param value
	 * @param weight
	 * @return a {@link WeightedValue} instance
	 */
	public static <V> WeightedValue<V> of( final V value, final Number weight )
	{
		return new WeightedValue<V>( value, weight );
	}

	public static <V> WeightedValue<V>
		of( final Map.Entry<V, ? extends Number> entry )
	{
		return of( entry.getKey(), entry.getValue() );
	}

	public static <V> List<WeightedValue<V>>
		of( final Map<V, ? extends Number> weights )
	{
		return weights.entrySet().stream().map( WeightedValue::of )
				.collect( Collectors.toList() );
	}

	/** */
	private Number weight;

	/**
	 * {@link WeightedValue} zero-arg bean constructor
	 */
	protected WeightedValue()
	{
	}

	/**
	 * {@link WeightedValue} constructor
	 * 
	 * @param value
	 * @param weight
	 */
	public WeightedValue( final V value, final Number weight )
	{
		Objects.requireNonNull( value );
		wrap( value );
		this.weight = weight;
	}

	@Override
	public String toString()
	{
		return getValue() + " (w=" + getWeight() + ")";
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean equals( final Object that )
	{
		return super.equals( that ) && getWeight() == null
				? ((WeightedValue<V>) that).getWeight() == null
				: getWeight().equals( ((WeightedValue<V>) that).getWeight() );
	}

	/**
	 * @return the value
	 */
	public V getValue()
	{
		return unwrap();
	}

	/**
	 * @return the weight
	 */
	public Number getWeight()
	{
		return this.weight;
	}

	protected void apply( final Function<Number, Number> func )
	{
		this.weight = func.apply( this.weight );
	}

	public static BigDecimal sum( final Collection<WeightedValue<?>> wvs )
	{
		BigDecimal result = BigDecimal.ZERO;
		for( WeightedValue<?> wv : wvs )
			result = result.add( DecimalUtil.valueOf( wv.getWeight() ) );
		return result;
	}

	public static void normalize( final Collection<WeightedValue<?>> weights )
	{
		final BigDecimal sum = sum( weights );
		weights.forEach( weight ->
		{
			weight.apply( w ->
			{
				return DecimalUtil.valueOf( w ).divide( sum,
						DecimalUtil.DEFAULT_CONTEXT );
			} );
		} );
	}

	@SuppressWarnings( "serial" )
	public static class Ordinal<V extends Comparable<? super V>>
		extends WeightedValue<V> implements Comparable<WeightedValue<V>>
	{
		public Ordinal( final V key, final Number value )
		{
			super( key, value );
		}

		@Override
		public int compareTo( final WeightedValue<V> o )
		{
			return Util.compare( getValue(), o.getValue() );
		}
	}

	/**
	 * @param key
	 * @param value
	 * @return an {@link Ordinal} instance
	 */
	public static <V extends Comparable<? super V>> WeightedValue<V>
		ofOrdinal( final V key, final Number value )
	{
		return new Ordinal<V>( key, value );
	}

	public static <V extends Comparable<? super V>>
		NavigableSet<WeightedValue<V>>
		ofOrdinal( final Map<V, ? extends Number> values )
	{
		final NavigableSet<WeightedValue<V>> result = new ConcurrentSkipListSet<>();
		values.forEach( ( key, value ) ->
		{
			result.add( ofOrdinal( key, value ) );
		} );
		return result;
	}

	public static <V extends Comparable<? super V>, WV extends WeightedValue<? extends V>>
		NavigableSet<WeightedValue<Bin<V>>> stratify(
			final Iterable<WV> weights, final Iterable<Bin<V>> exclusiveRanges )
	{
		final Collection<WV> remaining = new ArrayList<>();
		weights.forEach( weight ->
		{
			remaining.add( weight );
		} );
		final NavigableSet<WeightedValue<Bin<V>>> result = new ConcurrentSkipListSet<>();
		exclusiveRanges.forEach( range ->
		{
			BigDecimal weight = BigDecimal.ZERO;
			WV i;
			for( Iterator<WV> it = remaining.iterator(); it.hasNext(); )
			{
				if( range.contains( (i = it.next()).getValue() ) )
				{
					weight = weight.add( DecimalUtil.valueOf( i.getWeight() ),
							DecimalUtil.DEFAULT_CONTEXT );
					it.remove();
				}
			}
			if( weight.compareTo( BigDecimal.ZERO ) != 0 )
				result.add( of( range, weight ) );
		} );
		return result;
	}

	interface Join<L, R>
	{
		L left();

		R right();

		static <L, R> Join<L, R> of( final L left, final R right )
		{
			return new Join<L, R>()
			{
				@Override
				public L left()
				{
					return left;
				}

				@Override
				public R right()
				{
					return right;
				}
			};
		}
	}

	interface JoinOrdinal<L extends Comparable<? super L>, R extends Comparable<? super R>>
		extends Join<L, R>, Comparable<JoinOrdinal<L, R>>
	{

		static <L extends Comparable<? super L>, R extends Comparable<? super R>>
			JoinOrdinal<L, R> of( final L left, final R right )
		{
			return new JoinOrdinal<L, R>()
			{
				@Override
				public L left()
				{
					return left;
				}

				@Override
				public R right()
				{
					return right;
				}

				@Override
				public int compareTo( final JoinOrdinal<L, R> o )
				{
					final int compareLeft = left().compareTo( o.left() );
					return compareLeft != 0 ? compareLeft
							: right().compareTo( o.right() );
				}
			};
		}
	}

	public static <V, W, R> NavigableSet<WeightedValue<R>> join(
		final Iterable<WeightedValue<V>> left,
		final Iterable<WeightedValue<W>> right,
		final BiFunction<V, W, R> joiner, final boolean ignoreZeroes )
	{
		final NavigableSet<WeightedValue<R>> result = new ConcurrentSkipListSet<>();
		left.forEach( v ->
		{
			right.forEach( w ->
			{
				final BigDecimal weight = DecimalUtil.valueOf( v.getWeight() )
						.multiply( DecimalUtil.valueOf( w.getWeight() ),
								DecimalUtil.DEFAULT_CONTEXT );
				if( ignoreZeroes && weight.compareTo( BigDecimal.ZERO ) != 0 )
					result.add( of( joiner.apply( v.getValue(), w.getValue() ),
							weight ) );
			} );
		} );
		return result;
	}

	public static <V, W> NavigableSet<WeightedValue<Join<V, W>>> join(
		final Iterable<WeightedValue<V>> left,
		final Iterable<WeightedValue<W>> right, final boolean ignoreZeroes )
	{
		return join( left, right, ( l, r ) ->
		{
			return Join.of( l, r );
		}, ignoreZeroes );
	}

	public static <V extends Comparable<? super V>, W extends Comparable<? super W>>
		NavigableSet<WeightedValue<JoinOrdinal<V, W>>>
		joinOrdinal( final Iterable<WeightedValue<V>> left,
			final Iterable<WeightedValue<W>> right, final boolean ignoreZeroes )
	{
		return join( left, right, ( l, r ) ->
		{
			return JoinOrdinal.of( l, r );
		}, ignoreZeroes );
	}

}