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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;

import javax.measure.Quantity;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Converter;

/**
 * {@link DistributionParsable}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface DistributionParsable
{
	/**
	 * @param <T> the type of values to draw
	 * @param paramType the type of parameter to parse
	 * @return a {@link ProbabilityDistribution} of {@link T} values
	 * @throws ParseException
	 * @see {@link ProbabilityDistribution.Parser#parse(String,Class)}
	 */
	ProbabilityDistribution<?> parse( Class<?> paramType )
		throws ParseException;

	default <T> ProbabilityDistribution<T>
		parseAndDraw( final Class<T> valueType ) throws ParseException
	{
		return parse( valueType ).ofType( valueType );
	}

	/**
	 * Parse numeric arguments using {@link BigDecimal#BigDecimal(String)}
	 * 
	 * @return a {@link ProbabilityDistribution} of {@link Number} values
	 * @throws ParseException if the distribution does not accept
	 *             {@link BigDecimal} parameters
	 * @see {@link ProbabilityDistribution.Parser#parse(String)}
	 */
//	@SuppressWarnings( "unchecked" )
	default ProbabilityDistribution<?> parse() throws ParseException
	{
		return parse( null );
	}

	default ProbabilityDistribution<Number> parseAndDrawNumeric()
		throws ParseException
	{
		return parse().ofType( Number.class );
	}

	/**
	 * @param <Q> the type of {@link Quantity} to draw
	 * @return a {@link QuantityDistribution}
	 * @throws ParseException if the distribution does not accept
	 *             {@link Quantity} parameters
	 * @see {@link ProbabilityDistribution.Parser#parse(String,Class)}
	 */
	@SuppressWarnings( "unchecked" )
	default QuantityDistribution<?> parseQuantity() throws ParseException
	{
		return parseQuantity( Quantity.class );
	}

	/**
	 * @param <Q> the type of {@link Quantity} to draw
	 * @param dimension the {@link Quantity} type
	 * @return a {@link QuantityDistribution} of {@link Q} values
	 * @throws ParseException if the distribution does not accept
	 *             {@link Quantity} parameters
	 * @see {@link ProbabilityDistribution.Parser#parse(String,Class)}
	 */
//	@SuppressWarnings( "unchecked" )
	default <Q extends Quantity<Q>> QuantityDistribution<Q>
		parseQuantity( final Class<Q> dimension ) throws ParseException
	{
		return QuantityDistribution.of( parse( Quantity.class )
				.map( q -> ((Quantity<?>) q).asType( dimension ) ) );
	}

	/**
	 * "getter" for building result
	 * 
	 * @return the configured {@link ProbabilityDistribution.Parser}, default
	 *         wraps a basic {@link DistributionFactory}
	 */
	default ProbabilityDistribution.Parser parser()
	{
		return new DistributionParser( DistributionFactory.instance() );
	}

	/**
	 * builder-like "setter" to allow chaining
	 * 
	 * @param factory
	 * @return reset the configured {@link ProbabilityDistribution.Parser},
	 *         default wraps a basic {@link DistributionFactory}
	 */
	default DistributionParsable with( final DistributionFactory factory )
	{
		return with( new DistributionParser( factory ) );
	}

	/**
	 * builder-like "setter" to allow chaining
	 * 
	 * @param parser
	 * @return reset the configured {@link ProbabilityDistribution.Parser},
	 *         default wraps a basic {@link DistributionFactory}
	 */
	default DistributionParsable
		with( final ProbabilityDistribution.Parser parser )
	{
		final DistributionParsable self = this;
		return new DistributionParsable()
		{
			@Override
			public ProbabilityDistribution.Parser parser()
			{
				return parser;
			}

			@Override
			public ProbabilityDistribution<?> parse( final Class<?> paramType )
				throws ParseException
			{
				return self.parse( paramType );
			}
		};
	}

	/**
	 * {@link FromString} utility for {@link Config}-interfaces
	 * 
	 * @param <T> the result type
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public class FromString implements Converter<DistributionParsable>
	{
		@Override
		public DistributionParsable convert( final Method method,
			final String input )
		{
			return new DistributionParsable()
			{
				@Override
				public ProbabilityDistribution<?>
					parse( final Class<?> paramType ) throws ParseException
				{
					return paramType == null ? parser().parse( input )
							: parser().parse( input, paramType );
				}

				@Override
				public String toString()
				{
					return input;
				}
			};
		}
	}
}