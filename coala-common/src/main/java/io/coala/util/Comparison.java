package io.coala.util;

import java.util.Comparator;
import java.util.Objects;

/**
 * {@link Comparison} utility class
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public enum Comparison
{

	/** the value compares as ordinally LESS */
	LESSER( -1 ),

	/** the value compares as ordinally EQUIVALENT */
	EQUIVALENT( 0 ),

	/** the value compares as ordinally GREATER */
	GREATER( 1 ),

	;

	private final int value;

	private Comparison( final int value )
	{
		this.value = value;
	}

	public int toInt()
	{
		return this.value;
	}

	public Comparison invert()
	{
		switch( this )
		{
		case LESSER:
			return GREATER;
		case GREATER:
			return LESSER;
		default:
			return this;
		}
	}

	@SuppressWarnings( "unchecked" )
	public static <T> int compare( final Comparable<? super T> o1, final T o2 )
	{
		Objects.requireNonNull( o1 );
		Objects.requireNonNull( o2 );
		return o1.compareTo( o2 );
	}

	@SuppressWarnings( "unchecked" )
	public static <T> int compare( final Comparator<T> comparator,
		final Object o1, final Object o2 )
	{
		Objects.requireNonNull( comparator );
		Objects.requireNonNull( o1 );
		Objects.requireNonNull( o2 );
		return comparator.compare( (T) o1, (T) o2 );
	}

	public static Comparison of( final int comparison )
	{
		return comparison == 0 ? EQUIVALENT : comparison < 0 ? LESSER : GREATER;
	}

	public static <T> Comparison of( final Comparable<? super T> o1, final T o2 )
	{
		return of( compare( o1, o2 ) );
	}

	public static <T> Comparison of( final Comparator<T> comparator,
		final Object o1, final Object o2 )
	{
		return of( compare( comparator, o1, o2 ) );
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 = o2</code>
	 */
	public static <T> boolean eq( final Comparable<? super T> o1, final T o2 )
	{
		return of( o1, o2 ) == EQUIVALENT;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 < o2</code>
	 */
	public static <T> boolean lt( final Comparable<? super T> o1, final T o2 )
	{
		return of( o1, o2 ) == LESSER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 =< o2</code>
	 */
	public static <T> boolean le( final Comparable<? super T> o1, final T o2 )
	{
		return of( o1, o2 ) != GREATER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 > o2</code>
	 */
	public static <T> boolean gt( final Comparable<? super T> o1, final T o2 )
	{
		return of( o1, o2 ) == GREATER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 >= o2</code>
	 */
	public static <T> boolean ge( final Comparable<? super T> o1, final T o2 )
	{
		return of( o1, o2 ) != LESSER;
	}

	public static <T> Matcher<T> is( final Comparable<T> self )
	{
		return new Matcher<T>( self );
	}

	public static class Matcher<T>
	{
		private final Comparable<T> self;

		private Matcher( final Comparable<T> self )
		{
			this.self = self;
		}

		public boolean eq( final T other )
		{
			return Comparison.eq( this.self, other );
		}

		public boolean lt( final T other )
		{
			return Comparison.lt( this.self, other );
		}

		public boolean le( final T other )
		{
			return Comparison.le( this.self, other );
		}

		public boolean gt( final T other )
		{
			return Comparison.gt( this.self, other );
		}

		public boolean ge( final T other )
		{
			return Comparison.ge( this.self, other );
		}
	}

}
