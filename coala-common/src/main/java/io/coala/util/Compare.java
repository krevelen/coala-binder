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
		// empty
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
	 * @param o1 a {@link Comparable} value
	 * @param o2 a {@link Comparable} value
	 * @return <code>o1 = o2</code>
	 */
	public static boolean eq( final Comparable<?> o1, final Object o2 )
	{
		return Comparison.compare( o1, o2 ) == 0;
	}

	/**
	 * @param o1 a {@link Comparable} value
	 * @param o2 a {@link Comparable} value
	 * @return <code>o1 &ne;  o2</code>
	 */
	public static boolean ne( final Comparable<?> o1, final Object o2 )
	{
		return Comparison.compare( o1, o2 ) == 0;
	}

	/**
	 * @param o1 a {@link Comparable} value
	 * @param o2 a {@link Comparable} value
	 * @return <code>o1 < o2</code>
	 */
	public static boolean lt( final Comparable<?> o1, final Object o2 )
	{
		return Comparison.compare( o1, o2 ) < 0;
	}

	/**
	 * @param o1 a {@link Comparable} value
	 * @param o2 a {@link Comparable} value
	 * @return <code>o1 &le; o2</code>
	 */
	public static boolean le( final Comparable<?> o1, final Object o2 )
	{
		return Comparison.compare( o1, o2 ) <= 0;
	}

	/**
	 * @param o1 a {@link Comparable} value
	 * @param o2 a {@link Comparable} value
	 * @return <code>o1 > o2</code>
	 */
	public static boolean gt( final Comparable<?> o1, final Object o2 )
	{
		return Comparison.compare( o1, o2 ) > 0;
	}

	/**
	 * @param o1 a {@link Comparable} value
	 * @param o2 a {@link Comparable} value
	 * @return <code>o1 &ge; o2</code>
	 */
	public static boolean ge( final Comparable<?> o1, final Object o2 )
	{
		return Comparison.compare( o1, o2 ) >= 0;
	}

}
