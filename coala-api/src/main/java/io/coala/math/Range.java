package io.coala.math;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Quantity;
import javax.measure.Unit;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.coala.exception.Thrower;
import io.coala.function.ThrowingFunction;
import io.coala.util.Compare;
import io.coala.util.Comparison;
import io.coala.util.InstanceParser;
import tec.uom.se.ComparableQuantity;

/**
 * {@link Range} similarities to Range in guava, easymock, jsr-330?
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
@JsonSerialize( using = Range.JsonSer.class )
public class Range<T extends Comparable> implements Comparable<Range<T>>
{

	public static final Range<?> INFINITE = of( null, false, null, false );

	@SuppressWarnings( "unchecked" )
	public static <T extends Comparable<?>> Range<T> infinite()
	{
		return (Range<T>) INFINITE;//of( null, null, null, null );
	}

	@SuppressWarnings( "serial" )
	public static class JsonSer extends StdSerializer<Range>
	{
		public JsonSer()
		{
			super( Range.class );
		}

		@Override
		public void serialize( final Range value, final JsonGenerator gen,
			final SerializerProvider provider ) throws IOException
		{
			gen.writeString( value.toString() );
		}
	}

	public static final String LOWER_INCLUSIVE_GROUP = "lincl";

	public static final String LOWER_VALUE_GROUP = "lower";

	public static final String UPPER_VALUE_GROUP = "upper";

	public static final String UPPER_INCLUSIVE_GROUP = "uincl";

	public static final String LOWER_INCLUSIVENESS = "[\\(\\u005B\\u003C\\u3008\\u2329]";

	public static final String EXTREME_SEPARATORS = ":;";

	public static final String UPPER_INCLUSIVENESS = "[\\)\\u005D\\u003E\\u3009\\u232A]";

	/**
	 * matches string representations like: <code>&#x3008;&larr;; max]</code> or
	 * <code>[0:15]</code>
	 */
	public static final String RANGE_FORMAT_REGEX = "^(?<"
			+ LOWER_INCLUSIVE_GROUP + ">" + LOWER_INCLUSIVENESS + ")" + "\\s*"
			+ "(?<" + LOWER_VALUE_GROUP + ">[^" + EXTREME_SEPARATORS + "]+)"
			+ "\\s*[" + EXTREME_SEPARATORS + "]\\s*" + "(?<" + UPPER_VALUE_GROUP
			+ ">[^>\\])]*)" + "\\s*(?<" + UPPER_INCLUSIVE_GROUP + ">"
			+ UPPER_INCLUSIVENESS + ")$";

	/**
	 * matches string representations like: <code>&#x3008;&larr; ; inf></code>
	 * or <code>[0:15]</code>
	 */
	public static final Pattern RANGE_FORMAT = Pattern
			.compile( RANGE_FORMAT_REGEX );

	/**
	 * matches string representations like: <code>&#x3008;&larr;; +inf></code>
	 * or <code>[0:15]</code>
	 */
	public static Range<BigDecimal> parse( final String range )
		throws ParseException
	{
		return parse( range, BigDecimal::new );
	}

	/**
	 * matches string representations like: <code>&#x3008;&larr;; +inf></code>
	 * or <code>[0:15]</code>
	 */
	public static <T extends Comparable<?>> Range<T> parse( final String range,
		final Class<T> valueType ) throws ParseException
	{
		return parse( range, InstanceParser.of( valueType )::parse );
	}

	/**
	 * matches string representations like: <code>&#x3008;&larr;; +inf></code>
	 * or <code>[10 year; 12 year]</code>
	 */
	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> Range<ComparableQuantity<Q>>
		parseQuantity( final String range, final Class<Q> dim )
			throws ParseException
	{
		return parse( range, QuantityUtil::valueOf )
				.map( q -> q.asType( dim ) );
	}

	/**
	 * @param timeRange
	 * @param valueType
	 * @return
	 * @throws ParseException
	 */
	public static <T extends Comparable<?>> Range<T> parse( final String range,
		final ThrowingFunction<String, T, ?> argParser ) throws ParseException
	{
		final Matcher m = RANGE_FORMAT.matcher( range.trim() );
		if( !m.find() ) return Thrower.throwNew( ParseException::new,
				() -> "Incorrect format, expected e.g. `[lower;upper>`, was: "
						+ range,
				0 );

		T lower, upper;
		boolean lowerIncl, upperIncl;
		try
		{
			lower = argParser.apply( m.group( LOWER_VALUE_GROUP ).trim() );
			lowerIncl = m.group( LOWER_INCLUSIVE_GROUP ).equals( "[" );
		} catch( Throwable e )
		{
			// assume infinity if not parsable (e.g. '-inf')
			lower = null;
			lowerIncl = false;
		}
		try
		{
			upper = argParser.apply( m.group( UPPER_VALUE_GROUP ).trim() );
			upperIncl = m.group( UPPER_INCLUSIVE_GROUP ).equals( "]" );
		} catch( Throwable e )
		{
			// assume infinity if not parsable (e.g. '+inf')
			upper = null;
			upperIncl = false;
		}
		return of( lower, lowerIncl, upper, upperIncl );
	}

	private Extreme<T> upper;

	private Extreme<T> lower;

	protected Range()
	{
		// bean constructor
	}

	public Range( final Extreme<T> minimum, final Extreme<T> maximum )
	{
		final Extreme<T> lower = minimum == null ? Extreme.negativeInfinity()
				: minimum;
		final Extreme<T> upper = maximum == null ? Extreme.positiveInfinity()
				: maximum;
		// sanity check
		if( Compare.gt( lower, upper ) )
			Thrower.throwNew( IllegalArgumentException::new,
					() -> "Range undefined, min: " + minimum + ", max: "
							+ maximum );

		this.lower = lower;
		this.upper = upper;
	}

	/** @return the minimum value, or {@code null} for (negative) infinity */
	public Extreme<T> getLower()
	{
		return this.lower;
	}

	/** @return the maximum value, or {@code null} for (positive) infinity */
	public Extreme<T> getUpper()
	{
		return this.upper;
	}

	/** convenience method */
	public T lowerValue()
	{
		return getLower().getValue();
	}

	/** convenience method */
	public T upperValue()
	{
		return getUpper().getValue();
	}

	/** convenience method */
	public boolean lowerInclusive()
	{
		return getLower().isInclusive();
	}

	/** convenience method */
	public boolean upperInclusive()
	{
		return getUpper().isInclusive();
	}

	/** convenience method */
	public boolean lowerFinite()
	{
		return getLower().isFinite();
	}

	/** convenience method */
	public boolean upperFinite()
	{
		return getUpper().isFinite();
	}

	/**
	 * @param value the {@link T} to test
	 * @return {@code true} iff this {@link Range} has a finite minimum that is
	 *         greater than specified value, {@code false} otherwise
	 */
	@SuppressWarnings( "unchecked" )
	public boolean gt( final T value )
	{
		return lowerFinite()
				&& (lowerInclusive() ? Comparison.lt( value, lowerValue() )
						: Comparison.le( value, lowerValue() ));
	}

	/**
	 * @param value the {@link T} to test
	 * @return {@code true} iff this {@link Range} has a finite maximum that is
	 *         smaller than specified value, {@code false} otherwise
	 */
	@SuppressWarnings( "unchecked" )
	public boolean lt( final T value )
	{
		return upperFinite()
				&& (upperInclusive() ? Comparison.gt( value, upperValue() )
						: Comparison.ge( value, upperValue() ));
	}

	/**
	 * @param value the {@link T} to test
	 * @return {@code true} iff this {@link Range} contains specified value
	 *         (i.e. is greater nor lesser)
	 */
	public Boolean contains( final T value )
	{
		return !gt( value ) && !lt( value );
	}

	public T crop( final T value )
	{
		return lt( value ) ? upperValue() : gt( value ) ? lowerValue() : value;
	}

	public boolean overlaps( final Range<T> that )
	{
		if( that.upperFinite() && this.gt( that.upperValue() ) ) return false;
		if( that.lowerFinite() && this.lt( that.lowerValue() ) ) return false;
		if( this.upperFinite() && that.gt( this.upperValue() ) ) return false;
		if( this.lowerFinite() && that.lt( this.lowerValue() ) ) return false;
		return true;
	}

	public Range<T> intersect( final Range<T> that )
	{
		if( this.overlaps( that ) )
			return of( Compare.max( this.getLower(), that.getLower() ),
					Compare.min( this.getUpper(), that.getUpper() ) );
		return null;
	}

	public <R extends Comparable<? super R>> Range<R>
		map( final Function<T, R> mapper )
	{
		final R lower = lowerFinite() ? mapper.apply( lowerValue() ) : null;
		final R upper = upperFinite() ? mapper.apply( upperValue() ) : null;
		return lower != null && upper != null && Compare.gt( lower, upper )
				? of( upper, upperInclusive(), lower, lowerInclusive() )
				: of( lower, lowerInclusive(), upper, upperInclusive() );
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder()
				.append( lowerInclusive() ? '[' : '<' ).append( getLower() );
		if( !lowerFinite() || !upperFinite()
				|| !lowerValue().equals( upperValue() ) )
			sb.append( "; " ).append( getUpper() );
		sb.append( upperInclusive() ? ']' : '>' );
		return sb.toString();
	}

	@Override
	public int compareTo( final Range<T> that )
	{
		return compare( this, that, Comparator.naturalOrder() );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean equals( final Object rhs )
	{
		if( rhs == null || rhs instanceof Range == false ) return false;
		final Range<T> that = (Range<T>) rhs;
		return that == this || ((lowerFinite()
				? getLower().equals( that.getLower() ) : !that.lowerFinite())
				&& (upperFinite() ? getUpper().equals( that.getUpper() )
						: !that.upperFinite()));
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
		return of( value, value );
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
		final boolean minimumInclusive, final T maximum,
		final boolean maximumInclusive )
	{
		return of(
				Extreme.lower( minimum, minimumInclusive && minimum != null ),
				Extreme.upper( maximum, maximumInclusive && maximum != null ) );
	}

	/**
	 * @param minimum the lower {@link Extreme}
	 * @param maximum the upper {@link Extreme}
	 * @return the {@link Range} instance
	 */
	public static <T extends Comparable<?>> Range<T>
		of( final Extreme<T> minimum, final Extreme<T> maximum )
	{
		return new Range<>( minimum, maximum );
	}

	public static <Q extends Quantity<Q>> Range<ComparableQuantity<Q>>
		of( final Number min, final Number max, final Unit<Q> unit )
	{
		return of( min == null ? null : QuantityUtil.valueOf( min, unit ),
				max == null ? null : QuantityUtil.valueOf( max, unit ) );
	}

	/**
	 * @param map the source mapping
	 * @return a submap view containing values with intersecting keys
	 */
	public <V> SortedMap<T, V> apply( final SortedMap<T, V> map )
	{
		return map.subMap( lowerValue(), upperValue() );
	}

	/**
	 * @param map the source mapping
	 * @return a submap view containing values with intersecting keys
	 */
	public <V> NavigableMap<T, V> apply( final NavigableMap<T, V> map )
	{
		return apply( map, false );
	}

	/**
	 * @param map the source mapping
	 * @param floorLower include the lower bound by flooring it (if possible)
	 * @return a SortedMap view containing only values of intersecting keys
	 */
	public <V> NavigableMap<T, V> apply( final NavigableMap<T, V> map,
		final boolean floorLower )
	{
		if( map.isEmpty() ) return map;
		// use previous key if floorLower==true (and finite lower bound)
		final T floor = floorLower && lowerFinite()
				? map.floorKey( lowerValue() ) : null;
		final T from = floor != null ? floor
				: lowerFinite() ? lowerValue() : map.firstKey();
		final boolean fromIncl = lowerInclusive() || !lowerFinite();
		final T to = upperFinite() ? upperValue() : map.lastKey();
		final boolean toIncl = upperInclusive() || !upperFinite();
		return map.subMap( from, fromIncl, to, toIncl );
	}

	/**
	 * @param set
	 * @param floorLower include the lower bound by flooring it (if possible)
	 * @return a subset view containing values of intersecting keys
	 */
	public NavigableSet<T> apply( final NavigableSet<T> set,
		final boolean floorLower )
	{
		if( set.isEmpty() ) return set;
		// use previous key if floorLower==true (and finite lower bound)
		final T floor = floorLower && lowerFinite() ? set.floor( lowerValue() )
				: null;
		final T from = floor != null ? floor
				: lowerFinite() ? lowerValue() : set.first();
		final boolean fromIncl = lowerInclusive() || !lowerFinite();
		final T to = upperFinite() ? upperValue() : set.last();
		final boolean toIncl = upperInclusive() || !upperFinite();
		return set.subSet( from, fromIncl, to, toIncl );
	}

	/**
	 * @param lhs comparison's left-hand-side
	 * @param rhs comparison's right-hand-side
	 * @param c the comparison's evaluator
	 * @return -1, 0 or 1 if lhs is respectively smaller, comparable, or bigger
	 *         than/to rhs
	 */
	public static <V extends Comparable> int compare( final Range<V> lhs,
		final Range<V> rhs, final Comparator<? super V> c )
	{
		return lhs.overlaps( rhs ) ? 0 // any overlap compares equal (symmetry)
				: lhs.lowerFinite() ? (rhs.lowerFinite() ? unequalOrElse(
						c.compare( lhs.lowerValue(), rhs.lowerValue() ),
						() -> (lhs.upperFinite() ? (rhs.upperFinite()
								// ;fin ? ;fin
								? c.compare( lhs.upperValue(),
										rhs.upperValue() )
								// ;fin < ;+inf
								: -1)
								: (rhs.upperFinite()
										// ;+inf > ;fin
										? 1
										// ;+inf = ;+inf
										: 0)) )
						// fin;* > -inf;*
						: 1)
						: (rhs.lowerFinite()
								// -inf;* < fin;*
								? -1
								// -inf;* = -inf;*
								: lhs.upperFinite()
										? (rhs.upperFinite()
												// -inf;fin ? -inf;fin
												? c.compare( lhs.upperValue(),
														rhs.upperValue() )
												// -inf;fin > -inf;+inf
												: 1)
										: (rhs.upperFinite()
												// -inf;+inf < -inf;fin
												? -1
												// -inf;+inf = -inf;+inf
												: 0));
	}

	static int unequalOrElse( final int comparison,
		final IntSupplier equalResolver )
	{
		return comparison != 0 ? comparison : equalResolver.getAsInt();
	}
}