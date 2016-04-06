package io.coala.math;

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
	LESS( -1 ),

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
		case LESS:
			return GREATER;
		case GREATER:
			return LESS;
		default:
			return this;
		}
	}

	public static Comparison of( final int comparison )
	{
		return comparison == 0 ? EQUIVALENT : comparison < 0 ? LESS : GREATER;
	}

	public static <T extends Comparable<? super T>> Comparison of( final T o1,
		final T o2 )
	{
		return of( o1.compareTo( o2 ) );
	}

	public static <T> Comparison of( final Comparator<? super T> comparator,
		final T o1, final T o2 )
	{
		return of( comparator.compare( o1, o2 ) );
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

}
