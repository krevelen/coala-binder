/* $Id$
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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * {@link OldInstant}
 * 
 * @version $Id$
 * @param <THIS> the concrete type of {@link OldInstant} to compare/build
 */
@Deprecated
public interface OldInstant<THIS extends OldInstant<THIS>>
	extends Serializable, Comparable<AbstractInstant<?>>
{

	/**
	 * The default time zone used to generate {@link Calendar} instances.
	 * Greenwich Mean Time (GMT) is practically equivalent with Universal Time
	 * Coordinated (UTC), the international atom time standard.
	 */
	TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

	/**
	 * The default time zone used to generate {@link Calendar} instances.
	 * Greenwich Mean Time (GMT) is practically equivalent with Universal Time
	 * Coordinated (UTC), the international atom time standard.
	 */
	DateTimeZone DEFAULT_DATETIME_ZONE = DateTimeZone.UTC;

	/** the default locale used to generate {@link Calendar} instances */
	Locale DEFAULT_LOCALE = Locale.ROOT;

	/** @return ID of the source clock generating this time instant */
	ClockID getClockID();

	/** @return the value of this {@link OldInstant} as {@link Number} */
	Number getValue();

	/** @see Number#intValue() */
	int intValue();

	/** @see Number#longValue() */
	long longValue();

	/** @see Number#floatValue() */
	float floatValue();

	/** @see Number#doubleValue() */
	double doubleValue();

	/** @return the {@link TimeUnit} of this {@link OldInstant} */
	TimeUnit getUnit();

	THIS plus( Number value );

	THIS plus( Number value, TimeUnit unit );

	THIS plus( OldInstant<?> value );

	THIS minus( Number value );

	THIS minus( Number value, TimeUnit unit );

	THIS minus( OldInstant<?> value );

	THIS multipliedBy( Number factor );

	THIS dividedBy( Number factor );

	@SuppressWarnings( "unchecked" )
	THIS max( THIS... others );

	@SuppressWarnings( "unchecked" )
	THIS min( THIS... others );

	boolean isBefore( Number value );

	boolean isBefore( Number value, TimeUnit unit );

	boolean isBefore( OldInstant<?> value );

	boolean isOnOrBefore( Number value );

	boolean isOnOrBefore( Number value, TimeUnit unit );

	boolean isOnOrBefore( OldInstant<?> value );

	boolean isAfter( Number value );

	boolean isAfter( Number value, TimeUnit unit );

	boolean isAfter( OldInstant<?> value );

	boolean isOnOrAfter( Number value );

	boolean isOnOrAfter( Number value, TimeUnit unit );

	boolean isOnOrAfter( OldInstant<?> value );

	Number dividedBy( OldInstant<?> value );

	/**
	 * Compare this {@link OldInstant} with another based on {@link ClockID}s and
	 * values (in base {@link TimeUnit}s)
	 * 
	 * @param other the final concrete {@link OldInstant} of the same type
	 * @return {@code x < 0} if this {@link OldInstant} is smaller, {@code x == 0}
	 *         if equal, and {@code x > 0} if greater than the specified
	 *         {@code other}
	 * 
	 * @see java.lang.Comparable#compareTo(Object)
	 */
	@Override
	int compareTo( AbstractInstant<?> other );

//	@JsonIgnore
//	TimeUnit getBaseUnit();

	THIS toUnit( TimeUnit unit );

//	THIS toBaseUnit();

	THIS toNanoseconds();

	THIS toMilliseconds();

	THIS toSeconds();

	THIS toMinutes();

	THIS toHours();

	THIS toDays();

	THIS toWeeks();

	/** @return the value of this {@link OldInstant} as {@link Date} */
	Date toDate();

	/** @return the value of this {@link OldInstant} as {@link Date} */
	Date toDate( Date offset );

	/** @return the value of this {@link OldInstant} as {@link DateTime} */
	DateTime toDateTime();

	/** @return the value of this {@link OldInstant} as {@link DateTime} */
	DateTime toDateTime( DateTime offset );

	/** @return the value of this {@link OldInstant} as {@link Calendar} */
	Calendar toCalendar();

	/** @return the value of this {@link OldInstant} as {@link Calendar} */
	Calendar toCalendar( Date offset );

	/**
	 * @param interval the duration between iterations
	 * @param max the (exclusive) limit, or {@code null} for unlimited
	 * @return the {@link Iterable} range of {@link OldInstant}s starting with this
	 *         {@link THIS}
	 */
	Iterable<THIS> getRange( OldInstant<?> interval, OldInstant<?> max );

	/**
	 * {@link Range}
	 *
	 * @param <I>
	 */
	class Range<I extends OldInstant<?>> implements Iterator<I>
	{

		/** */
		private final OldInstant<?> interval;

		/** */
		private final OldInstant<?> max;

		/** */
		private I time;

		/**
		 * {@link Range} constructor
		 * 
		 * @param start
		 * @param interval
		 * @param max the (exclusive) limit, or {@code null} for unlimited
		 */
		protected Range( final I start, final OldInstant<?> interval,
			final OldInstant<?> max )
		{
			this.interval = interval;
			this.max = max;
			this.time = start;
		}

		/**
		 * {@link Range} constructor
		 * 
		 * @param start the start {@link OldInstant}
		 * @param interval the duration between iterations
		 * @param max the (exclusive) limit, or {@code null} for unlimited
		 */
		public static <I extends OldInstant<?>> Range<I> of( final I start,
			final OldInstant<?> interval, final OldInstant<?> max )
		{
			return new Range<I>( start, interval, max );
		}

		/** @see Iterator#hasNext() */
		@Override
		public boolean hasNext()
		{
			return max == null || this.time.isBefore( max );
		}

		/** @see Iterator#next() */
		@SuppressWarnings( "unchecked" )
		@Override
		public I next()
		{
			final I result = this.time;
			this.time = (I) this.time.plus( this.interval );
			return result;
		}

		/** @see Iterator#remove() */
		@Override
		public void remove()
		{
			throw new IllegalStateException( "READ-ONLY RANGE ITERATOR" );
		}
	}

}