/* $Id: 6e0f40aa15b543af93929baf41c2959e5f96314b $
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

import io.coala.exception.Thrower;

/**
 * The basic {@link DistributionFactory} only supports deterministic,
 * categorical, uniform, empirical, triangular, bernoulli and gaussian/normal
 * distributions
 * 
 * @version $Id: 6e0f40aa15b543af93929baf41c2959e5f96314b $
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
	public ProbabilityDistribution<Long> createBinomial( final Number trials,
		final Number p )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "binomial" );
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

	@Override
	public ProbabilityDistribution<Double> createWeibull( final Number alpha,
		final Number beta )
	{
		return Thrower.throwNew( UnsupportedOperationException::new,
				() -> "weibull" );
	}
}