/* $Id: e8c61e1583154eddc4b550836c10dd09ffeb6e53 $
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
 */
package io.coala.time;

import org.apache.logging.log4j.Logger;

import io.coala.exception.CoalaRuntimeException;
import io.coala.log.LogUtil;

/**
 * {@link SimDuration}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class SimDuration extends AbstractInstant<SimDuration>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final Logger LOG = LogUtil.getLogger( SimDuration.class );

	/** */
	public static SimDuration ZERO = new SimDuration( 0, TimeUnit.MILLIS );

	/** */
	// private static final TimeUnit baseUnit = TimeUnit.MILLIS;

	/**
	 * {@link SimDuration} zero-arg bean constructor
	 */
	protected SimDuration()
	{
		// empty
	}

	/**
	 * {@link SimDuration} constructor
	 * 
	 * @param value
	 * @param unit
	 */
	public SimDuration( final Number value, final TimeUnit unit )
	{
		setValue( value );
		setUnit( unit );
	}

	@Override
	public SimDuration plus( final Number value )
	{
		return new SimDuration( getValue().doubleValue() + value.doubleValue(),
				getUnit() );
	}

	@Override
	public SimDuration toUnit( final TimeUnit unit )
	{
		Number toValue = null;
		try
		{
			toValue = unit.convertFrom( getValue(), getUnit() );
		} catch( final CoalaRuntimeException e )
		{
			LOG.warn( "Problem converting " + toString() + " to " + unit.name()
					+ ": " + e.getMessage() );
			return this;
		}
		return new SimDuration( toValue == null ? getValue() : toValue, unit );
	}

}
