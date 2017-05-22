package io.coala.math;

import java.util.function.Function;

import io.coala.util.Comparison;

/**
 * {@link Extreme}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public class Extreme<T extends Comparable> implements Comparable<Extreme<T>>
{

	public enum Inclusiveness
	{
		/** */
		INCLUSIVE,

		/** */
		EXCLUSIVE;

		public static Inclusiveness of( final Boolean inclusive )
		{
			return inclusive == null ? null : inclusive ? INCLUSIVE : EXCLUSIVE;
		}
	}

	/**
	 * {@link BoundaryPosition} refers to an extreme of some linear
	 * {@link Range}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public enum BoundaryPosition
	{

		/** also has lower {@link #ordinal()} for natural ordering */
		LOWER( Comparison.GREATER ),

		/** also has higher {@link #ordinal()} for natural ordering */
		UPPER( Comparison.LESSER );

		/** the infinity comparison value */
		private final Comparison limitComparison;

		private BoundaryPosition( final Comparison infinity )
		{
			this.limitComparison = infinity;
		}

		/**
		 * @return the {@link Comparison} for finite values toward the
		 *         (infinite) limit of this {@link BoundaryPosition} position
		 */
		public Comparison compareLimit()
		{
			return this.limitComparison;
		}
	}

	private final T value;

	private final Inclusiveness inclusive;

	private final BoundaryPosition boundary;

	public Extreme( final T value, final Inclusiveness inclusiveness,
		final BoundaryPosition position )
	{
		this.value = value;
		this.inclusive = value == null ? null : inclusiveness;
		this.boundary = position;
	}

	public T getValue()
	{
		return this.value;
	}

	/** @return {@code true} iff this value not represents INFINITY */
	public boolean isFinite()
	{
		return this.value != null;
	}

	/** @return {@code true} iff this value represents INFINITY */
	public boolean isInfinite()
	{
		return this.value == null;
	}

	/** @return {@code true} iff this value represents POSITIVE INFINITY */
	public boolean isPositiveInfinity()
	{
		return isInfinite() && isUpperBoundary();
	}

	/** @return {@code true} iff this value represents NEGATIVE INFINITY */
	public boolean isNegativeInfinity()
	{
		return isInfinite() && isLowerBoundary();
	}

	public boolean isInclusive()
	{
		return this.inclusive == Inclusiveness.INCLUSIVE;
	}

	public boolean isExclusive()
	{
		return this.inclusive == Inclusiveness.EXCLUSIVE;
	}

	public boolean isUpperBoundary()
	{
		return this.boundary == BoundaryPosition.UPPER;
	}

	public boolean isLowerBoundary()
	{
		return this.boundary == BoundaryPosition.LOWER;
	}

	public Comparison compareLimit()
	{
		return this.boundary.compareLimit();
	}

	@Override
	public String toString()
	{
		return isPositiveInfinity() ? "+inf"
				: isNegativeInfinity() ? "-inf" : getValue().toString();
	}

	@Override
	public int compareTo( final Extreme<T> that )
	{
		return compareWith( that ).toInt();
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean equals( final Object that )
	{
		return that != null && that instanceof Extreme
				&& ((Extreme<T>) that).boundary.equals( this.boundary )
				&& (this.inclusive == null
						? ((Extreme<T>) that).inclusive == null
						: this.inclusive
								.equals( ((Extreme<T>) that).inclusive ))
				&& (this.value == null ? ((Extreme<T>) that).value == null
						: this.value.equals( ((Extreme<T>) that).value ));
	}

	/**
	 * @param that
	 * @return
	 */
	public Comparison compareWith( final Extreme<T> that )
	{
		if( isInfinite() ) return that.isInfinite()
				? Comparison.of( this.boundary, that.boundary )
				: that.compareLimit();

		if( that.isInfinite() ) return that.compareLimit();

		@SuppressWarnings( "unchecked" )
		final Comparison valueCmp = Comparison.of( (Comparable) this.value,
				that.value );
		if( valueCmp != Comparison.EQUIVALENT ) return valueCmp;

		// equivalent values, check inclusiveness
		if( isInclusive() && !that.isInclusive() ) return compareLimit();

		if( !isInclusive() && that.isInclusive() ) return that.compareLimit();

		return Comparison.EQUIVALENT;
	}

	public static <T extends Comparable<?>> Extreme<T> negativeInfinity()
	{
		return of( null, null, BoundaryPosition.LOWER );
	}

	public static <T extends Comparable<?>> Extreme<T> positiveInfinity()
	{
		return of( null, null, BoundaryPosition.UPPER );
	}

	public static <T extends Comparable<?>> Extreme<T> lower( final T value,
		final Boolean inclusive )
	{
		return of( value, Inclusiveness.of( inclusive ),
				BoundaryPosition.LOWER );
	}

	public static <T extends Comparable<?>> Extreme<T> upper( final T value,
		final Boolean inclusive )
	{
		return of( value, Inclusiveness.of( inclusive ),
				BoundaryPosition.UPPER );
	}

	public static <T extends Comparable<?>> Extreme<T> of( final T value,
		final Inclusiveness inclusiveness, final BoundaryPosition position )
	{
		return new Extreme<T>( value, inclusiveness, position );
	}

	public <R extends Comparable> Extreme<R> map( final Function<T, R> mapper )
	{
		final R newVal = getValue() == null ? null : mapper.apply( getValue() );
		return of( newVal, this.inclusive, this.boundary );
	}
}