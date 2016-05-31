package io.coala.math;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.coala.json.Wrapper;

/**
 * {@link WeightedValue}
 * 
 * @param <V> the concrete type of value
 * @param <W> the concrete type of weight {@link Number}
 * @version $Id: a7842c5dc1c8963fe6c9721cdcda6c3b21980bb0 $
 * @author Rick van Krevelen
 */
public class WeightedValue<V, W extends Number> extends Wrapper.Simple<V>
	implements Serializable
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private W weight;

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
	public WeightedValue( final V value, final W weight )
	{
		Objects.requireNonNull( value );
		wrap( value );
		this.weight = weight;
	}

	@Override
	public String toString()
	{
		return "(" + getWeight() + " => " + getValue() + ") @" + hashCode();
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean equals( final Object that )
	{
		return super.equals( that ) && (getWeight() == null
				? ((WeightedValue<V, W>) that).getWeight() == null
				: getWeight().equals( ((WeightedValue<V, W>) that).getWeight() ));
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
	public W getWeight()
	{
		return this.weight;
	}

	@SuppressWarnings( "serial" )
	public static class Ordinal<V extends Comparable<? super V>, W extends Number>
		extends WeightedValue<V, W> implements Comparable<WeightedValue<V, W>>
	{
		public Ordinal( final V key, final W value )
		{
			super( key, value );
		}

		@Override
		public int compareTo( final WeightedValue<V, W> o )
		{
			return Util.compare( getValue(), o.getValue() );
		}
	}

	/**
	 * @param key
	 * @param value
	 * @return a {@link WeightedValue} instance
	 */
	public static <V, M extends Number> WeightedValue<V, M> of( final V key,
		final M value )
	{
		return new WeightedValue<V, M>( key, value );
	}

	public static <V, M extends Number> Set<WeightedValue<V, M>>
		of( final Map<V, M> values )
	{
		final Set<WeightedValue<V, M>> result = new HashSet<>();
		for( Entry<V, M> entry : values.entrySet() )
			result.add( of( entry.getKey(), entry.getValue() ) );
		return result;
	}

	/**
	 * @param key
	 * @param value
	 * @return an {@link Ordinal} instance
	 */
	public static <V extends Comparable<? super V>, M extends Number>
		WeightedValue<V, M> ofOrdinal( final V key, final M value )
	{
		return new Ordinal<V, M>( key, value );
	}

	public static <V extends Comparable<? super V>, M extends Number>
		NavigableSet<WeightedValue<V, M>> ofOrdinal( final Map<V, M> values )
	{
		final NavigableSet<WeightedValue<V, M>> result = new ConcurrentSkipListSet<>();
		for( Entry<V, M> entry : values.entrySet() )
			result.add( ofOrdinal( entry.getKey(), entry.getValue() ) );
		return result;
	}
}