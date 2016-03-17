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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import io.coala.exception.x.ExceptionBuilder;
import io.coala.util.DecimalUtil;
import io.coala.util.InstanceParser;

/**
 * {@link RandomDistribution} is similar to a {@link javax.inject.Provider} but
 * rather a factory that generates values that vary according to some
 * distribution
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface RandomDistribution<T> extends Serializable
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
	 * {@link RandomDistribution}s
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

		public static <T> RandomDistribution<T> asConstant( final T constant )
		{
			return new RandomDistribution<T>()
			{
				@Override
				public T draw()
				{
					return constant;
				}
			};
		}

		public static <N extends Number, Q extends Quantity>
			RandomDistribution<Amount<Q>>
			asAmount( final RandomDistribution<N> dist, final Unit<Q> unit )
		{
			return new RandomDistribution<Amount<Q>>()
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

		public static <T, P> RandomDistribution<T> valueOf( final String dist,
			final Parser parser, final Class<P> argType )
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
	 * {@link Parser} generates {@link RandomDistribution}s of specific shapes
	 */
	interface Parser
	{
		Factory getFactory();

		RandomNumberStream getStream();

		<T, V> RandomDistribution<T> parse( String name,
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

			@Override
			public RandomNumberStream getStream()
			{
				return getFactory().getStream();
			}

			@SuppressWarnings( "unchecked" )
			@Override
			public <T, V> RandomDistribution<T> parse( final String label,
				final List<ProbabilityMass<V, ?>> args )
			{
				if( args.isEmpty() ) throw ExceptionBuilder.unchecked(
						"Missing distribution parameters: %s", label ).build();

				if( getFactory() == null ) return (RandomDistribution<T>) Util
						.asConstant( args.get( 0 ).getValue() );

				switch( label.toLowerCase() )
				{
				case "const":
				case "constant":
					return (RandomDistribution<T>) getFactory()
							.getConstant( args.get( 0 ).getValue() );

				case "enum":
				case "enumerated":
					return (RandomDistribution<T>) getFactory()
							.getEnumerated( args );

				case "geom":
				case "geometric":
					return (RandomDistribution<T>) getFactory()
							.getGeometric( (Number) args.get( 0 ).getValue() );

				case "hypergeom":
				case "hypergeometric":
					return (RandomDistribution<T>) getFactory()
							.getHypergeometric(
									(Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue(),
									(Number) args.get( 2 ).getValue() );

				case "pascal":
					return (RandomDistribution<T>) getFactory().getPascal(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "poisson":
					return (RandomDistribution<T>) getFactory().getPoisson(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "zipf":
					return (RandomDistribution<T>) getFactory().getZipf(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "beta":
					return (RandomDistribution<T>) getFactory().getBeta(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "cauchy":
					return (RandomDistribution<T>) getFactory().getCauchy(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "chi":
				case "chisquared": // TODO = Pearson?
					return (RandomDistribution<T>) getFactory()
							.getChiSquared( (Number) args.get( 0 ).getValue() );

				case "exp":
				case "exponent":
				case "exponential":
					return (RandomDistribution<T>) getFactory().getExponential(
							(Number) args.get( 0 ).getValue() );

				case "f":
					return (RandomDistribution<T>) getFactory().getF(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "gamma":
					return (RandomDistribution<T>) getFactory().getGamma(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "levy":
					return (RandomDistribution<T>) getFactory().getLevy(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "lognormal":
					return (RandomDistribution<T>) getFactory().getLogNormal(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "normal":
					return (RandomDistribution<T>) getFactory().getNormal(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "pareto":
					return (RandomDistribution<T>) getFactory().getPareto(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "t":
					return (RandomDistribution<T>) getFactory()
							.getT( (Number) args.get( 0 ).getValue() );

				case "tria":
				case "triangular":
					return (RandomDistribution<T>) getFactory().getTriangular(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue(),
							(Number) args.get( 2 ).getValue() );

				case "uniformdiscrete":
				case "uniforminteger":
					return (RandomDistribution<T>) getFactory()
							.getUniformInteger(
									(Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue() );

				case "uniformreal":
				case "uniformcontinuous":
					return (RandomDistribution<T>) getFactory().getUniformReal(
							(Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "uniform":
				case "uniformenum":
				case "uniformenumerated":
					final List<T> values = new ArrayList<>();
					for( ProbabilityMass<V, ?> pair : args )
						values.add( (T) pair.getValue() );
					return (RandomDistribution<T>) getFactory()
							.getUniformEnumerated( values.toArray() );

				case "weibull":
					return (RandomDistribution<T>) getFactory().getWeibull(
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
	 * {@link Factory} generates {@link RandomDistribution}s of specific shapes
	 */
	interface Factory
	{

		/**
		 * @return the {@link RandomNumberStream} used in creating
		 *         {@link RandomDistribution}s
		 */
		RandomNumberStream getStream();

		/**
		 * @param value the constant to be returned on each draw
		 * @return the constant {@link RandomDistribution}
		 */
		<T> RandomDistribution<T> getConstant( T value );

		/**
		 * @param trials number of consecutive successes
		 * @param p probability of a single success
		 * @return the binomial {@link RandomDistribution}
		 */
		RandomDistribution<Long> getBinomial( Number trials, Number p );

		/**
		 * @param probabilities the {@link ProbabilityMass} function enumerated
		 *            for each value
		 * @return the enumerated {@link RandomDistribution}
		 */
		<T> RandomDistribution<T>
			getEnumerated( List<ProbabilityMass<T, ?>> probabilities );

		/**
		 * @param p
		 * @return the geometric {@link RandomDistribution}
		 */
		RandomDistribution<Long> getGeometric( Number p );

		/**
		 * @param populationSize
		 * @param numberOfSuccesses
		 * @param sampleSize
		 * @return the hypergeometric {@link RandomDistribution}
		 */
		RandomDistribution<Long> getHypergeometric( Number populationSize,
			Number numberOfSuccesses, Number sampleSize );

		/**
		 * @param r
		 * @param p
		 * @return the Pascal {@link RandomDistribution}
		 */
		RandomDistribution<Long> getPascal( Number r, Number p );

		/**
		 * @param alpha
		 * @param beta
		 * @return the Poisson {@link RandomDistribution}
		 */
		RandomDistribution<Long> getPoisson( Number alpha, Number beta );

		/**
		 * @param alpha
		 * @param beta
		 * @return the Zipf {@link RandomDistribution}
		 */
		RandomDistribution<Long> getZipf( Number numberOfElements,
			Number exponent );

		/**
		 * @param alpha
		 * @param beta
		 * @return the &beta; {@link RandomDistribution}
		 */
		RandomDistribution<Double> getBeta( Number alpha, Number beta );

		/**
		 * @param median
		 * @param scale
		 * @return the Cauchy {@link RandomDistribution}
		 */
		RandomDistribution<Double> getCauchy( Number median, Number scale );

		/**
		 * @param degreesOfFreedom
		 * @return the &chi;<sup>2</sup> {@link RandomDistribution}
		 */
		RandomDistribution<Double> getChiSquared( Number degreesOfFreedom );

		/**
		 * @param mean
		 * @return the exponential {@link RandomDistribution}
		 */
		RandomDistribution<Double> getExponential( Number mean );

		/**
		 * @param numeratorDegreesOfFreedom
		 * @param denominatorDegreesOfFreedom
		 * @return the F {@link RandomDistribution}
		 */
		RandomDistribution<Double> getF( Number numeratorDegreesOfFreedom,
			Number denominatorDegreesOfFreedom );

		/**
		 * @param shape
		 * @param scale
		 * @return the &gamma; {@link RandomDistribution}
		 */
		RandomDistribution<Double> getGamma( Number shape, Number scale );

		/**
		 * @param mu
		 * @param c
		 * @return the Levy {@link RandomDistribution}
		 */
		RandomDistribution<Double> getLevy( Number mu, Number c );

		/**
		 * @param scale
		 * @param shape
		 * @return the log Gaussian/Normal {@link RandomDistribution}
		 */
		RandomDistribution<Double> getLogNormal( Number scale, Number shape );

		/**
		 * @param mean
		 * @param sd the standard deviation
		 * @return the Gaussian / Normal {@link RandomDistribution}
		 */
		RandomDistribution<Double> getNormal( Number mean, Number sd );

		// TODO scrap or wrap (double <-> Number) using closures?
		/**
		 * @param means
		 * @param covariances
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<double[]> getMultivariateNormal( double[] means,
			double[][] covariances );

		/**
		 * @param scale
		 * @param shape
		 * @return the Pareto {@link RandomDistribution}
		 */
		RandomDistribution<Double> getPareto( Number scale, Number shape );

		/**
		 * @param denominatorDegreesOfFreedom
		 * @return the T {@link RandomDistribution}
		 */
		RandomDistribution<Double> getT( Number denominatorDegreesOfFreedom );

		/**
		 * @param a
		 * @param b
		 * @param c
		 * @return the triangular {@link RandomDistribution}
		 */
		RandomDistribution<Double> getTriangular( Number a, Number b,
			Number c );

		/**
		 * @param lower
		 * @param upper
		 * @return the uniform discrete/integer {@link RandomDistribution}
		 */
		RandomDistribution<Long> getUniformInteger( Number lower,
			Number upper );

		/**
		 * @param lower
		 * @param upper
		 * @return the uniform continuous/real {@link RandomDistribution}
		 */
		RandomDistribution<Double> getUniformReal( Number lower, Number upper );

		/**
		 * @param values the enumerated values
		 * @return the uniform enumerated {@link RandomDistribution}
		 */
		@SuppressWarnings( "unchecked" )
		<T> RandomDistribution<T> getUniformEnumerated( T... values );

		/**
		 * @param alpha
		 * @param beta
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getWeibull( Number alpha, Number beta );

		// TODO RandomDistribution<Double> getBernoulli();

		// TODO RandomDistribution<Double> getErlang();

		// TODO RandomDistribution<Double> getPearson5();

		// TODO RandomDistribution<Double> getPearson6();

		// TODO RandomDistribution<Double> getNegativeBionomial();
	}

}