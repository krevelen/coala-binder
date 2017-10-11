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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.measure.Quantity;
import javax.measure.Unit;

import org.apache.logging.log4j.Logger;

import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.math.QuantityJsonModule;
import io.coala.math.QuantityUtil;
import io.coala.math.WeightedValue;
import io.coala.random.ProbabilityDistribution.Factory;
import io.coala.util.InstanceParser;

/**
 * {@link DistributionParser} generates {@link ProbabilityDistribution}s of
 * specific shapes or probability mass (discrete) or density (continuous)
 * functions
 */
public class DistributionParser implements ProbabilityDistribution.Parser
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
	public static final String DISTRIBUTION_FORMAT_REGEX = "^(?<" + DIST_GROUP
			+ ">[^\\(]+)\\s*\\((?<" + PARAMS_GROUP + ">[^)]*)\\)$";

	/**
	 * matches string representations like:
	 * <code>dist(arg1; arg2; &hellip;)</code> or
	 * <code>dist(v1:w1; v2:w2; &hellip;)</code>
	 */
	public static final Pattern DISTRIBUTION_FORMAT = Pattern
			.compile( DISTRIBUTION_FORMAT_REGEX );

	private final ProbabilityDistribution.Factory factory;

	@Inject
	public DistributionParser( final ProbabilityDistribution.Factory factory )
	{
		this.factory = factory;
	}

	/** @return a {@link Factory} of {@link ProbabilityDistribution}s */
	@Override
	public ProbabilityDistribution.Factory getFactory()
	{
		return this.factory;
	}

	/**
	 * @param <P> the type of argument to parse
	 * @param dist the {@link String} representation as
	 *            {@code "dist(arg1; arg2; ...)"}, for instance
	 *            {@code "gauss(mean, stdev)"} or
	 *            {@code "enum(val1:w1; val2:w2; ...)"}
	 * @param parser the {@link DistributionParser}
	 * @param argType the concrete argument {@link Class}
	 * @return a {@link ProbabilityDistribution} of {@link T} values
	 * @throws Exception
	 */
	@SuppressWarnings( "unchecked" )
	@Override
	public <T, P> ProbabilityDistribution<T> parse( final String dist,
		final Class<P> argType ) throws ParseException
	{
		final Matcher m = DISTRIBUTION_FORMAT.matcher( dist.trim() );
		if( !m.find() ) throw new ParseException(
				"Incorrect format, expected <dist>(p0;p1;p2), was: " + dist,
				0 );
		final List<WeightedValue<P>> params = new ArrayList<>();

		// FIXME register separate Jackson Module artifact
		if( Quantity.class.isAssignableFrom( argType ) )
			QuantityJsonModule.checkRegistered( JsonUtil.getJOM() );

		final InstanceParser<P> argParser = InstanceParser.of( argType );
		for( String valuePair : m.group( PARAMS_GROUP )
				.split( PARAM_SEPARATORS ) )
			try
			{
				if( valuePair.trim().isEmpty() ) continue; // empty parentheses
				final String[] valueWeights = valuePair
						.split( WEIGHT_SEPARATORS );
				params.add( valueWeights.length == 1 // no weight given
						? WeightedValue.of(
								argParser.parseOrTrimmed( valuePair ),
								BigDecimal.ONE )
						: WeightedValue.of(
								argParser.parseOrTrimmed( valueWeights[0] ),
								new BigDecimal( valueWeights[1].trim() ) ) );
			} catch( final Throwable t )
			{
				Thrower.rethrowUnchecked( t );
			}
		if( params.isEmpty() && argType.isEnum() )
			for( P constant : argType.getEnumConstants() )
			params.add( WeightedValue.of( constant, BigDecimal.ONE ) );
		if( !Quantity.class.isAssignableFrom( argType ) || params.isEmpty() )
			return parse( m.group( DIST_GROUP ), params );

		// convert parameter to Number type and check quantity compatibility
		final Unit<?> firstUnit = QuantityUtil
				.unitOf( params.get( 0 ).getValue() );
		final List<WeightedValue<BigDecimal>> numbers = params.stream()
				.map( wv -> WeightedValue.of( QuantityUtil
						.decimalValue( (Quantity<?>) wv.getValue(), firstUnit ),
						wv.getWeight() ) )
				.collect( Collectors.toList() );
		return (ProbabilityDistribution<T>) parse( m.group( DIST_GROUP ),
				numbers ).toQuantities( firstUnit );
	}

	/**
	 * @param <T> the type of value in the {@link ProbabilityDistribution}
	 * @param <V> the type of arguments
	 * @param name the symbol of the {@link ProbabilityDistribution}, e.g.
	 *            {@code "poisson"} or {@code "const"}
	 * @param args the arguments as a {@link List} of {@link WeightedValue}
	 *            pairs with at least a value of type {@link T} and possibly
	 *            some numeric weight
	 * @return a {@link ProbabilityDistribution}
	 */
	@SuppressWarnings( "unchecked" )
	@Override
	public <T, V> ProbabilityDistribution<T> parse( final String label,
		final List<WeightedValue<V>> args ) throws ParseException
	{
		if( args.isEmpty() )
			throw new ParseException( "Missing distribution parameters",
					label.length() );

		if( getFactory() == null )
		{
			final T value = (T) args.get( 0 ).getValue();
			LOG.warn( "Missing {}; creating '{}' as Deterministic<{}> -> {}",
					ProbabilityDistribution.Factory.class.getSimpleName(),
					label, value.getClass().getSimpleName(), value );
			return ProbabilityDistribution.createDeterministic( value );
		}

		// FIXME use some singleton/modular registration of types and labels
		switch( label.trim().toLowerCase( Locale.ROOT ) )
		{
		case "const":
		case "constant":
		case "degen":
		case "degenerate":
		case "determ":
		case "deterministic":
			return (ProbabilityDistribution<T>) getFactory()
					.createDeterministic( args.get( 0 ).getValue() );

		case "p":
		case "bool":
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
		throw new ParseException( "Unknown distribution symbol: " + label, 0 );
	}
}