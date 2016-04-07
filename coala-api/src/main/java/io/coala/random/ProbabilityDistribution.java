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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionFactory;
import io.coala.math.FrequencyDistribution;
import io.coala.math.MeasureUtil;
import io.coala.math.ValueWeight;
import io.coala.util.InstanceParser;
import io.coala.util.Instantiator;
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
@SuppressWarnings( "serial" )
public abstract class ProbabilityDistribution<T> implements Serializable
{

	/**
	 * @return the next pseudo-random sample
	 */
	public abstract T draw();

	// TODO RandomNumberStream getStream();

	// TODO List<?> getParameters();

	// continuous: https://www.wikiwand.com/en/Probability_density_function
	// discrete: https://www.wikiwand.com/en/Probability_density_function
	// TODO probabilityOf(T value)

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
	 * {@link Util} provides static helper methods for
	 * {@link ProbabilityDistribution}s
	 * 
	 * @version $Id: a7842c5dc1c8963fe6c9721cdcda6c3b21980bb0 $
	 * @author Rick van Krevelen
	 */
//	class Util implements io.coala.util.Util
//	{
//
//		private Util()
//		{
//		}

	/**
	 * @param <T> the type of value
	 * @param value the constant to be returned on each draw
	 * @return a degenerate or deterministic {@link ProbabilityDistribution}
	 */
	public static <T> ProbabilityDistribution<T>
		createDeterministic( final T value )
	{
		return new ProbabilityDistribution<T>()
		{
			@Override
			public T draw()
			{
				return value;
			}
		};
	}

	public static <T, S> ProbabilityDistribution<S> transform(
		final ProbabilityDistribution<T> dist, final Func1<T, S> transform )
	{
		return new ProbabilityDistribution<S>()
		{
			@Override
			public S draw()
			{
				return transform.call( dist.draw() );
			}
		};
	}

	/**
	 * wraps a {@link ProbabilityDistribution} over the full index range
	 * <em>{0, &hellip;, n-1}</em> of specified {@link Enum} constants
	 * 
	 * @param <N> the type of (discrete) {@link Number} drawn originally
	 * @param <E> the type of {@link Enum} value to produce
	 * @param dist the {@link ProbabilityDistribution} of {@link N} values
	 * @param enumType the {@link Class} to resolve
	 * @return a uniform categorical {@link ProbabilityDistribution} of
	 *         {@link E} values
	 */
	public static <N extends Number, E extends Enum<E>>
		ProbabilityDistribution<E>
		toEnum( final ProbabilityDistribution<N> dist, final Class<E> enumType )
	{
		return transform( dist, new Func1<N, E>()
		{
			@Override
			public E call( final N n )
			{
				return enumType.getEnumConstants()[n.intValue()];
			}
		} );
	}

	/**
	 * wraps a {@link ProbabilityDistribution} to construct instances of another
	 * type which has a suitable constructor
	 * 
	 * @param <A> the type of constructor arguments
	 * @param <T> the type of instances to construct
	 * @param dist the {@link ProbabilityDistribution} of {@link A} values
	 * @param valueType the {@link Class} to instantiate
	 * @return a {@link ProbabilityDistribution} of {@link T} values
	 */
	public static <A, T> ProbabilityDistribution<T> toInstancesOf(
		final ProbabilityDistribution<A> dist, final Class<T> valueType )
	{
		return transform( dist, new Func1<A, T>()
		{
			@Override
			public T call( final A arg )
			{
				return Instantiator.instantiate( valueType, arg );
			}
		} );
	}

	/**
	 * @param <Q> the measurement {@link Quantity} to assign
	 * @param dist the {@link ProbabilityDistribution} to wrap
	 * @param unit the {@link Unit} of measurement to assign
	 * @return an {@link Arithmetic} {@link ProbabilityDistribution} for measure
	 *         {@link Amount}s from drawn {@link Number}s, with an attempt to
	 *         maintain exactness
	 */
	public static <Q extends Quantity> Arithmetic<Q> toArithmetic(
		final ProbabilityDistribution<? extends Number> dist,
		final Unit<Q> unit )
	{
		return Arithmetic.Simple.of( new ProbabilityDistribution<Amount<Q>>()
		{
			@Override
			public Amount<Q> draw()
			{
				return MeasureUtil.toAmount( dist.draw(), unit );
			}
		} );
	}
//	}

	/**
	 * {@link Arithmetic} is a {@link ProbabilityDistribution} for
	 * {@link Amount}s of some {@link Quantity}, decorated with arithmetic
	 * transforms of e.g. {@link Amount#times(double)} etc.
	 * 
	 * @param <Q> the type of {@link Quantity} for produced {@link Amount}s
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static abstract class Arithmetic<Q extends Quantity>
		extends ProbabilityDistribution<Amount<Q>>
	{

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param transform a unary {@link Func1} to transform {@link Amount}s
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 */
		public abstract <R extends Quantity> Arithmetic<R>
			transform( Func1<Amount<Q>, Amount<R>> transform );

		// continuous: https://www.wikiwand.com/en/Probability_density_function
		// discrete: https://www.wikiwand.com/en/Probability_density_function
		// TODO cumulativeProbabilityOf(T value)

		/**
		 * @param that the {@link Amount} to be added
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#plus(Amount)
		 */
		public abstract Arithmetic<Q> plus( Amount<?> that );

		/**
		 * @param that the {@link Amount} to be subtracted
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#minus(Amount)
		 */
		public abstract Arithmetic<Q> minus( Amount<?> that );

		/**
		 * @param factor the exact scaling factor
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#times(long)
		 */
		public abstract Arithmetic<Q> times( long factor );

		/**
		 * @param factor the approximate scaling factor
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#times(double)
		 */
		public abstract Arithmetic<Q> times( double factor );

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param factor the measure multiplier {@link Amount}
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#times(Amount)
		 */
		public abstract <R extends Quantity> Arithmetic<R>
			times( Amount<?> factor );

		/**
		 * @param divisor the exact divisor
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#divide(long)
		 */
		public abstract Arithmetic<Q> divide( long divisor );

		/**
		 * @param divisor the approximate divisor
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#divide(double)
		 */
		public abstract Arithmetic<Q> divide( double divisor );

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param divisor the divisor {@link Amount}
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#divide(Amount)
		 */
		public abstract <R extends Quantity> Arithmetic<R>
			divide( Amount<?> divisor );

		/**
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#inverse()
		 */
		public abstract <R extends Quantity> Arithmetic<R> inverse();

		/**
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#abs()
		 */
		public abstract Arithmetic<Q> abs();

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#sqrt()
		 */
		public abstract <R extends Quantity> Arithmetic<R> sqrt();

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param n the root's order (n != 0)
		 * @return a chained {@link Arithmetic} {@link ProbabilityDistribution}
		 * @see Amount#root(int)
		 */
		public abstract <R extends Quantity> Arithmetic<R> root( int n );

		/**
		 * @param <R> the new type of {@link Quantity} after transformation
		 * @param exp the exponent
		 * @return <code>this<sup>exp</sup></code>
		 * @see Amount#pow(int)
		 */
		public abstract <R extends Quantity> Arithmetic<R> pow( int exp );

		/**
		 * {@link Simple} implements a {@link Arithmetic}
		 * {@link ProbabilityDistribution}
		 * 
		 * @param <Q>
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		public static class Simple<Q extends Quantity> extends Arithmetic<Q>
		{
			public static <Q extends Quantity> Simple<Q>
				of( final ProbabilityDistribution<Amount<Q>> dist )
			{
				return new Simple<Q>( dist );
			}

			private final ProbabilityDistribution<Amount<Q>> dist;

			protected Simple( final ProbabilityDistribution<Amount<Q>> dist )
			{
				this.dist = dist;
			}

			@Override
			public Amount<Q> draw()
			{
				return this.dist.draw();
			}

			@Override
			public <R extends Quantity> Simple<R>
				transform( final Func1<Amount<Q>, Amount<R>> transform )
			{
				return of( new ProbabilityDistribution<Amount<R>>()
				{
					@Override
					public Amount<R> draw()
					{
						return transform.call( dist.draw() );
					}
				} );
			}

			@Override
			public Simple<Q> plus( final Amount<?> that )
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

			@Override
			public Simple<Q> minus( final Amount<?> that )
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

			@Override
			public Simple<Q> times( final long factor )
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

			@Override
			public Simple<Q> times( final double factor )
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

			@Override
			public <R extends Quantity> Simple<R>
				times( final Amount<?> factor )
			{
				return transform( new Func1<Amount<Q>, Amount<R>>()
				{
					@SuppressWarnings( "unchecked" )
					@Override
					public Amount<R> call( final Amount<Q> t )
					{
						return (Amount<R>) t.times( factor );
					}
				} );
			}

			@Override
			public Simple<Q> divide( final long divisor )
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

			@Override
			public Simple<Q> divide( final double divisor )
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

			@Override
			public <R extends Quantity> Simple<R>
				divide( final Amount<?> divisor )
			{
				return transform( new Func1<Amount<Q>, Amount<R>>()
				{
					@SuppressWarnings( "unchecked" )
					@Override
					public Amount<R> call( final Amount<Q> t )
					{
						return (Amount<R>) t.divide( divisor );
					}
				} );
			}

			@Override
			public Simple<Q> abs()
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

			@Override
			public <R extends Quantity> Simple<R> inverse()
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

			@Override
			public <R extends Quantity> Simple<R> sqrt()
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

			@Override
			public <R extends Quantity> Simple<R> root( final int n )
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

			@Override
			public <R extends Quantity> Simple<R> pow( final int exp )
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
	}

	/**
	 * {@link Parser} generates {@link ProbabilityDistribution}s of specific
	 * shapes or probability mass (discrete) or density (continuous) functions
	 */
	public static class Parser
	{

		/**
		 * the PARAM_SEPARATORS exclude comma character <code>','</code> due to
		 * its common use as separator of decimals (e.g. <code>XX,X</code>) or
		 * of thousands (e.g. <code>X,XXX,XXX.XX</code>)
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
		public <T, P> ProbabilityDistribution<T> parse( final String dist,
			final Class<P> argType ) throws Exception
		{
			final Matcher m = DISTRIBUTION_FORMAT.matcher( dist.trim() );
			if( !m.find() ) throw ExceptionFactory.createChecked(
					"Problem parsing probability distribution: {}", dist );
			final List<ValueWeight<P, ?>> params = new ArrayList<>();
			for( String valuePair : m.group( PARAMS_GROUP )
					.split( PARAM_SEPARATORS ) )
			{
				if( valuePair.trim().isEmpty() ) continue; // empty parentheses
				final String[] valueWeights = valuePair
						.split( WEIGHT_SEPARATORS );
				if( valueWeights.length == 1 )
				{
					params.add( ValueWeight.of( InstanceParser.of( argType )
							.parseOrTrimmed( valuePair ), 1 ) );
				} else
					params.add( ValueWeight.of(
							InstanceParser.of( argType )
									.parseOrTrimmed( valueWeights[0] ),
							new BigDecimal( valueWeights[1] ) ) );
			}
			final Integer one = Integer.valueOf( 1 );
			if( params.isEmpty() && argType.isEnum() )
				for( P constant : argType.getEnumConstants() )
				params.add( ValueWeight.of( constant, one ) );
			return parse( m.group( DIST_GROUP ), params );
		}

		/**
		 * @param <T> the type of value in the {@link ProbabilityDistribution}
		 * @param <V> the type of arguments
		 * @param name the symbol of the {@link ProbabilityDistribution}
		 * @param args the arguments as a {@link List} of {@link ValueWeight}
		 *            pairs with at least a value of type {@link T} and possibly
		 *            some numeric weight (as necessary for e.g. }
		 * @return a {@link ProbabilityDistribution}
		 */
		@SuppressWarnings( "unchecked" )
		public <T, V> ProbabilityDistribution<T> parse( final String label,
			final List<ValueWeight<V, ?>> args )
		{
			if( args.isEmpty() ) throw ExceptionFactory.createUnchecked(
					"Missing distribution parameters: {}", label );

			if( getFactory() == null )
				return (ProbabilityDistribution<T>) createDeterministic(
						args.get( 0 ).getValue() );

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
				for( ValueWeight<V, ?> pair : args )
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
		RandomNumberStream getStream();

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
		 * <img alt="Probability density function" height="150" src=
		 * "https://upload.wikimedia.org/wikipedia/commons/thumb/3/38/2D-simplex.svg/440px-2D-simplex.svg.png"/>
		 * 
		 * @param <T> the type of value to draw
		 * @param probabilities the {@link ValueWeight} enumeration (i.e.
		 *            probability mass function)
		 * @return a categorical {@link ProbabilityDistribution}
		 * @see <a href="https://www.wikiwand.com/en/Categorical_distribution">
		 *      Wikipedia</a> and <a href=
		 *      "https://www.wolframalpha.com/input/?i=bernoulli+distribution">
		 *      Wolfram &alpha;</a>
		 */
		<T> ProbabilityDistribution<T>
			createCategorical( List<ValueWeight<T, ?>> probabilities );

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
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP45422d3he4dhebi328d000058fagd36che5g435?MSPStoreType=image/gif&s=55"/>
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
		 * <img alt="Probability density function" height="150" src=
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
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP8501hgb0bgf73624fc5000051a73dc4e9a96c67?MSPStoreType=image/gif&s=16"/>
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
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP7871b54g7d40h84965d00003da5f5hdbcc5c814?MSPStoreType=image/gif&s=43"/>
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
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP6191i49013gc13cf2hb000022b4ceehae6f37ea?MSPStoreType=image/gif&s=36"/>
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
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP88621eh12e8g98651g400003ee0348d29i1g4d4?MSPStoreType=image/gif&s=52"/>
		 * 
		 * @param mean
		 * @return an exponential {@link ProbabilityDistribution}
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
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP13671i38add4aggc73hh000014324ii26c1f66f7?MSPStoreType=image/gif&s=53"/>
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
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP63311b1eag7ab2ig548b00004782c2i0edach659?MSPStoreType=image/gif&s=2"/>
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
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP52951h27f65cf34d4e2h00003hac30i250a1h7fa?MSPStoreType=image/gif&s=44"/>
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
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP241g2gaabgif7c3faf0000186db998af94bfha?MSPStoreType=image/gif&s=25"/>
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
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP57711dh32162d1de95bc0000182h50icfigi6hd0?MSPStoreType=image/gif&s=53"/>
		 * 
		 * @param mean &mu;
		 * @param stDev &sigma; (standard deviation)
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
		 * <img alt="Probability density function" height="150" src=""/>
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
		 * "https://www5a.wolframalpha.com/Calculate/MSP/MSP12122ihg1d9af4a54280000415853423eie0hh4?MSPStoreType=image/gif&s=16"/>
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
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP656228c7a6e598g3d76000057af6a6d61g9e468?MSPStoreType=image/gif&s=41"/>
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
		 * "https://www4f.wolframalpha.com/Calculate/MSP/MSP11821h766ih567i7ifd30000345ii2eh6a6e27ee?MSPStoreType=image/gif&s=34"/>
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
		 * "https://www5b.wolframalpha.com/Calculate/MSP/MSP25522008f4d573002dac00004iec9101ia30f3ab?MSPStoreType=image/gif&s=41"/>
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

		<Q extends Quantity> Arithmetic<Q> fitNormal(
			FrequencyDistribution.Interval<Q, ?> values, Unit<Q> unit );

		// wavelets, e.g.https://github.com/cscheiblich/JWave  => pdf?

//		signal / harmonic( Number initialAmplitude,
//			Number initialAngularFrequency, Number initialPhase ) => pdf?

//		polynomial regression ( Number... initialCoefficients ) => pdf?

	}

}