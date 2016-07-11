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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Provider;
import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.apache.logging.log4j.Logger;
import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionFactory;
import io.coala.log.LogUtil;
import io.coala.math.FrequencyDistribution;
import io.coala.math.MeasureUtil;
import io.coala.math.Range;
import io.coala.math.WeightedValue;
import io.coala.util.InstanceParser;
import io.coala.util.Instantiator;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * {@link ProbabilityDistribution} is similar to a {@link javax.inject.Provider}
 * but rather a factory that generates values that vary according to some
 * distribution
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface ProbabilityDistribution<T> //extends Serializable
{

	/**
	 * @return the next pseudo-random sample
	 */
	T draw();

	// continuous: https://www.wikiwand.com/en/Probability_density_function
	// discrete: https://www.wikiwand.com/en/Probability_mass_function
	// TODO probabilityOf(T value), cumulativeProbabilityOf(T value)

	/**
	 * From
	 * <a href="https://en.wikipedia.org/wiki/Errors_and_residuals">Wikipedia
	 * </a>: The statistical <b>error</b> (or <b>disturbance</b>) of an observed
	 * value is the deviation of the observed value from the (unobservable) true
	 * value of a quantity of interest (for example, a population mean).
	 */
//	T getError();

	/**
	 * From
	 * <a href="https://en.wikipedia.org/wiki/Errors_and_residuals">Wikipedia
	 * </a>: the <b>residual</b> (or fitting deviation) of an observed value is
	 * the difference between the observed value and the estimated value of the
	 * quantity of interest (for example, a sample mean).
	 */
//	T getResidual();

	/**
	 * @param <T> the type of value
	 * @param value the constant to be returned on each draw
	 * @return a degenerate or deterministic {@link ProbabilityDistribution}
	 */
	static <T> ProbabilityDistribution<T> createDeterministic( final T value )
	{
		final ProbabilityDistribution<T> result = new ProbabilityDistribution<T>()
		{
			@Override
			public T draw()
			{
				return value;
			}
		};
		return result;
	}

	static ProbabilityDistribution<Boolean>
		createBernoulli( final PseudoRandom rng, final Number probability )
	{
		final ProbabilityDistribution<Boolean> result = new ProbabilityDistribution<Boolean>()
		{
			@Override
			public Boolean draw()
			{
				return rng.nextDouble() < probability.doubleValue();
			}
		};
		return result;
	}

	/**
	 * @param <T> the type of value
	 * @param callable the {@link Callable} providing values, for JRE8
	 * @return a decorator {@link ProbabilityDistribution}
	 * @see https://github.com/orfjackal/retrolambda
	 */
	public static <T> ProbabilityDistribution<T>
		of( final Callable<T> callable )
	{
		return new ProbabilityDistribution<T>()
		{
			@Override
			public T draw()
			{
				try
				{
					return callable.call();
				} catch( final Exception e )
				{
					throw ExceptionFactory.createUnchecked( e,
							"Problem drawing new value from {}", callable );
				}
			}
		};
	}

	/**
	 * @param <T> the type of value
	 * @param provider the {@link Provider} providing values
	 * @return a decorator {@link ProbabilityDistribution}
	 */
	public static <T> ProbabilityDistribution<T>
		of( final Provider<T> provider )
	{
		return new ProbabilityDistribution<T>()
		{
			@Override
			public T draw()
			{
				return provider.get();
			}
		};
	}

	/**
	 * @param <T> the type of value
	 * @param func the {@link Func0} providing values, for rxJava
	 * @return a decorator {@link ProbabilityDistribution}
	 */
	public static <T> ProbabilityDistribution<T> of( final Func0<T> func )
	{
		return new ProbabilityDistribution<T>()
		{
			@Override
			public T draw()
			{
				return func.call();
			}
		};
	}

	default <R> ProbabilityDistribution<R> apply( final Func1<T, R> transform )
	{
		final ProbabilityDistribution<T> self = this;
		return new ProbabilityDistribution<R>()
		{
			@Override
			public R draw()
			{
				return transform.call( self.draw() );
			}
		};
	}

	/**
	 * wraps a {@link ProbabilityDistribution} over the full index range
	 * <em>{0, &hellip;, n-1}</em> of specified {@link Enum} constants
	 * 
	 * @param <E> the type of {@link Enum} value to produce
	 * @param enumType the {@link Class} to resolve
	 * @return a uniform categorical {@link ProbabilityDistribution} of
	 *         {@link E} values
	 */
	default <E extends Enum<E>> ProbabilityDistribution<E>
		toEnum( final Class<E> enumType )
	{
		return apply( new Func1<T, E>()
		{
			@Override
			public E call( final T n )
			{
				return enumType.getEnumConstants()[((Number) n).intValue()];
			}
		} );
	}

	/**
	 * wraps a {@link ProbabilityDistribution} to construct instances of another
	 * type which has a suitable constructor
	 * 
	 * @param <S> the type of instances to construct
	 * @param valueType the {@link Class} to instantiate
	 * @return a {@link ProbabilityDistribution} of {@link S} values
	 */
	default <S> ProbabilityDistribution<S>
		toInstancesOf( final Class<S> valueType )
	{
		return apply( new Func1<T, S>()
		{
			@Override
			public S call( final T arg )
			{
				return Instantiator.instantiate( valueType, arg );
			}
		} );
	}

	/**
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @return an {@link ArithmeticDistribution} {@link ProbabilityDistribution}
	 *         for measure {@link Amount}s from drawn {@link Number}s, with an
	 *         attempt to maintain exactness
	 */
	@SuppressWarnings( "unchecked" )
	default <Q extends Quantity> ArithmeticDistribution<Q> toAmounts()
	{
		return ArithmeticDistribution
				.of( (ProbabilityDistribution<Amount<Q>>) this );
	}

	/**
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @param unit the {@link Unit} of measurement to assign
	 * @return an {@link ArithmeticDistribution} {@link ProbabilityDistribution}
	 *         for measure {@link Amount}s from drawn {@link Number}s, with an
	 *         attempt to maintain exactness
	 */
	@SuppressWarnings( "unchecked" )
	default <Q extends Quantity> ArithmeticDistribution<Q>
		toAmounts( final Unit<Q> unit )
	{
		return ArithmeticDistribution
				.of( (ProbabilityDistribution<? extends Number>) this, unit );
	}

	/**
	 * {@link ArithmeticDistribution} is a {@link ProbabilityDistribution} for
	 * {@link Amount}s of some {@link Quantity}, decorated with arithmetic
	 * transforms of e.g. {@link Amount#times(double)} etc.
	 * 
	 * @param <Q> the type of {@link Quantity} for produced {@link Amount}s
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface ArithmeticDistribution<Q extends Quantity>
		extends ProbabilityDistribution<Amount<Q>>
	{
		public static <Q extends Quantity> ArithmeticDistribution<Q>
			of( final ProbabilityDistribution<Amount<Q>> dist )
		{
			return new ArithmeticDistribution<Q>()
			{
				@Override
				public Amount<Q> draw()
				{
					return dist.draw();
				}
			};
		}

		/**
		 * @param <N> the measurement value {@link Number} type
		 * @param <Q> the measurement {@link Quantity} to assign
		 * @param dist the {@link ProbabilityDistribution} to wrap
		 * @param unit the {@link Unit} of measurement to assign
		 * @return an {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution} for measure {@link Amount}s
		 *         from drawn {@link Number}s, with an attempt to maintain
		 *         exactness
		 */
		public static <N extends Number, Q extends Quantity>
			ArithmeticDistribution<Q>
			of( final ProbabilityDistribution<N> dist, final Unit<Q> unit )
		{
			return of( new ProbabilityDistribution<Amount<Q>>()
			{
				@Override
				public Amount<Q> draw()
				{
					return MeasureUtil.toAmount( dist.draw(), unit );
				}
			} );
		}

		/**
		 * @param <N> the type of {@link Number} value
		 * @param value the constant to be returned on each draw
		 * @return a degenerate or deterministic {@link ProbabilityDistribution}
		 */
		public static <N extends Number, Q extends Quantity>
			ArithmeticDistribution<Q> of( final N value, final Unit<Q> unit )
		{
			final Amount<Q> constant = MeasureUtil.toAmount( value, unit );
			return of( createDeterministic( constant ) );
		}

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param transform a unary {@link Func1} to transform {@link Amount}s
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 */
		default <R extends Quantity> ArithmeticDistribution<R>
			transform( final Func1<Amount<Q>, Amount<R>> transform )
		{
			final ArithmeticDistribution<Q> self = this;
			return of( new ProbabilityDistribution<Amount<R>>()
			{
				@Override
				public Amount<R> draw()
				{
					return transform.call( self.draw() );
				}
			} );
		}

		/**
		 * @param unit the {@link Unit} to convert to
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#to(Unit)
		 */
		default ArithmeticDistribution<Q> to( final Unit<Q> unit )
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.to( unit );
				}
			} );
		}

		/**
		 * @param that the {@link Amount} to be added
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#plus(Amount)
		 */
		default ArithmeticDistribution<Q> plus( final Amount<?> that )
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.plus( that );
				}
			} );
		}

		/**
		 * @param that the {@link Amount} to be subtracted
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#minus(Amount)
		 */
		default ArithmeticDistribution<Q> minus( final Amount<?> that )
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.minus( that );
				}
			} );
		}

		/**
		 * @param factor the exact scaling factor
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#times(long)
		 */
		default ArithmeticDistribution<Q> times( final long factor )
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.times( factor );
				}
			} );
		}

		/**
		 * @param factor the approximate scaling factor
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#times(double)
		 */
		default ArithmeticDistribution<Q> times( final double factor )
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.times( factor );
				}
			} );
		}

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param factor the measure multiplier {@link Amount}
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#times(Amount)
		 */
		default <R extends Quantity> ArithmeticDistribution<R>
			times( final Amount<?> factor, final Unit<R> unit )
		{
			return transform( new Func1<Amount<Q>, Amount<R>>()
			{
				@Override
				public Amount<R> call( final Amount<Q> t )
				{
					return t.times( factor ).to( unit );
				}
			} );
		}

		/**
		 * @param divisor the exact divisor
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#divide(long)
		 */
		default ArithmeticDistribution<Q> divide( final long divisor )
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.divide( divisor );
				}
			} );
		}

		/**
		 * @param divisor the approximate divisor
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#divide(double)
		 */
		default ArithmeticDistribution<Q> divide( final double divisor )
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.divide( divisor );
				}
			} );
		}

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param divisor the divisor {@link Amount}
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#divide(Amount)
		 */
		default <R extends Quantity> ArithmeticDistribution<R>
			divide( final Amount<?> divisor, final Unit<R> unit )
		{
			return transform( new Func1<Amount<Q>, Amount<R>>()
			{
				@Override
				public Amount<R> call( final Amount<Q> t )
				{
					return t.divide( divisor ).to( unit );
				}
			} );
		}

		/**
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#inverse()
		 */
		default <R extends Quantity> ArithmeticDistribution<R> inverse()
		{
			return transform( new Func1<Amount<Q>, Amount<R>>()
			{
				@SuppressWarnings( "unchecked" )
				@Override
				public Amount<R> call( final Amount<Q> t )
				{
					return (Amount<R>) t.inverse();
				}
			} );
		}

		/**
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#abs()
		 */
		default ArithmeticDistribution<Q> abs()
		{
			return transform( new Func1<Amount<Q>, Amount<Q>>()
			{
				@Override
				public Amount<Q> call( final Amount<Q> t )
				{
					return t.abs();
				}
			} );
		}

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#sqrt()
		 */
		default <R extends Quantity> ArithmeticDistribution<R> sqrt()
		{
			return transform( new Func1<Amount<Q>, Amount<R>>()
			{
				@SuppressWarnings( "unchecked" )
				@Override
				public Amount<R> call( final Amount<Q> t )
				{
					return (Amount<R>) t.sqrt();
				}
			} );
		}

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param n the root's order (n != 0)
		 * @return a chained {@link ArithmeticDistribution}
		 *         {@link ProbabilityDistribution}
		 * @see Amount#root(int)
		 */
		default <R extends Quantity> ArithmeticDistribution<R>
			root( final int n )
		{
			return transform( new Func1<Amount<Q>, Amount<R>>()
			{
				@SuppressWarnings( "unchecked" )
				@Override
				public Amount<R> call( final Amount<Q> t )
				{
					return (Amount<R>) t.root( n );
				}
			} );
		}

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param exp the exponent
		 * @return <code>this<sup>exp</sup></code>
		 * @see Amount#pow(int)
		 */
		default <R extends Quantity> ArithmeticDistribution<R>
			pow( final int exp )
		{
			return transform( new Func1<Amount<Q>, Amount<R>>()
			{
				@SuppressWarnings( "unchecked" )
				@Override
				public Amount<R> call( final Amount<Q> t )
				{
					return (Amount<R>) t.pow( exp );
				}
			} );
		}
	}

	/**
	 * {@link Multivariate} describes individual-level features/strata known for
	 * the population-level distribution of births among mothers such as age,
	 * income etc.
	 * 
	 * @param <K> the type of criteria/variates
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Multivariate<K>
	{
		Map<K, Object> values();

		@SuppressWarnings( "unchecked" )
		static <V> boolean match( final Range<?> range, final V value )
		{
			return ((Range<? super V>) range).contains( value );
		}

		default boolean match( final Map<K, Range<?>> filter )
		{
			for( Map.Entry<K, Range<?>> entry : filter.entrySet() )
				if( !match( entry.getValue(), values().get( entry.getKey() ) ) )
					return false;
			return true;
		}

		default void addValues( final Map<K, Set<Object>> ranges )
		{
			for( Map.Entry<K, Object> value : values().entrySet() )
				ranges.computeIfAbsent( value.getKey(), t ->
				{
					return new HashSet<Object>();
				} ).add( value.getValue() );
		}

		default <T extends Multivariate<K>> void checkRemovableValues(
			final Map<K, Set<Object>> ranges, final Iterable<T> others )
		{
			final Map<K, Object> removable = new HashMap<>( values() );
			for( T other : others )
			{
				removable.entrySet().removeIf( e ->
				{
					return e.getValue()
							.equals( other.values().get( e.getKey() ) );
				} );
				if( removable.isEmpty() ) break;
			}
		}
	}

	/**
	 * {@link MultivariateDistribution} restrict the sampling to multivariate
	 * items or units
	 * 
	 * @param <T> the type of {@link Multivariate} values to sample
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface MultivariateDistribution<T extends Multivariate<?>>
		extends ProbabilityDistribution<T>
	{
		// ProbabilityDistribution<? extends V> sampler(K variate);

		void register( T item );

		@SuppressWarnings( "unchecked" )
		default void register( T... items )
		{
			if( items != null ) for( T item : items )
				register( item );
		}

		default void register( Iterable<T> items )
		{
			if( items != null ) for( T item : items )
				register( item );
		}

		void unregister( T item );

		@SuppressWarnings( "unchecked" )
		default void unregister( T... items )
		{
			if( items != null ) for( T item : items )
				unregister( item );
		}

		default void unregister( Iterable<T> items )
		{
			if( items != null ) for( T item : items )
				unregister( item );
		}

		static <T extends Multivariate<K>, K> MultivariateDistribution<T> of(
			final PseudoRandom stream, final Collection<T> items,
			final Map<K, ProbabilityDistribution<Range<?>>> variates )
		{
			final Map<K, Set<Object>> ranges = new HashMap<>();
			items.forEach( t ->
			{
				t.addValues( ranges );
			} );

			final MultivariateDistribution<T> result = new MultivariateDistribution<T>()
			{
				@Override
				public T draw()
				{
					if( items.isEmpty() ) return null;
					if( items.size() == 1 ) return items.iterator().next();
					final List<T> candidates = new ArrayList<>();
					final Map<K, Range<?>> filter = new HashMap<>();
					variates.entrySet().forEach( e ->
					{
						filter.put( e.getKey(), e.getValue().draw() );
					} );
					items.forEach( t ->
					{
						if( t.match( filter ) ) candidates.add( t );
					} ); 
					return candidates
							.get( stream.nextInt( candidates.size() ) );
				}

				@Override
				public void register( final T item )
				{
					items.add( item );
					item.addValues( ranges );
				}

				@Override
				public void unregister( final T item )
				{
					items.remove( item );
					item.checkRemovableValues( ranges, items );
				}
			};
			return result;
		}
	}

	/**
	 * {@link Parser} generates {@link ProbabilityDistribution}s of specific
	 * shapes or probability mass (discrete) or density (continuous) functions
	 */
	public static class Parser
	{
		/** */
		private static final Logger LOG = LogUtil.getLogger( Parser.class );

		/**
		 * the PARAM_SEPARATORS exclude comma character <code>','</code> due to
		 * its common use as separator of decimals (e.g. <code>XX,X</code>) or
		 * of thousands (e.g. <code>n,nnn,nnn.nn</code>)
		 */
		public static final String PARAM_SEPARATORS = "[;]";

		public static final String WEIGHT_SEPARATORS = "[:]";

		public static final String DIST_GROUP = "dist";

		public static final String PARAMS_GROUP = "params";

		/**
		 * matches string representations like:
		 * <code>dist(arg1; arg2; &hellip;)</code> or
		 * <code>dist(v1:w1; v2:w2; &hellip;)</code>
		 */
		public static final Pattern DISTRIBUTION_FORMAT = Pattern
				.compile( "^(?<" + DIST_GROUP + ">[^\\(]+)\\((?<" + PARAMS_GROUP
						+ ">[^)]*)\\)$" );

		private final Factory factory;

		public Parser( final Factory factory )
		{
			this.factory = factory;
		}

		/** @return a {@link Factory} of {@link ProbabilityDistribution}s */
		public Factory getFactory()
		{
			return this.factory;
		}

		/**
		 * @param <P> the type of argument to parse
		 * @param dist the {@link String} representation
		 * @param parser the {@link Parser}
		 * @param argType the concrete argument {@link Class}
		 * @return a {@link ProbabilityDistribution} of {@link T} values
		 * @throws Exception
		 */
		@SuppressWarnings( "unchecked" )
		public <T, P> ProbabilityDistribution<T> parse( final String dist,
			final Class<P> argType ) throws Exception
		{
			final Matcher m = DISTRIBUTION_FORMAT.matcher( dist.trim() );
			if( !m.find() ) throw ExceptionFactory.createChecked(
					"Problem parsing probability distribution: {}", dist );
			final List<WeightedValue<P, ?>> params = new ArrayList<>();
			final InstanceParser<P> argParser = InstanceParser.of( argType );
			for( String valuePair : m.group( PARAMS_GROUP )
					.split( PARAM_SEPARATORS ) )
			{
				if( valuePair.trim().isEmpty() ) continue; // empty parentheses
				final String[] valueWeights = valuePair
						.split( WEIGHT_SEPARATORS );
				params.add(
						valueWeights.length == 1 // no weight given
								? WeightedValue.of(
										argParser.parseOrTrimmed( valuePair ),
										BigDecimal.ONE )
								: WeightedValue.of(
										argParser.parseOrTrimmed(
												valueWeights[0] ),
										new BigDecimal( valueWeights[1] ) ) );
			}
			if( params.isEmpty() && argType.isEnum() )
				for( P constant : argType.getEnumConstants() )
				params.add( WeightedValue.of( constant, BigDecimal.ONE ) );
			final ProbabilityDistribution<T> result = parse(
					m.group( DIST_GROUP ), params );
			if( !Amount.class.isAssignableFrom( argType ) || params.isEmpty() )
				return result;

			final Amount<?> first = (Amount<?>) params.get( 0 ).getValue();
			// check parameter value unit compatibility
			for( int i = params.size() - 1; i > 0; i-- )
				if( !((Amount<?>) params.get( i ).getValue()).getUnit()
						.isCompatible( first.getUnit() ) )
					throw ExceptionFactory.createUnchecked(
							"quantities incompatible of {} and {}", first,
							params.get( i ).getValue() );
			return (ProbabilityDistribution<T>) result.toAmounts();
		}

		/**
		 * @param <T> the type of value in the {@link ProbabilityDistribution}
		 * @param <V> the type of arguments
		 * @param name the symbol of the {@link ProbabilityDistribution}
		 * @param args the arguments as a {@link List} of {@link WeightedValue}
		 *            pairs with at least a value of type {@link T} and possibly
		 *            some numeric weight (as necessary for e.g. }
		 * @return a {@link ProbabilityDistribution}
		 */
		@SuppressWarnings( "unchecked" )
		public <T, V> ProbabilityDistribution<T> parse( final String label,
			final List<WeightedValue<V, ?>> args )
		{
			if( args.isEmpty() ) throw ExceptionFactory.createUnchecked(
					"Missing distribution parameters: {}", label );

			if( getFactory() == null )
			{
				final T value = (T) args.get( 0 ).getValue();
				LOG.warn( "No {} set, creating Deterministic<{}>: {}",
						Factory.class.getSimpleName(),
						value.getClass().getSimpleName(), value );
				return createDeterministic( value );
			}

			switch( label.toLowerCase( Locale.ROOT ) )
			{
			case "const":
			case "constant":
				return (ProbabilityDistribution<T>) getFactory()
						.createDeterministic( args.get( 0 ).getValue() );

			case "enum":
			case "enumerated":
			case "categorical":
			case "multinoulli":
				return (ProbabilityDistribution<T>) getFactory()
						.createCategorical( args );

			case "geom":
			case "geometric":
				return (ProbabilityDistribution<T>) getFactory()
						.createGeometric( (Number) args.get( 0 ).getValue() );

			case "hypergeom":
			case "hypergeometric":
				return (ProbabilityDistribution<T>) getFactory()
						.createHypergeometric(
								(Number) args.get( 0 ).getValue(),
								(Number) args.get( 1 ).getValue(),
								(Number) args.get( 2 ).getValue() );

			case "pascal":
				return (ProbabilityDistribution<T>) getFactory().createPascal(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "poisson":
				return (ProbabilityDistribution<T>) getFactory()
						.createPoisson( (Number) args.get( 0 ).getValue() );

			case "zipf":
				return (ProbabilityDistribution<T>) getFactory().createZipf(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "beta":
				return (ProbabilityDistribution<T>) getFactory().createBeta(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "cauchy":
			case "cauchy-lorentz":
			case "lorentz":
			case "lorentzian":
			case "breit-wigner":
				return (ProbabilityDistribution<T>) getFactory().createCauchy(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "chi":
			case "chisquare":
			case "chisquared":
			case "chi-square":
			case "chi-squared":
				return (ProbabilityDistribution<T>) getFactory()
						.createChiSquared( (Number) args.get( 0 ).getValue() );

			case "exp":
			case "exponent":
			case "exponential":
				return (ProbabilityDistribution<T>) getFactory()
						.createExponential( (Number) args.get( 0 ).getValue() );

			case "pearson6":
			case "beta-prime":
			case "inverted-beta":
			case "f":
				return (ProbabilityDistribution<T>) getFactory().createF(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "pearson3":
			case "erlang": // where arg1 is an integer)
			case "gamma":
				return (ProbabilityDistribution<T>) getFactory().createGamma(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "levy":
				return (ProbabilityDistribution<T>) getFactory().createLevy(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "lognormal":
			case "log-normal":
			case "gibrat":
				return (ProbabilityDistribution<T>) getFactory()
						.createLogNormal( (Number) args.get( 0 ).getValue(),
								(Number) args.get( 1 ).getValue() );

			case "gauss":
			case "gaussian":
			case "normal":
				return (ProbabilityDistribution<T>) getFactory().createNormal(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "pareto":
			case "pareto1":
				return (ProbabilityDistribution<T>) getFactory().createPareto(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );

			case "students-t":
			case "t":
				return (ProbabilityDistribution<T>) getFactory()
						.createT( (Number) args.get( 0 ).getValue() );

			case "tria":
			case "triangular":
				return (ProbabilityDistribution<T>) getFactory()
						.createTriangular( (Number) args.get( 0 ).getValue(),
								(Number) args.get( 1 ).getValue(),
								(Number) args.get( 2 ).getValue() );

			case "uniform-discrete":
			case "uniform-integer":
				return (ProbabilityDistribution<T>) getFactory()
						.createUniformDiscrete(
								(Number) args.get( 0 ).getValue(),
								(Number) args.get( 1 ).getValue() );

			case "uniform":
			case "uniform-real":
			case "uniform-continuous":
				return (ProbabilityDistribution<T>) getFactory()
						.createUniformContinuous(
								(Number) args.get( 0 ).getValue(),
								(Number) args.get( 1 ).getValue() );

			case "uniform-enum":
			case "uniform-enumerated":
			case "uniform-categorical":
				final List<T> values = new ArrayList<>();
				for( WeightedValue<V, ?> pair : args )
					values.add( (T) pair.getValue() );
				return (ProbabilityDistribution<T>) getFactory()
						.createUniformCategorical( values.toArray() );

			case "frechet":
			case "weibull":
				return (ProbabilityDistribution<T>) getFactory().createWeibull(
						(Number) args.get( 0 ).getValue(),
						(Number) args.get( 1 ).getValue() );
			}
			throw ExceptionFactory.createUnchecked(
					"Unknown distribution symbol: {}", label );
		}
	}

	/**
	 * {@link Factory} generates {@link ProbabilityDistribution}s, a small
	 * subset of those mentioned on
	 * <a href="https://www.wikiwand.com/en/List_of_probability_distributions">
	 * Wikipedia</a>
	 */
	public static interface Factory
	{

		/**
		 * @return the {@link RandomNumberStream} used in creating
		 *         {@link ProbabilityDistribution}s
		 */
		PseudoRandom getStream();

		/**
		 * @param <T> the type of constant drawn
		 * @param value the constant to be returned on each draw
		 * @return a degenerate or deterministic {@link ProbabilityDistribution}
		 */
		<T> ProbabilityDistribution<T> createDeterministic( T value );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Binomial_distribution_pmf.svg/600px-Binomial_distribution_pmf.svg.png"/>
		 * 
		 * @param trials number of consecutive successes
		 * @param p probability of a single success
		 * @return a binomial {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Binomial_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=binomial+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createBinomial( Number trials, Number p );

		/**
		 * multinoulli
		 * 
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/2D-simplex.svg/440px-2D-simplex.svg.png"/>
		 * 
		 * @param <T> the type of value to draw
		 * @param probabilities the {@link WeightedValue} enumeration (i.e.
		 *            probability mass function)
		 * @return a categorical {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Categorical_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=bernoulli+distribution">
		 *      Wolfram &alpha;</a>
		 */
		<T, WV extends WeightedValue<T, ?>> ProbabilityDistribution<T>
			createCategorical( List<WV> probabilities );

		/**
		 * @param p
		 * @return
		 */
		ProbabilityDistribution<Boolean> createBernoulli( Number p );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Geometric_pmf.svg/900px-Geometric_pmf.svg.png"/>
		 * 
		 * @param p <em>&isin; (0, 1]</em> probability of success
		 * @return a (non-shifted) geometric {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Geometric_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=geometric+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createGeometric( Number p );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/en/2/22/FishersNoncentralHypergeometric1.png"/>
		 * 
		 * @param populationSize <em>N</em>
		 * @param numberOfSuccesses <em>m</em>
		 * @param sampleSize <em>n</em> (number of trials or draws)
		 * @return a hypergeometric {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Hypergeometric_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=Hypergeometric+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createHypergeometric(
			Number populationSize, Number numberOfSuccesses,
			Number sampleSize );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/8/83/Negbinomial.gif"/>
		 * 
		 * @param r (positive) number of successes
		 * @param p <em>&isin; (0, 1)</em> probability of success
		 * @return a Pascal (discrete negative binomial)
		 *         {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Negative_binomial_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=pascal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createPascal( Number r, Number p );

		/**
		 * Typically used for: event rate = hazard (Cox) = incidence
		 * (epidemiology) = how frequent? <br/>
		 * <img alt= "Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Poisson_pmf.svg/650px-Poisson_pmf.svg.png"/>
		 * 
		 * @param mean &mu; or &lambda;
		 * @return a Poisson {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Poisson_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=poisson+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createPoisson( Number mean );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Zipf_distribution_PMF.png/650px-Zipf_distribution_PMF.png"/>
		 * 
		 * @param numberOfElements <em>N</em> (positive)
		 * @param exponent <em>s</em> (positive)
		 * @return a Zipf's power law {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Zipf%27s_law">Wikipedia</a>
		 *      and <a href=
		 *      "https://www.wolframalpha.com/input/?i=zipf+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createZipf( Number numberOfElements,
			Number exponent );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/Beta_distribution_pdf.svg/650px-Beta_distribution_pdf.svg.png"/>
		 * 
		 * @param alpha shape (positive)
		 * @param beta shape (positive)
		 * @return a &beta; {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Beta_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=beta+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createBeta( Number alpha, Number beta );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8c/Cauchy_pdf.svg/600px-Cauchy_pdf.svg.png"/>
		 * 
		 * @param median the location <em>a</em>
		 * @param scale <em>b</em> (positive)
		 * @return a Lorentz or Cauchy {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Cauchy_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=cauchy+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createCauchy( Number median,
			Number scale );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/3/35/Chi-square_pdf.svg/642px-Chi-square_pdf.svg.png"/>
		 * 
		 * @param degreesOfFreedom <em>k</em>
		 * @return a &chi;<sup>2</sup> {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Chi-squared_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=chi+squared+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double>
			createChiSquared( Number degreesOfFreedom );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Exponential_pdf.svg/650px-Exponential_pdf.svg.png"/>
		 * 
		 * @param mean &lambda;
		 * @return a (negative) exponential {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Exponential_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=exponential+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createExponential( Number mean );

		/**
		 * @param values the <em>n</em> empirical values
		 * @return an empirical {@link ProbabilityDistribution} of <em>n/10</em>
		 *         bins without underlying probability mass function assumptions
		 */
		@SuppressWarnings( "unchecked" )
		<T extends Number> ProbabilityDistribution<Double>
			createEmpirical( T... values );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/F_pdf.svg/650px-F_pdf.svg.png"/>
		 * 
		 * @param numeratorDegreesOfFreedom <em>n</em>
		 * @param denominatorDegreesOfFreedom <em>m</em>
		 * @return an F-{@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/F-distribution">Wikipedia
		 *      </a> and
		 *      <a href="https://www.wolframalpha.com/input/?i=f+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createF(
			Number numeratorDegreesOfFreedom,
			Number denominatorDegreesOfFreedom );

		/**
		 * The Gamma distribution family also includes
		 * <a href="https://www.wikiwand.com/en/Erlang_distribution">Erlang
		 * distributions</a> (where shape <em>k</em> is an integer) <img alt=
		 * "Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e6/Gamma_distribution_pdf.svg/650px-Gamma_distribution_pdf.svg.png"/>
		 * 
		 * @param shape &alpha; or <em>k</em>
		 * @param scale &beta; or &theta;
		 * @return a &Gamma; (gamma) {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Gamma_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=gamma+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createGamma( Number shape,
			Number scale );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6e/Levy0_distributionPDF.svg/650px-Levy0_distributionPDF.svg.png"/>
		 * 
		 * @param mu
		 * @param c
		 * @return a L&eacute;vy {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/L%C3%A9vy_distribution">
		 *      Wikipedia</a> and <a
		 *      href=""https://www.wolframalpha.com/input/?i=levy+distribution>
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createLevy( Number mu, Number c );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/PDF-log_normal_distributions.svg/600px-PDF-log_normal_distributions.svg.png"/>
		 * 
		 * @param scale
		 * @param shape
		 * @return a log-normal {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Log-normal_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=lognormal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createLogNormal( Number scale,
			Number shape );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Normal_Distribution_PDF.svg/700px-Normal_Distribution_PDF.svg.png"/>
		 * 
		 * @param mean &mu;
		 * @param stDev &sigma; > 0 (standard deviation)
		 * @return a Gaussian or Normal {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Normal_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=normal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createNormal( Number mean,
			Number stDev );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/5/57/Multivariate_Gaussian.png/600px-Multivariate_Gaussian.png"/>
		 * 
		 * @param means
		 * @param covariances
		 * @return a Multivariate Normal {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Multivariate_normal_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=multivariate+normal+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<double[]> createMultinormal( double[] means,
			double[][] covariances );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/Probability_density_function_of_Pareto_distribution.svg/650px-Probability_density_function_of_Pareto_distribution.svg.png"/>
		 * 
		 * @param scale
		 * @param shape
		 * @return a Pareto {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Pareto_distribution">
		 *      Wikipedia </a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=pareto+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createPareto( Number scale,
			Number shape );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Student_t_pdf.svg/650px-Student_t_pdf.svg.png"/>
		 * 
		 * @param degreesOfFreedom <em>v</em>
		 * @return a Pearson type VII or Student's T
		 *         {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Student%27s_t-distribution">
		 *      Wikipedia</a> and
		 *      <a href="https://www.wolframalpha.com/input/?i=t+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createT( Number degreesOfFreedom );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/4/45/Triangular_distribution_PMF.png/650px-Triangular_distribution_PMF.png"/>
		 * 
		 * @param a <em>min</em>
		 * @param b <em>peak</em>
		 * @param c <em>max</em>
		 * @return a triangular {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Triangular_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=triangular+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createTriangular( Number a, Number b,
			Number c );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1f/Uniform_discrete_pmf_svg.svg/650px-Uniform_discrete_pmf_svg.svg.png"/>
		 * 
		 * @param min <em>a</em>
		 * @param max <em>b</em>
		 * @return a uniform discrete/integer {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Uniform_distribution_(discrete)">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=discrete+uniform+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Long> createUniformDiscrete( Number min,
			Number max );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Uniform_Distribution_PDF_SVG.svg/500px-Uniform_Distribution_PDF_SVG.svg.png"/>
		 * 
		 * @param min
		 * @param max
		 * @return a uniform continuous/real {@link ProbabilityDistribution}
		 * @see <a href=
		 *      "https://www.wikiwand.com/en/Uniform_distribution_(continuous)">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=uniform+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createUniformContinuous( Number min,
			Number max );

		/**
		 * wraps a {@link #createUniformDiscrete(Number, Number)} over the full
		 * index range <em>{0, &hellip;, n-1}</em> of specified value
		 * enumeration
		 * 
		 * @param <T> the type of value to draw
		 * @param values the value enumeration
		 * @return a uniform categorical {@link ProbabilityDistribution}
		 * @see <a href= "https://www.wikiwand.com/en/Categorical_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=discrete+uniform+distribution">
		 *      Wolfram &alpha;</a>
		 */
		@SuppressWarnings( "unchecked" )
		<T> ProbabilityDistribution<T> createUniformCategorical( T... values );

		/**
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/5/58/Weibull_PDF.svg/650px-Weibull_PDF.svg.png"/>
		 * 
		 * @param alpha shape &alpha; (positive)
		 * @param beta scale &beta; (positive)
		 * @return a Weibull {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Weibull_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=weibull+distribution">
		 *      Wolfram &alpha;</a>
		 */
		ProbabilityDistribution<Double> createWeibull( Number alpha,
			Number beta );
	}

	public static interface Fitter
	{

		Factory getFactory();

		<Q extends Quantity> ArithmeticDistribution<Q> fitNormal(
			FrequencyDistribution.Interval<Q, ?> values, Unit<Q> unit );

		// wavelets, e.g.https://github.com/cscheiblich/JWave  => pdf?

//		signal / harmonic( Number initialAmplitude,
//			Number initialAngularFrequency, Number initialPhase ) => pdf?

//		polynomial regression ( Number... initialCoefficients ) => pdf?

	}

}