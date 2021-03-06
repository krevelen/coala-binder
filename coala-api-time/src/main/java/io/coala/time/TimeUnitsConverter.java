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
package io.coala.time;

import java.lang.reflect.Method;

import javax.measure.Unit;
import javax.measure.format.ParserException;

import org.aeonbits.owner.Converter;

import io.coala.math.QuantityUtil;
import tec.uom.se.quantity.Quantities;

/**
 * {@link TimeUnitsConverter} overrides
 * {@link QuantityUtil#valueOf(CharSequence)} and
 * {@link Quantities#getQuantity(CharSequence)}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class TimeUnitsConverter implements Converter<Unit<?>>
{
	@Override
	public Unit<?> convert( final Method method, final String input )
	{
		try
		{
			return TimeUnits.UNIT_FORMAT.parse( input );
		} catch( final ParserException e )
		{
			throw new IllegalArgumentException( e.getParsedString(), e );
		}
	}
}