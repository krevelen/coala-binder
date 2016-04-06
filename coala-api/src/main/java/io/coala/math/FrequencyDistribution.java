package io.coala.math;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.measure.Measurable;
import javax.measure.quantity.Quantity;

import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionFactory;

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

	Long getSumFrequency();

	T getMode();

	Map<T, Long> getFrequencies();

	Iterable<T> uniqueValues();

	Long frequencyOf( T phenomenon );

	THIS add( T phenomenon );

	@SuppressWarnings( "unchecked" )
	THIS add( T... phenomena );

	THIS add( Iterable<T> phenomena );

	THIS add( T phenomenon, Long addendum );

	THIS add( FrequencyDistribution<T, ?> phenomena );

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
		NavigableMap<T, Long> getFrequencies();

		NavigableMap<T, Long> getCumulatives();

		Long cumulativeOf( T phenomenon );

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
	 * {@link Bin}
	 * 
	 * @param <Q> the {@link Quantity} of extreme {@link Amount} values
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Bin<Q extends Quantity> extends Range<Amount<Q>>
		implements Comparable<Bin<Q>>
	{
		private final Amount<Q> kernel;

		public Bin( final Extreme<Amount<Q>> minimum,
			final Extreme<Amount<Q>> maximum )
		{
			super( minimum, maximum );
			this.kernel = minimum.isInfinity()
					? (maximum.isInfinity() ? null : maximum.getValue())
					: maximum.isInfinity() ? minimum.getValue()
							: minimum.getValue().plus( maximum.getValue() )
									.divide( 2 );
		}

		@Override
		public int compareTo( final Bin<Q> o )
		{
			return getKernel().compareTo( o.getKernel() );
		}

		public Amount<Q> getKernel()
		{
			return this.kernel;
		}

		public static <Q extends Quantity> Bin<Q> of( final Amount<Q> minIncl,
			final Amount<Q> maxExcl )
		{
			return new Bin<Q>( Extreme.lower( minIncl, minIncl != null ),
					Extreme.upper( maxExcl, false ) );
		}
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

		private final Map<T, Long> frequencies;

		private final Map<T, Long> unmodifiable;

		private T mode;

		private Long modeFreq;

		private Long sum;

		public Simple()
		{
			this( new HashMap<T, Long>() );
		}

		protected Simple( final Map<T, Long> frequencies )
		{
			this.frequencies = frequencies;

			// initialize mode
			for( Entry<T, Long> entry : frequencies.entrySet() )
			{
				updateMode( entry.getKey(), entry.getValue() );
				updateSum( entry.getValue() );
			}

			this.unmodifiable = Collections.unmodifiableMap( this.frequencies );
		}

		@Override
		public T getMode()
		{
			return this.mode;
		}

		@Override
		public Long getSumFrequency()
		{
			return this.sum;
		}

		@Override
		public Map<T, Long> getFrequencies()
		{
			return this.unmodifiable;
		}

		@Override
		public Iterable<T> uniqueValues()
		{
			return getFrequencies().keySet();
		}

		@Override
		public Long frequencyOf( final T phenomenon )
		{
			return getFrequencies().get( phenomenon );
		}

		protected void updateMode( final T phenomenon, final Long frequency )
		{
			if( frequency <= this.modeFreq ) return;
			this.mode = phenomenon;
			this.modeFreq = frequency;
		}

		protected void updateSum( final Long addendum )
		{
			this.sum += addendum;
		}

		protected synchronized void put( final T phenomenon,
			final Long frequency, final Long delta )
		{
			this.frequencies.put( phenomenon, frequency );
			updateMode( phenomenon, frequency );
			updateSum( delta );
		}

		@Override
		public synchronized THIS add( final T phenomenon, final Long addendum )
		{
			if( addendum < 1L ) throw ExceptionFactory.createUnchecked(
					"Can't add {} (=< 0) of {}", addendum, phenomenon );

			final Long oldFreq = this.frequencies.get( phenomenon );
			final Long newFreq = oldFreq == null ? addendum
					: addendum + oldFreq;
			put( phenomenon, newFreq, addendum );
			return (THIS) this;
		}

		@Override
		public THIS add( final T phenomenon )
		{
			return add( phenomenon, 1L );
		}

		@Override
		public THIS add( final T... phenomena )
		{
			for( T phenomenon : phenomena )
				add( phenomenon );
			return (THIS) this;
		}

		@Override
		public THIS add( final Iterable<T> phenomena )
		{
			for( T phenomenon : phenomena )
				add( phenomenon );
			return (THIS) this;
		}

		@Override
		public THIS add( final FrequencyDistribution<T, ?> frequencies )
		{
			for( Entry<T, Long> entry : frequencies.getFrequencies()
					.entrySet() )
				add( entry.getKey(), entry.getValue() );
			return (THIS) this;
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

		private final NavigableMap<T, Long> unmodifiable;

		private final NavigableMap<T, Long> cumulatives;

		private final NavigableMap<T, Long> cumulativesUnmodifiable;

		private Range<T> range;

		private T median;

		public SimpleOrdinal()
		{
			this( new ConcurrentSkipListMap<T, Long>() );
		}

		protected SimpleOrdinal( final Map<T, Long> frequencies )
		{
			this( frequencies instanceof NavigableMap
					? (NavigableMap<T, Long>) frequencies
					: new ConcurrentSkipListMap<T, Long>( frequencies ) );
		}

		protected SimpleOrdinal( final NavigableMap<T, Long> frequencies )
		{
			super( frequencies );

			// initialize range
			this.range = Range.infinite();

			// initialize median
			this.median = null;

			// initialize cumulative frequencies and median
			this.cumulatives = new ConcurrentSkipListMap<T, Long>();
			long cumulative = 0L;
			for( Entry<T, Long> entry : frequencies.entrySet() )
			{
				cumulative += entry.getValue();
				this.cumulatives.put( entry.getKey(), cumulative );
			}

			if( cumulative > 0L ) updateMedian();

			// FIXME in JRE1.8: Collections.unmodifiableNavigableMap( m )
			this.unmodifiable = frequencies;
			this.cumulativesUnmodifiable = this.cumulatives;
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
		protected void updateCumulatives( final NavigableMap<T, Long> tail,
			final Long delta )
		{
			Long cumulative = cumulativeOf( tail.firstKey() )
					- frequencyOf( tail.firstKey() );
			for( Entry<T, Long> entry : tail.entrySet() )
			{
				cumulative += entry.getValue();
				this.cumulatives.put( entry.getKey(), cumulative );
			}

			final T last = getFrequencies().lastKey();
			if( cumulativeOf( last ) != getSumFrequency() )
				throw ExceptionFactory.createUnchecked(
						"Cumulative frequency {} of last entry {} != sum {}",
						cumulativeOf( last ), last, getSumFrequency() );
		}

		protected void updateMedian()
		{
			final Long semi = getSumFrequency() / 2;
			T median = getCumulatives().firstKey();
			for( Entry<T, Long> entry : getCumulatives().entrySet() )
			{
				if( entry.getValue() <= semi )
					median = entry.getKey();
				else //if(hasMiddle)
					break;
			}
			this.median = median;
		}

		@Override
		protected synchronized void put( final T phenomenon,
			final Long frequency, final Long delta )
		{
			super.put( phenomenon, frequency, delta );

			updateRange( phenomenon );
			updateCumulatives( getFrequencies().tailMap( phenomenon, true ),
					delta );
			updateMedian();
		}

		@Override
		public NavigableMap<T, Long> getFrequencies()
		{
			return this.unmodifiable;
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
		public NavigableMap<T, Long> getCumulatives()
		{
			return this.cumulativesUnmodifiable;
		}

		@Override
		public Long cumulativeOf( final T phenomenon )
		{
			return getCumulatives().get( phenomenon );
		}
	}

	// A,2
	// B,4
	// C,5
	// D,10

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

		private NavigableMap<Amount<Q>, Bin<Q>> bins;

		private Amount<Q> mean;

		@Override
		protected void updateMedian()
		{
			super.updateMedian();
			// TODO interpolate if necessary (when getSumFrequencies()%2==0)
			throw ExceptionFactory.createUnchecked( "not supported");
		}

		protected void updateMean()
		{
			throw ExceptionFactory.createUnchecked( "not supported");
		}

		protected Bin<Q> resolveBin( final Amount<Q> amount )
		{
			return this.bins.floorEntry( amount ).getValue();
		}

		@Override
		public synchronized THIS add( final Amount<Q> amount,
			final Long addendum )
		{
			final Bin<Q> bin = resolveBin( amount );
			updateMean();
			return super.add( bin.getKernel(), addendum );
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

		@Override
		public Amount<Q> getSum()
		{
			// TODO Auto-generated method stub
			return null;
		}

	}
}
