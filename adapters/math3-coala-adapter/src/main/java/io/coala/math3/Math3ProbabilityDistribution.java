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
import io.coala.random.RandomNumberStream;

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

	public static <T> Math3ProbabilityDistribution<T>
		wrap( final EnumeratedDistribution<T> dist )
	{
		return new Math3ProbabilityDistribution<T>()
		{
			@Override
			public T draw()
			{
				return dist.sample();
			}
		};
	}

	public static Math3ProbabilityDistribution<Long>
		wrap( final IntegerDistribution dist )
	{
		return new Math3ProbabilityDistribution<Long>()
		{
			@Override
			public Long draw()
			{
				return Long.valueOf( dist.sample() );
			}
		};
	}

	public static Math3ProbabilityDistribution<Double>
		wrap( final RealDistribution dist )
	{
		return new Math3ProbabilityDistribution<Double>()
		{
			@Override
			public Double draw()
			{
				return Double.valueOf( dist.sample() );
			}
		};
	}

	public static Math3ProbabilityDistribution<double[]>
		wrap( final MultivariateRealDistribution dist )
	{
		return new Math3ProbabilityDistribution<double[]>()
		{
			@Override
			public double[] draw()
			{
				return dist.sample();
			}
		};
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

		public static Factory of( final RandomNumberStream stream )
		{
			return new Factory( stream );
		}

		/** the {@link RandomNumberStream} wrapping {@link #rng} */
		private final RandomNumberStream stream;

		/** the Math3 {@link RandomGenerator} */
		private final RandomGenerator rng;

		protected Factory( final RandomNumberStream stream )
		{
			this.stream = stream;
			this.rng = Math3RandomNumberStream.toRandomGenerator( stream );
		}

		@Override
		public RandomNumberStream getStream()
		{
			return this.stream;
		}

		@Override
		public <T> ProbabilityDistribution<T>
			createDeterministic( final T constant )
		{
			return ProbabilityDistribution.of( constant );
		}

		@Override
		public ProbabilityDistribution<Long>
			createBinomial( final Number trials, final Number p )
		{
			return wrap( new BinomialDistribution( this.rng, trials.intValue(),
					p.doubleValue() ) );
		}

		@Override
		public <T, WV extends WeightedValue<T, ?>> ProbabilityDistribution<T>
			createCategorical( final List<WV> probabilities )
		{
			return wrap( new EnumeratedDistribution<T>( this.rng,
					toPropabilityMassFunction( probabilities ) ) );
		}

		@Override
		public ProbabilityDistribution<Long> createGeometric( final Number p )
		{
			return wrap(
					new GeometricDistribution( this.rng, p.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Long> createHypergeometric(
			final Number populationSize, final Number numberOfSuccesses,
			final Number sampleSize )
		{
			return wrap( new HypergeometricDistribution( this.rng,
					populationSize.intValue(), numberOfSuccesses.intValue(),
					sampleSize.intValue() ) );
		}

		@Override
		public ProbabilityDistribution<Long> createPascal( final Number r,
			final Number p )
		{
			return wrap( new PascalDistribution( this.rng, r.intValue(),
					p.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Long> createPoisson( final Number mean )
		{
			return wrap( new PoissonDistribution( this.rng, mean.doubleValue(),
					PoissonDistribution.DEFAULT_EPSILON,
					PoissonDistribution.DEFAULT_MAX_ITERATIONS ) );
		}

		@Override
		public ProbabilityDistribution<Long>
			createZipf( final Number numberOfElements, final Number exponent )
		{
			return wrap( new ZipfDistribution( this.rng,
					numberOfElements.intValue(), exponent.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double> createBeta( final Number alpha,
			final Number beta )
		{
			return wrap( new BetaDistribution( this.rng, alpha.doubleValue(),
					beta.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double>
			createCauchy( final Number median, final Number scale )
		{
			return wrap( new CauchyDistribution( this.rng, median.doubleValue(),
					scale.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double>
			createChiSquared( final Number degreesOfFreedom )
		{
			return wrap( new ChiSquaredDistribution( this.rng,
					degreesOfFreedom.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double>
			createExponential( final Number mean )
		{
			return wrap( new ExponentialDistribution( this.rng,
					mean.doubleValue() ) );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <T extends Number> ProbabilityDistribution<Double>
			createEmpirical( final T... values )
		{
			final EmpiricalDistribution result = new EmpiricalDistribution(
					values.length / 10, this.rng );
			result.load( toDoubles( values ) );
			return wrap( result );
		}

		@Override
		public ProbabilityDistribution<Double> createF(
			final Number numeratorDegreesOfFreedom,
			final Number denominatorDegreesOfFreedom )
		{
			return wrap( new FDistribution( this.rng,
					numeratorDegreesOfFreedom.doubleValue(),
					denominatorDegreesOfFreedom.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double> createGamma( final Number shape,
			final Number scale )
		{
			return wrap( new GammaDistribution( this.rng, shape.doubleValue(),
					scale.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double> createLevy( final Number mu,
			final Number c )
		{
			return wrap( new LevyDistribution( this.rng, mu.doubleValue(),
					c.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double>
			createLogNormal( final Number scale, final Number shape )
		{
			return wrap( new LogNormalDistribution( this.rng,
					scale.doubleValue(), shape.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double> createNormal( final Number mean,
			final Number sd )
		{
			return wrap( new NormalDistribution( this.rng, mean.doubleValue(),
					sd.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<double[]> createMultinormal(
			final double[] means, final double[][] covariances )
		{
			return wrap( new MultivariateNormalDistribution( this.rng, means,
					covariances ) );
		}

		@Override
		public ProbabilityDistribution<Double> createPareto( final Number scale,
			final Number shape )
		{
			return wrap( new ParetoDistribution( this.rng, scale.doubleValue(),
					shape.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double>
			createT( final Number degreesOfFreedom )
		{
			return wrap( new TDistribution( this.rng,
					degreesOfFreedom.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double> createTriangular( final Number a,
			final Number b, final Number c )
		{
			return wrap( new TriangularDistribution( this.rng, a.doubleValue(),
					b.doubleValue(), c.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double>
			createUniformContinuous( final Number lower, final Number upper )
		{
			return wrap( new UniformRealDistribution( this.rng,
					lower.doubleValue(), upper.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Double>
			createWeibull( final Number alpha, final Number beta )
		{
			return wrap( new WeibullDistribution( this.rng, alpha.doubleValue(),
					beta.doubleValue() ) );
		}

		@Override
		public ProbabilityDistribution<Long>
			createUniformDiscrete( final Number lower, final Number upper )
		{
			return wrap( new UniformIntegerDistribution( this.rng,
					lower.intValue(), upper.intValue() ) );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <T> ProbabilityDistribution<T>
			createUniformCategorical( final T... values )
		{
			return wrap( new EnumeratedDistribution<T>( this.rng,
					toPropabilityMassFunction( values ) ) );
		}

		@Override
		public ProbabilityDistribution<Boolean>
			createBernoulli( final double probability )
		{
			return new ProbabilityDistribution<Boolean>()
			{
				@Override
				public Boolean draw()
				{
					return rng.nextDouble() < probability;
				}
			};
//			return wrap( new EnumeratedDistribution<Boolean>( this.rng,
//					Arrays.asList( Pair.create( Boolean.TRUE, probability ),
//							Pair.create( Boolean.FALSE, 1 - probability ) ) ) );
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

		public static Fitter of( final RandomNumberStream stream )
		{
			return of( Factory.of( stream ) );
		}

		public static Fitter of( final ProbabilityDistribution.Factory factory )
		{
			return new Fitter( factory );
		}

		/** */
		private static final Logger LOG = LogManager.getLogger( Fitter.class );

		/** */
		private final ProbabilityDistribution.Factory factory;

		/**
		 * {@link Fitter} constructor
		 * 
		 * @param factory the {@link ProbabilityDistribution.Factory}
		 */
		protected Fitter( final ProbabilityDistribution.Factory factory )
		{
			this.factory = factory;
		}

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