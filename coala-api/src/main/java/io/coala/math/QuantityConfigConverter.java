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
package io.coala.math;

import java.lang.reflect.Method;

import org.aeonbits.owner.Converter;

import tec.uom.se.ComparableQuantity;

/**
 * {@link QuantityConfigConverter} for OWNER-based configurations
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class QuantityConfigConverter implements Converter<ComparableQuantity<?>>
{
	@Override
	public ComparableQuantity<?> convert( final Method method,
		final String input )
	{
		return QuantityUtil.valueOf( input );
	}
}