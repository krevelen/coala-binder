package io.coala.util;

import java.util.Objects;

/**
 * {@link Compare}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Compare implements Util
{

	/**
	 * {@link Compare} inaccessible singleton constructor
	 */
	private Compare()
	{
	}

	public static <T extends Comparable<?>> T max( final T o1, final T o2 )
	{
		return lt( o1, o2 ) ? o2 : o1;
	}

	@SuppressWarnings( "unchecked" )
	public static <T extends Comparable<?>> T max( final T o1, final T... o )
	{
		T result = o1;
		if( o != null ) for( T o2 : o )
			result = max( result, o2 );
		return result;
	}

	public static <T extends Comparable<?>> T max( final Iterable<T> o )
	{
		Objects.requireNonNull( o );
		T result = null;
		for( T o2 : o )
			result = result == null ? o2 : max( result, o2 );
		return result;
	}

	public static <T extends Comparable<?>> T min( final T o1, final T o2 )
	{
		return gt( o1, o2 ) ? o2 : o1;
	}

	@SuppressWarnings( "unchecked" )
	public static <T extends Comparable<?>> T min( final T o1, final T... o )
	{
		T result = o1;
		if( o != null ) for( T o2 : o )
			result = min( result, o2 );
		return result;
	}

	public static <T extends Comparable<?>> T min( final Iterable<T> o )
	{
		Objects.requireNonNull( o );
		T result = null;
		for( T o2 : o )
			result = result == null ? o2 : min( result, o2 );
		return result;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 = o2</code>
	 */
	public static <T extends Comparable<?>> boolean eq( final T o1, final T o2 )
	{
		return Comparison.eq( o1, o2 );
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 < o2</code>
	 */
	public static <T extends Comparable<?>> boolean lt( final T o1, final T o2 )
	{
		return Comparison.lt( o1, o2 );
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 =< o2</code>
	 */
	public static <T extends Comparable<?>> boolean le( final T o1, final T o2 )
	{
		return Comparison.le( o1, o2 );
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 > o2</code>
	 */
	public static <T extends Comparable<?>> boolean gt( final T o1, final T o2 )
	{
		return Comparison.gt( o1, o2 );
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 >= o2</code>
	 */
	public static <T extends Comparable<?>> boolean ge( final T o1, final T o2 )
	{
		return Comparison.ge( o1, o2 );
	}

}
