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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionBuilder;
import io.coala.util.DecimalUtil;
import io.coala.util.InstanceParser;

/**
 * {@link ProbabilityDistribution} is similar to a {@link javax.inject.Provider}
 * but rather a factory that generates values that vary according to some
 * distribution
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface ProbabilityDistribution<T> extends Serializable
{

	/**
	 * @return the next pseudo-random sample
	 */
	T draw();

	/**
	 * {@link ProbabilityMass}
	 * 
	 * @param <V> the concrete type of random value to draw
	 * @param <M> the concrete type of probability mass {@link Number}
	 * @version $Id: a7842c5dc1c8963fe6c9721cdcda6c3b21980bb0 $
	 * @author Rick van Krevelen
	 */
	class ProbabilityMass<V, M extends Number> implements Serializable
	{

		/** */
		private static final long serialVersionUID = 1L;

		/** */
		private V value;

		/** */
		private M mass;

		/**
		 * {@link ProbabilityMass} zero-arg bean constructor
		 */
		protected ProbabilityMass()
		{
		}

		/**
		 * {@link ProbabilityMass} constructor
		 * 
		 * @param key
		 * @param value
		 */
		public ProbabilityMass( final V key, final M value )
		{
			this.value = key;
			this.mass = value;
		}

		/**
		 * @return the drawable value
		 */
		public V getValue()
		{
			return this.value;
		}

		/**
		 * @return the probability mass
		 */
		public M getMass()
		{
			return this.mass;
		}

		/**
		 * @param key
		 * @param value
		 * @return
		 */
		public static <V, M extends Number> ProbabilityMass<V, M>
			of( final V key, final M value )
		{
			return new ProbabilityMass<V, M>( key, value );
		}

		@Override
		public String toString()
		{
			return this.getClass().getName() + "(" + this.getMass() + " => "
					+ this.getValue() + ")@" + this.hashCode();
		}
	}

	/**
	 * {@link Util} provides static helper methods for
	 * {@link ProbabilityDistribution}s
	 * 
	 * @version $Id: a7842c5dc1c8963fe6c9721cdcda6c3b21980bb0 $
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "serial" )
	class Util implements io.coala.util.Util
	{

		private Util()
		{
		}

		public static <T> ProbabilityDistribution<T>
			asConstant( final T constant )
		{
			return new ProbabilityDistribution<T>()
			{
				@Override
				public T draw()
				{
					return constant;
				}
			};
		}

		public static <N extends Number, Q extends Quantity>
			ProbabilityDistribution<Amount<Q>> asAmount(
				final ProbabilityDistribution<N> dist, final Unit<Q> unit )
		{
			return new ProbabilityDistribution<Amount<Q>>()
			{
				@Override
				public Amount<Q> draw()
				{
					final Number value = dist.draw();
					return value instanceof Long || value instanceof Integer
							|| (value instanceof BigDecimal && DecimalUtil
									.isExact( (BigDecimal) value ))
											? Amount.valueOf( value.longValue(),
													unit )
											: Amount.valueOf(
													value.doubleValue(), unit );
				}
			};
		}

		private static final Pattern distPattern = Pattern
				.compile( "^(?<dist>[^\\(]+)\\((?<params>[^)]*)\\)$" );

		public static <T, P> ProbabilityDistribution<T> valueOf(
			final String dist, final Parser parser, final Class<P> argType )
		{
			final Matcher m = distPattern.matcher( dist.trim() );
			if( !m.find() ) throw ExceptionBuilder
					.unchecked( "Problem parsing distribution: %s", dist )
					.build();
			final List<ProbabilityMass<P, ?>> params = new ArrayList<>();
			for( String valuePair : m.group( "params" ).split( "[;]" ) )
			{
				if( valuePair.trim().isEmpty() ) continue; // empty parentheses
				final String[] valueWeights = valuePair.split( "[:]" );
				if( valueWeights.length == 1 )
				{
					params.add( ProbabilityMass.of( InstanceParser.of( argType )
							.parseOrTrimmed( valuePair ), 1 ) );
				} else
					params.add( ProbabilityMass.of(
							InstanceParser.of( argType )
									.parseOrTrimmed( valueWeights[0] ),
							new BigDecimal( valueWeights[1] ) ) );
			}
			if( params.isEmpty() && argType.isEnum() )
				for( P constant : argType.getEnumConstants() )
				params.add( ProbabilityMass.of( constant, 1 ) );
			return parser.parse( m.group( "dist" ), params );
		}
	}

	/**
	 * {@link Parser} generates {@link ProbabilityDistribution}s of specific
	 * shapes
	 */
	interface Parser
	{
		/** @return a {@link Factory} of {@link ProbabilityDistribution}s */
		Factory getFactory();

		/**
		 * @param name the symbol of the {@link ProbabilityDistribution}
		 * @param args the arguments for the {@link ProbabilityDistribution}
		 * @return a {@link ProbabilityDistribution}
		 */
		<T, V> ProbabilityDistribution<T> parse( String name,
			List<ProbabilityMass<V, ?>> args );

		/**
		 * {@link ConstantDistributionParser}
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		class Simple implements Parser
		{
			private final Factory factory;

			/**
			 * {@link Simple} constructor will generate only constant
			 * distributions of the first parsed argument
			 */
			public Simple()
			{
				this( null );
			}

			public Simple( final Factory factory )
			{
				this.factory = factory;
			}

			@Override
			public Factory getFactory()
			{
				return this.factory;
			}

			@SuppressWarnings( "unchecked" )
			@Override
			public <T, V> ProbabilityDistribution<T> parse( final String label,
				final List<ProbabilityMass<V, ?>> args )
			{
				if( args.isEmpty() ) throw ExceptionBuilder.unchecked(
						"Missing distribution parameters: %s", label ).build();

				if( getFactory() == null )
					return (ProbabilityDistribution<T>) Util
							.asConstant( args.get( 0 ).getValue() );

				switch( label.toLowerCase( Locale.ROOT ) )
				{
				case "const":
				case "constant":
					return (ProbabilityDistribution<T>) getFactory()
							.getConstant( args.get( 0 ).getValue() );

				case "enum":
				case "enumerated":
					return (ProbabilityDistribution<T>) getFactory()
							.getCategorical( args );

				case "geom":
				case "geometric":
					return (ProbabilityDistribution<T>) getFactory()
							.getGeometric( (Number) args.get( 0 ).getValue() );

				case "hypergeom":
				case "hypergeometric":
					return (ProbabilityDistribution<T>) getFactory()
							.getHypergeometric(
									(Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue(),
									(Number) args.get( 2 ).getValue() );

				case "pascal":
					return (ProbabilityDistribution<T>) getFactory().getPascal(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "poisson":
					return (ProbabilityDistribution<T>) getFactory()
							.getPoisson( (Number) args.get( 0 ).getValue() );

				case "zipf":
					return (ProbabilityDistribution<T>) getFactory().getZipf(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "beta":
					return (ProbabilityDistribution<T>) getFactory().getBeta(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "cauchy":
				case "cauchy-lorentz":
				case "lorentz":
				case "lorentzian":
				case "breit-wigner":
					return (ProbabilityDistribution<T>) getFactory().getCauchy(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "chi":
				case "chisquare": // TODO = Pearson?
				case "chisquared": // TODO = Pearson?
				case "chi-square": // TODO = Pearson?
				case "chi-squared": // TODO = Pearson?
					return (ProbabilityDistribution<T>) getFactory()
							.getChiSquared( (Number) args.get( 0 ).getValue() );

				case "exp":
				case "exponent":
				case "exponential":
					return (ProbabilityDistribution<T>) getFactory()
							.getExponential(
									(Number) args.get( 0 ).getValue() );

				case "pearson6":
				case "beta-prime":
				case "inverted-beta":
				case "f":
					return (ProbabilityDistribution<T>) getFactory().getF(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "pearson3":
				case "gamma":
					return (ProbabilityDistribution<T>) getFactory().getGamma(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "levy":
					return (ProbabilityDistribution<T>) getFactory().getLevy(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "lognormal":
				case "log-normal":
				case "gibrat":
					return (ProbabilityDistribution<T>) getFactory()
							.getLogNormal( (Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue() );

				case "gauss":
				case "gaussian":
				case "normal":
					return (ProbabilityDistribution<T>) getFactory().getNormal(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "pareto":
				case "pareto1":
					return (ProbabilityDistribution<T>) getFactory().getPareto(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "students-t":
				case "t":
					return (ProbabilityDistribution<T>) getFactory()
							.getT( (Number) args.get( 0 ).getValue() );

				case "tria":
				case "triangular":
					return (ProbabilityDistribution<T>) getFactory()
							.getTriangular( (Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue(),
									(Number) args.get( 2 ).getValue() );

				case "uniform-discrete":
				case "uniform-integer":
					return (ProbabilityDistribution<T>) getFactory()
							.getUniformDiscrete(
									(Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue() );

				case "uniform":
				case "uniform-real":
				case "uniform-continuous":
					return (ProbabilityDistribution<T>) getFactory()
							.getUniformContinuous(
									(Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue() );

				case "uniform-enum":
				case "uniform-enumerated":
					final List<T> values = new ArrayList<>();
					for( ProbabilityMass<V, ?> pair : args )
						values.add( (T) pair.getValue() );
					return (ProbabilityDistribution<T>) getFactory()
							.getUniformCategorical( values.toArray() );

				case "frechet":
				case "weibull":
					return (ProbabilityDistribution<T>) getFactory().getWeibull(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );
				}
				throw ExceptionBuilder
						.unchecked( "Unknown distribution symbol: %s", label )
						.build();
			}
		}
	}

	/**
	 * {@link Factory} generates {@link ProbabilityDistribution}s of specific
	 * shapes
	 * 
	 * TODO Bernoulli, Erlang, Pearson5, Pearson6, NegativeBionomial
	 */
	interface Factory
	{

		/**
		 * @return the {@link RandomNumberStream} used in creating
		 *         {@link ProbabilityDistribution}s
		 */
		RandomNumberStream getStream();

		/**
		 * @param value the constant to be returned on each draw
		 * @return a constant {@link ProbabilityDistribution}
		 */
		<T> ProbabilityDistribution<T> getConstant( T value );

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
		ProbabilityDistribution<Long> getBinomial( Number trials, Number p );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/2D-simplex.svg/440px-2D-simplex.svg.png"/>
		 * 
		 * @param probabilities the {@link ProbabilityMass} function enumerated
		 *            for each value
		 * @return an enumerated {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Categorical_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=bernoulli+distribution">
		 *      Wolfram &alpha;</a>
		 */
		<T> ProbabilityDistribution<T>
			getCategorical( List<ProbabilityMass<T, ?>> probabilities );

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
		ProbabilityDistribution<Long> getGeometric( Number p );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP45422d3he4dhebi328d000058fagd36che5g435?MSPStoreType=image/gif&s=55"/>
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
		ProbabilityDistribution<Long> getHypergeometric( Number populationSize,
			Number numberOfSuccesses, Number sampleSize );

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
		ProbabilityDistribution<Long> getPascal( Number r, Number p );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Poisson_pmf.svg/650px-Poisson_pmf.svg.png"/>
		 * 
		 * @param mean &mu; or &lambda;
		 * @return a Poisson {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Poisson_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=poisson+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> getPoisson( Number mean );

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
		ProbabilityDistribution<Long> getZipf( Number numberOfElements,
			Number exponent );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP8501hgb0bgf73624fc5000051a73dc4e9a96c67?MSPStoreType=image/gif&s=16"/>
		 * 
		 * @param alpha shape (positive)
		 * @param beta shape (positive)
		 * @return a &beta; {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Beta_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=beta+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getBeta( Number alpha, Number beta );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP7871b54g7d40h84965d00003da5f5hdbcc5c814?MSPStoreType=image/gif&s=43"/>
		 * 
		 * @param median the location <em>a</em>
		 * @param scale <em>b</em> (positive)
		 * @return a Lorentz or Cauchy {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Cauchy_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=cauchy+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getCauchy( Number median,
			Number scale );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP6191i49013gc13cf2hb000022b4ceehae6f37ea?MSPStoreType=image/gif&s=36"/>
		 * 
		 * @param degreesOfFreedom <em>k</em>
		 * @return a &chi;<sup>2</sup> {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Chi-squared_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=chi+squared+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double>
			getChiSquared( Number degreesOfFreedom );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP88621eh12e8g98651g400003ee0348d29i1g4d4?MSPStoreType=image/gif&s=52"/>
		 * 
		 * @param mean
		 * @return an exponential {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Exponential_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=exponential+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getExponential( Number mean );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP13671i38add4aggc73hh000014324ii26c1f66f7?MSPStoreType=image/gif&s=53"/>
		 * 
		 * @param numeratorDegreesOfFreedom <em>n</em>
		 * @param denominatorDegreesOfFreedom <em>m</em>
		 * @return an F-{@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/F-distribution">Wikipedia
		 *      </a> and
		 *      <a href="https://www.wolframalpha.com/input/?i=f+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getF( Number numeratorDegreesOfFreedom,
			Number denominatorDegreesOfFreedom );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP63311b1eag7ab2ig548b00004782c2i0edach659?MSPStoreType=image/gif&s=2"/>
		 * 
		 * @param shape &alpha; or <em>k</em>
		 * @param scale &beta; or &theta;
		 * @return a &Gamma; (gamma) {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Gamma_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=gamma+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getGamma( Number shape, Number scale );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP52951h27f65cf34d4e2h00003hac30i250a1h7fa?MSPStoreType=image/gif&s=44"/>
		 * 
		 * @param mu
		 * @param c
		 * @return a L&eacute;vy {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/L%C3%A9vy_distribution">
		 *      Wikipedia</a> and <a
		 *      href=""https://www.wolframalpha.com/input/?i=levy+distribution>
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getLevy( Number mu, Number c );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP241g2gaabgif7c3faf0000186db998af94bfha?MSPStoreType=image/gif&s=25"/>
		 * 
		 * @param scale
		 * @param shape
		 * @return a log-normal {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Log-normal_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=lognormal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getLogNormal( Number scale,
			Number shape );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP57711dh32162d1de95bc0000182h50icfigi6hd0?MSPStoreType=image/gif&s=53"/>
		 * 
		 * @param mean &mu;
		 * @param stDev &sigma; (standard deviation)
		 * @return a Gaussian or Normal {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Normal_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=normal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getNormal( Number mean, Number stDev );

		// TODO scrap or wrap (double <-> Number) using closures of Iterable?
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
		ProbabilityDistribution<double[]> getMultinormal( double[] means,
			double[][] covariances );

		/**
		 * <img alt="Probability density function" height="150" src=""/>
		 * 
		 * @param scale
		 * @param shape
		 * @return a Pareto {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Pareto_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=pareto+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getPareto( Number scale, Number shape );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP12122ihg1d9af4a54280000415853423eie0hh4?MSPStoreType=image/gif&s=16"/>
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
		ProbabilityDistribution<Double> getT( Number degreesOfFreedom );

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
		ProbabilityDistribution<Double> getTriangular( Number a, Number b,
			Number c );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP656228c7a6e598g3d76000057af6a6d61g9e468?MSPStoreType=image/gif&s=41"/>
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
		ProbabilityDistribution<Long> getUniformDiscrete( Number min,
			Number max );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP11821h766ih567i7ifd30000345ii2eh6a6e27ee?MSPStoreType=image/gif&s=34"/>
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
		ProbabilityDistribution<Double> getUniformContinuous( Number min,
			Number max );

		/**
		 * wraps a {@link #getUniformDiscrete(Number, Number)} over the full
		 * index range <em>{0, &hellip;, n-1}</em> of specified value
		 * enumeration
		 * 
		 * @param <T> the type of value
		 * @param values the enumeration
		 * @return a uniform enumerated {@link ProbabilityDistribution}
		 * @see <a href= "https://www.wikiwand.com/en/Categorical_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=discrete+uniform+distribution">
		 *      Wolfram &alpha;</a>
		 */
		@SuppressWarnings( "unchecked" )
		<T> ProbabilityDistribution<T> getUniformCategorical( T... values );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP25522008f4d573002dac00004iec9101ia30f3ab?MSPStoreType=image/gif&s=41"/>
		 * 
		 * @param alpha shape &alpha; (positive)
		 * @param beta scale &beta; (positive)
		 * @return a Weibull {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Weibull_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=weibull+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> getWeibull( Number alpha, Number beta );
	}

}