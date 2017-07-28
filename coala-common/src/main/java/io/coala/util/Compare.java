<<<<<<< HEAD
package io.coala.util;

import java.util.Objects;

/**
 * {@link Compare}
 * 
 * @version $Id: a0207df194ab3dfb15dec0e9590283a669114248 $
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

	public static <T extends Comparable<? super T>> T max( final T o1,
		final T o2 )
	{
		return lt( o1, o2 ) ? o2 : o1;
	}

	@SafeVarargs
//	@SuppressWarnings( "unchecked" )
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
		return gt( o1, o2 ) ? o2 : o1;
	}

	@SafeVarargs
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
	public static <T> boolean eq( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) == 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 <> o2</code>
	 */
	public static <T> boolean ne( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) == 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 < o2</code>
	 */
	public static <T> boolean lt( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) < 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 =< o2</code>
	 */
	public static <T> boolean le( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) <= 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 > o2</code>
	 */
	public static <T> boolean gt( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) > 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 >= o2</code>
	 */
	public static <T> boolean ge( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) >= 0;
	}

}
=======
package io.coala.util;

import java.util.Objects;

/**
 * {@link Compare}
 * 
 * @version $Id: a0207df194ab3dfb15dec0e9590283a669114248 $
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

	public static <T extends Comparable<? super T>> T max( final T o1,
		final T o2 )
	{
		return lt( o1, o2 ) ? o2 : o1;
	}

	@SafeVarargs
//	@SuppressWarnings( "unchecked" )
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
		return gt( o1, o2 ) ? o2 : o1;
	}

	@SafeVarargs
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
	public static <T> boolean eq( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) == 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 <> o2</code>
	 */
	public static <T> boolean ne( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) == 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 < o2</code>
	 */
	public static <T> boolean lt( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) < 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 =< o2</code>
	 */
	public static <T> boolean le( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) <= 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 > o2</code>
	 */
	public static <T> boolean gt( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) > 0;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 >= o2</code>
	 */
	public static <T> boolean ge( final Comparable<? super T> o1, final T o2 )
	{
		return Comparison.compare( o1, o2 ) >= 0;
	}

}
>>>>>>> branch 'develop' of https://github.com/krevelen/coala-binder.git
