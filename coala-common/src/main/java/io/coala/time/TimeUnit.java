/* $Id: a430a87b173a24c397020182393912dbe22cce06 $
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

import io.coala.exception.ExceptionFactory;

/**
 * {@link TimeUnit}
 */
@Deprecated
public enum TimeUnit
{
	/** */
	TICKS( "t" ),

	/** */
	NANOS( "ns" ),

	/** */
	MILLIS( "ms" ),

	/** */
	SECONDS( "s" ),

	/** */
	MINUTES( "m" ),

	/** */
	HOURS( "h" ),

	/** */
	DAYS( "d" ),

	/** */
	WEEKS( "w" ),

	;

	/** */
	private final String shortName;

	/**
	 * {@link TimeUnit} constructor
	 * 
	 * @param shortName
	 */
	private TimeUnit( final String shortName )
	{
		this.shortName = shortName;
	}

	/** @see Enum#toString() */
	@Override
	public String toString()
	{
		return this.shortName;
	}

	public static TimeUnit fromShortName( final String shortName )
	{
		for( TimeUnit unit : values() )
			if( unit.toString().equals( shortName ) ) return unit;

		throw new IllegalArgumentException(
				"TimeUnit unknown with short name: '" + shortName + "'" );
	}

	/**
	 * @param fromTime
	 * @param unit
	 * @return
	 */
	public Number convertFrom( final Instant<?> fromInstant )
	{
		return convertFrom( fromInstant.getValue(), fromInstant.getUnit() );
	}

	/**
	 * @param fromTime
	 * @param unit
	 * @return
	 */
	public Number convertFrom( final Number fromTime, final TimeUnit fromUnit )
	{
		if( fromTime == null ) return null;

		if( fromUnit == null ) throw ExceptionFactory
				.createUnchecked( "No TimeUnit specified for conversion" );

		if( equals( fromUnit ) ) // no conversion necessary
			return fromTime;

		if( fromUnit == TICKS ) throw ExceptionFactory.createUnchecked(
				"Illegal conversion from unit {}", fromUnit.name() );

		switch( this )
		{
		case NANOS:
			return 1000.0
					* MILLIS.convertFrom( fromTime, fromUnit ).doubleValue();
		case MILLIS:
			return fromUnit.ordinal() > ordinal()
					? 1000.0 * SECONDS.convertFrom( fromTime, fromUnit )
							.doubleValue()
					: NANOS.convertFrom( fromTime, fromUnit ).doubleValue()
							/ 1000.0;
		case SECONDS:
			return fromUnit.ordinal() > ordinal()
					? 60.0 * MINUTES.convertFrom( fromTime, fromUnit )
							.doubleValue()
					: MILLIS.convertFrom( fromTime, fromUnit ).doubleValue()
							/ 1000.0;
		case MINUTES:
			return fromUnit.ordinal() > ordinal()
					? 60.0 * HOURS.convertFrom( fromTime, fromUnit )
							.doubleValue()
					: SECONDS.convertFrom( fromTime, fromUnit ).doubleValue()
							/ 60.0;
		case HOURS:
			return fromUnit.ordinal() > ordinal()
					? 24.0 * DAYS.convertFrom( fromTime, fromUnit )
							.doubleValue()
					: MINUTES.convertFrom( fromTime, fromUnit ).doubleValue()
							/ 60.0;
		case DAYS:
			return fromUnit.ordinal() > ordinal()
					? 7.0 * WEEKS.convertFrom( fromTime, fromUnit )
							.doubleValue()
					: HOURS.convertFrom( fromTime, fromUnit ).doubleValue()
							/ 24.0;
		case WEEKS:
			return DAYS.convertFrom( fromTime, fromUnit ).doubleValue() / 7.0;

		case TICKS:
			throw ExceptionFactory.createUnchecked( "Can't convert to {}",
					this );
		}
		throw ExceptionFactory.createUnchecked( "Unknown from unit {}",
				fromUnit );
	}

}
