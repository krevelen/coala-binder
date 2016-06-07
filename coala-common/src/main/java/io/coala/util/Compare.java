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

	public static <T extends Comparable<? super T>> T max( final T o1,
		final T o2 )
	{
		return o1.compareTo( o2 ) < 0 ? o2 : o1;
	}

	@SuppressWarnings( "unchecked" )
	public static <T extends Comparable<? super T>> T max( final T o1,
		final T... o )
	{
		T result = o1;
		if( o != null ) for( T o2 : o )
			result = max( result, o2 );
		return result;
	}

	public static <T extends Comparable<? super T>> T max( final Iterable<T> o )
	{
		Objects.requireNonNull( o );
		T result = null;
		for( T o2 : o )
			result = result == null ? o2 : max( result, o2 );
		return result;
	}

	public static <T extends Comparable<? super T>> T min( final T o1,
		final T o2 )
	{
		return o1.compareTo( o2 ) > 0 ? o2 : o1;
	}

	@SuppressWarnings( "unchecked" )
	public static <T extends Comparable<? super T>> T min( final T o1,
		final T... o )
	{
		T result = o1;
		if( o != null ) for( T o2 : o )
			result = min( result, o2 );
		return result;
	}

	public static <T extends Comparable<? super T>> T min( final Iterable<T> o )
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
	public static <T extends Comparable<? super T>> boolean eq( final T o1,
		final T o2 )
	{
		Objects.requireNonNull( o1 );
		Objects.requireNonNull( o2 );
		return Comparison.of( o1, o2 ) == Comparison.EQUIVALENT;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 < o2</code>
	 */
	public static <T extends Comparable<? super T>> boolean lt( final T o1,
		final T o2 )
	{
		Objects.requireNonNull( o1 );
		Objects.requireNonNull( o2 );
		return Comparison.of( o1, o2 ) == Comparison.LESSER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 =< o2</code>
	 */
	public static <T extends Comparable<? super T>> boolean le( final T o1,
		final T o2 )
	{
		return !gt( o1, o2 );
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 > o2</code>
	 */
	public static <T extends Comparable<? super T>> boolean gt( final T o1,
		final T o2 )
	{
		Objects.requireNonNull( o1 );
		Objects.requireNonNull( o2 );
		return Comparison.of( o1, o2 ) == Comparison.GREATER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 >= o2</code>
	 */
	public static <T extends Comparable<? super T>> boolean ge( final T o1,
		final T o2 )
	{
		return !lt( o1, o2 );
	}

}
