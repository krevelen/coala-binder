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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.jscience.physics.amount.Amount;

import io.coala.exception.ExceptionFactory;
import io.coala.log.LogUtil;
import io.coala.math.WeightedValue;
import io.coala.random.ProbabilityDistribution.Factory;
import io.coala.util.InstanceParser;

/**
 * {@link DistributionParser} generates {@link ProbabilityDistribution}s of
 * specific shapes or probability mass (discrete) or density (continuous)
 * functions
 */
public class DistributionParser
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( DistributionParser.class );

	/**
	 * the PARAM_SEPARATORS exclude comma character <code>','</code> due to its
	 * common use as separator of decimals (e.g. <code>XX,X</code>) or of
	 * thousands (e.g. <code>n,nnn,nnn.nn</code>)
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
	public static final Pattern DISTRIBUTION_FORMAT = Pattern.compile( "^(?<"
			+ DIST_GROUP + ">[^\\(]+)\\((?<" + PARAMS_GROUP + ">[^)]*)\\)$" );

	private final ProbabilityDistribution.Factory factory;

	@Inject
	public DistributionParser( final ProbabilityDistribution.Factory factory )
	{
		this.factory = factory;
	}

	/** @return a {@link Factory} of {@link ProbabilityDistribution}s */
	public ProbabilityDistribution.Factory getFactory()
	{
		return this.factory;
	}

	/**
	 * @param <P> the type of argument to parse
	 * @param dist the {@link String} representation
	 * @param parser the {@link DistributionParser}
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
		final List<WeightedValue<P>> params = new ArrayList<>();
		final InstanceParser<P> argParser = InstanceParser.of( argType );
		for( String valuePair : m.group( PARAMS_GROUP )
				.split( PARAM_SEPARATORS ) )
		{
			if( valuePair.trim().isEmpty() ) continue; // empty parentheses
			final String[] valueWeights = valuePair.split( WEIGHT_SEPARATORS );
			params.add( valueWeights.length == 1 // no weight given
					? WeightedValue.of( argParser.parseOrTrimmed( valuePair ),
							BigDecimal.ONE )
					: WeightedValue.of(
							argParser.parseOrTrimmed( valueWeights[0] ),
							new BigDecimal( valueWeights[1] ) ) );
		}
		if( params.isEmpty() && argType.isEnum() )
			for( P constant : argType.getEnumConstants() )
			params.add( WeightedValue.of( constant, BigDecimal.ONE ) );
		final ProbabilityDistribution<T> result = parse( m.group( DIST_GROUP ),
				params );
		if( !Amount.class.isAssignableFrom( argType ) || params.isEmpty() )
			return result;

		/* try {@link AmountDistribution} */
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
		final List<WeightedValue<V>> args )
	{
		if( args.isEmpty() ) throw ExceptionFactory.createUnchecked(
				"Missing distribution parameters: {}", label );

		if( getFactory() == null )
		{
			final T value = (T) args.get( 0 ).getValue();
			LOG.warn( "No {} set, creating Deterministic<{}>: {}",
					ProbabilityDistribution.Factory.class.getSimpleName(),
					value.getClass().getSimpleName(), value );
			return ProbabilityDistribution.createDeterministic( value );
		}

		switch( label.toLowerCase( Locale.ROOT ) )
		{
		case "const":
		case "constant":
		case "degen":
		case "degenerate":
		case "determ":
		case "deterministic":
			return (ProbabilityDistribution<T>) getFactory()
					.createDeterministic( args.get( 0 ).getValue() );

		case "bernoulli":
			return (ProbabilityDistribution<T>) getFactory()
					.createBernoulli( (Number) args.get( 0 ).getValue() );

		case "binom":
		case "binomial":
			return (ProbabilityDistribution<T>) getFactory().createBinomial(
					(Number) args.get( 0 ).getValue(),
					(Number) args.get( 1 ).getValue() );

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
					.createHypergeometric( (Number) args.get( 0 ).getValue(),
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
			return (ProbabilityDistribution<T>) getFactory().createLogNormal(
					(Number) args.get( 0 ).getValue(),
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
			return (ProbabilityDistribution<T>) getFactory().createTriangular(
					(Number) args.get( 0 ).getValue(),
					(Number) args.get( 1 ).getValue(),
					(Number) args.get( 2 ).getValue() );

		case "uniform-discrete":
		case "uniform-integer":
			return (ProbabilityDistribution<T>) getFactory()
					.createUniformDiscrete( (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

		case "uniform":
		case "uniform-real":
		case "uniform-continuous":
			return (ProbabilityDistribution<T>) getFactory()
					.createUniformContinuous( (Number) args.get( 0 ).getValue(),
							(Number) args.get( 1 ).getValue() );

		case "uniform-enum":
		case "uniform-enumerated":
		case "uniform-categorical":
			final List<T> values = new ArrayList<>();
			for( WeightedValue<V> pair : args )
				values.add( (T) pair.getValue() );
			return (ProbabilityDistribution<T>) getFactory()
					.createUniformCategorical( values.toArray() );

		case "frechet":
		case "weibull":
			return (ProbabilityDistribution<T>) getFactory().createWeibull(
					(Number) args.get( 0 ).getValue(),
					(Number) args.get( 1 ).getValue() );
		}
		throw ExceptionFactory
				.createUnchecked( "Unknown distribution symbol: {}", label );
	}
}