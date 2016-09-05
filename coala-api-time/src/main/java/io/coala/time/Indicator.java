/* $Id: 5d2e9ca0fc795a06e4f438219e486a84ec8fbd97 $
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

import javax.measure.quantity.Quantity;

import org.jscience.physics.amount.Amount;

import io.coala.math.MeasureUtil;
import io.coala.math.Range;

/**
 * {@link Indicator} is a linear-time {@link Signal} of {@link Amount}s
 * 
 * @param <Q> the type of {@link Quantity} being indicated
 * @version $Id: 5d2e9ca0fc795a06e4f438219e486a84ec8fbd97 $
 * @author Rick van Krevelen
 */
public class Indicator<Q extends Quantity>
	extends Signal.SimpleOrdinal<Amount<Q>>
{

	public static <Q extends Quantity> Indicator<Q>
		of( final Scheduler scheduler, final Amount<Q> initialValue )
	{
		return new Indicator<Q>( scheduler, TimeInvariant.of( initialValue ) );
	}

	private final TimeInvariant<Amount<Q>> timeInvariant;

	public Indicator( final Scheduler scheduler,
		final TimeInvariant<Amount<Q>> timeInvariant )
	{
		super( scheduler, Range.infinite(), timeInvariant::get );
		this.timeInvariant = timeInvariant;
	}

	public void setValue( final Amount<Q> amount )
	{
		this.timeInvariant.set( amount );
	}

	/**
	 * @param size
	 */
	public void add( final Number value )
	{
		setValue( current().plus( MeasureUtil.toAmount( value ) ) );
	}
}