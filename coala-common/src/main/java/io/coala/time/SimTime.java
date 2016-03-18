/* $Id: 705ca0d273e0582c7ee098f61066703449a55692 $
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

import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import io.coala.exception.CoalaRuntimeException;
import io.coala.json.JSONConvertible;
import io.coala.log.LogUtil;

/**
 * {@link SimTime} is an {@link Instant} with a particular base unit and
 * implementing {@link JSONConvertible}
 * 
 * @version $Id: 705ca0d273e0582c7ee098f61066703449a55692 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class SimTime extends AbstractInstant<SimTime>
{

	/**
	 * {@link Factory}
	 * 
	 * @version $Id: 705ca0d273e0582c7ee098f61066703449a55692 $
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	public interface Factory extends io.coala.factory.Factory
	{

		/**
		 * @param value
		 * @param unit
		 * @return the new {@link SimTime} object
		 */
		SimTime create( final Number value, final TimeUnit unit );

	}

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final Logger LOG = LogUtil.getLogger( SimTime.class );

	/** */
	public static final SimTime ZERO = new SimTime( null, 0, TimeUnit.MILLIS,
			null );

	/** */
	// private TimeUnit baseUnit;

	/** redundant storage */
	private Date isoTime;

	/**
	 * {@link SimTime} zero-arg bean constructor
	 */
	protected SimTime()
	{
		// empty
	}

	/**
	 * {@link SimTime} constructor
	 * 
	 * @param source
	 * @param value
	 * @param unit
	 * @param offset
	 */
	public SimTime( final ClockID source, final Number value,
		final TimeUnit unit, final Date offset )
	{
		// this.baseUnit = baseUnit;
		setValue( value );
		setUnit( unit );
		setClockID( source );
		Date isoDate = null;
		if( offset != null ) try
		{
			isoDate = new Date( offset.getTime()
					+ TimeUnit.MILLIS.convertFrom( value, unit ).longValue() );
		} catch( final RuntimeException ignore )
		{
			// LOG.warn("Problem converting to ISO date", e);
		}
		setIsoTime( isoDate );
	}

	/**
	 * @return the isoTime
	 */
	public Date getIsoTime()
	{
		return this.isoTime;
	}

	/**
	 * @param isoTime the isoTime to set
	 */
	protected void setIsoTime( final Date isoTime )
	{
		this.isoTime = isoTime;
	}

	/** */
	public static final String READABLE_DATETIME_SHORT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	@Override
	public String toString()
	{
		final StringBuilder result = new StringBuilder( String.format(
				"%1.4f%s", getValue().doubleValue(), getUnit().toString() ) );
		if( getClockID() != null && getClockID().getValue() != null
				&& !getClockID().getValue().isEmpty() )
			result.append( " @" ).append( getClockID().getValue() );
		if( getIsoTime() != null ) result.append( " (" )
				.append( new DateTime( getIsoTime() )
						.toString( READABLE_DATETIME_SHORT_FORMAT ) )
				.append( ')' );
		return result.toString();
	}

	/**
	 * @return the derived offset, i.e. the ISO date for {@link SimTime#ZERO}
	 */
	public Date calcOffset()
	{
		long millis = 0;
		try
		{
			// avoid using: getMillis() or toMilliseconds() or toUnit()
			millis = TimeUnit.MILLIS.convertFrom( getValue(), getUnit() )
					.longValue();
		} catch( final CoalaRuntimeException e )
		{
			// LOG.warn("Problem converting to " + TimeUnit.MILLIS, e);
			millis = getValue().longValue();
		}
		return new Date(
				getIsoTime() == null ? 0 : getIsoTime().getTime() - millis );
	}

	@Override
	public SimTime toUnit( final TimeUnit unit )
	{
		Number toValue = null;
		try
		{
			toValue = unit.convertFrom( getValue(), getUnit() );
		} catch( final CoalaRuntimeException e )
		{
			LOG.warn( "Problem converting " + toString() + " to " + unit.name(),
					e );
			return this;
		}
		if( toValue == null ) LOG.warn(
				"Problem converting " + toString() + " to " + unit.name() );
		return new SimTime(
				// getBaseUnit(),
				getClockID(), toValue == null ? getValue() : toValue, unit,
				calcOffset() );
	}

	@Override
	public SimTime plus( final Number value )
	{
		return new SimTime(
				// getBaseUnit(),
				getClockID(), getValue().doubleValue() + value.doubleValue(),
				getUnit(), calcOffset() );
	}
}