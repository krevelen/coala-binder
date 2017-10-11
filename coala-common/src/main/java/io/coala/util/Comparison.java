package io.coala.util;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;

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
	public static <T> int compare( final Comparable<? super T> o1,
		final Object o2 )
	{
		if( Objects.requireNonNull( o1 ) == Objects.requireNonNull( o2 ) )
			return 0;
		return o1.compareTo( (T) o2 );
	}

	@SuppressWarnings( "unchecked" )
	public static <T> int compare( final Comparator<T> comparator,
		final Object o1, final Object o2 )
	{
		if( Objects.requireNonNull( o1 ) == Objects.requireNonNull( o2 ) )
			return 0;
		return Objects.requireNonNull( comparator ).compare( (T) o1, (T) o2 );
	}

	/**
	 * @param m1
	 * @param m2
	 * @return <0 iff less or smaller values, 0 iff equal, >0 otherwise
	 */
	public static <K, V> int compare( final SortedMap<K, V> m1,
		final Map<K, V> m2 )
	{
		if( m1.equals( m2 ) ) return 0;
		for( Map.Entry<K, V> e : m1.entrySet() )
		{
			final V v = m2.get( e.getKey() );
			if( v == null ) return -1;
			if( e.getValue() == null ) return 1;
			if( !e.getValue().equals( v ) )
			{
				@SuppressWarnings( { "unchecked", "rawtypes" } )
				int comparison = Comparison.compare( (Comparable) e.getValue(),
						v );
				if( comparison != 0 ) return comparison;
			}
		}
		for( Object k2 : m2.keySet() )
			if( !m1.containsKey( k2 ) ) return 1;
		return 0;
	}

	public static Comparison of( final int comparison )
	{
		return comparison == 0 ? EQUIVALENT : comparison < 0 ? LESSER : GREATER;
	}

	public static <T> Comparison of( final Comparable<? super T> o1,
		final T o2 )
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
		return compare( o1, o2 ) == 0;//of( o1, o2 ) == EQUIVALENT;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 < o2</code>
	 */
	public static <T> boolean lt( final Comparable<? super T> o1, final T o2 )
	{
		return compare( o1, o2 ) < 0;//of( o1, o2 ) == LESSER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 =< o2</code>
	 */
	public static <T> boolean le( final Comparable<? super T> o1, final T o2 )
	{
		return compare( o1, o2 ) < 1;//of( o1, o2 ) != GREATER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 > o2</code>
	 */
	public static <T> boolean gt( final Comparable<? super T> o1, final T o2 )
	{
		return compare( o1, o2 ) > 0;//of( o1, o2 ) == GREATER;
	}

	/**
	 * @param <T> the type of {@link Comparable}
	 * @param o1 a {@link T} value
	 * @param o2 a {@link T} value
	 * @return <code>o1 >= o2</code>
	 */
	public static <T> boolean ge( final Comparable<? super T> o1, final T o2 )
	{
		return compare( o1, o2 ) > -1;//of( o1, o2 ) != LESSER;
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
