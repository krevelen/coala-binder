package io.coala.math;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.measure.Measurable;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionFactory;
import io.coala.util.Compare;

/**
 * {@link FrequencyDistribution} counts phenomena with a
 * <a href="https://www.wikiwand.com/en/Level_of_measurement#Nominal_level">
 * nominal level of measurement</a>
 * 
 * @param <T> the type of phenomena
 * @param <THIS> the concrete type of {@link FrequencyDistribution}
 * @version $Id$
 * @author Rick van Krevelen
 * @see <a href="https://www.wikiwand.com/en/Frequency_distribution">Wikipedia
 *      </a>
 */
public interface FrequencyDistribution<T, THIS extends FrequencyDistribution<T, THIS>>
{

	Map<T, Amount<Dimensionless>> getFrequencies();

	Amount<Dimensionless> getSumFrequency();

	T getMode();

	THIS add( T phenomenon, Amount<Dimensionless> count );

	default THIS add( final T phenomenon, final long count )
	{
		return add( phenomenon, Amount.valueOf( count, Unit.ONE ) );
	}

	default THIS add( final T phenomenon )
	{
		return add( phenomenon, Amount.ONE );
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
		for( Entry<T, Amount<Dimensionless>> entry : frequencies
				.getFrequencies().entrySet() )
			add( entry.getKey(), entry.getValue() );
		return (THIS) this;
	}

	default Iterable<T> uniqueValues()
	{
		return getFrequencies().keySet();
	}

	default Amount<Dimensionless> frequencyOf( final T phenomenon )
	{
		return getFrequencies().get( phenomenon );
	}

	default Amount<Dimensionless> proportionOf( final T phenomenon,
		final Unit<Dimensionless> unit )
	{
		return frequencyOf( phenomenon ).divide( getSumFrequency() ).to( unit );
	}

	default Map<T, Amount<Dimensionless>>
		toProportions( final Unit<Dimensionless> unit )
	{
		final Map<T, Amount<Dimensionless>> result = new HashMap<>();
		for( T value : uniqueValues() )
			result.put( value, proportionOf( value, unit ) );
		return result;
	}

	/**
	 * {@link Ordinal}-type {@link FrequencyDistribution} counts
	 * {@link Comparable} phenomena with an <a href=
	 * "https://www.wikiwand.com/en/Level_of_measurement#Ordinal_scale">ordinal
	 * scale</a>
	 * 
	 * @param <T> the type of {@link Comparable} phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Ordinal<T extends Comparable<? super T>, THIS extends Ordinal<T, THIS>>
		extends FrequencyDistribution<T, THIS>
	{

		T getMedian();

		Range<T> getRange();

		@Override
		NavigableMap<T, Amount<Dimensionless>> getFrequencies();

		@Override
		NavigableMap<T, Amount<Dimensionless>>
			toProportions( Unit<Dimensionless> unit );

		NavigableMap<T, Amount<Dimensionless>> getCumulatives();

		Amount<Dimensionless> cumulativeFrequencyOf( T phenomenon );

		Amount<Dimensionless> cumulativeProportionOf( T phenomenon,
			Unit<Dimensionless> unit );

		NavigableMap<T, Amount<Dimensionless>>
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
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Interval<Q extends Quantity, THIS extends Interval<Q, THIS>>
		extends Ordinal<Amount<Q>, THIS>
	{

		NavigableMap<Amount<Q>, Bin<Amount<Q>>> getBins();

		Amount<Q> getMean();

	}

	/**
	 * {@link Ratio}-type {@link FrequencyDistribution} counts
	 * {@link Measurable} phenomena with a
	 * <a href="https://www.wikiwand.com/en/Level_of_measurement#Ratio_scale">
	 * ratio scale</a> (having an absolute origin)
	 * 
	 * @param <Q> the type of {@link Quantity} of the measurable phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Ratio<Q extends Quantity, THIS extends Ratio<Q, THIS>>
		extends Interval<Q, THIS>
	{

		Amount<Q> getSum();

	}

	/**
	 * {@link Simple} implements {@link FrequencyDistribution} backed by a
	 * {@link HashMap}
	 * 
	 * @param <T> the type of phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "unchecked" )
	class Simple<T, THIS extends Simple<T, THIS>>
		implements FrequencyDistribution<T, THIS>
	{

		private final Map<T, Amount<Dimensionless>> frequencies;

		private Amount<Dimensionless> sumFreq;

		private Amount<Dimensionless> modeFreq;

		private T mode;

		public Simple()
		{
			this( new HashMap<T, Amount<Dimensionless>>() );
		}

		protected Simple( final Map<T, Amount<Dimensionless>> frequencies )
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
		public Amount<Dimensionless> getSumFrequency()
		{
			return this.sumFreq;
		}

		@Override
		public Map<T, Amount<Dimensionless>> getFrequencies()
		{
			return this.frequencies;
		}

		protected void updateMode( final T phenomenon,
			final Amount<Dimensionless> frequency )
		{
			if( !frequency.isLargerThan( this.modeFreq ) ) return;
			this.mode = phenomenon;
			this.modeFreq = frequency;
		}

		protected void updateSum( final Amount<Dimensionless> addendum )
		{
			this.sumFreq = this.sumFreq.plus( addendum );
		}

		protected void put( final T phenomenon,
			final Amount<Dimensionless> frequency,
			final Amount<Dimensionless> delta )
		{
			synchronized( getFrequencies() )
			{
				getFrequencies().put( phenomenon, frequency );
				updateMode( phenomenon, frequency );
				updateSum( delta );
			}
		}

		@Override
		public THIS add( final T phenomenon, final Amount<Dimensionless> count )
		{
			synchronized( getFrequencies() )
			{
				if( MeasureUtil.isNegative( count ) ) throw ExceptionFactory
						.createUnchecked( "Can't add count {} (< 0) of {}",
								count, phenomenon );

				final Amount<Dimensionless> oldFreq = getFrequencies()
						.get( phenomenon );
				final Amount<Dimensionless> newFreq = oldFreq == null ? count
						: count.plus( oldFreq );
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
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class SimpleOrdinal<T extends Comparable<? super T>, THIS extends SimpleOrdinal<T, THIS>>
		extends Simple<T, THIS> implements Ordinal<T, THIS>
	{

		private final NavigableMap<T, Amount<Dimensionless>> cumulatives;

		private Range<T> range;

		private T median;

		public SimpleOrdinal()
		{
			this( new ConcurrentSkipListMap<T, Amount<Dimensionless>>() );
		}

		protected SimpleOrdinal(
			final Map<T, Amount<Dimensionless>> frequencies )
		{
			this( frequencies instanceof NavigableMap
					? (NavigableMap<T, Amount<Dimensionless>>) frequencies
					: new ConcurrentSkipListMap<T, Amount<Dimensionless>>(
							frequencies ) );
		}

		protected SimpleOrdinal(
			final NavigableMap<T, Amount<Dimensionless>> frequencies )
		{
			super( frequencies );

			// initialize range
			this.range = Range.infinite();

			// initialize median
			this.median = null;

			// initialize cumulative frequencies and median
			this.cumulatives = new ConcurrentSkipListMap<T, Amount<Dimensionless>>();
			Amount<Dimensionless> cumulative = Amount.ZERO;
			for( Entry<T, Amount<Dimensionless>> entry : frequencies
					.entrySet() )
			{
				cumulative = cumulative.plus( entry.getValue() );
				this.cumulatives.put( entry.getKey(), cumulative );
			}

			if( Compare.gt( cumulative, Amount.ZERO ) )
				this.median = calcMedian();
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
			final NavigableMap<T, Amount<Dimensionless>> tail,
			final Amount<Dimensionless> delta )
		{
			Amount<Dimensionless> cumulative = cumulativeFrequencyOf(
					tail.firstKey() ).minus( frequencyOf( tail.firstKey() ) );
			this.cumulatives.clear();
			for( Entry<T, Amount<Dimensionless>> entry : tail.entrySet() )
				this.cumulatives.put( entry.getKey(),
						cumulative = cumulative.plus( entry.getValue() ) );

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
			final Amount<Dimensionless> semi = getSumFrequency().divide( 2 );
			T median = getCumulatives().firstKey();
			for( Entry<T, Amount<Dimensionless>> entry : getCumulatives()
					.entrySet() )
			{
				if( entry.getValue().isGreaterThan( semi ) ) return median;
				median = entry.getKey();
			}
			return median;
		}

		@Override
		protected void put( final T phenomenon,
			final Amount<Dimensionless> frequency,
			final Amount<Dimensionless> delta )
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
		public NavigableMap<T, Amount<Dimensionless>> getFrequencies()
		{
			return (NavigableMap<T, Amount<Dimensionless>>) super.getFrequencies();
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
		public NavigableMap<T, Amount<Dimensionless>> getCumulatives()
		{
			return this.cumulatives;
		}

		@Override
		public Amount<Dimensionless> cumulativeFrequencyOf( final T phenomenon )
		{
			return getCumulatives().get( phenomenon );
		}

		@Override
		public Amount<Dimensionless> cumulativeProportionOf( final T phenomenon,
			final Unit<Dimensionless> unit )
		{
			return cumulativeFrequencyOf( phenomenon )
					.divide( getSumFrequency() ).to( unit );
		}

		@Override
		public NavigableMap<T, Amount<Dimensionless>>
			toProportions( final Unit<Dimensionless> unit )
		{
			final NavigableMap<T, Amount<Dimensionless>> result = new ConcurrentSkipListMap<>();
			for( T value : uniqueValues() )
				result.put( value, proportionOf( value, unit ) );
			return result;
		}

		@Override
		public NavigableMap<T, Amount<Dimensionless>>
			toCumulativeProportions( final Unit<Dimensionless> unit )
		{
			final NavigableMap<T, Amount<Dimensionless>> result = new ConcurrentSkipListMap<>();
			for( T value : uniqueValues() )
				result.put( value, cumulativeProportionOf( value, unit ) );
			return result;
		}
	}

	/**
	 * {@link SimpleInterval} implements an {@link Interval}-type
	 * {@link FrequencyDistribution} of {@link Measurable} phenomena backed by a
	 * {@link ConcurrentSkipListMap}
	 * 
	 * @param <Q> the type of {@link Quantity} of the measurable phenomena
	 * @param <THIS> the concrete type of {@link FrequencyDistribution}
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class SimpleInterval<Q extends Quantity, THIS extends SimpleInterval<Q, THIS>>
		extends SimpleOrdinal<Amount<Q>, THIS> implements Interval<Q, THIS>
	{

		private NavigableMap<Amount<Q>, Bin<Amount<Q>>> bins;

		private Amount<Q> median;

		private Amount<Q> sum;

		private Amount<Q> mean;

		@Override
		protected Amount<Q> calcMedian()
		{
			final boolean interpolate = !getSumFrequency().isExact()
					|| getSumFrequency().getExactValue() % 2 == 0;
			final Amount<Dimensionless> halfFreq = getSumFrequency()
					.plus( Amount.ONE ).divide( 2 );
			Entry<Amount<Q>, Amount<Dimensionless>> prev = getCumulatives()
					.firstEntry();
			for( Entry<Amount<Q>, Amount<Dimensionless>> entry : getCumulatives()
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
					return prev.getKey().plus( entry.getKey() ).divide( 2 );

				// otherwise? return before/on-half entry upon passing
				return prev.getKey();
			}
			return prev.getKey();
		}

		protected Bin<Amount<Q>> resolveBin( final Amount<Q> amount )
		{
			// FIXME initialize and resolve bins, incl infinite extremes?

			final Entry<?, Bin<Amount<Q>>> floor = getBins()
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
		public THIS add( final Amount<Q> value,
			final Amount<Dimensionless> count )
		{
			synchronized( getFrequencies() )
			{
				final Bin<Amount<Q>> bin = resolveBin( value );
				super.add( bin.getKernel(), count );
				this.sum = this.sum.plus( value.times( count ) );
				this.mean = this.sum.divide( getSumFrequency() )
						.to( this.sum.getUnit() );
				return (THIS) this;
			}
		}

		@Override
		public Amount<Q> getMedian()
		{
			return this.median;
		}

		public Amount<Q> getSum()
		{
			return this.sum;
		}

		@Override
		public NavigableMap<Amount<Q>, Bin<Amount<Q>>> getBins()
		{
			return this.bins;
		}

		@Override
		public Amount<Q> getMean()
		{
			return this.mean;
		}

	}

	class SimpleRatio<Q extends Quantity, THIS extends SimpleRatio<Q, THIS>>
		extends SimpleInterval<Q, THIS> implements Ratio<Q, THIS>
	{
		// explicitly expose #getSum() used by SimpleInterval to calculate mean
	}
}
