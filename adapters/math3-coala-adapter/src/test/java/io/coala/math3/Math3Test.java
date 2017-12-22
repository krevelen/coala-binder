/* $Id: 63dbfe3c7b33783ed0e0de485d21717c0a276a0e $
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.measure.Quantity;

import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Mutable;
import org.aeonbits.owner.util.Collections;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.guice4.Guice4LocalBinder;
import io.coala.log.LogUtil;
import io.coala.math.DecimalUtil;
import io.coala.math.MatrixBuilder;
import io.coala.random.DistributionParser;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.PseudoRandom;
import io.reactivex.Observable;

/**
 * {@link Math3Test}
 * 
 * @version $Id: 63dbfe3c7b33783ed0e0de485d21717c0a276a0e $
 * @author Rick van Krevelen
 */
public class Math3Test
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( Math3Test.class );

	enum MyValue
	{
		v1, v2, v3;
	}

	@SuppressWarnings( "rawtypes" )
	@Test
	public void testParser() throws Exception
	{
		final LocalBinder binder = Guice4LocalBinder.of( LocalConfig.builder()
				.withId( "testMath3" ) // use fixed id for reproducible seeding
				.withProvider( PseudoRandom.Factory.class,
						Math3PseudoRandom.MersenneTwisterFactory.class )
				.withProvider( ProbabilityDistribution.Factory.class,
						Math3ProbabilityDistribution.Factory.class )
				.build() );
		LOG.info( "Starting Math3 parser test, binder: {}", binder );
		final DistributionParser parser = binder
				.inject( DistributionParser.class );

		final ProbabilityDistribution<Quantity> dist2 = parser
				.parse( "const(2.01 day)", Quantity.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw constant {}: {}", i, dist2.draw() );

		final ProbabilityDistribution<BigDecimal> dist1 = parser
				.parse( "uniform(5.1;6.2)", BigDecimal.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw BigDecimal {}: {}", i, dist1.draw() );

//		LOG.trace( "amount {}", Amount.valueOf( 3.2, Unit.ONE ) );
		final ProbabilityDistribution<Quantity> dist0 = parser
				.parse( "uniform-enum(2 ;3 )", Quantity.class ); //± 1.1E-16
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw Object {}: {}", i, dist0.draw() );

		final ProbabilityDistribution<MyValue> dist3 = parser
				.parse( "uniform-enum()", MyValue.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw MyValue {}: {}", i, dist3.draw() );

		final ProbabilityDistribution<MyValue> dist4 = parser
				.parse( "uniform-enum( v1; v3 )", MyValue.class );
		for( int i = 0; i < 10; i++ )
			LOG.trace( "draw MyValue subset {}: {}", i, dist4.draw() );
	}

	public interface SIRConfig extends Mutable
	{
		@DefaultValue( "" + 12 )
		double reproduction();

		@DefaultValue( "" + 14 )
		double recovery();

		@Separator( "," )
		@DefaultValue( "99999,1,0" )
		long[] population();

		@DefaultValue( "0,50" )
		double[] t();

		Long seed();

		default Observable<Map.Entry<Double, double[]>>
			deterministic( final Supplier<FirstOrderIntegrator> integrators )
		{
			return Observable.create( sub ->
			{
				final double gamma = 1. / recovery();
				final double beta = gamma * reproduction();
				final double[] y0 = Arrays.stream( population() )
						.mapToDouble( n -> n ).toArray();
				final double[] t = t();

				try
				{
					final FirstOrderIntegrator integrator = integrators.get();

					integrator.addStepHandler( new StepHandler()
					{
						@Override
						public void init( final double t0, final double[] y0,
							final double t )
						{
							// initial values
							sub.onNext( Collections.entry( t0, y0 ) );
						}

						@Override
						public void handleStep(
							final StepInterpolator interpolator,
							final boolean isLast )
							throws MaxCountExceededException
						{
							final Map.Entry<Double, double[]> yt = Collections
									.entry( interpolator.getInterpolatedTime(),
											interpolator
													.getInterpolatedState() );
							sub.onNext( yt );
							if( isLast ) sub.onComplete();
						}
					} );

					integrator.integrate( new FirstOrderDifferentialEquations()
					{
						@Override
						public int getDimension()
						{
							return y0.length;
						}

						@Override
						public void computeDerivatives( final double t,
							final double[] y, final double[] yp )
						{
							// SIR terms (flow rates)
							final double n = y[0] + y[1] + y[2],
									flow_si = beta * y[0] * y[1] / n,
									flow_ir = gamma * y[1];

							yp[0] = -flow_si;
							yp[1] = flow_si - flow_ir;
							yp[2] = flow_ir;
						}
					}, t[0], y0, t[1], y0 );
				} catch( final Exception e )
				{
					sub.onError( e );
				}
			} );
		}

		// TODO provide Gillespie method as function : rates[i] -> (dt,i)
		default Observable<Entry<Double, long[]>> stochasticGillespie()
		{
			return Observable.create( sub ->
			{
				final double gamma = 1. / recovery();
				final double beta = gamma * reproduction();
				final long[] y = population();
				final double[] T = t();

				final Long seed = seed();
				final RandomGenerator rng = new MersenneTwister(
						seed == null ? System.currentTimeMillis() : seed );

				// initial values
				sub.onNext( Collections.entry( T[0], y ) );
				for( double t = T[0]; t < T[1]; )
				{
					// SIR terms (flow rates)
					final double n = y[0] + y[1] + y[2],
							flow_si = beta * y[0] * y[1] / n,
							flow_ir = gamma * y[1],
							flow_sum = flow_si + flow_ir;

					// Gillespie two-step iteration or "tau" method
					// 1. advance time to next event
					t += new ExponentialDistribution( rng, 1d / flow_sum )
							.sample();
					// 2. determine event (s->i, i->r, ...)
					if( rng.nextDouble() < flow_si / flow_sum )
					{
						y[0]--; // from S
						y[1]++; // to I
					} else
					{
						y[1]--; // from I
						y[2]++; // to R
					}
					sub.onNext( Collections.entry( t, y ) );
				}
				sub.onComplete();
			} );
		}

		default Observable<Entry<Double, long[]>> stochasticResistance()
		{
			return Observable.create( sub ->
			{
//				// [X,Y,R] (Sellke, 1983)
//				final long[] y = population();
//				// rho/removal hazard rate (Sellke, 1983)
//				final double gamma = 1. / recovery();
//				// resistance/maximum accumulated "exposure to infection" (Sellke, 1983)
//				final double beta = gamma * reproduction();
//				final double beta_per_N = beta / (y[0] + y[1] + y[2]);
//				final double[] T = t();
//
//				final Long seed = seed();
//				final RandomGenerator rng = new MersenneTwister(
//						seed == null ? System.currentTimeMillis() : seed );
//
//				final ExponentialDistribution resistanceDist = new ExponentialDistribution(
//						rng, 1 );
//
//				final ExponentialDistribution recoveryDist = new ExponentialDistribution(
//						rng, gamma / beta_per_N );
//
//				final TreeMap<Double, Double> resistance = LongStream.range(0,y[0]).mapToDouble(i->resistanceDist.sample() ).sorted();
//				for( int i = 0; i < y[0]; i++ )
//
//					// initial values
//					sub.onNext( Collections.entry( T[0], y ) );
//
//				for( double t = T[0]; t < T[1]; )
//				{
//					// SIR terms (flow rates)
//					final double n = y[0] + y[1] + y[2],
//							flow_si = beta * y[0] * y[1] / n,
//							flow_ir = gamma * y[1],
//							flow_sum = flow_si + flow_ir;
//
//					// Gillespie two-step iteration or "tau" method
//					// 1. advance time to next event
//					t += new ExponentialDistribution( rng, 1d / flow_sum )
//							.sample();
//					// 2. determine event (s->i, i->r, ...)
//					if( rng.nextDouble() < flow_si / flow_sum )
//					{
//						y[0]--; // from S
//						y[1]++; // to I
//					} else
//					{
//						y[1]--; // from I
//						y[2]++; // to R
//					}
//					sub.onNext( Collections.entry( t, y ) );
//				}
				sub.onComplete();
			} );
		}
	}

	@Test
	public void testSIR()
	{
		final SIRConfig conf = ConfigFactory.create( SIRConfig.class );
		final double[] T = conf.t();
		final long[] pop = conf.population();

		LOG.info( "Starting with SIR: {} over T={}", pop, T );

		final int t = (int) T[0], n = (int) T[1];
		final MatrixBuilder sample = MatrixBuilder
				.sparse( n - t + 2, pop.length * 3 ).label( "Results" )
				.labelColumns( "~dS.dt", "~dI.dt", "~dR.dt", "S_gil(t)",
						"I_gil(t)", "R_gil(t)", "S_res(t)", "I_res(t)",
						"R_res(t)" )
				.labelRows( i -> "t=" + i );

		// the Dormand-Prince (embedded Runge-Kutta) ODE integrator
		// see https://www.wikiwand.com/en/Runge%E2%80%93Kutta_methods
		conf.deterministic( () -> new DormandPrince853Integrator( 1.0E-8, 10,
				1.0E-20, 1.0E-20 ) )
				.filter( yt -> DecimalUtil.ceil( yt.getKey() ).longValue() < n )
				.forEach( yt -> sample.withContent(
						Arrays.stream( yt.getValue() ).mapToObj( d -> d ),
						DecimalUtil.ceil( yt.getKey() ).longValue(), 0 ) );

		// see https://www.wikiwand.com/en/Gillespie_algorithm
		conf.stochasticGillespie()
				.filter( yt -> DecimalUtil.ceil( yt.getKey() ).longValue() < n )
				.forEach( yt -> sample.withContent(
						Arrays.stream( yt.getValue() ).mapToObj( d -> d ),
						DecimalUtil.ceil( yt.getKey() ).longValue(),
						pop.length ) );

		LOG.trace( "result: \n{}", sample.build().toString() );
		LOG.info( "Complete" );
	}
}
