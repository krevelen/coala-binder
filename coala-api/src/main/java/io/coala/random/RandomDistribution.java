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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import io.coala.exception.x.ExceptionBuilder;

/**
 * {@link RandomDistribution}
 * 
 * @version $Revision: 332 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 *
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
							|| (value instanceof BigDecimal
									&& NumberUtil.isExact( (BigDecimal) value ))
											? Amount.valueOf( value.longValue(),
													unit )
											: Amount.valueOf(
													value.doubleValue(), unit );
				}
			};
		}

		private static final Pattern distPattern = Pattern
				.compile( "^(?<dist>[^\\(]+)\\((?<params>[^)]*)\\)$" );

		public static <T> RandomDistribution<T> valueOf( final String dist,
			final Class<T> valueType )
		{
			final Matcher m = distPattern.matcher( dist.trim() );
			if( !m.find() ) throw ExceptionBuilder
					.unchecked( "Problem parsing distribution: %s", dist )
					.build();
			final String name = m.group( "dist" ).toLowerCase();
			switch( name )
			{
			case "const":
			case "constant":
				return Util.asConstant(
						valueOf( valueType, m.group( "params" ) ) );
			}
			throw ExceptionBuilder.unchecked( "Unknown distribution symbol: %s",
					m.group( "dist" ) ).build();
		}

		public static <T> T valueOf( final Class<T> paramType,
			final String value )
		{
			try
			{
				return paramType
						.cast( paramType.getMethod( "valueOf", String.class )
								.invoke( paramType, value ) );
			} catch( final Exception e )
			{
				try
				{
					return paramType.cast(
							paramType.getMethod( "valueOf", CharSequence.class )
									.invoke( paramType, value ) );
				} catch( final Exception e1 )
				{
					throw ExceptionBuilder
							.unchecked( e1, "Problem parsing value: %s", value )
							.build();
				}
			}
		}
	}

	/**
	 * {@link Factory} generates {@link RandomDistribution}s of specific shapes
	 */
	interface Factory
	{

		/**
		 * @param constant the constant to be returned on each draw
		 * @return the {@link RandomDistribution}
		 */
		<T> RandomDistribution<T> getConstant( T constant );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param trials number of consecutive successes
		 * @param p probability of a single success
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Long> getBinomial( RandomNumberStream rng,
			Number trials, Number p );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param probabilities the probability mass function enumerated for
		 *            each value
		 * @return the {@link RandomDistribution}
		 */
		<T> RandomDistribution<T> getEnumerated( RandomNumberStream rng,
			List<ProbabilityMass<T, Number>> probabilities );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param probabilities the probability mass function enumerated for
		 *            each value
		 * @return the {@link RandomDistribution}
		 */
//		<N extends Number> RandomDistribution<N> getEnumeratedNumber(
//			RandomNumberStream rng,
//			List<ProbabilityMass<N, Number>> probabilities );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param p
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Long> getGeometric( RandomNumberStream rng,
			Number p );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param populationSize
		 * @param numberOfSuccesses
		 * @param sampleSize
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Long> getHypergeometric( RandomNumberStream rng,
			Number populationSize, final Number numberOfSuccesses,
			final Number sampleSize );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param r
		 * @param p
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Long> getPascal( RandomNumberStream rng, Number r,
			Number p );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param alpha
		 * @param beta
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Long> getPoisson( RandomNumberStream rng,
			Number alpha, Number beta );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param lower
		 * @param upper
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Long> getUniformInteger( RandomNumberStream rng,
			Number lower, Number upper );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param values
		 * @return the {@link RandomDistribution}
		 */
		@SuppressWarnings( "unchecked" )
		<T> RandomDistribution<T> getUniformEnum( RandomNumberStream rng,
			T... values );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param alpha
		 * @param beta
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Long> getZipf( RandomNumberStream rng,
			Number numberOfElements, Number exponent );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param alpha
		 * @param beta
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getBeta( RandomNumberStream rng,
			Number alpha, Number beta );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param median
		 * @param scale
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getCauchy( RandomNumberStream rng,
			Number median, Number scale );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param degreesOfFreedom
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getChiSquared( RandomNumberStream rng,
			Number degreesOfFreedom );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param mean
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getExponential( RandomNumberStream rng,
			Number mean );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param numeratorDegreesOfFreedom
		 * @param denominatorDegreesOfFreedom
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getF( RandomNumberStream rng,
			Number numeratorDegreesOfFreedom,
			Number denominatorDegreesOfFreedom );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param shape
		 * @param scale
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getGamma( RandomNumberStream rng,
			Number shape, Number scale );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param mu
		 * @param c
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getLevy( RandomNumberStream rng, Number mu,
			Number c );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param scale
		 * @param shape
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getLogNormal( RandomNumberStream rng,
			Number scale, Number shape );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param mean
		 * @param sd the standard deviation
		 * @return the Gaussian / Normal {@link RandomDistribution}
		 */
		RandomDistribution<Double> getNormal( RandomNumberStream rng,
			Number mean, Number sd );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param scale
		 * @param shape
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getPareto( RandomNumberStream rng,
			Number scale, Number shape );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param denominatorDegreesOfFreedom
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getT( RandomNumberStream rng,
			Number denominatorDegreesOfFreedom );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param a
		 * @param b
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getTriangular( RandomNumberStream rng,
			Number a, Number b, Number c );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param lower
		 * @param upper
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getUniformReal( RandomNumberStream rng,
			Number lower, Number upper );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param alpha
		 * @param beta
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getWeibull( RandomNumberStream rng,
			Number alpha, Number beta );

		// TODO scrap or wrap (double <-> Number) using closures?
		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param means
		 * @param covariances
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<double[]> getMultivariateNormal(
			RandomNumberStream rng, double[] means, double[][] covariances );

		// TODO RandomDistribution<Double> getBernoulli();

		// TODO RandomDistribution<Double> getErlang();

		// TODO RandomDistribution<Double> getPearson5();

		// TODO RandomDistribution<Double> getPearson6();

		// TODO RandomDistribution<Double> getNegativeBionomial();
	}

}