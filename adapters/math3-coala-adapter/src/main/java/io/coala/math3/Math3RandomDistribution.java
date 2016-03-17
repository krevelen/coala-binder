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
import java.util.List;

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
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.distribution.TriangularDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

import io.coala.exception.x.ExceptionBuilder;
import io.coala.random.RandomDistribution;
import io.coala.random.RandomNumberStream;

/**
 * {@link Math3RandomDistribution} creates {@link RandomDistribution}s
 * implemented by Apache's commons-math3
 * 
 * @version $Id: 9535a51bd51d7c4d1b66f64b408b7d57515371ff $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public abstract class Math3RandomDistribution<S>
	implements RandomDistribution<S>
{

	public static <T> Math3RandomDistribution<T>
		wrap( final EnumeratedDistribution<T> dist )
	{
		return new Math3RandomDistribution<T>()
		{
			@Override
			public T draw()
			{
				return dist.sample();
			}
		};
	}

	public static Math3RandomDistribution<Long>
		wrap( final IntegerDistribution dist )
	{
		return new Math3RandomDistribution<Long>()
		{
			@Override
			public Long draw()
			{
				return Long.valueOf( dist.sample() );
			}
		};
	}

	public static Math3RandomDistribution<Double>
		wrap( final RealDistribution dist )
	{
		return new Math3RandomDistribution<Double>()
		{
			@Override
			public Double draw()
			{
				return Double.valueOf( dist.sample() );
			}
		};
	}

	public static Math3RandomDistribution<double[]>
		wrap( final MultivariateRealDistribution dist )
	{
		return new Math3RandomDistribution<double[]>()
		{
			@Override
			public double[] draw()
			{
				return dist.sample();
			}
		};
	}

	public static <T> List<Pair<T, Double>> toPropabilityMassFunction(
		final List<ProbabilityMass<T, ?>> probabilities )
	{
		final List<Pair<T, Double>> pmf = new ArrayList<>();
		if( probabilities == null || probabilities.isEmpty() )
			throw ExceptionBuilder.unchecked( "Must have some value(s)" )
					.build();
		for( ProbabilityMass<T, ?> p : probabilities )
			pmf.add( Pair.create( p.getValue(), p.getMass().doubleValue() ) );
		return pmf;
	}

	@SuppressWarnings( "unchecked" )
	public static <T> List<Pair<T, Double>>
		toPropabilityMassFunction( final T... values )
	{
		final Double w = Double.valueOf( 1d );
		final List<Pair<T, Double>> pmf = new ArrayList<>();
		if( values == null || values.length == 0 ) throw ExceptionBuilder
				.unchecked( "Must have some value(s)" ).build();
		for( T value : values )
			pmf.add( Pair.create( value, w ) );
		return pmf;
	}

	/**
	 * {@link Factory} creates {@link RandomDistribution}s implemented by
	 * Apache's commons-math3 toolkit
	 * 
	 * @version $Id: 9535a51bd51d7c4d1b66f64b408b7d57515371ff $
	 * @author Rick van Krevelen
	 */
	public static class Factory implements RandomDistribution.Factory
	{

		public static Factory of( final RandomNumberStream stream )
		{
			return new Factory( stream );
		}

		private final RandomNumberStream stream;

		private final RandomGenerator rng;

		public Factory( final RandomNumberStream stream )
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
		public <T> RandomDistribution<T> getConstant( final T constant )
		{
			return RandomDistribution.Util.asConstant( constant );
		}

		@Override
		public RandomDistribution<Long> getBinomial( final Number trials,
			final Number p )
		{
			return wrap( new BinomialDistribution( this.rng, trials.intValue(),
					p.doubleValue() ) );
		}

		@Override
		public <T> RandomDistribution<T> getEnumerated(

			final List<ProbabilityMass<T, ?>> probabilities )
		{
			return wrap( new EnumeratedDistribution<T>( this.rng,
					toPropabilityMassFunction( probabilities ) ) );
		}

		@Override
		public RandomDistribution<Long> getGeometric( final Number p )
		{
			return wrap(
					new GeometricDistribution( this.rng, p.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Long> getHypergeometric(
			final Number populationSize, final Number numberOfSuccesses,
			final Number sampleSize )
		{
			return wrap( new HypergeometricDistribution( this.rng,
					populationSize.intValue(), numberOfSuccesses.intValue(),
					sampleSize.intValue() ) );
		}

		@Override
		public RandomDistribution<Long> getPascal( final Number r,
			final Number p )
		{
			return wrap( new PascalDistribution( this.rng, r.intValue(),
					p.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Long> getPoisson( final Number alpha,
			final Number beta )
		{
			return wrap( new BinomialDistribution( this.rng, alpha.intValue(),
					beta.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Long> getZipf( final Number numberOfElements,
			final Number exponent )
		{
			return wrap( new ZipfDistribution( this.rng,
					numberOfElements.intValue(), exponent.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getBeta( final Number alpha,
			final Number beta )
		{
			return wrap( new BetaDistribution( this.rng, alpha.doubleValue(),
					beta.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getCauchy( final Number median,
			final Number scale )
		{
			return wrap( new CauchyDistribution( this.rng, median.doubleValue(),
					scale.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double>
			getChiSquared( final Number degreesOfFreedom )
		{
			return wrap( new ChiSquaredDistribution( this.rng,
					degreesOfFreedom.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getExponential( final Number mean )
		{
			return wrap( new ExponentialDistribution( this.rng,
					mean.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getF(
			final Number numeratorDegreesOfFreedom,
			final Number denominatorDegreesOfFreedom )
		{
			return wrap( new FDistribution( this.rng,
					numeratorDegreesOfFreedom.doubleValue(),
					denominatorDegreesOfFreedom.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getGamma( final Number shape,
			final Number scale )
		{
			return wrap( new GammaDistribution( this.rng, shape.doubleValue(),
					scale.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getLevy( final Number mu,
			final Number c )
		{
			return wrap( new LevyDistribution( this.rng, mu.doubleValue(),
					c.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getLogNormal( final Number scale,
			final Number shape )
		{
			return wrap( new LogNormalDistribution( this.rng,
					scale.doubleValue(), shape.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getNormal( final Number mean,
			final Number sd )
		{
			return wrap( new NormalDistribution( this.rng, mean.doubleValue(),
					sd.doubleValue() ) );
		}

		@Override
		public RandomDistribution<double[]> getMultivariateNormal(
			final double[] means, final double[][] covariances )
		{
			return wrap( new MultivariateNormalDistribution( this.rng, means,
					covariances ) );
		}

		@Override
		public RandomDistribution<Double> getPareto( final Number scale,
			final Number shape )
		{
			return wrap( new ParetoDistribution( this.rng, scale.doubleValue(),
					shape.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getT( final Number degreesOfFreedom )
		{
			return wrap( new TDistribution( this.rng,
					degreesOfFreedom.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getTriangular( final Number a,
			final Number b, final Number c )
		{
			return wrap( new TriangularDistribution( this.rng, a.doubleValue(),
					b.doubleValue(), c.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getUniformReal( final Number lower,
			final Number upper )
		{
			return wrap( new UniformRealDistribution( this.rng,
					lower.doubleValue(), upper.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getWeibull( final Number alpha,
			final Number beta )
		{
			return wrap( new WeibullDistribution( this.rng, alpha.doubleValue(),
					beta.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Long> getUniformInteger( final Number lower,
			final Number upper )
		{
			return wrap( new UniformIntegerDistribution( this.rng,
					lower.intValue(), upper.intValue() ) );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <T> RandomDistribution<T>
			getUniformEnumerated( final T... values )
		{
			return wrap( new EnumeratedDistribution<T>( this.rng,
					toPropabilityMassFunction( values ) ) );
		}
	}
}