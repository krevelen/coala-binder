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

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Converter;

/**
 * {@link DistributionConverter} utility for {@link Config}-interfaces
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class DistributionConverter<T>
	implements Converter<DistributionParsable<T>>
{
	@Override
	public DistributionParsable<T> convert( final Method method,
		final String input )
	{
		return ( p, t ) ->
		{
			return p.parse( input, t );
		};
	}
}