/* $Id: a7842c5dc1c8963fe6c9721cdcda6c3b21980bb0 $
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Provider;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.aeonbits.owner.Config;
import org.jscience.physics.amount.Amount;

import io.coala.exception.Thrower;
import io.coala.math.FrequencyDistribution;
import io.coala.math.MeasureUtil;
import io.coala.math.WeightedValue;
import io.coala.util.Instantiator;
import rx.functions.Func0;

/**
 * {@link ProbabilityDistribution} is similar to a {@link javax.inject.Provider}
 * but rather a factory that generates values that vary according to some
 * distribution
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface ProbabilityDistribution<T> //extends Serializable
{

	/**
	 * @return the next pseudo-random sample
	 */
	T draw();

	// continuous: https://www.wikiwand.com/en/Probability_density_function
	// discrete: https://www.wikiwand.com/en/Probability_mass_function
	// TODO probabilityOf(T value), cumulativeProbabilityOf(T value)

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
		return () ->
		{
			return value;
		};
	}

	static ProbabilityDistribution<Boolean>
		createBernoulli( final PseudoRandom rng, final Number probability )
	{
		final double p = probability.doubleValue();
		return () ->
		{
			return rng.nextDouble() < p;
		};
	}

	/**
	 * @param <T> the type of value
	 * @param callable the {@link Callable} providing values, for JRE8
	 * @return a decorator {@link ProbabilityDistribution}
	 * @see https://github.com/orfjackal/retrolambda
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
	 * @param provider the {@link Provider} providing values
	 * @return a decorator {@link ProbabilityDistribution}
	 */
	static <T> ProbabilityDistribution<T> of( final Provider<T> provider )
	{
		return () ->
		{
			return provider.get();
		};
	}

	/**
	 * @param <T> the type of value
	 * @param func the {@link Func0} providing values, for rxJava
	 * @return a decorator {@link ProbabilityDistribution}
	 */
	static <T> ProbabilityDistribution<T> of( final Supplier<T> func )
	{
		return () ->
		{
			return func.get();
		};
	}

	default <R> ProbabilityDistribution<R>
		apply( final Function<T, R> transform )
	{
		return () ->
		{
			return transform.apply( this.draw() );
		};
	}

	/**
	 * transforms {@link int} draws from this {@link ProbabilityDistribution} in
	 * the index range <em>{0, &hellip;, n-1}</em> of the specified {@link Enum}
	 * type's constants
	 * 
	 * @param <E> the type of {@link Enum} value to produce
	 * @param enumType the {@link Class} to resolve
	 * @return a uniform categorical {@link ProbabilityDistribution} of
	 *         {@link E} values
	 */
	default <E extends Enum<E>> ProbabilityDistribution<E>
		toEnum( final Class<E> enumType )
	{
		return apply( n ->
		{
			return enumType.getEnumConstants()[((Number) n).intValue()];
		} );
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
		return apply( arg ->
		{
			return Instantiator.instantiate( valueType, arg );
		} );
	}

	/**
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @return an {@link AmountDistribution} {@link ProbabilityDistribution} for
	 *         measure {@link Amount}s from drawn {@link Number}s, with an
	 *         attempt to maintain exactness
	 */
	@SuppressWarnings( "unchecked" )
	default <Q extends Quantity> AmountDistribution<Q> toAmounts()
	{
		return (AmountDistribution<Q>) toAmounts( Unit.ONE );
	}

	/**
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @param unit the {@link Unit} of measurement to assign
	 * @return an {@link AmountDistribution} for measure {@link Amount}s from
	 *         drawn {@link Number}s, with an attempt to maintain exactness
	 */
	@SuppressWarnings( "unchecked" )
	default <Q extends Quantity> AmountDistribution<Q>
		toAmounts( final Unit<Q> unit )
	{
		if( this instanceof AmountDistribution )
			return (AmountDistribution<Q>) this;

//		final List<Class<?>> args = TypeArguments
//				.of( ProbabilityDistribution.class, getClass() );
//		if( !args.isEmpty() && args.get( 0 ) != null /* no runtime type yet */ )
//		{
//			if( Amount.class.isAssignableFrom( args.get( 0 ) ) )
//				return AmountDistribution
//						.of( (ProbabilityDistribution<Amount<Q>>) this );
//			if( !Number.class.isAssignableFrom( args.get( 0 ) ) )
//				return Thrower.throwNew( UnsupportedOperationException.class,
//						"No default conversion from {} to Amounts",
//						args.get( 0 ) );
//		}
		return AmountDistribution.of( () ->
		{
			final T result = draw();
			if( !Amount.class.isAssignableFrom( result.getClass() ) )
				return (Amount<Q>) MeasureUtil.toAmount( (Number) draw(),
						unit );
			return (Amount<Q>) result;
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
		<T> ProbabilityDistribution<T> createDeterministic( T value );

		/**
		 * @param p
		 * @return a binomial distribution with n=1
		 */
		ProbabilityDistribution<Boolean> createBernoulli( Number p );

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
		<T, WV extends WeightedValue<T>> ProbabilityDistribution<T>
			createCategorical( Iterable<WV> probabilities );

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
		 * Typically used for: event rate = hazard (Cox) = incidence
		 * (epidemiology) = how frequent? <br/>
		 * <img alt= "Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Poisson_pmf.svg/650px-Poisson_pmf.svg.png"/>
		 * 
		 * @param mean &mu; or &lambda;
		 * @return a Poisson {@link ProbabilityDistribution}
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
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Exponential_pdf.svg/650px-Exponential_pdf.svg.png"/>
		 * 
		 * @param mean &lambda;
		 * @return a (negative) exponential {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Exponential_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=exponential+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createExponential( Number mean );

		/**
		 * @param values the <em>n</em> empirical values
		 * @return an empirical {@link ProbabilityDistribution} of <em>n/10</em>
		 *         bins without underlying probability mass function assumptions
		 */
		@SuppressWarnings( "unchecked" )
		<T extends Number> ProbabilityDistribution<Double>
			createEmpirical( T... values );

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
		 * distributions</a> (where shape <em>k</em> is an integer) <img alt=
		 * "Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Gamma_distribution_pdf.svg/650px-Gamma_distribution_pdf.svg.png"/>
		 * 
		 * @param shape &alpha; or <em>k</em>
		 * @param scale &beta; or &theta;
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
		ProbabilityDistribution<Double> createNormal( Number mean,
			Number stDev );

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
		 * @param a <em>min</em>
		 * @param b <em>peak</em>
		 * @param c <em>max</em>
		 * @return a triangular {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Triangular_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=triangular+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createTriangular( Number a, Number b,
			Number c );

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
		ProbabilityDistribution<Long> createUniformDiscrete( Number min,
			Number max );

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
		ProbabilityDistribution<Double> createUniformContinuous( Number min,
			Number max );

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
		@SuppressWarnings( "unchecked" )
		<T> ProbabilityDistribution<T> createUniformCategorical( T... values );

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
		@SuppressWarnings( "unchecked" )
		<T, V> ProbabilityDistribution<T> parse( String label,
			List<WeightedValue<V>> args ) throws ParseException;
	}

	interface Fitter
	{

		Factory getFactory();

		<Q extends Quantity> AmountDistribution<Q> fitNormal(
			FrequencyDistribution.Interval<Q, ?> values, Unit<Q> unit );

		// wavelets, e.g.https://github.com/cscheiblich/JWave  => pdf?

//		signal / harmonic( Number initialAmplitude,
//			Number initialAngularFrequency, Number initialPhase ) => pdf?

//		polynomial regression ( Number... initialCoefficients ) => pdf?

	}

	/**
	 * @param encloser
	 * @param field
	 * @param binder
	 */
	static void injectDistribution( final Object encloser, final Field field,
		final Supplier<Parser> parser )
	{
		if( !ProbabilityDistribution.class.isAssignableFrom( field.getType() ) )
			Thrower.throwNew( UnsupportedOperationException.class,
					"@{} only injects extensions of {}",
					InjectDist.class.getSimpleName(), Config.class );
		field.setAccessible( true );
		final InjectDist annot = field.getAnnotation( InjectDist.class );
		try
		{
			final ProbabilityDistribution<?> parsedDist = parser.get()
					.parse( annot.value(), annot.paramType() );
			if( field.getType().isAssignableFrom( parsedDist.getClass() ) )
			{
				field.set( encloser, parsedDist );
				return;
			} else if( AmountDistribution.class
					.isAssignableFrom( field.getType() ) )
			{
				final Unit<?> unit = annot.unit().isEmpty() ? Unit.ONE
						: Unit.valueOf( annot.unit() );
//				final Class<?> fieldDim = TypeArguments
//						.of( AmountDistribution.class, field.getType()
//								.asSubclass( AmountDistribution.class ) )
//						.get( 0 );
//				final Class<?> parsedDim = TypeArguments.of( Unit.class, unit
//						.getStandardUnit().getClass().asSubclass( Unit.class ) )
//						.get( 0 );
//
//				LogUtil.getLogger( ProbabilityDistribution.class ).trace(
//						"Convert amounts from parsed {} to injected {}",
//						parsedDim, fieldDim );
//				if( fieldDim == parsedDim )

				// FIXME injects raw, check unit compatibility in helper method?
				field.set( encloser, parsedDist.toAmounts( unit ) );

//				else
//					Thrower.throwNew( UnsupportedOperationException.class,
//							"Can't convert amounts from parsed {} to injected {}",
//							parsedDim.getTypeName(), fieldDim.getTypeName() );
			} else
				Thrower.throwNew( UnsupportedOperationException.class,
						"Can't convert values from parsed {} to injected {}",
						parsedDist.getClass().getTypeName(),
						field.getType().getTypeName() );
//			final Class<?> fieldValueType = TypeArguments
//					.of( ProbabilityDistribution.class, field.getType()
//							.asSubclass( ProbabilityDistribution.class ) )
//					.get( 0 );
//			final Class<?> parsedValueType = TypeArguments
//					.of( ProbabilityDistribution.class, parsedDist.getClass() )
//					.get( 0 );
//			if( fieldValueType == null
//					|| fieldValueType.isAssignableFrom( parsedValueType ) )
//				field.set( encloser, parsedDist );
//			else if( Amount.class.isAssignableFrom( fieldValueType )
//					&& Number.class.isAssignableFrom( parsedValueType ) )
//			{
//				final Unit<?> unit = annot.unit().isEmpty() ? Unit.ONE
//						: Unit.valueOf( annot.unit() );
//				field.set( encloser, parsedDist.toAmounts( unit ) );
//			} else
//				Thrower.throwNew( UnsupportedOperationException.class,
//						"Can't convert values from parsed {} to injected {}",
//						InjectDist.class.getSimpleName(), parsedValueType,
//						fieldValueType );
		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
		}
	}

}