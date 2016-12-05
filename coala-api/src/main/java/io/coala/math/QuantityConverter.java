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

import org.aeonbits.owner.Config;

import io.coala.config.JsonConverter;
import tec.uom.se.ComparableQuantity;

/**
 * {@link QuantityConverter} will parse a raw {@link ComparableQuantity}.
 * <p>
 * <b>WARNING</b>: {@link ClassCastException} (due to parsed vs. declared
 * {@link Quantity} dimension mismatch) is only thrown at run-time upon access
 * to the respective {@link Config} property's proxy method access
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class QuantityConverter extends JsonConverter<ComparableQuantity<?>>
{
	@Override
	public ComparableQuantity<?> convert( final Method method,
		final String input )
	{
		return QuantityUtil.valueOf( input );
	}
}