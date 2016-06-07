package io.coala.util;

import java.util.Comparator;

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

	public static Comparison of( final int comparison )
	{
		return comparison == 0 ? EQUIVALENT : comparison < 0 ? LESSER : GREATER;
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

}
