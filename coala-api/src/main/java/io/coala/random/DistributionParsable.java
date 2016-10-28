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

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Converter;

import io.coala.random.ProbabilityDistribution.Parser;

/**
 * {@link DistributionParsable}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface DistributionParsable<T>
{
	/**
	 * @param <T> the type of values to draw
	 * @param dist the {@link String} representation
	 * @param paramType the type of parameter to parse
	 * @return a {@link ProbabilityDistribution} of {@link T} values
	 * @throws ParseException
	 * @see {@link ProbabilityDistribution.Parser#parse(String,Class)}
	 */
	ProbabilityDistribution<T> parse( ProbabilityDistribution.Parser distParser,
		Class<?> paramType ) throws ParseException;

	/**
	 * @param <T> the type of values to draw
	 * @param dist the {@link String} representation
	 * @return a {@link ProbabilityDistribution} of {@link T} values
	 * @throws ParseException
	 * @see {@link ProbabilityDistribution.Parser#parse(String,Class)}
	 */
	default ProbabilityDistribution<T>
		parse( ProbabilityDistribution.Parser distParser ) throws ParseException
	{
		return parse( distParser, BigDecimal.class );
	}

	/**
	 * {@link FromString} utility for
	 * {@link Config}-interfaces
	 * 
	 * @param <T> the result type
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public class FromString<T>
		implements Converter<DistributionParsable<T>>
	{
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		@Override
		public DistributionParsable<T> convert( final Method method,
			final String input )
		{
			return new DistributionParsable()
			{
				@Override
				public ProbabilityDistribution parse( final Parser p,
					final Class t ) throws ParseException
				{
					return p.parse( input, t );
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