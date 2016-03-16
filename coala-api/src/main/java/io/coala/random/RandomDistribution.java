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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.coala.exception.x.ExceptionBuilder;
import io.coala.json.x.JsonUtil;

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
					params.add( ProbabilityMass
							.of( valueOf( argType, valuePair ), 1 ) );
				} else
					params.add( ProbabilityMass.of(
							valueOf( argType, valueWeights[0] ),
							new BigDecimal( valueWeights[1] ) ) );
			}
			if( params.isEmpty() && argType.isEnum() )
				for( P constant : argType.getEnumConstants() )
				params.add( ProbabilityMass.of( constant, 1 ) );
			return parser.parse( m.group( "dist" ), params );
		}

		interface Converter<T>
		{
			T valueOf( String value ) throws Exception;
		}

		public static <T> Converter<T> of( final Class<T> valueType,
			final Method method )
		{
			return new Converter<T>()
			{
				@Override
				public T valueOf( final String value ) throws Exception
				{
					return valueType.cast( method.invoke( valueType, value ) );
				}
			};
		}

		public static <T> Converter<T> of( final Class<T> valueType,
			final Constructor<T> constructor )
		{
			return new Converter<T>()
			{
				@Override
				public T valueOf( final String value ) throws Exception
				{
					return valueType.cast( constructor.newInstance( value ) );
				}
			};
		}

		public static <T> Converter<T> of( final Class<T> valueType,
			final ObjectMapper om )
		{
			JsonUtil.checkRegistered( om, valueType );
			return new Converter<T>()
			{
				@Override
				public T valueOf( final String value ) throws Exception
				{
					return om.readValue( "\"" + value + "\"", valueType );
				}
			};
		}

		private static final Map<Class<?>, Converter<?>> converterCache = new HashMap<>();

		public static Method getMethod( final Class<?> valueType,
			final String name, final Class<?>... argTypes ) throws Exception
		{
			final Method result = valueType.getMethod( name, argTypes );
			if( !Modifier.isStatic( result.getModifiers() ) )
				throw new IllegalAccessException( name + "("
						+ Arrays.asList( argTypes ) + ") not static" );
			if( !result.isAccessible() ) result.setAccessible( true );
			return result;
		}

		public static <T> Constructor<T> getConstructor(
			final Class<T> valueType, final Class<?>... argTypes )
			throws Exception
		{
			final Constructor<T> result = valueType.getConstructor( argTypes );
			if( !result.isAccessible() ) result.setAccessible( true );
			return result;
		}

		public static <T> Converter<T> getConverter( final Class<T> valueType )
		{
			@SuppressWarnings( "unchecked" )
			Converter<T> result = (Converter<T>) converterCache
					.get( valueType );
			if( result != null ) return result;

			try
			{
				if( valueType.isInterface() )
					result = of( valueType, JsonUtil.getJOM() );
				else
					result = of( valueType,
							getMethod( valueType, "valueOf", String.class ) );
			} catch( final Exception e )
			{
				try
				{
					result = of( valueType, getMethod( valueType, "valueOf",
							CharSequence.class ) );
				} catch( final Exception e1 )
				{
					try
					{
						result = of( valueType,
								getConstructor( valueType, String.class ) );
					} catch( final Exception e2 )
					{
						try
						{
							result = of( valueType, getConstructor( valueType,
									CharSequence.class ) );
						} catch( final Exception e3 )
						{
							throw ExceptionBuilder.unchecked( e3,
									"Problem parsing type: %s", valueType )
									.build();
						}
					}
				}
			}
			converterCache.put( valueType, result );
			return result;
		}

		public static <T> T valueOf( final Class<T> valueType,
			final String value )
		{
			final Converter<T> converter = getConverter( valueType );
			try
			{
				return converter.valueOf( value );
			} catch( final Exception e )
			{
				try
				{
					return converter.valueOf( value.trim() );
				} catch( final Exception e1 )
				{
					throw ExceptionBuilder.unchecked( e1,
							"Problem parsing type: %s from (trimmed) value: %s",
							valueType, value ).build();
				}
			}
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

			private final RandomNumberStream stream;

			/**
			 * {@link Simple} constructor will generate only constant
			 * distributions of the first parsed argument
			 */
			public Simple()
			{
				this( null, null );
			}

			public Simple( final Factory factory,
				final RandomNumberStream stream )
			{
				this.factory = factory;
				this.stream = stream;
			}

			@Override
			public Factory getFactory()
			{
				return this.factory;
			}

			@Override
			public RandomNumberStream getStream()
			{
				return this.stream;
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
							.getEnumerated( getStream(), args );

				case "geom":
				case "geometric":
					return (RandomDistribution<T>) getFactory().getGeometric(
							getStream(), (Number) args.get( 0 ).getValue() );

				case "hypergeom":
				case "hypergeometric":
					return (RandomDistribution<T>) getFactory()
							.getHypergeometric( getStream(),
									(Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue(),
									(Number) args.get( 2 ).getValue() );

				case "pascal":
					return (RandomDistribution<T>) getFactory().getPascal(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "poisson":
					return (RandomDistribution<T>) getFactory().getPoisson(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "zipf":
					return (RandomDistribution<T>) getFactory().getZipf(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "beta":
					return (RandomDistribution<T>) getFactory().getBeta(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "cauchy":
					return (RandomDistribution<T>) getFactory().getCauchy(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "chi":
				case "chisquared": // TODO = Pearson?
					return (RandomDistribution<T>) getFactory().getChiSquared(
							getStream(), (Number) args.get( 0 ).getValue() );

				case "exp":
				case "exponent":
				case "exponential":
					return (RandomDistribution<T>) getFactory().getExponential(
							getStream(), (Number) args.get( 0 ).getValue() );

				case "f":
					return (RandomDistribution<T>) getFactory().getF(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "gamma":
					return (RandomDistribution<T>) getFactory().getGamma(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "levy":
					return (RandomDistribution<T>) getFactory().getLevy(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "lognormal":
					return (RandomDistribution<T>) getFactory().getLogNormal(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "normal":
					return (RandomDistribution<T>) getFactory().getNormal(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "pareto":
					return (RandomDistribution<T>) getFactory().getPareto(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "t":
					return (RandomDistribution<T>) getFactory().getT(
							getStream(), (Number) args.get( 0 ).getValue() );

				case "tria":
				case "triangular":
					return (RandomDistribution<T>) getFactory().getTriangular(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue(),
							(Number) args.get( 2 ).getValue() );

				case "uniformdiscrete":
				case "uniforminteger":
					return (RandomDistribution<T>) getFactory()
							.getUniformInteger( getStream(),
									(Number) args.get( 0 ).getValue(),
									(Number) args.get( 1 ).getValue() );

				case "uniformreal":
				case "uniformcontinuous":
					return (RandomDistribution<T>) getFactory().getUniformReal(
							getStream(), (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

				case "uniform":
				case "uniformenum":
				case "uniformenumerated":
					final List<T> values = new ArrayList<>();
					for( ProbabilityMass<V, ?> pair : args )
						values.add( (T) pair.getValue() );
					return (RandomDistribution<T>) getFactory()
							.getUniformEnumerated( getStream(),
									values.toArray() );

				case "weibull":
					return (RandomDistribution<T>) getFactory().getWeibull(
							getStream(), (Number) args.get( 0 ).getValue(),
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
			List<ProbabilityMass<T, ?>> probabilities );

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
			Number populationSize, Number numberOfSuccesses,
			Number sampleSize );

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

		// TODO scrap or wrap (double <-> Number) using closures?
		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param means
		 * @param covariances
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<double[]> getMultivariateNormal(
			RandomNumberStream rng, double[] means, double[][] covariances );

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
		RandomDistribution<Long> getUniformInteger( RandomNumberStream rng,
			Number lower, Number upper );

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
		 * @param values
		 * @return the {@link RandomDistribution}
		 */
		@SuppressWarnings( "unchecked" )
		<T> RandomDistribution<T> getUniformEnumerated( RandomNumberStream rng,
			T... values );

		/**
		 * @param rng the {@link RandomNumberStream}
		 * @param alpha
		 * @param beta
		 * @return the {@link RandomDistribution}
		 */
		RandomDistribution<Double> getWeibull( RandomNumberStream rng,
			Number alpha, Number beta );

		// TODO RandomDistribution<Double> getBernoulli();

		// TODO RandomDistribution<Double> getErlang();

		// TODO RandomDistribution<Double> getPearson5();

		// TODO RandomDistribution<Double> getPearson6();

		// TODO RandomDistribution<Double> getNegativeBionomial();
	}

}