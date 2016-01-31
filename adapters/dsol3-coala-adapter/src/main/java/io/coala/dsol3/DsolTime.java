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
package io.coala.dsol3;

import java.math.BigDecimal;

import javax.inject.Provider;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import io.coala.exception.x.ExceptionBuilder;
import io.coala.json.x.Wrapper;
import io.coala.time.x.Instant;
import io.coala.time.x.TimeSpan;
import nl.tudelft.simulation.dsol.simtime.SimTime;
import nl.tudelft.simulation.dsol.simtime.TimeUnit;

/**
 * {@link DsolTime} extends a DSOL {@link SimTime} to become a {@link Wrapper}
 * of {@link BigDecimal} time values for maximal compatibility with COALA
 * {@link Instant} values
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class DsolTime extends SimTime<BigDecimal, BigDecimal, DsolTime>
	implements Wrapper<BigDecimal>
{

	/** the ZERO constant */
	public static final DsolTime ZERO = valueOf( 0d );

	/** the DEFAULT_UNIT - TODO read from config */
	public static final TimeUnit DEFAULT_UNIT = TimeUnit.DAY;

	private static final Unit<?> DEFAULT_QUANTITY_UNIT = resolve(
			DEFAULT_UNIT );

	/** the DEFAULT_PROVIDER constant - TODO read from config */
	public static final Provider<DsolTime> DEFAULT_PROVIDER = new Provider<DsolTime>()
	{
		@Override
		public DsolTime get()
		{
			return new DsolTime();
		}
	};

	public static Unit<?> resolve( final TimeUnit unit )
	{
		switch( unit )
		{
		case DAY:
			return NonSI.DAY;
		case HOUR:
			return NonSI.HOUR;
		case MILLISECOND:
			return TimeSpan.MILLIS;
		case MINUTE:
			return NonSI.MINUTE;
		case SECOND:
			return SI.SECOND;
		case WEEK:
			return NonSI.WEEK;
		case YEAR:
			return NonSI.YEAR;
		case UNIT:
			return Unit.ONE;
		default:
			throw ExceptionBuilder.unchecked( "Unsupported unit: " + unit )
					.build();
		}
	}

	/**
	 * @param time
	 * @return
	 */
	public static DsolTime
		valueOf( @SuppressWarnings( "rawtypes" ) final SimTime time)
	{
		try
		{
			return valueOf( TimeSpan.valueOf( (Number) time.get() ) );
		} catch( final Throwable t )
		{
			throw ExceptionBuilder
					.unchecked( t, "Problem converting time: ", time ).build();
		}
	}

	/**
	 * @param absoluteTime the {@link Instant}
	 * @return the new {@link DsolTime}
	 */
	public static DsolTime valueOf( final Instant absoluteTime )
	{
		return valueOf( absoluteTime.unwrap() );
	}

	/**
	 * @param absoluteTime the {@link TimeSpan}
	 * @return the new {@link DsolTime}
	 */
	@SuppressWarnings( "unchecked" )
	public static DsolTime valueOf( final TimeSpan absoluteTime )
	{
		return valueOf( absoluteTime.to( DEFAULT_QUANTITY_UNIT ).getValue() );
	}

	/**
	 * @param absoluteTime
	 * @return the new {@link DsolTime}
	 */
	public static DsolTime valueOf( final long absoluteTime )
	{
		return valueOf( BigDecimal.valueOf( absoluteTime ) );
	}

	/**
	 * @param absoluteTime
	 * @return the new {@link DsolTime}
	 */
	public static DsolTime valueOf( final double absoluteTime )
	{
		return valueOf( BigDecimal.valueOf( absoluteTime ) );
	}

	/**
	 * @param absoluteTime
	 * @return the new {@link DsolTime}
	 */
	public static DsolTime valueOf( final BigDecimal absoluteTime )
	{
		final DsolTime result = DEFAULT_PROVIDER.get();
		result.wrap( absoluteTime );
		return result;
	}

	/**
	 * @return the zero constant as {@link DsolTime}
	 */
	public static DsolTime zero()
	{
		return ZERO;
	}

	public Instant toInstant()
	{
		// FIXME convert to millis from which time unit?
		return Instant.valueOf( unwrap() );
	}

	/**
	 * {@link DsolTime} zero-arg bean constructor
	 */
	protected DsolTime()
	{
		super( null ); // initialize empty
	}

	private BigDecimal value;

	@Override
	public BigDecimal unwrap()
	{
		return this.value;
	}

	@Override
	public void wrap( final BigDecimal absoluteTime )
	{
		this.value = absoluteTime;
	}

	/**
	 * @deprecated please use {@link #add(Number))}
	 */
	@Deprecated
	@Override
	public void add( final BigDecimal relativeTime )
	{
		throw ExceptionBuilder.unchecked( "Please use thread-safe add(..)" )
				.build();
	}

	/**
	 * @deprecated please use {@link #subtract(BigDecimal))}
	 */
	@Deprecated
	@Override
	public void subtract( final BigDecimal relativeTime )
	{
		throw ExceptionBuilder.unchecked( "Please use thread-safe minus(..)" )
				.build();
	}

	@Override
	public BigDecimal minus( final DsolTime absoluteTime )
	{
		return unwrap().subtract( absoluteTime.unwrap() );
	}

	public DsolTime subtract( final DsolTime relativeTime )
	{
		return minus( relativeTime.unwrap() );
	}

	public DsolTime add( final DsolTime relativeTime )
	{
		return plus( relativeTime.unwrap() );
	}

	@Override
	public DsolTime copy()
	{
		return valueOf( get() );
	}

	@Override
	public BigDecimal get()
	{
		return unwrap();
	}

	@Override
	public void set( final BigDecimal absoluteTime )
	{
		wrap( absoluteTime );
	}

	@Override
	public DsolTime setZero()
	{
		return zero();
	}

}