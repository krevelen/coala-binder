/* $Id$
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
 */
package io.coala.random;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;

import io.coala.exception.Thrower;
import io.coala.math.DecimalUtil;
import io.coala.math.FrequencyDistribution;
import io.coala.math.QuantityUtil;
import io.coala.math.Range;
import io.coala.math.WeightedValue;
import io.coala.util.Compare;
import io.coala.util.Instantiator;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * {@link ProbabilityDistribution} is similar to a {@link javax.inject.Provider}
 * but rather a factory that generates values that vary according to some
 * distribution
 * 
 * <p>
 * TODO: provide #toString() as reverse of {@link Parser#parse(String)},
 * #error(), #fitResidual(), #probabilityOf(T t), #cumulativeProbabilityOf(T t),
 * &hellip;
 * 
 * see continuous: https://www.wikiwand.com/en/Probability_density_function and
 * discrete: https://www.wikiwand.com/en/Probability_mass_function
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
@FunctionalInterface
public interface ProbabilityDistribution<T> extends Supplier<T>
{

	/**
	 * @return the next pseudo-random sample
	 */
	T draw();

	default T get()
	{
		return draw();
	}

	/**
	 * From
	 * <a href="https://en.wikipedia.org/wiki/Errors_and_residuals">Wikipedia
	 * </a>: The statistical <b>error</b> (or <b>disturbance</b>) of an observed
	 * value is the deviation of the observed value from the (unobservable) true
	 * value of a quantity of interest (for example, a population mean).
	 */
//	T getError();

	/**
	 * From
	 * <a href="https://en.wikipedia.org/wiki/Errors_and_residuals">Wikipedia
	 * </a>: the <b>residual</b> (or fitting deviation) of an observed value is
	 * the difference between the observed value and the estimated value of the
	 * quantity of interest (for example, a sample mean).
	 */
//	T getResidual();

	/**
	 * @param <T> the type of value
	 * @param value the constant to be returned on each draw
	 * @return a degenerate or deterministic {@link ProbabilityDistribution}
	 */
	static <T> ProbabilityDistribution<T> createDeterministic( final T value )
	{
		return () -> value;
	}

	/**
	 * @param rng the {@link PseudoRandom} number generator
	 * @param probability the probability of drawing {@link Boolean#TRUE}
	 * @return a <a href="">Bernoulli</a> {@link ProbabilityDistribution}
	 */
	static ProbabilityDistribution<Boolean>
		createBernoulli( final PseudoRandom rng, final Number probability )
	{
		return () -> rng.nextDouble() < probability.doubleValue();
	}

	/**
	 * &ldquo;Multi-noulli&rdquo;
	 * 
	 * <img alt="Probability density function" height="150" src=
	 * "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/2D-simplex.svg/440px-2D-simplex.svg.png"/>
	 * 
	 * @param <T> the type of value to draw
	 * @param pmf the {@link WeightedValue} enumeration (i.e. probability mass
	 *            function)
	 * @return a categorical {@link ProbabilityDistribution}
	 * @see <a href="https://www.wikiwand.com/en/Categorical_distribution">
	 *      Wikipedia</a> and <a href=
	 *      "https://www.wolframalpha.com/input/?i=bernoulli+distribution">
	 *      Wolfram &alpha;</a>
	 */
	@SuppressWarnings( "unchecked" )
	static <T, WV extends WeightedValue<T>> ProbabilityDistribution<T>
		createCategorical( final PseudoRandom rng, final Stream<WV> pmf )
	{
		final AtomicReference<BigDecimal> sum = new AtomicReference<>(
				BigDecimal.ZERO );
		final HashMap<T, BigDecimal> map = pmf.map( wv -> WeightedValue
				.of( wv.getValue(), DecimalUtil.valueOf( wv.getWeight() ) ) )
				.filter( wv ->
				{
					final BigDecimal w = (BigDecimal) wv.getWeight();
					if( w.signum() < 0 )
						return Thrower.throwNew( IllegalArgumentException::new,
								() -> "Illegal value weight: " + wv );
					if( w.signum() == 0 ) return false;
					sum.updateAndGet( s -> s.add( w ) );
					return true;
				} )
				.collect( Collectors.toMap( wv -> wv.getValue(),
						wv -> (BigDecimal) wv.getWeight(), BigDecimal::add,
						HashMap::new ) );

		// sanity check
		if( map.isEmpty() ) return Thrower
				.throwNew( IllegalArgumentException::new, () -> "Empty" );
		if( sum.get().signum() < 1 ) return Thrower
				.throwNew( IllegalStateException::new, () -> "Sum: " + sum );

		if( map.size() == 1 )
			return createDeterministic( map.keySet().iterator().next() );

		final Object[] values = new Object[map.size()];
		final double[] wNormCum = new double[map.size()];
		final AtomicInteger index = new AtomicInteger();
		map.forEach( ( v_i, w_i ) ->
		{
			final int i = index.get();
			values[i] = v_i;
			final double w = DecimalUtil.divide( w_i, sum.get() ).doubleValue();
			wNormCum[i] = i == 0 ? w : (w + wNormCum[i - 1]);
			index.incrementAndGet();
		} );
		return () ->
		{
			final double rnd = rng.nextDouble();
			final int i = Arrays.binarySearch( wNormCum, rnd );
			return (T) (i < 0 ? values[-i - 1] : values[i]);
		};
	}

	static <T extends Number> ProbabilityDistribution<BigDecimal>
		createTriangular( final PseudoRandom rng, final T min, final T mode,
			final T max )
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
		return () ->
		{
			final BigDecimal p = BigDecimal.valueOf( rng.nextDouble() );
			return Compare.lt( p, lowerDivTotal )
					? DecimalUtil.add( minBD,
							DecimalUtil.sqrt( p.multiply( lowerTimesTotal ) ) )
					: DecimalUtil.subtract( maxBD,
							DecimalUtil.sqrt( BigDecimal.ONE.subtract( p )
									.multiply( upperTimesTotal ) ) );
		};
	}

	static <T extends Number> ProbabilityDistribution<Double> createEmpirical(
		final PseudoRandom rng, final Stream<? extends Number> observations,
		final int binCount )
	{
		// sanity check
		if( binCount < 1 ) return Thrower
				.throwNew( IllegalArgumentException::new, () -> "n_bins < 1" );
		final TreeMap<BigDecimal, Long> counts = observations
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
				.of( pmf -> createCategorical( rng, pmf ),
						range -> WeightedValue.listOf( range.apply( counts ) )
								.stream() );

		return () -> dist.draw( rng.nextElement( bins ) ).doubleValue();
	}

	static ProbabilityDistribution<Double> createNormal( final PseudoRandom rng,
		final Number mean, final Number stDev )
	{
		return () -> rng.nextGaussian() * stDev.doubleValue()
				+ mean.doubleValue();
	}

	static <T> ProbabilityDistribution<T> createUniformCategorical(
		final PseudoRandom rng, final Stream<T> values )
	{
		final List<T> list = values.collect( Collectors.toList() );
		return () -> rng.nextElement( list );
	}

	static ProbabilityDistribution<Double> createUniformContinuous(
		final PseudoRandom rng, final Number min, final Number max )
	{
		final double range = max.doubleValue() - min.doubleValue();
		if( range <= 0 ) return Thrower.throwNew( IllegalArgumentException::new,
				() -> "range: " + min + " > " + max );
		return () -> min.doubleValue() + rng.nextDouble() * range;
	}

	static ProbabilityDistribution<Long> createUniformDiscrete(
		final PseudoRandom rng, final Number min, final Number max )
	{
		final long lower = min.longValue();
		final long range = max.longValue() - lower;
		if( range <= 0 ) return Thrower.throwNew( IllegalArgumentException::new,
				() -> "range: " + min + " > " + max );
		return () -> lower + rng.nextLong( max.longValue() - lower );
	}

	static void checkUniformRange( final Range<? extends Number> range )
	{
		if( range.lowerInclusive() && range.upperInclusive() ) return;

		Thrower.throwNew( IllegalArgumentException::new,
				() -> "Exclusive/infinite bounds not allowed: " + range );
	}

	/**
	 * @param <T> the type of value
	 * @param callable the {@link Callable} for drawing values
	 * @return a decorator {@link ProbabilityDistribution}
	 */
	static <T> ProbabilityDistribution<T> of( final Callable<T> callable )
	{
		return () ->
		{
			try
			{
				return callable.call();
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
				return null;
			}
		};
	}

	/**
	 * @param <T> the type of value
	 * @param provider the {@link Provider} for drawing values
	 * @return a {@link ProbabilityDistribution}
	 */
//	static <T> ProbabilityDistribution<T> of( final Provider<T> provider )
//	{
//		return () -> provider.get();
//	}

	/**
	 * @param <T> the type of value
	 * @param func the {@link Supplier} for drawing values
	 * @return a {@link ProbabilityDistribution}
	 */
//	static <T> ProbabilityDistribution<T> of( final Supplier<T> func )
//	{
//		return () -> func.get();
//	}

	default <R> ProbabilityDistribution<R> map( final Function<T, R> transform )
	{
		return () -> transform.apply( this.draw() );
	}

	default <R> ProbabilityDistribution<R> ofType( final Class<R> valueType )
	{
		return map( valueType::cast );
	}

	/**
	 * transforms {@link Number#intValue()} draws from this Number
	 * {@link ProbabilityDistribution} in the index range <em>{0, &hellip;,
	 * n-1}</em> of the specified {@link Enum} type's constants
	 * 
	 * @param <E> the type of {@link Enum} value to produce
	 * @param enumType the {@link Class} to resolve
	 * @return a uniform categorical {@link ProbabilityDistribution} of
	 *         {@link E} values
	 */
	default <E extends Enum<E>> ProbabilityDistribution<E>
		toEnum( final Class<E> enumType )
	{
		return map( n -> enumType.getEnumConstants()[((Number) n).intValue()] );
	}

	/**
	 * wraps a {@link ProbabilityDistribution} to construct instances of another
	 * type which has a suitable constructor
	 * 
	 * @param <S> the type of instances to construct
	 * @param valueType the {@link Class} to instantiate
	 * @return a {@link ProbabilityDistribution} of {@link S} values
	 */
	default <S> ProbabilityDistribution<S>
		toInstancesOf( final Class<S> valueType )
	{
		return map( arg -> Instantiator.instantiate( valueType, arg ) );
	}

	/**
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @return a {@link QuantityDistribution} transforming {@link #draw()}
	 *         results into {@link Quantity quantity}, preserving precision
	 */
//	@SuppressWarnings( "unchecked" )
	default QuantityDistribution<Dimensionless> toQuantities()
	{
		return toQuantities( QuantityUtil.PURE );
	}

	/**
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @param unit the {@link Unit} of measurement to assign
	 * @return an {@link QuantityDistribution} for measure {@link Quantity}s
	 *         from drawn {@link Number}s, with an attempt to maintain exactness
	 */
	@SuppressWarnings( "unchecked" )
	default <Q extends Quantity<Q>> QuantityDistribution<Q>
		toQuantities( final Unit<Q> unit )
	{
		if( this instanceof QuantityDistribution )
			return (QuantityDistribution<Q>) this;

		return QuantityDistribution.of( () ->
		{
			final T result = draw();
			if( result == null ) return null;
			if( Quantity.class.isAssignableFrom( result.getClass() ) )
				return ((Quantity<Q>) result).to( unit );
			if( Number.class.isAssignableFrom( result.getClass() ) )
				return (Quantity<Q>) QuantityUtil.valueOf( (Number) result,
						unit );
			return Thrower.throwNew( IllegalArgumentException::new,
					() -> "Can't convert to Quantity: " + result );
		} );
	}

	/**
	 * {@link Factory} generates {@link ProbabilityDistribution}s, a small
	 * subset of those mentioned on
	 * <a href="https://www.wikiwand.com/en/List_of_probability_distributions">
	 * Wikipedia</a>
	 */
	interface Factory
	{

		/**
		 * @return the {@link PseudoRandom} used in creating
		 *         {@link ProbabilityDistribution}s
		 */
		PseudoRandom getStream();

		/**
		 * @param <T> the type of constant drawn
		 * @param value the constant to be returned on each draw
		 * @return a degenerate or deterministic {@link ProbabilityDistribution}
		 */
		default <T> ProbabilityDistribution<T> createDeterministic( T value )
		{
			return ProbabilityDistribution.createDeterministic( value );
		}

		/**
		 * @param p
		 * @return a binomial distribution with n=1
		 */
		default ProbabilityDistribution<Boolean> createBernoulli( Number p )
		{
			return ProbabilityDistribution.createBernoulli( getStream(), p );
		}

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Binomial_distribution_pmf.svg/600px-Binomial_distribution_pmf.svg.png"/>
		 * 
		 * @param trials number of consecutive successes
		 * @param p probability of a single success
		 * @return a binomial {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Binomial_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=binomial+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createBinomial( Number trials, Number p );

		/**
		 * &ldquo;Multi-noulli&rdquo;
		 * 
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/2D-simplex.svg/440px-2D-simplex.svg.png"/>
		 * 
		 * @param <T> the type of value to draw
		 * @param probabilities the {@link WeightedValue} enumeration (i.e.
		 *            probability mass function)
		 * @return a categorical {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Categorical_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=bernoulli+distribution">
		 *      Wolfram &alpha;</a>
		 */
		default <T, WV extends WeightedValue<T>> ProbabilityDistribution<T>
			createCategorical( final Stream<WV> probabilities )
		{
			return ProbabilityDistribution.createCategorical( getStream(),
					probabilities );
		}

		default <T, WV extends WeightedValue<T>> ProbabilityDistribution<T>
			createCategorical( final Iterable<WV> probabilities )
		{
			return createCategorical( StreamSupport
					.stream( probabilities.spliterator(), false ) );
		}

		default <T, WV extends WeightedValue<T>>
			Single<ProbabilityDistribution<T>>
			createCategorical( final Observable<WV> probabilities )
		{
			return Single.<ProbabilityDistribution<T>>fromCallable(
					() -> createCategorical(
							probabilities.blockingIterable() ) );
		}

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Geometric_pmf.svg/900px-Geometric_pmf.svg.png"/>
		 * 
		 * @param p <em>&isin; (0, 1]</em> probability of success
		 * @return a (non-shifted) geometric {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Geometric_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=geometric+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createGeometric( Number p );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/en/2/22/FishersNoncentralHypergeometric1.png"/>
		 * 
		 * @param populationSize <em>N</em>
		 * @param numberOfSuccesses <em>m</em>
		 * @param sampleSize <em>n</em> (number of trials or draws)
		 * @return a hypergeometric {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Hypergeometric_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=Hypergeometric+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createHypergeometric(
			Number populationSize, Number numberOfSuccesses,
			Number sampleSize );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/8/83/Negbinomial.gif"/>
		 * 
		 * @param r (positive) number of successes
		 * @param p <em>&isin; (0, 1)</em> probability of success
		 * @return a Pascal (discrete negative binomial)
		 *         {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Negative_binomial_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=pascal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createPascal( Number r, Number p );

		/**
		 * (Poisson limit theorem: poisson(m) ~ binomial(m, n->inf)) describes:
		 * event rate (frequency) = hazard (Cox) = incidence (epidemiology) =
		 * how often? <br/>
		 * <img alt= "Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Poisson_pmf.svg/650px-Poisson_pmf.svg.png"/>
		 * 
		 * @param mean &mu; or &lambda;
		 * @return a Poisson {@link ProbabilityDistribution}
		 * @see <a href="http://stats.stackexchange.com/a/2094">mathematical
		 *      relation with Exponential (= how soon?)</a>
		 * @see #createExponential(Number)
		 * @see <a href="https://www.wikiwand.com/en/Poisson_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=poisson+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createPoisson( Number mean );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Zipf_distribution_PMF.png/650px-Zipf_distribution_PMF.png"/>
		 * 
		 * @param numberOfElements <em>N</em> (positive)
		 * @param exponent <em>s</em> (positive)
		 * @return a Zipf's power law {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Zipf%27s_law">Wikipedia</a>
		 *      and <a href=
		 *      "https://www.wolframalpha.com/input/?i=zipf+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createZipf( Number numberOfElements,
			Number exponent );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Beta_distribution_pdf.svg/650px-Beta_distribution_pdf.svg.png"/>
		 * 
		 * @param alpha shape (positive)
		 * @param beta shape (positive)
		 * @return a &beta; {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Beta_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=beta+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createBeta( Number alpha, Number beta );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Cauchy_pdf.svg/600px-Cauchy_pdf.svg.png"/>
		 * 
		 * @param median the location <em>a</em>
		 * @param scale <em>b</em> (positive)
		 * @return a Lorentz or Cauchy {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Cauchy_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=cauchy+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createCauchy( Number median,
			Number scale );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/3/35/Chi-square_pdf.svg/642px-Chi-square_pdf.svg.png"/>
		 * 
		 * @param degreesOfFreedom <em>k</em>
		 * @return a &chi;<sup>2</sup> {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Chi-squared_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=chi+squared+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double>
			createChiSquared( Number degreesOfFreedom );

		/**
		 * Exponential distribution (specific case of
		 * {@link #createGamma(Number, Number) Gamma}) describes the time
		 * between i.i.d. events given their mean rate or survival time = how
		 * soon?<br/>
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Exponential_pdf.svg/650px-Exponential_pdf.svg.png"/>
		 * 
		 * @param rate &lambda; = 1/&beta; (rate = inverse survival)
		 * @return a (negative) exponential {@link ProbabilityDistribution}
		 * @see #createPoisson(Number) @ see
		 *      <a href="http://stats.stackexchange.com/a/2094">relation with
		 *      Poisson</a>
		 * @see <a href="https://www.wikiwand.com/en/Exponential_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=exponential+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createExponential( Number mean );

		/**
		 * @param observations the <em>n</em> empirical observations
		 * @param binCount the number of observations per aggregate bin
		 * @return an empirical {@link ProbabilityDistribution} of <em>n/10</em>
		 *         bins without underlying probability mass function assumptions
		 */
		default <T extends Number> ProbabilityDistribution<Double>
			createEmpirical( final Stream<? extends Number> observations,
				final int binCount )
		{
			return ProbabilityDistribution.createEmpirical( getStream(),
					observations, binCount );
		}

		default <T extends Number> ProbabilityDistribution<Double>
			createEmpirical( final Collection<? extends Number> observations )
		{
			final int binCount = Math.max( 1,
					observations.size() / VALUES_PER_BIN );
			return createEmpirical( observations, binCount );
		}

		@SuppressWarnings( "unchecked" )
		default <T extends Number> ProbabilityDistribution<Double>
			createEmpirical( final Iterable<T> observations,
				final int binCount )
		{
			return createEmpirical(
					StreamSupport.stream( observations.spliterator(), false ),
					binCount );
		}

		@SuppressWarnings( "unchecked" )
		default <T extends Number> ProbabilityDistribution<Double>
			createEmpirical( final T... observations )
		{
			final int binCount = Math.max( 1,
					observations.length / VALUES_PER_BIN );
			return createEmpirical( Arrays.stream( observations ), binCount );
		}

		@SuppressWarnings( "unchecked" )
		default <T extends Number> ProbabilityDistribution<Double>
			createEmpirical( final int binCount, final T... observations )
		{
			return createEmpirical( Arrays.stream( observations ), binCount );
		}

		default <T extends Number> Single<ProbabilityDistribution<Double>>
			createEmpirical( final Observable<T> observations )
		{
			return createEmpirical( observations, VALUES_PER_BIN );
		}

		int VALUES_PER_BIN = 10;

		default <T extends Number> Single<ProbabilityDistribution<Double>>
			createEmpirical( final Observable<T> observations,
				final int valuesPerBin )
		{
			return Single.create( sub ->
			{
				try
				{
					final List<T> values = observations.toList().blockingGet();
					final int binCount = Math.max( 1,
							values.size() / valuesPerBin );
					sub.onSuccess( createEmpirical( values, binCount ) );
				} catch( final Exception e )
				{
					sub.onError( e );
				}
			} );
		}

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/F_pdf.svg/650px-F_pdf.svg.png"/>
		 * 
		 * @param numeratorDegreesOfFreedom <em>n</em>
		 * @param denominatorDegreesOfFreedom <em>m</em>
		 * @return an F-{@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/F-distribution">Wikipedia
		 *      </a> and
		 *      <a href="https://www.wolframalpha.com/input/?i=f+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createF(
			Number numeratorDegreesOfFreedom,
			Number denominatorDegreesOfFreedom );

		/**
		 * The Gamma distribution family also includes
		 * <a href="https://www.wikiwand.com/en/Erlang_distribution">Erlang
		 * distributions</a> (where shape <em>k</em> is an integer)
		 * <img alt= "Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Gamma_distribution_pdf.svg/650px-Gamma_distribution_pdf.svg.png"/>
		 * 
		 * @param shape &alpha; = <em>k</em>
		 * @param scale &theta; or 1/&beta; (scale = inverse rate)
		 * @return a &Gamma; (gamma) {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Gamma_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=gamma+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createGamma( Number shape,
			Number scale );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Levy0_distributionPDF.svg/650px-Levy0_distributionPDF.svg.png"/>
		 * 
		 * @param mu
		 * @param c
		 * @return a L&eacute;vy {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/L%C3%A9vy_distribution">
		 *      Wikipedia</a> and <a
		 *      href=""https://www.wolframalpha.com/input/?i=levy+distribution>
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createLevy( Number mu, Number c );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/PDF-log_normal_distributions.svg/600px-PDF-log_normal_distributions.svg.png"/>
		 * 
		 * @param scale
		 * @param shape
		 * @return a log-normal {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Log-normal_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=lognormal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createLogNormal( Number scale,
			Number shape );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Normal_Distribution_PDF.svg/700px-Normal_Distribution_PDF.svg.png"/>
		 * 
		 * @param mean &mu;
		 * @param stDev &sigma; > 0 (standard deviation)
		 * @return a Gaussian or Normal {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Normal_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=normal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		default ProbabilityDistribution<Double> createNormal( Number mean,
			Number stDev )
		{
			return ProbabilityDistribution.createNormal( getStream(), mean,
					stDev );
		}

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/5/57/Multivariate_Gaussian.png/600px-Multivariate_Gaussian.png"/>
		 * 
		 * @param means
		 * @param covariances
		 * @return a Multivariate Normal {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Multivariate_normal_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=multivariate+normal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<double[]> createMultinormal( double[] means,
			double[][] covariances );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/Probability_density_function_of_Pareto_distribution.svg/650px-Probability_density_function_of_Pareto_distribution.svg.png"/>
		 * 
		 * @param scale
		 * @param shape
		 * @return a Pareto {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Pareto_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=pareto+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createPareto( Number scale,
			Number shape );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Student_t_pdf.svg/650px-Student_t_pdf.svg.png"/>
		 * 
		 * @param degreesOfFreedom <em>v</em>
		 * @return a Pearson type VII or Student's T
		 *         {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Student%27s_t-distribution">
		 *      Wikipedia</a> and
		 *      <a href="https://www.wolframalpha.com/input/?i=t+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createT( Number degreesOfFreedom );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/4/45/Triangular_distribution_PMF.png/650px-Triangular_distribution_PMF.png"/>
		 * 
		 * @param min
		 * @param max
		 * @param mode
		 * @return a triangular {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Triangular_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=triangular+distribution">
		 *      Wolfram &alpha;</a>
		 */
		default ProbabilityDistribution<Double> createTriangular(
			final Number min, final Number mode, final Number max )
		{
			return ProbabilityDistribution
					.createTriangular( getStream(), min, mode, max )
					.map( Number::doubleValue );
		}

		default ProbabilityDistribution<BigDecimal> createTriangular(
			final BigDecimal min, final BigDecimal mode, final BigDecimal max )
		{
			return ProbabilityDistribution.createTriangular( getStream(), min,
					mode, max );
		}

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1f/Uniform_discrete_pmf_svg.svg/650px-Uniform_discrete_pmf_svg.svg.png"/>
		 * 
		 * @param min <em>a</em>
		 * @param max <em>b</em>
		 * @return a uniform discrete/integer {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Uniform_distribution_(discrete)">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=discrete+uniform+distribution">
		 *      Wolfram &alpha;</a>
		 */
		default ProbabilityDistribution<Long> createUniformDiscrete( Number min,
			Number max )
		{
			return ProbabilityDistribution.createUniformDiscrete( getStream(),
					min, max );
		}

		default ProbabilityDistribution<Long>
			createUniformDiscrete( final Range<? extends Number> range )
		{
			checkUniformRange( range );
			return createUniformDiscrete( range.lowerValue(),
					range.upperValue() );
		}

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Uniform_Distribution_PDF_SVG.svg/500px-Uniform_Distribution_PDF_SVG.svg.png"/>
		 * 
		 * @param min
		 * @param max
		 * @return a uniform continuous/real {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Uniform_distribution_(continuous)">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=uniform+distribution">
		 *      Wolfram &alpha;</a>
		 */
		default ProbabilityDistribution<Double>
			createUniformContinuous( Number min, Number max )
		{
			return ProbabilityDistribution.createUniformContinuous( getStream(),
					min, max );
		}

		default ProbabilityDistribution<Double>
			createUniformContinuous( final Range<? extends Number> range )
		{
			checkUniformRange( range );
			return createUniformContinuous( range.lowerValue(),
					range.upperValue() );
		}

		/**
		 * wraps a {@link #createUniformDiscrete(Number, Number)} over the full
		 * index range <em>{0, &hellip;, n-1}</em> of specified value
		 * enumeration
		 * 
		 * @param <T> the type of value to draw
		 * @param values the value enumeration
		 * @return a uniform categorical {@link ProbabilityDistribution}
		 * @see <a href= "https://www.wikiwand.com/en/Categorical_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=discrete+uniform+distribution">
		 *      Wolfram &alpha;</a>
		 */
		default <T> Single<ProbabilityDistribution<T>>
			createUniformCategorical( Observable<T> values )
		{
			return Single.create( sub ->
			{
				try
				{
					sub.onSuccess( createUniformCategorical(
							values.blockingIterable() ) );
				} catch( final Exception e )
				{
					sub.onError( e );
				}
			} );
		}

		@SuppressWarnings( "unchecked" )
		default <T> ProbabilityDistribution<T>
			createUniformCategorical( final Iterable<T> values )
		{
			return createUniformCategorical(
					StreamSupport.stream( values.spliterator(), false ) );
		}

		@SuppressWarnings( "unchecked" )
		default <T> ProbabilityDistribution<T>
			createUniformCategorical( final Stream<T> values )
		{
			return ProbabilityDistribution
					.createUniformCategorical( getStream(), values );
		}

		@SuppressWarnings( "unchecked" )
		default <T> ProbabilityDistribution<T>
			createUniformCategorical( final T... values )
		{
			return createUniformCategorical( Arrays.stream( values ) );
		}

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/Weibull_PDF.svg/650px-Weibull_PDF.svg.png"/>
		 * 
		 * @param alpha shape &alpha; (positive)
		 * @param beta scale &beta; (positive)
		 * @return a Weibull {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Weibull_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=weibull+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createWeibull( Number alpha,
			Number beta );
	}

	interface Parser
	{

		/** @return a {@link Factory} of {@link ProbabilityDistribution}s */
		Factory getFactory();

		/**
		 * @param <T> the type of values to draw
		 * @param dist the {@link String} representation
		 * @return a {@link ProbabilityDistribution} of {@link T} values
		 * @throws ParseException
		 */
		default <T> ProbabilityDistribution<T> parse( final String dist )
			throws ParseException
		{
			return parse( dist, BigDecimal.class );
		}

		@SuppressWarnings( "unchecked" )
		default QuantityDistribution<?> parseQuantity( final String dist )
			throws ParseException
		{
			return parseQuantity( dist, Quantity.class );
		}

//		@SuppressWarnings( "unchecked" )
		default <Q extends Quantity<Q>> QuantityDistribution<Q> parseQuantity(
			final String dist, final Class<Q> qty ) throws ParseException
		{
			return QuantityDistribution.of( parse( dist, Quantity.class )
					.map( q -> ((Quantity<?>) q).asType( qty ) ) );
		}

//		@SuppressWarnings( "unchecked" )
		default <Q extends Quantity<Q>> QuantityDistribution<Q> parseQuantity(
			final String dist, final Unit<Q> unit ) throws ParseException
		{
			return parse( dist, Quantity.class ).toQuantities( unit );
		}

		/**
		 * @param <T> the type of values to draw
		 * @param <P> the type of parameter to parse
		 * @param dist the {@link String} representation
		 * @param argType the concrete argument {@link Class}
		 * @return a {@link ProbabilityDistribution} of {@link T} values
		 * @throws ParseException
		 */
		<T, P> ProbabilityDistribution<T> parse( String dist, Class<P> argType )
			throws ParseException;

		/**
		 * @param <T> the type of value in the {@link ProbabilityDistribution}
		 * @param <V> the type of parameters
		 * @param name the symbol of the {@link ProbabilityDistribution}
		 * @param args the arguments as a {@link List} of {@link WeightedValue}
		 *            pairs with at least a parameter of type {@link T} and
		 *            possibly some numeric weight (e.g. in categorical
		 *            distributions)
		 * @return a {@link ProbabilityDistribution}
		 * @throws ParseException
		 */
//		@SuppressWarnings( "unchecked" )
		<T, V> ProbabilityDistribution<T> parse( String label,
			List<WeightedValue<V>> args ) throws ParseException;
	}

	interface Fitter
	{

		Factory getFactory();

		<Q extends Quantity<Q>> QuantityDistribution<Q> fitNormal(
			FrequencyDistribution.Interval<Q, ?> values, Unit<Q> unit );

		// wavelets, e.g.https://github.com/cscheiblich/JWave  => pdf?

//		signal / harmonic( Number initialAmplitude,
//			Number initialAngularFrequency, Number initialPhase ) => pdf?

//		polynomial regression ( Number... initialCoefficients ) => pdf?

	}
}