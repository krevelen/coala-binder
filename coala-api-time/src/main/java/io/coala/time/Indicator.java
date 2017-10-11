/* $Id: 01dc7612a810c2dd7cb72089ff66146f1f91fbda $
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

import java.util.concurrent.atomic.AtomicReference;

import javax.measure.Quantity;

import io.coala.math.QuantityUtil;
import io.coala.math.Range;
import tec.uom.se.ComparableQuantity;

/**
 * {@link Indicator} is a linear-time {@link Signal} of {@link Amount}s
 * 
 * @param <Q> the type of {@link Quantity} being indicated
 * @version $Id: 01dc7612a810c2dd7cb72089ff66146f1f91fbda $
 * @author Rick van Krevelen
 */
public class Indicator<Q extends Quantity<Q>>
	extends Signal.SimpleOrdinal<ComparableQuantity<Q>>
{

	public static <Q extends Quantity<Q>> Indicator<Q>
		of( final Scheduler scheduler, final Quantity<Q> initialValue )
	{
		return new Indicator<Q>( scheduler,
				new AtomicReference<>( QuantityUtil.valueOf( initialValue ) ) );
	}

	private final AtomicReference<ComparableQuantity<Q>> value;

	public Indicator( final Scheduler scheduler,
		final AtomicReference<ComparableQuantity<Q>> value )
	{
		super( scheduler, Range.infinite(), t -> value.get() );
		this.value = value;
	}

	public void setValue( final Quantity<Q> amount )
	{
		this.value.set( QuantityUtil.valueOf( amount ) );
	}

	/**
	 * @param size
	 */
	@SuppressWarnings( "unchecked" )
	public void add( final Number value )
	{
		setValue( (Q) current()
				.add( QuantityUtil.valueOf( value, current().getUnit() ) ) );
	}
}