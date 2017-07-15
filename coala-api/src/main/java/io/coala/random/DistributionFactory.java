/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.random;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import io.coala.exception.Thrower;
import io.coala.math.DecimalUtil;
import io.coala.math.Range;
import io.coala.math.WeightedValue;
import io.coala.util.Compare;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * The basic {@link DistributionFactory} only supports deterministic,
 * categorical, uniform, empirical, triangular, bernoulli and gaussian/normal
 * distributions
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class DistributionFactory implements ProbabilityDistribution.Factory
{

	private static DistributionFactory INSTANCE = null;

	/**
	 * @return a basic {@link DistributionFactory} that only supports
	 *         deterministic, categorical, uniform, empirical, triangular,
	 *         bernoulli and gaussian/normal distributions
	 */
	public static DistributionFactory instance()
	{
		return INSTANCE != null ? INSTANCE = null
				: (INSTANCE = new DistributionFactory());
	}

	private final PseudoRandom stream;

	public DistributionFactory()
	{
		this( PseudoRandom.JavaRandom.Factory.instance()
				.create( PseudoRandom.Config.NAME_DEFAULT ) );
	}

	public DistributionFactory( final PseudoRandom stream )
	{
		this.stream = stream;
	}

	@Override
	public PseudoRandom getStream()
	{
		return this.stream;
	}

	@Override
	public <T> ProbabilityDistribution<T> createDeterministic( final T value )
	{
		return ProbabilityDistribution.createDeterministic( value );
	}

	@Override
	public ProbabilityDistribution<Boolean> createBernoulli( final Number p )
	{
		return ProbabilityDistribution.createBernoulli( getStream(), p );
	}

	@Override
	public ProbabilityDistribution<Long> createBinomial( final Number trials,
		final Number p )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "binomial" );
	}

	@Override
	public <T, WV extends WeightedValue<T>> Single<ProbabilityDistribution<T>>
		createCategorical( final Observable<WV> probabilities )
	{
		return Single.<ProbabilityDistribution<T>>fromCallable( () ->
		{
			// async iteration
			final Map<T, BigDecimal> map = probabilities
					.toMap( wv -> wv.getValue(),
							wv -> DecimalUtil.valueOf( wv.getWeight() ),
							TreeMap::new )
					.blockingGet();
			// sanity check
			if( map.isEmpty() ) return Thrower
					.throwNew( IllegalArgumentException::new, () -> "empty" );

			// sync iteration
			final BigDecimal total = map.values().parallelStream()
					.reduce( BigDecimal::add ).orElse( BigDecimal.ZERO );
			return () ->
			{
				final BigDecimal sum_n = DecimalUtil.multiply( total,
						getStream().nextBigDecimal() );
				BigDecimal sum_i = BigDecimal.ZERO;
				for( Map.Entry<T, BigDecimal> wv : map.entrySet() )
					if( Compare.ge( sum_i = sum_i.add( wv.getValue() ),
							sum_n ) )
						return wv.getKey();
				return null;
			};
		} );
	}

	@Override
	public ProbabilityDistribution<Long> createGeometric( final Number p )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "geometric" );
	}

	@Override
	public ProbabilityDistribution<Long> createHypergeometric(
		final Number populationSize, final Number numberOfSuccesses,
		final Number sampleSize )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "hypergeometric" );
	}

	@Override
	public ProbabilityDistribution<Long> createPascal( final Number r,
		final Number p )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "pascal" );
	}

	@Override
	public ProbabilityDistribution<Long> createPoisson( final Number mean )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "poisson" );
	}

	@Override
	public ProbabilityDistribution<Long>
		createZipf( final Number numberOfElements, final Number exponent )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "zipf" );
	}

	@Override
	public ProbabilityDistribution<Double> createBeta( final Number alpha,
		final Number beta )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "beta" );
	}

	@Override
	public ProbabilityDistribution<Double> createCauchy( final Number median,
		final Number scale )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "cauchy" );
	}

	@Override
	public ProbabilityDistribution<Double>
		createChiSquared( final Number degreesOfFreedom )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "chi-squared" );
	}

	@Override
	public ProbabilityDistribution<Double>
		createExponential( final Number mean )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "exponential" );
	}

	@Override
	public <T extends Number> ProbabilityDistribution<Double> createEmpirical(
		final Iterable<? extends Number> observations, final int binCount )
	{
		// sanity check
		if( binCount < 1 ) return Thrower
				.throwNew( IllegalArgumentException::new, () -> "n_bins < 1" );
		final TreeMap<BigDecimal, Long> counts = StreamSupport
				.stream( Objects.requireNonNull( observations ).spliterator(),
						true )
				.collect( Collectors.groupingBy( DecimalUtil::valueOf,
						TreeMap::new, Collectors.counting() ) );
		if( counts.size() < binCount )
			return Thrower.throwNew( IllegalArgumentException::new,
					() -> "|n| < n_bins" );

		final BigDecimal binSize = DecimalUtil.divide(
				counts.lastKey().subtract( counts.firstKey() ), binCount );
		final List<Range<BigDecimal>> bins = IntStream
				.range( 0,
						binCount - 1 )
				.mapToObj( i -> Range.of(
						counts.firstKey()
								.add( DecimalUtil.multiply( binSize, i ) ),
						true,
						counts.firstKey()
								.add( DecimalUtil.multiply( binSize, i + 1 ) ),
						i == binCount - 1 ) )
				.collect( Collectors.toList() );

		// FIXME use Gaussians per bin, i.e. createNormal( bin_mean, bin_stdev )
		final ConditionalDistribution<BigDecimal, Range<BigDecimal>> dist = ConditionalDistribution
				.of( this::createCategorical, range -> WeightedValue
						.listOf( range.apply( counts ) ) );

		return () -> dist.draw( getStream().nextElement( bins ) ).doubleValue();
	}

	@Override
	public ProbabilityDistribution<Double> createF(
		final Number numeratorDegreesOfFreedom,
		final Number denominatorDegreesOfFreedom )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "f" );
	}

	@Override
	public ProbabilityDistribution<Double> createGamma( final Number shape,
		final Number scale )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "gamma" );
	}

	@Override
	public ProbabilityDistribution<Double> createLevy( final Number mu,
		final Number c )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "levy" );
	}

	@Override
	public ProbabilityDistribution<Double> createLogNormal( final Number scale,
		final Number shape )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "log-normal" );
	}

	@Override
	public ProbabilityDistribution<Double> createNormal( final Number mean,
		final Number stDev )
	{
		return () -> getStream().nextGaussian() * stDev.doubleValue()
				+ mean.doubleValue();
	}

	@Override
	public ProbabilityDistribution<double[]>
		createMultinormal( final double[] means, final double[][] covariances )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "multinormal" );
	}

	@Override
	public ProbabilityDistribution<Double> createPareto( final Number scale,
		final Number shape )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "pareto" );
	}

	@Override
	public ProbabilityDistribution<Double>
		createT( final Number degreesOfFreedom )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "student-t" );
	}

	public <T extends Number> ProbabilityDistribution<BigDecimal>
		createTriangular( final Supplier<T> supplier, final T min, final T max,
			final T mode )
	{
		final BigDecimal modeBD = DecimalUtil.valueOf( mode ),
				minBD = DecimalUtil.valueOf( min ),
				maxBD = DecimalUtil.valueOf( max ),
				lower = modeBD.subtract( minBD ),
				upper = maxBD.subtract( modeBD ),
				total = maxBD.subtract( minBD ),
				lowerDivTotal = DecimalUtil.divide( lower, total ),
				lowerTimesTotal = DecimalUtil.multiply( lower, total ),
				upperTimesTotal = DecimalUtil.multiply( upper, total );
		return ProbabilityDistribution
				.of( supplier::get ).map(
						DecimalUtil::valueOf )
				.map( p -> Compare.lt( p, lowerDivTotal )
						? DecimalUtil.add( minBD,
								DecimalUtil
										.sqrt( p.multiply( lowerTimesTotal ) ) )
						: DecimalUtil.subtract( maxBD,
								DecimalUtil.sqrt( BigDecimal.ONE.subtract( p )
										.multiply( upperTimesTotal ) ) ) );
	}

	@Override
	public ProbabilityDistribution<Double> createTriangular( final Number min,
		final Number max, final Number mode )
	{
		return createTriangular( getStream()::nextDouble, min, max, mode )
				.map( DecimalUtil::doubleValue );
	}

	@Override
	public ProbabilityDistribution<Long>
		createUniformDiscrete( final Number min, final Number max )
	{
		final long lower = min.longValue();
		return () -> lower + getStream().nextLong( max.longValue() - lower );
	}

	@Override
	public ProbabilityDistribution<Double>
		createUniformContinuous( final Number min, final Number max )
	{
		return () -> min.doubleValue() + getStream().nextDouble()
				* (max.doubleValue() - min.doubleValue());
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> Single<ProbabilityDistribution<T>>
		createUniformCategorical( final Observable<T> values )
	{
		return Single.<ProbabilityDistribution<T>>fromCallable( () ->
		{
			final List<T> list = values.toList().blockingGet();
			return () -> getStream().nextElement( list );
		} );
	}

	@Override
	public ProbabilityDistribution<Double> createWeibull( final Number alpha,
		final Number beta )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "weibull" );
	}
}