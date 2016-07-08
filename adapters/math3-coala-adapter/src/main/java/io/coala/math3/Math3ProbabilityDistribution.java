/* $Id: 9535a51bd51d7c4d1b66f64b408b7d57515371ff $
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
package io.coala.math3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.LevyDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MultivariateRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.distribution.PascalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.distribution.TriangularDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionFactory;
import io.coala.math.FrequencyDistribution;
import io.coala.math.WeightedValue;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.PseudoRandom;

/**
 * {@link Math3ProbabilityDistribution} creates {@link ProbabilityDistribution}s
 * implemented by Apache's commons-math3
 * 
 * @version $Id: 9535a51bd51d7c4d1b66f64b408b7d57515371ff $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public abstract class Math3ProbabilityDistribution<S>
	extends ProbabilityDistribution<S>
{

	@SafeVarargs
	public static <T, S> Math3ProbabilityDistribution<T> of(
		final EnumeratedDistribution<T> dist, final PseudoRandom stream,
		final S... args )
	{
		final Math3ProbabilityDistribution<T> result = new Math3ProbabilityDistribution<T>()
		{
			@Override
			public T draw()
			{
				return dist.sample();
			}
		};
		result.stream = stream;
		result.params = Arrays.asList( args );
		return result;
	}

	@SafeVarargs
	public static <S> Math3ProbabilityDistribution<Long> of(
		final IntegerDistribution dist, final PseudoRandom stream,
		final S... args )
	{
		final Math3ProbabilityDistribution<Long> result = new Math3ProbabilityDistribution<Long>()
		{
			@Override
			public Long draw()
			{
				return Long.valueOf( dist.sample() );
			}
		};
		result.stream = stream;
		return result;
	}

	@SafeVarargs
	public static <S> Math3ProbabilityDistribution<Double> of(
		final RealDistribution dist, final PseudoRandom stream,
		final S... args )
	{
		final Math3ProbabilityDistribution<Double> result = new Math3ProbabilityDistribution<Double>()
		{
			@Override
			public Double draw()
			{
				return Double.valueOf( dist.sample() );
			}
		};
		result.stream = stream;
		return result;
	}

	@SafeVarargs
	public static <S> Math3ProbabilityDistribution<double[]> of(
		final MultivariateRealDistribution dist, final PseudoRandom stream,
		final S... args )
	{
		final Math3ProbabilityDistribution<double[]> result = new Math3ProbabilityDistribution<double[]>()
		{
			@Override
			public double[] draw()
			{
				return dist.sample();
			}
		};
		result.stream = stream;
		return result;
	}

	/**
	 * @param valueWeights the {@link List} of {@link WeightedValue}s
	 * @return a probability mass function as {@link List} of {@link Pair}s
	 */
	public static <T, WV extends WeightedValue<T, ?>> List<Pair<T, Double>>
		toPropabilityMassFunction( final List<WV> valueWeights )
	{
		final List<Pair<T, Double>> pmf = new ArrayList<>();
		if( valueWeights == null || valueWeights.isEmpty() )
			throw ExceptionFactory.createUnchecked( "Must have some value(s)" );
		for( WeightedValue<T, ?> p : valueWeights )
			pmf.add( Pair.create( p.getValue(), p.getWeight().doubleValue() ) );
		return pmf;
	}

	/**
	 * @param <T> the type of value
	 * @param values the value array
	 * @return a probability mass function as {@link List} of {@link Pair}s
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> List<Pair<T, Double>>
		toPropabilityMassFunction( final T... values )
	{
		final Double w = Double.valueOf( 1d );
		final List<Pair<T, Double>> pmf = new ArrayList<>();
		if( values == null || values.length == 0 )
			throw ExceptionFactory.createUnchecked( "Must have some value(s)" );
		for( T value : values )
			pmf.add( Pair.create( value, w ) );
		return pmf;
	}

	@SuppressWarnings( "unchecked" )
	public static <T extends Number> double[] toDoubles( final T... values )
	{
		final double[] result = new double[values.length];
		int i = 0;
		for( T value : values )
			result[i++] = value.doubleValue();
		return result;
	}

	/**
	 * {@link Factory} creates {@link ProbabilityDistribution}s implemented by
	 * Apache's commons-math3 toolkit
	 * 
	 * @version $Id: 9535a51bd51d7c4d1b66f64b408b7d57515371ff $
	 * @author Rick van Krevelen
	 */
	public static class Factory implements ProbabilityDistribution.Factory
	{

		public static Factory of( final PseudoRandom stream )
		{
			final Factory result = new Factory();
			result.stream = stream;
			result.rng = Math3PseudoRandom.toRandomGenerator( stream );
			return result;
		}

		/** the {@link PseudoRandom} wrapping {@link #rng} */
		private PseudoRandom stream;

		/** the Math3 {@link RandomGenerator} */
		private RandomGenerator rng;

		@Override
		public PseudoRandom getStream()
		{
			return this.stream;
		}

		@Override
		public <T> ProbabilityDistribution<T>
			createDeterministic( final T constant )
		{
			return ProbabilityDistribution.createDeterministic( constant );
		}

		@Override
		public ProbabilityDistribution<Long>
			createBinomial( final Number trials, final Number p )
		{
			return Math3ProbabilityDistribution.of(
					new BinomialDistribution( this.rng, trials.intValue(),
							p.doubleValue() ),
					this.stream, trials.intValue(), p.doubleValue() );
		}

		@Override
		public ProbabilityDistribution<Boolean>
			createBernoulli( final Number probability )
		{
			return ProbabilityDistribution.createBernoulli( this.stream,
					probability );
		}

		@Override
		public <T, WV extends WeightedValue<T, ?>> ProbabilityDistribution<T>
			createCategorical( final List<WV> probabilities )
		{
			return Math3ProbabilityDistribution.of(
					new EnumeratedDistribution<T>( this.rng,
							toPropabilityMassFunction( probabilities ) ),
					this.stream, probabilities );
		}

		@Override
		public ProbabilityDistribution<Long> createGeometric( final Number p )
		{
			return Math3ProbabilityDistribution.of(
					new GeometricDistribution( this.rng, p.doubleValue() ),
					this.stream, p );
		}

		@Override
		public ProbabilityDistribution<Long> createHypergeometric(
			final Number populationSize, final Number numberOfSuccesses,
			final Number sampleSize )
		{
			return Math3ProbabilityDistribution.of(
					new HypergeometricDistribution( this.rng,
							populationSize.intValue(),
							numberOfSuccesses.intValue(),
							sampleSize.intValue() ),
					this.stream, populationSize, numberOfSuccesses,
					sampleSize );
		}

		@Override
		public ProbabilityDistribution<Long> createPascal( final Number r,
			final Number p )
		{
			return Math3ProbabilityDistribution
					.of( new PascalDistribution( this.rng, r.intValue(),
							p.doubleValue() ), this.stream, r, p );
		}

		@Override
		public ProbabilityDistribution<Long> createPoisson( final Number mean )
		{
			return Math3ProbabilityDistribution.of(
					new PoissonDistribution( this.rng, mean.doubleValue(),
							PoissonDistribution.DEFAULT_EPSILON,
							PoissonDistribution.DEFAULT_MAX_ITERATIONS ),
					this.stream, mean );
		}

		@Override
		public ProbabilityDistribution<Long>
			createZipf( final Number numberOfElements, final Number exponent )
		{
			return Math3ProbabilityDistribution.of(
					new ZipfDistribution( this.rng, numberOfElements.intValue(),
							exponent.doubleValue() ),
					this.stream, numberOfElements, exponent );
		}

		@Override
		public ProbabilityDistribution<Double> createBeta( final Number alpha,
			final Number beta )
		{
			return Math3ProbabilityDistribution
					.of( new BetaDistribution( this.rng, alpha.doubleValue(),
							beta.doubleValue() ), this.stream, alpha, beta );
		}

		@Override
		public ProbabilityDistribution<Double>
			createCauchy( final Number median, final Number scale )
		{
			return Math3ProbabilityDistribution
					.of( new CauchyDistribution( this.rng, median.doubleValue(),
							scale.doubleValue() ), this.stream, median, scale );
		}

		@Override
		public ProbabilityDistribution<Double>
			createChiSquared( final Number degreesOfFreedom )
		{
			return Math3ProbabilityDistribution.of(
					new ChiSquaredDistribution( this.rng,
							degreesOfFreedom.doubleValue() ),
					this.stream, degreesOfFreedom );
		}

		@Override
		public ProbabilityDistribution<Double>
			createExponential( final Number mean )
		{
			return Math3ProbabilityDistribution.of(
					new ExponentialDistribution( this.rng, mean.doubleValue() ),
					this.stream, mean );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <T extends Number> ProbabilityDistribution<Double>
			createEmpirical( final T... values )
		{
			final EmpiricalDistribution result = new EmpiricalDistribution(
					values.length / 10, this.rng );
			result.load( toDoubles( values ) );
			return Math3ProbabilityDistribution.of( result, this.stream,
					values );
		}

		@Override
		public ProbabilityDistribution<Double> createF(
			final Number numeratorDegreesOfFreedom,
			final Number denominatorDegreesOfFreedom )
		{
			return Math3ProbabilityDistribution.of(
					new FDistribution( this.rng,
							numeratorDegreesOfFreedom.doubleValue(),
							denominatorDegreesOfFreedom.doubleValue() ),
					this.stream, numeratorDegreesOfFreedom,
					denominatorDegreesOfFreedom );
		}

		@Override
		public ProbabilityDistribution<Double> createGamma( final Number shape,
			final Number scale )
		{
			return Math3ProbabilityDistribution
					.of( new GammaDistribution( this.rng, shape.doubleValue(),
							scale.doubleValue() ), this.stream, shape, scale );
		}

		@Override
		public ProbabilityDistribution<Double> createLevy( final Number mu,
			final Number c )
		{
			return Math3ProbabilityDistribution
					.of( new LevyDistribution( this.rng, mu.doubleValue(),
							c.doubleValue() ), this.stream, mu, c );
		}

		@Override
		public ProbabilityDistribution<Double>
			createLogNormal( final Number scale, final Number shape )
		{
			return Math3ProbabilityDistribution.of(
					new LogNormalDistribution( this.rng, scale.doubleValue(),
							shape.doubleValue() ),
					this.stream, scale, shape );
		}

		@Override
		public ProbabilityDistribution<Double> createNormal( final Number mean,
			final Number sd )
		{
			return Math3ProbabilityDistribution
					.of( new NormalDistribution( this.rng, mean.doubleValue(),
							sd.doubleValue() ), this.stream, mean, sd );
		}

		@Override
		public ProbabilityDistribution<double[]> createMultinormal(
			final double[] means, final double[][] covariances )
		{
			return Math3ProbabilityDistribution
					.of( new MultivariateNormalDistribution( this.rng, means,
							covariances ), this.stream, means, covariances );
		}

		@Override
		public ProbabilityDistribution<Double> createPareto( final Number scale,
			final Number shape )
		{
			return Math3ProbabilityDistribution
					.of( new ParetoDistribution( this.rng, scale.doubleValue(),
							shape.doubleValue() ), this.stream, scale, shape );
		}

		@Override
		public ProbabilityDistribution<Double>
			createT( final Number degreesOfFreedom )
		{
			return Math3ProbabilityDistribution.of(
					new TDistribution( this.rng,
							degreesOfFreedom.doubleValue() ),
					this.stream, degreesOfFreedom );
		}

		@Override
		public ProbabilityDistribution<Double> createTriangular( final Number a,
			final Number b, final Number c )
		{
			return Math3ProbabilityDistribution.of(
					new TriangularDistribution( this.rng, a.doubleValue(),
							b.doubleValue(), c.doubleValue() ),
					this.stream, a, b, c );
		}

		@Override
		public ProbabilityDistribution<Double>
			createUniformContinuous( final Number lower, final Number upper )
		{
			return Math3ProbabilityDistribution.of(
					new UniformRealDistribution( this.rng, lower.doubleValue(),
							upper.doubleValue() ),
					this.stream, lower, upper );
		}

		@Override
		public ProbabilityDistribution<Double>
			createWeibull( final Number alpha, final Number beta )
		{
			return Math3ProbabilityDistribution
					.of( new WeibullDistribution( this.rng, alpha.doubleValue(),
							beta.doubleValue() ), this.stream, alpha, beta );
		}

		@Override
		public ProbabilityDistribution<Long>
			createUniformDiscrete( final Number lower, final Number upper )
		{
			return Math3ProbabilityDistribution.of(
					new UniformIntegerDistribution( this.rng, lower.intValue(),
							upper.intValue() ),
					this.stream, lower, upper );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <T> ProbabilityDistribution<T>
			createUniformCategorical( final T... values )
		{
			return Math3ProbabilityDistribution.of(
					new EnumeratedDistribution<T>( this.rng,
							toPropabilityMassFunction( values ) ),
					this.stream, values );
		}
	}

	/**
	 * {@link Fitter}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class Fitter implements ProbabilityDistribution.Fitter
	{

		public static Fitter of( final PseudoRandom stream )
		{
			return of( Factory.of( stream ) );
		}

		public static Fitter of( final ProbabilityDistribution.Factory factory )
		{
			final Fitter result = new Fitter();
			result.factory = factory;
			return result;
		}

		/** */
		private static final Logger LOG = LogManager.getLogger( Fitter.class );

		/** */
		private ProbabilityDistribution.Factory factory;

		@Override
		public ProbabilityDistribution.Factory getFactory()
		{
			return this.factory;
		}

		public <Q extends Quantity> ArithmeticDistribution<Q> fitNormal(
			final FrequencyDistribution.Interval<Q, ?> freq,
			final Unit<Q> unit )
		{
			final WeightedObservedPoints points = new WeightedObservedPoints();
			for( Entry<Amount<Q>, Amount<Dimensionless>> entry : freq
					.toProportions( Unit.ONE ).entrySet() )
				points.add( entry.getKey().getEstimatedValue(),
						entry.getValue().getEstimatedValue() );
			final double[] params = GaussianCurveFitter.create()
					.fit( points.toList() );
			LOG.trace( "Fitted Gaussian with parameters: [norm,mu,sd]={}",
					Arrays.asList( params ) );
			return getFactory().createNormal( params[1], params[2] )
					.toAmounts( unit ).times( params[0] );
		}

	}
}