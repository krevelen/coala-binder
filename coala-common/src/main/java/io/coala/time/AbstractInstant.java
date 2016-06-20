/* $Id: 34cb0fcc5b4ad7f7fb2f5f5e43fadf1efcd2bdfe $
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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.coala.json.JSONConvertible;
import io.coala.json.JsonUtil;
import io.coala.log.InjectLogger;

/**
 * {@link AbstractInstant}
 * 
 * @version $Id: 34cb0fcc5b4ad7f7fb2f5f5e43fadf1efcd2bdfe $
 * 
 * @param <THIS> the concrete {@link AbstractInstant} type
 */
// @Embeddable
@JsonInclude( Include.NON_NULL )
@Deprecated
public abstract class AbstractInstant<THIS extends AbstractInstant<THIS>>
// extends Number
	implements Instant<THIS>, JSONConvertible<THIS>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private static Logger LOG;

	/** */
	// @Embedded
	private ClockID clockID;

	/** */
	private Number value;

	/** */
	private TimeUnit unit;

	/**
	 * {@link AbstractInstant} constructor
	 */
	protected AbstractInstant()
	{
		//
	}

	/**
	 * {@link AbstractInstant} constructor
	 * 
	 * @param source
	 * @param value
	 * @param unit
	 */
	protected AbstractInstant( final ClockID source, final Number value,
		final TimeUnit unit )
	{
		setClockID( source );
		setValue( value );
		setUnit( unit );
	}

	@Override
	public synchronized ClockID getClockID()
	{
		return this.clockID;
	}

	/**
	 * @param source the source to set
	 */
	protected synchronized void setClockID( final ClockID source )
	{
		this.clockID = source;
	}

	@Override
	public synchronized Number getValue()
	{
		return this.value;
	}

	/**
	 * @param value the value to set
	 */
	protected synchronized void setValue( final Number value )
	{
		this.value = value;
	}

	@Override
	public synchronized TimeUnit getUnit()
	{
		return this.unit;
	}

	/**
	 * @param unit the unit to set
	 */
	protected synchronized void setUnit( final TimeUnit unit )
	{
		this.unit = unit;
	}

	/**
	 * @param clockID the {@link ClockID}
	 * @param value the {@link Number} value
	 * @param unit the {@link TimeUnit}
	 * @return this {@link THIS} object
	 */
	@SuppressWarnings( "unchecked" )
	public THIS withTime( final ClockID clockID, final Number value,
		final TimeUnit unit )
	{
		setValue( value );
		setUnit( unit );
		setClockID( clockID );
		return (THIS) this;
	}

	@Override
	public String toString()
	{
		return String.format( "%s %s @%s", getValue(), getUnit(),
				getClockID() );
	}

	@Override
	public int compareTo( final Instant<?> other )
	{
		final int compareClockID = getClockID().compareTo( other.getClockID() );
		if( compareClockID != 0 ) return compareClockID;

		return Double.compare( doubleValue(),
				other.toUnit( getUnit() ).doubleValue() );
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((this.clockID == null) ? 0 : this.clockID.hashCode());
		result = prime * result
				+ ((this.unit == null) ? 0 : this.unit.hashCode());
		result = prime * result
				+ ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean equals( Object obj )
	{
		if( this == obj ) return true;
		if( obj == null || getClass() != obj.getClass() ) return false;

		final THIS other = (THIS) obj;

		if( getClockID() == null )
		{
			if( other.getClockID() != null ) return false;
		} else if( !getClockID().equals( other.getClockID() ) ) return false;

		if( this.getUnit() != other.getUnit() ) return false;

		if( this.getValue() == null )
		{
			if( other.getValue() != null ) return false;
		} else if( !this.getValue().equals( other.getValue() ) ) return false;

		return true;
	}

	@Override
	public int intValue()
	{
		return getValue().intValue();
	}

	@Override
	public long longValue()
	{
		return getValue().longValue();
	}

	@Override
	public float floatValue()
	{
		return getValue().floatValue();
	}

	@Override
	public double doubleValue()
	{
		return getValue().doubleValue();
	}

	@Override
	public THIS toNanoseconds()
	{
		return toUnit( TimeUnit.NANOS );
	}

	@Override
	public THIS toMilliseconds()
	{
		return toUnit( TimeUnit.MILLIS );
	}

	@JsonIgnore
	public long getMillis()
	{
		return toMilliseconds().longValue();
	}

	@Override
	public THIS toSeconds()
	{
		return toUnit( TimeUnit.SECONDS );
	}

	@Override
	public THIS toMinutes()
	{
		return toUnit( TimeUnit.MINUTES );
	}

	@Override
	public THIS toHours()
	{
		return toUnit( TimeUnit.HOURS );
	}

	@Override
	public THIS toDays()
	{
		return toUnit( TimeUnit.DAYS );
	}

	@Override
	public THIS toWeeks()
	{
		return toUnit( TimeUnit.WEEKS );
	}

	@Override
	public Date toDate()
	{
		return new Date( getMillis() );
	}

	@Override
	public Date toDate( final Date offset )
	{
		return new Date( offset.getTime() + getMillis() );
	}

	@Override
	public DateTime toDateTime()
	{
		return new DateTime( getMillis(), DEFAULT_DATETIME_ZONE );
	}

	@Override
	public DateTime toDateTime( final DateTime offset )
	{
		return offset.plus( getMillis() );
	}

	@Override
	public Calendar toCalendar()
	{
		final Calendar result = GregorianCalendar
				.getInstance( DEFAULT_TIME_ZONE, DEFAULT_LOCALE );
		result.setTimeInMillis( getMillis() );
		return result;
	}

	@Override
	public Calendar toCalendar( final Date offset )
	{
		final Calendar result = GregorianCalendar
				.getInstance( DEFAULT_TIME_ZONE, DEFAULT_LOCALE );
		result.setTimeInMillis(
				offset.getTime() + toMilliseconds().longValue() );
		return result;
	}

	@Override
	public THIS plus( final Number value, final TimeUnit unit )
	{
		return plus( getUnit().convertFrom( value, unit ).doubleValue() );
	}

	@Override
	public THIS plus( final Instant<?> value )
	{
		return plus( value.toUnit( getUnit() ).doubleValue() );
	}

	@Override
	public THIS minus( final Number value )
	{
		return plus( -value.doubleValue() );
	}

	@Override
	public THIS minus( final Number value, final TimeUnit unit )
	{
		return plus( -value.doubleValue(), unit );
	}

	@Override
	public THIS minus( final Instant<?> value )
	{
		return minus( value.toUnit( getUnit() ).doubleValue() );
	}

	@Override
	public THIS multipliedBy( final Number factor )
	{
		return minus( (factor.doubleValue() - 1) * getValue().doubleValue() );
	}

	@Override
	public THIS dividedBy( final Number factor )
	{
		return multipliedBy( 1. / factor.doubleValue() );
	}

	@Override
	public Number dividedBy( final Instant<?> value )
	{
		return doubleValue() / value.toUnit( getUnit() ).doubleValue();
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public THIS max( final THIS... others )
	{
		THIS result = (THIS) this;
		if( others != null && others.length != 0 ) for( THIS other : others )
			if( other.isAfter( result ) ) result = other;
		return result;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public THIS min( final THIS... others )
	{
		THIS result = (THIS) this;
		if( others != null && others.length != 0 ) for( THIS other : others )
			if( other.isBefore( result ) ) result = other;
		return result;
	}

	@Override
	public boolean isBefore( final Number value )
	{
		return isBefore( value, getUnit() );
	}

	@Override
	public boolean isBefore( final Number value, final TimeUnit unit )
	{
		return doubleValue() < getUnit().convertFrom( value, unit )
				.doubleValue();
	}

	@Override
	public boolean isBefore( final Instant<?> value )
	{
		return isBefore( value.getValue(), value.getUnit() );
	}

	@Override
	public boolean isOnOrBefore( final Number value )
	{
		return !isAfter( value );
	}

	@Override
	public boolean isOnOrBefore( final Number value, final TimeUnit unit )
	{
		return !isAfter( value, unit );
	}

	@Override
	public boolean isOnOrBefore( final Instant<?> value )
	{
		return !isAfter( value );
	}

	@Override
	public boolean isOnOrAfter( final Number value )
	{
		return !isBefore( value );
	}

	@Override
	public boolean isOnOrAfter( final Number value, final TimeUnit unit )
	{
		return !isBefore( value, unit );
	}

	@Override
	public boolean isOnOrAfter( final Instant<?> value )
	{
		return !isBefore( value );
	}

	@Override
	public boolean isAfter( final Number value )
	{
		return isAfter( value, getUnit() );
	}

	@Override
	public boolean isAfter( final Number value, final TimeUnit unit )
	{
		// try
		// {
		return doubleValue() > getUnit().convertFrom( value, unit )
				.doubleValue();
		// } catch (final CoalaRuntimeException e)
		// {
		// return value.doubleValue() > doubleValue();
		// }
	}

	@Override
	public boolean isAfter( final Instant<?> value )
	{
		return isAfter( value.getValue(), value.getUnit() );
	}

	@Override
	public Iterable<THIS> getRange( final Instant<?> interval,
		final Instant<?> max )
	{
		@SuppressWarnings( "unchecked" )
		final Iterator<THIS> iterator = Range.of( (THIS) this, interval, max );
		return new Iterable<THIS>()
		{
			@Override
			public Iterator<THIS> iterator()
			{
				return iterator;
			}
		};
	}

	@Override
	public String toJSON()
	{
		return JsonUtil.toJSON( this );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public THIS fromJSON( final String jsonValue )
	{
		return (THIS) JsonUtil.valueOf( jsonValue, getClass() );
	}

}
