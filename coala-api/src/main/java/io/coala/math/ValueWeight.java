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
 * {@link ValueWeight}
 * 
 * @param <V> the concrete type of value
 * @param <W> the concrete type of weight {@link Number}
 * @version $Id: a7842c5dc1c8963fe6c9721cdcda6c3b21980bb0 $
 * @author Rick van Krevelen
 */
public class ValueWeight<V, W extends Number> extends Wrapper.Simple<V>
	implements Serializable
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private W weight;

	/**
	 * {@link ValueWeight} zero-arg bean constructor
	 */
	protected ValueWeight()
	{
	}

	/**
	 * {@link ValueWeight} constructor
	 * 
	 * @param value
	 * @param weight
	 */
	public ValueWeight( final V value, final W weight )
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
				? ((ValueWeight<V, W>) that).getWeight() == null
				: getWeight().equals( ((ValueWeight<V, W>) that).getWeight() ));
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
		extends ValueWeight<V, W> implements Comparable<ValueWeight<V, W>>
	{
		public Ordinal( final V key, final W value )
		{
			super( key, value );
		}

		@Override
		public int compareTo( final ValueWeight<V, W> o )
		{
			return Util.compare( getValue(), o.getValue() );
		}
	}

	/**
	 * @param key
	 * @param value
	 * @return a {@link ValueWeight} instance
	 */
	public static <V, M extends Number> ValueWeight<V, M> of( final V key,
		final M value )
	{
		return new ValueWeight<V, M>( key, value );
	}

	public static <V, M extends Number> Set<ValueWeight<V, M>>
		of( final Map<V, M> values )
	{
		final Set<ValueWeight<V, M>> result = new HashSet<>();
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
		ValueWeight<V, M> ofOrdinal( final V key, final M value )
	{
		return new Ordinal<V, M>( key, value );
	}

	public static <V extends Comparable<? super V>, M extends Number>
		NavigableSet<ValueWeight<V, M>> ofOrdinal( final Map<V, M> values )
	{
		final NavigableSet<ValueWeight<V, M>> result = new ConcurrentSkipListSet<>();
		for( Entry<V, M> entry : values.entrySet() )
			result.add( ofOrdinal( entry.getKey(), entry.getValue() ) );
		return result;
	}
}