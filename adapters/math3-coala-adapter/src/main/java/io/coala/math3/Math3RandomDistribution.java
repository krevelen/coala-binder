/* $Id: d2aa0ffb71e06dcaa7fa7a232e0d266a15ac5718 $
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
import org.apache.commons.math3.util.Pair;

import io.coala.random.RandomDistribution;
import io.coala.random.RandomNumberStream;

/**
 * {@link Math3RandomDistribution} creates {@link RandomDistribution}s
 * implemented by Apache's commons-math3
 * 
 * @version $Id$
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
		final List<ProbabilityMass<T, Number>> probabilities )
	{
		final List<Pair<T, Double>> pmf = new ArrayList<>();
		if( probabilities != null )
			for( ProbabilityMass<T, Number> p : probabilities )
			pmf.add( Pair.create( p.getValue(), p.getMass().doubleValue() ) );
		return pmf;
	}

	/**
	 * {@link Factory} creates {@link RandomDistribution}s implemented by
	 * Apache's commons-math3 toolkit
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class Factory implements RandomDistribution.Factory
	{
		@Override
		public <T> RandomDistribution<T> getConstant( final T constant )
		{
			return RandomDistribution.Util.asConstant( constant );
		}

		@Override
		public RandomDistribution<Long> getBinomial(
			final RandomNumberStream rng, final Number trials, final Number p )
		{
			return wrap( new BinomialDistribution(
					Math3RandomNumberStream.unwrap( rng ), trials.intValue(),
					p.doubleValue() ) );
		}

		@Override
		public <T> RandomDistribution<T> getEnumerated(
			final RandomNumberStream rng,
			final List<ProbabilityMass<T, Number>> probabilities )
		{
			return wrap( new EnumeratedDistribution<T>(
					Math3RandomNumberStream.unwrap( rng ),
					toPropabilityMassFunction( probabilities ) ) );
		}

		@Override
		public RandomDistribution<Long>
			getGeometric( final RandomNumberStream rng, final Number p )
		{
			return wrap( new GeometricDistribution(
					Math3RandomNumberStream.unwrap( rng ), p.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Long> getHypergeometric(
			final RandomNumberStream rng, final Number populationSize,
			final Number numberOfSuccesses, final Number sampleSize )
		{
			return wrap( new HypergeometricDistribution(
					Math3RandomNumberStream.unwrap( rng ),
					populationSize.intValue(), numberOfSuccesses.intValue(),
					sampleSize.intValue() ) );
		}

		@Override
		public RandomDistribution<Long> getPascal( final RandomNumberStream rng,
			final Number r, final Number p )
		{
			return wrap( new PascalDistribution(
					Math3RandomNumberStream.unwrap( rng ), r.intValue(),
					p.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Long> getPoisson(
			final RandomNumberStream rng, final Number alpha,
			final Number beta )
		{
			return wrap( new BinomialDistribution(
					Math3RandomNumberStream.unwrap( rng ), alpha.intValue(),
					beta.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Long> getUniformInteger(
			final RandomNumberStream rng, final Number lower,
			final Number upper )
		{
			return wrap( new UniformIntegerDistribution(
					Math3RandomNumberStream.unwrap( rng ), lower.intValue(),
					upper.intValue() ) );
		}

		@Override
		public RandomDistribution<Long> getZipf( final RandomNumberStream rng,
			final Number numberOfElements, final Number exponent )
		{
			return wrap( new ZipfDistribution(
					Math3RandomNumberStream.unwrap( rng ),
					numberOfElements.intValue(), exponent.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getBeta( final RandomNumberStream rng,
			final Number alpha, final Number beta )
		{
			return wrap(
					new BetaDistribution( Math3RandomNumberStream.unwrap( rng ),
							alpha.doubleValue(), beta.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getCauchy(
			final RandomNumberStream rng, final Number median,
			final Number scale )
		{
			return wrap( new CauchyDistribution(
					Math3RandomNumberStream.unwrap( rng ), median.doubleValue(),
					scale.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getChiSquared(
			final RandomNumberStream rng, final Number degreesOfFreedom )
		{
			return wrap( new ChiSquaredDistribution(
					Math3RandomNumberStream.unwrap( rng ),
					degreesOfFreedom.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double>
			getExponential( final RandomNumberStream rng, final Number mean )
		{
			return wrap( new ExponentialDistribution(
					Math3RandomNumberStream.unwrap( rng ),
					mean.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getF( final RandomNumberStream rng,
			final Number numeratorDegreesOfFreedom,
			final Number denominatorDegreesOfFreedom )
		{
			return wrap(
					new FDistribution( Math3RandomNumberStream.unwrap( rng ),
							numeratorDegreesOfFreedom.doubleValue(),
							denominatorDegreesOfFreedom.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getGamma(
			final RandomNumberStream rng, final Number shape,
			final Number scale )
		{
			return wrap( new GammaDistribution(
					Math3RandomNumberStream.unwrap( rng ), shape.doubleValue(),
					scale.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getLevy( final RandomNumberStream rng,
			final Number mu, final Number c )
		{
			return wrap(
					new LevyDistribution( Math3RandomNumberStream.unwrap( rng ),
							mu.doubleValue(), c.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getLogNormal(
			final RandomNumberStream rng, final Number scale,
			final Number shape )
		{
			return wrap( new LogNormalDistribution(
					Math3RandomNumberStream.unwrap( rng ), scale.doubleValue(),
					shape.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getNormal(
			final RandomNumberStream rng, final Number mean, final Number sd )
		{
			return wrap( new NormalDistribution(
					Math3RandomNumberStream.unwrap( rng ), mean.doubleValue(),
					sd.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getPareto(
			final RandomNumberStream rng, final Number scale,
			final Number shape )
		{
			return wrap( new ParetoDistribution(
					Math3RandomNumberStream.unwrap( rng ), scale.doubleValue(),
					shape.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getT( final RandomNumberStream rng,
			final Number degreesOfFreedom )
		{
			return wrap(
					new TDistribution( Math3RandomNumberStream.unwrap( rng ),
							degreesOfFreedom.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getTriangular(
			final RandomNumberStream rng, final Number a, final Number b,
			final Number c )
		{
			return wrap( new TriangularDistribution(
					Math3RandomNumberStream.unwrap( rng ), a.doubleValue(),
					b.doubleValue(), c.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getUniformReal(
			final RandomNumberStream rng, final Number lower,
			final Number upper )
		{
			return wrap( new UniformRealDistribution(
					Math3RandomNumberStream.unwrap( rng ), lower.doubleValue(),
					upper.doubleValue() ) );
		}

		@Override
		public RandomDistribution<Double> getWeibull(
			final RandomNumberStream rng, final Number alpha,
			final Number beta )
		{
			return wrap( new WeibullDistribution(
					Math3RandomNumberStream.unwrap( rng ), alpha.doubleValue(),
					beta.doubleValue() ) );
		}

		@Override
		public RandomDistribution<double[]> getMultivariateNormal(
			final RandomNumberStream rng, final double[] means,
			final double[][] covariances )
		{
			return wrap( new MultivariateNormalDistribution(
					Math3RandomNumberStream.unwrap( rng ), means,
					covariances ) );
		}
	}
}