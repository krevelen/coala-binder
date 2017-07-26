package io.coala.math;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;

import io.coala.exception.ExceptionFactory;
import io.coala.exception.Thrower;
import io.coala.util.Compare;
import tec.uom.se.ComparableQuantity;

/**
 * {@link FrequencyDistribution} counts phenomena with a
 * <a href="https://www.wikiwand.com/en/Level_of_measurement#Nominal_level">
 * nominal level of measurement</a>
 * 
 * @param <T> the type of phenomena
 * @param <THIS> the concrete type of {@link FrequencyDistribution}
 * @version $Id: 0565cc0db6eb642749fbed05abbda7e37e75e86e $
 * @author Rick van Krevelen
 * @see <a href="https://www.wikiwand.com/en/Frequency_distribution">Wikipedia
 *      </a>
 */
public interface FrequencyDistribution<T, THIS extends FrequencyDistribution<T, THIS>>
{

	Map<T, BigDecimal> getFrequencies();

	BigDecimal getSumFrequency();

	T getMode();

	THIS add( T phenomenon, Number count );

	default THIS add( final T phenomenon, final Quantity<Dimensionless> count )
	{
		return add( phenomenon, count.getValue() );
	}

	default THIS add( final T phenomenon )
	{
		return add( phenomenon, BigDecimal.ONE );
	}

	@SuppressWarnings( "unchecked" )
	default THIS add( final T... phenomena )
	{
		for( T phenomenon : phenomena )
			add( phenomenon );
		return (THIS) this;
	}

	@SuppressWarnings( "unchecked" )
	default THIS add( final Iterable<T> phenomena )
	{
		for( T phenomenon : phenomena )
			add( phenomenon );
		return (THIS) this;
	}

	@SuppressWarnings( "unchecked" )
	default THIS add( final FrequencyDistribution<T, ?> frequencies )
	{
		frequencies.getFrequencies().forEach( this::add );
		return (THIS) this;
	}

	default Iterable<T> uniqueValues()
	{
		return getFrequencies().keySet();
	}

	default BigDecimal frequencyOf( final T phenomenon )
	{
		return getFrequencies().get( phenomenon );
	}

	default BigDecimal proportionOf( final T phenomenon )
	{
		return frequencyOf( phenomenon ).divide( getSumFrequency() );
	}

	default ComparableQuantity<Dimensionless> proportionOf( final T phenomenon,
		final Unit<Dimensionless> unit )
	{
		return QuantityUtil.valueOf( proportionOf( phenomenon ) ).to( unit );
	}

	default Map<T, BigDecimal> toProportions()
	{
		return StreamSupport.stream( uniqueValues().spliterator(), true )
				.collect( Collectors.toMap( value -> value,
						value -> proportionOf( value ) ) );
	}

	default Map<T, ComparableQuantity<Dimensionless>>
		toProportions( final Unit<Dimensionless> unit )
	{
		return StreamSupport.stream( uniqueValues().spliterator(), true )
				.collect( Collectors.toMap( value -> value,
						value -> proportionOf( value, unit ) ) );
	}

	/**
	 * {@link Ordinal}-type {@link FrequencyDistribution} counts
	 * {@link Comparable} phenomena with an <a href=
	 * "https://www.wikiwand.com/en/Level_of_measurement#Ordinal_scale">ordinal
	 * scale</a>
	 * 
	 * @param <T> the type of {@link Comparable} phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id: 0565cc0db6eb642749fbed05abbda7e37e75e86e $
	 * @author Rick van Krevelen
	 */
	interface Ordinal<T extends Comparable<? super T>, THIS extends Ordinal<T, THIS>>
		extends FrequencyDistribution<T, THIS>
	{

		T getMedian();

		Range<T> getRange();

		@Override
		NavigableMap<T, BigDecimal> getFrequencies();

		@Override
		default NavigableMap<T, BigDecimal> toProportions()
		{
			return StreamSupport.stream( uniqueValues().spliterator(), true )
					.collect( Collectors.toMap( value -> value,
							value -> proportionOf( value ),
							( v1, v2 ) -> Thrower.throwNew(
									IllegalStateException::new,
									() -> "Can't merge " + v1 + " and " + v2 ),
							() -> new ConcurrentSkipListMap<>() ) );
		}

		@Override
		default NavigableMap<T, ComparableQuantity<Dimensionless>>
			toProportions( final Unit<Dimensionless> unit )
		{
			return StreamSupport.stream( uniqueValues().spliterator(), true )
					.collect( Collectors.toMap( value -> value,
							value -> proportionOf( value, unit ),
							( v1, v2 ) -> Thrower.throwNew(
									IllegalStateException::new,
									() -> "Can't merge " + v1 + " and " + v2 ),
							() -> new ConcurrentSkipListMap<>() ) );
		}

		NavigableMap<T, BigDecimal> getCumulatives();

		BigDecimal cumulativeFrequencyOf( T phenomenon );

		ComparableQuantity<Dimensionless> cumulativeProportionOf( T phenomenon,
			Unit<Dimensionless> unit );

		NavigableMap<T, ComparableQuantity<Dimensionless>>
			toCumulativeProportions( Unit<Dimensionless> unit );

	}

	/**
	 * {@link Interval}-type {@link FrequencyDistribution} counts
	 * {@link Measurable} phenomena with an <a href=
	 * "https://www.wikiwand.com/en/Level_of_measurement#Interval_scale">
	 * interval scale</a>
	 * 
	 * @param <Q> the {@link Quantity} type of the {@link Measurable} phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id: 0565cc0db6eb642749fbed05abbda7e37e75e86e $
	 * @author Rick van Krevelen
	 */
	interface Interval<Q extends Quantity<Q>, THIS extends Interval<Q, THIS>>
		extends Ordinal<ComparableQuantity<Q>, THIS>
	{

		NavigableMap<ComparableQuantity<Q>, Bin<ComparableQuantity<Q>>>
			getBins();

		ComparableQuantity<Q> getMean();

	}

	/**
	 * {@link Ratio}-type {@link FrequencyDistribution} counts
	 * {@link Measurable} phenomena with a
	 * <a href="https://www.wikiwand.com/en/Level_of_measurement#Ratio_scale">
	 * ratio scale</a> (having an absolute origin)
	 * 
	 * @param <Q> the type of {@link Quantity} of the measurable phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id: 0565cc0db6eb642749fbed05abbda7e37e75e86e $
	 * @author Rick van Krevelen
	 */
	interface Ratio<Q extends Quantity<Q>, THIS extends Ratio<Q, THIS>>
		extends Interval<Q, THIS>
	{

		ComparableQuantity<Q> getSum();

	}

	/**
	 * {@link Simple} implements {@link FrequencyDistribution} backed by a
	 * {@link HashMap}
	 * 
	 * @param <T> the type of phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id: 0565cc0db6eb642749fbed05abbda7e37e75e86e $
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "unchecked" )
	class Simple<T, THIS extends Simple<T, THIS>>
		implements FrequencyDistribution<T, THIS>
	{

		private final Map<T, BigDecimal> frequencies;

		private BigDecimal sumFreq;

		private BigDecimal modeFreq;

		private T mode;

		public Simple()
		{
			this( new HashMap<>() );
		}

		protected Simple( final Map<T, BigDecimal> frequencies )
		{
			this.frequencies = frequencies;

			// initialize mode
			frequencies.forEach( ( key, value ) ->
			{
				updateMode( key, value );
				updateSum( value );
			} );
		}

		@Override
		public T getMode()
		{
			return this.mode;
		}

		@Override
		public BigDecimal getSumFrequency()
		{
			return this.sumFreq;
		}

		@Override
		public Map<T, BigDecimal> getFrequencies()
		{
			return this.frequencies;
		}

		protected void updateMode( final T phenomenon,
			final BigDecimal frequency )
		{
			if( Compare.le( frequency, this.modeFreq ) ) return;
			this.mode = phenomenon;
			this.modeFreq = frequency;
		}

		protected void updateSum( final BigDecimal addendum )
		{
			this.sumFreq = this.sumFreq.add( addendum );
		}

		protected void put( final T phenomenon, final BigDecimal frequency,
			final BigDecimal delta )
		{
			synchronized( getFrequencies() )
			{
				getFrequencies().put( phenomenon, frequency );
				updateMode( phenomenon, frequency );
				updateSum( delta );
			}
		}

		@Override
		public THIS add( final T phenomenon, final Number c )
		{
			synchronized( getFrequencies() )
			{
				final BigDecimal count = DecimalUtil.valueOf( c );
				if( count.signum() < 0 ) throw ExceptionFactory.createUnchecked(
						"Can't add count {} (< 0) of {}", count, phenomenon );

				final BigDecimal oldFreq = getFrequencies().get( phenomenon );
				final BigDecimal newFreq = oldFreq == null ? count
						: count.add( oldFreq );
				put( phenomenon, newFreq, count );
				return (THIS) this;
			}
		}

		public static <T, THIS extends Simple<T, THIS>> Simple<T, ?>
			of( final T phenomenon )
		{
			return new Simple<T, THIS>().add( phenomenon );
		}

		public static <T, THIS extends Simple<T, THIS>> Simple<T, THIS>
			of( final T... phenomena )
		{
			return new Simple<T, THIS>().add( phenomena );
		}

		public static <T, THIS extends Simple<T, THIS>> Simple<T, THIS>
			of( final Iterable<T> phenomena )
		{
			return new Simple<T, THIS>().add( phenomena );
		}
	}

	/**
	 * {@link SimpleOrdinal} implements an {@link Ordinal}-type
	 * {@link FrequencyDistribution} of {@link Comparable} (i.e. ordinal)
	 * phenomena backed by a {@link ConcurrentSkipListMap}
	 * 
	 * @param <T> the type of {@link Comparable} (i.e. ordinal) phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id: 0565cc0db6eb642749fbed05abbda7e37e75e86e $
	 * @author Rick van Krevelen
	 */
	class SimpleOrdinal<T extends Comparable<? super T>, THIS extends SimpleOrdinal<T, THIS>>
		extends Simple<T, THIS> implements Ordinal<T, THIS>
	{

		protected static final BigDecimal TWO = new BigDecimal( 2 );

		private final NavigableMap<T, BigDecimal> cumulatives;

		private Range<T> range;

		private T median;

		public SimpleOrdinal()
		{
			this( new ConcurrentSkipListMap<>() );
		}

		protected SimpleOrdinal( final Map<T, BigDecimal> frequencies )
		{
			this( frequencies instanceof NavigableMap
					? (NavigableMap<T, BigDecimal>) frequencies
					: new ConcurrentSkipListMap<>( frequencies ) );
		}

		protected SimpleOrdinal( final NavigableMap<T, BigDecimal> frequencies )
		{
			super( frequencies );

			// initialize range
			this.range = Range.infinite();

			// initialize median
			this.median = null;

			// initialize cumulative frequencies and median
			this.cumulatives = new ConcurrentSkipListMap<>();
			BigDecimal cumulative = BigDecimal.ZERO;
			for( Entry<T, BigDecimal> entry : frequencies.entrySet() )
			{
				cumulative = cumulative.add( entry.getValue() );
				this.cumulatives.put( entry.getKey(), cumulative );
			}

			if( cumulative.signum() > 0 ) this.median = calcMedian();
		}

		protected void updateRange( final T phenomenon )
		{
			if( !this.range.contains( phenomenon ) )
				this.range = Range.of( getFrequencies().firstKey(),
						getFrequencies().lastKey() );
		}

		/**
		 * @param tail the phenomena of which to update cumulative frequencies
		 * @param delta the difference to add
		 */
		protected void updateCumulatives(
			final NavigableMap<T, BigDecimal> tail, final BigDecimal delta )
		{
			BigDecimal cumulative = cumulativeFrequencyOf( tail.firstKey() )
					.subtract( frequencyOf( tail.firstKey() ) );
			this.cumulatives.clear();
			for( Entry<T, BigDecimal> entry : tail.entrySet() )
				this.cumulatives.put( entry.getKey(),
						cumulative = cumulative.add( entry.getValue() ) );

			final T last = getFrequencies().lastKey();
			if( cumulativeFrequencyOf( last )
					.compareTo( getSumFrequency() ) != 0 )
				throw ExceptionFactory.createUnchecked(
						"Cumulative frequency {} of last entry {} != sum {}",
						cumulativeFrequencyOf( last ), last,
						getSumFrequency() );
		}

		protected T calcMedian()
		{
			final BigDecimal semi = getSumFrequency().divide( TWO );
			T median = getCumulatives().firstKey();
			for( Entry<T, BigDecimal> entry : getCumulatives().entrySet() )
			{
				if( Compare.gt( entry.getValue(), semi ) ) return median;
				median = entry.getKey();
			}
			return median;
		}

		@Override
		protected void put( final T phenomenon, final BigDecimal frequency,
			final BigDecimal delta )
		{
			synchronized( getFrequencies() )
			{
				super.put( phenomenon, frequency, delta );

				updateRange( phenomenon );
				updateCumulatives( getFrequencies().tailMap( phenomenon, true ),
						delta );
				this.median = calcMedian();
			}
		}

		@Override
		public NavigableMap<T, BigDecimal> getFrequencies()
		{
			return (NavigableMap<T, BigDecimal>) super.getFrequencies();
		}

		@Override
		public T getMedian()
		{
			return this.median;
		}

		@Override
		public Range<T> getRange()
		{
			return this.range;
		}

		@Override
		public NavigableMap<T, BigDecimal> getCumulatives()
		{
			return this.cumulatives;
		}

		@Override
		public BigDecimal cumulativeFrequencyOf( final T phenomenon )
		{
			return getCumulatives().get( phenomenon );
		}

		@Override
		public ComparableQuantity<Dimensionless> cumulativeProportionOf(
			final T phenomenon, final Unit<Dimensionless> unit )
		{
			return QuantityUtil.valueOf( cumulativeFrequencyOf( phenomenon )
					.divide( getSumFrequency() ) ).to( unit );
		}

		@Override
		public NavigableMap<T, ComparableQuantity<Dimensionless>>
			toProportions( final Unit<Dimensionless> unit )
		{
			final NavigableMap<T, ComparableQuantity<Dimensionless>> result = new ConcurrentSkipListMap<>();
			for( T value : uniqueValues() )
				result.put( value, proportionOf( value, unit ) );
			return result;
		}

		@Override
		public NavigableMap<T, ComparableQuantity<Dimensionless>>
			toCumulativeProportions( final Unit<Dimensionless> unit )
		{
			return getCumulatives().entrySet().parallelStream().collect(
					Collectors.toMap( e -> e.getKey(), e -> QuantityUtil
							.valueOf( e.getValue().divide( getSumFrequency() ) )
							.to( unit ),
							( v1, v2 ) -> Thrower.throwNew(
									IllegalStateException::new, () -> "Can't merge "
											+ v1 + " and " + v2 ),
							() -> new ConcurrentSkipListMap<>() ) );
		}
	}

	/**
	 * {@link SimpleInterval} implements an {@link Interval}-type
	 * {@link FrequencyDistribution} of {@link Measurable} phenomena backed by a
	 * {@link ConcurrentSkipListMap}
	 * 
	 * @param <Q> the type of {@link Quantity} of the measurable phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id: 0565cc0db6eb642749fbed05abbda7e37e75e86e $
	 * @author Rick van Krevelen
	 */
	class SimpleInterval<Q extends Quantity<Q>, THIS extends SimpleInterval<Q, THIS>>
		extends SimpleOrdinal<ComparableQuantity<Q>, THIS>
		implements Interval<Q, THIS>
	{

		private NavigableMap<ComparableQuantity<Q>, Bin<ComparableQuantity<Q>>> bins;

		private ComparableQuantity<Q> median;

		private ComparableQuantity<Q> sum;

		private ComparableQuantity<Q> mean;

//		@SuppressWarnings( "unchecked" )
		@Override
		protected ComparableQuantity<Q> calcMedian()
		{
			final boolean interpolate = !DecimalUtil
					.isExact( getSumFrequency() )
					|| getSumFrequency().remainder( TWO ).signum() == 0;
			final BigDecimal halfFreq = getSumFrequency().add( BigDecimal.ONE )
					.divide( TWO );
			Entry<ComparableQuantity<Q>, BigDecimal> prev = getCumulatives()
					.firstEntry();
			for( Entry<ComparableQuantity<Q>, BigDecimal> entry : getCumulatives()
					.tailMap( prev.getKey(), false ).entrySet() )
			{
				// first pass halfway for median
				if( Compare.le( entry.getValue(), halfFreq ) )
				{
					prev = entry;
					continue;
				}
				// interpolate? before- and after-half entries if uneven sumfreq
				if( interpolate && prev != null
						&& Compare.eq( prev.getValue(), halfFreq ) )
					return (ComparableQuantity<Q>) prev.getKey()
							.add( entry.getKey() ).divide( TWO );

				// otherwise? return before/on-half entry upon passing
				return prev.getKey();
			}
			return prev.getKey();
		}

		protected Bin<ComparableQuantity<Q>>
			resolveBin( final ComparableQuantity<Q> amount )
		{
			// FIXME initialize and resolve bins, incl infinite extremes?

			final Entry<?, Bin<ComparableQuantity<Q>>> floor = getBins()
					.floorEntry( amount );
//			Objects.requireNonNull( floor, "Amount out of range: " + amount );
			if( floor != null ) return floor.getValue();
//			final Entry<?, Bin<Q>> ceil = getBins().ceilingEntry( amount );
//			if( ceil != null ) return ceil.getValue();
			throw new IndexOutOfBoundsException(
					"Amount " + amount + " out of range: " + getRange() );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public THIS add( final ComparableQuantity<Q> value, final Number count )
		{
			synchronized( getFrequencies() )
			{
				final Bin<ComparableQuantity<Q>> bin = resolveBin( value );
				super.add( bin.getKernel(), count );
				this.sum = this.sum.add( value.multiply( count ) );
				this.mean = this.sum.divide( getSumFrequency() );
				return (THIS) this;
			}
		}

		@Override
		public ComparableQuantity<Q> getMedian()
		{
			return this.median;
		}

		public ComparableQuantity<Q> getSum()
		{
			return this.sum;
		}

		@Override
		public NavigableMap<ComparableQuantity<Q>, Bin<ComparableQuantity<Q>>>
			getBins()
		{
			return this.bins;
		}

		@Override
		public ComparableQuantity<Q> getMean()
		{
			return this.mean;
		}
	}

	class SimpleRatio<Q extends Quantity<Q> & Comparable<Quantity<Q>>, THIS extends SimpleRatio<Q, THIS>>
		extends SimpleInterval<Q, THIS> implements Ratio<Q, THIS>
	{
		// explicitly expose #getSum() used by SimpleInterval to calculate mean
	}
}
