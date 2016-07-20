package io.coala.math;

import java.util.Objects;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import io.coala.util.Compare;
import io.coala.util.Comparison;

/**
 * {@link Range}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Range<T extends Comparable<?>> implements Comparable<Range<T>>
{

	private Extreme<T> maximum;

	private Extreme<T> minimum;

	protected Range()
	{

	}

	public Range( final Extreme<T> minimum, final Extreme<T> maximum )
	{
		// sanity check
		Objects.requireNonNull( minimum );
		Objects.requireNonNull( maximum );

		this.minimum = Compare.min( minimum, maximum );
		this.maximum = Compare.max( minimum, maximum );
	}

	/** @return the minimum value, or {@code null} for (negative) infinity */
	public Extreme<T> getMinimum()
	{
		return this.minimum;
	}

	/** @return the maximum value, or {@code null} for (positive) infinity */
	public Extreme<T> getMaximum()
	{
		return this.maximum;
	}

	/**
	 * @param value the {@link T} to test
	 * @return {@code true} iff this {@link Range} has a finite minimum that is
	 *         greater than specified value, {@code false} otherwise
	 */
	public boolean isGreaterThan( final T value )
	{
		final T min = getMinimum().getValue();
		if( min == null ) return false;
		return getMinimum().isInclusive() ? Comparison.lt( value, min )
				: Comparison.le( value, min );
	}

	/**
	 * @param value the {@link T} to test
	 * @return {@code true} iff this {@link Range} has a finite maximum that is
	 *         smaller than specified value, {@code false} otherwise
	 */
	public boolean isLessThan( final T value )
	{
		final T max = getMaximum().getValue();
		if( max == null ) return false;
		return getMaximum().isInclusive() ? Comparison.gt( value, max )
				: Comparison.ge( value, max );
	}

	/**
	 * @param value the {@link T} to test
	 * @return {@code true} iff this {@link Range} contains specified value
	 */
	public Boolean contains( final T value )
	{
		return !isGreaterThan( value ) && !isLessThan( value );
	}

	public boolean overlaps( final Range<T> that )
	{
		return intersect( that ) != null;
	}

	public Range<T> intersect( final Range<T> that )
	{
		return of( Compare.max( this.getMinimum(), that.getMinimum() ),
				Compare.min( this.getMaximum(), that.getMaximum() ) );
	}

	@Override
	public int compareTo( final Range<T> that )
	{
		final int minComparison = this.getMinimum()
				.compareTo( that.getMinimum() );
		return minComparison != 0 ? minComparison
				: this.getMaximum().compareTo( that.getMaximum() );
	}

	public static <T extends Comparable<?>> Range<T> infinite()
	{
		return of( null, null, null, null );
	}

	/**
	 * @param minimum
	 * @return a {@link Range} representing <code>[x,&rarr;)</code>
	 */
	public static <T extends Comparable<?>> Range<T>
		upFromAndIncluding( final T minimum )
	{
		return upFrom( minimum, true );
	}

	/**
	 * @param minimum
	 * @param minimumInclusive
	 * @return a {@link Range} representing <code>[x,&rarr;)</code> or
	 *         <code>(x,&rarr;)</code>
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends Comparable<?>> Range<T> upFrom( final T minimum,
		final Boolean minimumInclusive )
	{
		return of( Extreme.lower( minimum, minimumInclusive ),
				(Extreme<T>) Extreme.positiveInfinity() );
	}

	/**
	 * @param maximum
	 * @return a {@link Range} representing <code>(&larr;,x]</code>
	 */
	public static <T extends Comparable<?>> Range<T>
		upToAndIncluding( final T maximum )
	{
		return upFrom( maximum, true );
	}

	/**
	 * @param maximum
	 * @param maximumInclusive
	 * @return a {@link Range} representing <code>(&larr;,x]</code> or
	 *         <code>(&larr;,x)</code>
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends Comparable<?>> Range<T> upTo( final T maximum,
		final Boolean maximumInclusive )
	{
		return of( (Extreme<T>) Extreme.negativeInfinity(),
				Extreme.upper( maximum, maximumInclusive ) );
	}

	/**
	 * @param value the value (inclusive) or {@code null} for infinite range
	 * @return the {@link Range} instance
	 */
	public static <T extends Comparable<?>> Range<T> of( final T value )
	{
		return of( value, true, value, true );
	}

	/**
	 * @param minimum the lower bound (inclusive) or {@code null} for infinite
	 * @param maximum the upper bound (inclusive) or {@code null} for infinite
	 * @return the {@link Range} instance
	 */
	public static <T extends Comparable<?>> Range<T> of( final T minimum,
		final T maximum )
	{
		return of( minimum, minimum != null, maximum, maximum != null );
	}

	/**
	 * @param minimum the lower bound or {@code null} for infinite
	 * @param minimumInclusive whether the lower bound is inclusive
	 * @param maximum the upper bound or {@code null} for infinite
	 * @param maximumInclusive whether the upper bound is inclusive
	 * @return the {@link Range} instance
	 */
	public static <T extends Comparable<?>> Range<T> of( final T minimum,
		final Boolean minimumInclusive, final T maximum,
		final Boolean maximumInclusive )
	{
		return of( Extreme.lower( minimum, minimumInclusive ),
				Extreme.upper( maximum, maximumInclusive ) );
	}

	/**
	 * @param minimum the lower {@link Extreme}
	 * @param maximum the upper {@link Extreme}
	 * @return the {@link Range} instance
	 */
	public static <T extends Comparable<?>> Range<T>
		of( final Extreme<T> minimum, final Extreme<T> maximum )
	{
		return new Range<T>( minimum, maximum );
	}

	public static <Q extends Quantity> Range<Amount<Q>> of( final Number min,
		final Number max, final Unit<Q> unit )
	{
		return of( min == null ? null : MeasureUtil.toAmount( min, unit ),
				max == null ? null : MeasureUtil.toAmount( max, unit ) );
	}
}
