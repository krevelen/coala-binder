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
import java.util.stream.StreamSupport;

import io.coala.exception.Thrower;
import io.coala.math.WeightedValue;
import io.coala.util.DecimalUtil;

public class DistributionFactory implements ProbabilityDistribution.Factory
{

	private static DistributionFactory INSTANCE = null;

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
		return Thrower.throwNew( UnsupportedOperationException.class,
				"binomial" );
	}

	@Override
	public <T, WV extends WeightedValue<T>> ProbabilityDistribution<T>
		createCategorical( final Iterable<WV> probabilities )
	{
		final BigDecimal total = StreamSupport
				.stream( probabilities.spliterator(), true )
				.map( wv -> DecimalUtil
						.valueOf( wv.getWeight().doubleValue() ) )
				.reduce( BigDecimal::add ).orElse( BigDecimal.ZERO );
		return () ->
		{
			BigDecimal sum = BigDecimal.ZERO,
					stop = total.multiply( getStream().nextBigDecimal() );
			for( WV wv : probabilities )
			{
				sum = sum.add( DecimalUtil.valueOf( wv.getWeight() ) );
				if( sum.compareTo( stop ) < 0 ) continue;
				return wv.getValue();
			}
			return null;
		};
	}

	@Override
	public ProbabilityDistribution<Long> createGeometric( final Number p )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"geometric" );
	}

	@Override
	public ProbabilityDistribution<Long> createHypergeometric(
		final Number populationSize, final Number numberOfSuccesses,
		final Number sampleSize )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"hypergeometric" );
	}

	@Override
	public ProbabilityDistribution<Long> createPascal( final Number r,
		final Number p )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"pascal" );
	}

	@Override
	public ProbabilityDistribution<Long> createPoisson( final Number mean )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"poisson" );
	}

	@Override
	public ProbabilityDistribution<Long>
		createZipf( final Number numberOfElements, final Number exponent )
	{
		return Thrower.throwNew( UnsupportedOperationException.class, "zipf" );
	}

	@Override
	public ProbabilityDistribution<Double> createBeta( final Number alpha,
		final Number beta )
	{
		return Thrower.throwNew( UnsupportedOperationException.class, "beta" );
	}

	@Override
	public ProbabilityDistribution<Double> createCauchy( final Number median,
		final Number scale )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"cauchy" );
	}

	@Override
	public ProbabilityDistribution<Double>
		createChiSquared( final Number degreesOfFreedom )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"chi-squared" );
	}

	@Override
	public ProbabilityDistribution<Double>
		createExponential( final Number mean )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"exponential" );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T extends Number> ProbabilityDistribution<Double>
		createEmpirical( final T... values )
	{
		return () -> values[getStream().nextInt( values.length )].doubleValue();
	}

	@Override
	public ProbabilityDistribution<Double> createF(
		final Number numeratorDegreesOfFreedom,
		final Number denominatorDegreesOfFreedom )
	{
		return Thrower.throwNew( UnsupportedOperationException.class, "f" );
	}

	@Override
	public ProbabilityDistribution<Double> createGamma( final Number shape,
		final Number scale )
	{
		return Thrower.throwNew( UnsupportedOperationException.class, "gamma" );
	}

	@Override
	public ProbabilityDistribution<Double> createLevy( final Number mu,
		final Number c )
	{
		return Thrower.throwNew( UnsupportedOperationException.class, "levy" );
	}

	@Override
	public ProbabilityDistribution<Double> createLogNormal( final Number scale,
		final Number shape )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"log-normal" );
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
		return Thrower.throwNew( UnsupportedOperationException.class,
				"multinormal" );
	}

	@Override
	public ProbabilityDistribution<Double> createPareto( final Number scale,
		final Number shape )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"pareto" );
	}

	@Override
	public ProbabilityDistribution<Double>
		createT( final Number degreesOfFreedom )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"student-t" );
	}

	@Override
	public ProbabilityDistribution<Double> createTriangular( final Number min,
		final Number max, final Number mode )
	{
		final double lower = mode.doubleValue() - min.doubleValue();
		final double upper = max.doubleValue() - mode.doubleValue();
		final double total = max.doubleValue() - min.doubleValue();
		return () ->
		{
			final double p = getStream().nextDouble();
			return p < lower / total
					? min.doubleValue() + StrictMath.sqrt( p * lower * total )
					: max.doubleValue()
							- StrictMath.sqrt( (1 - p) * upper * total );
		};
	}

	@Override
	public ProbabilityDistribution<Long>
		createUniformDiscrete( final Number min, final Number max )
	{
		return () -> (long) min.intValue()
				+ getStream().nextInt( max.intValue() - min.intValue() );
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
	public <T> ProbabilityDistribution<T>
		createUniformCategorical( final T... values )
	{
		return () -> values[getStream().nextInt( values.length )];
	}

	@Override
	public ProbabilityDistribution<Double> createWeibull( final Number alpha,
		final Number beta )
	{
		return Thrower.throwNew( UnsupportedOperationException.class,
				"weibull" );
	}
}