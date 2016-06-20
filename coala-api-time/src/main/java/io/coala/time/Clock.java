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

import io.coala.name.Identifiable;

import java.util.Calendar;
import java.util.Date;

/**
 * {@link Clock}
 * 
 * @param <ID> the type of {@link ClockID} for this {@link Clock} type
 * @param <I> the type of {@link Instant} for this {@link Clock} type
 * @param <THIS> the (sub)type of {@link Clock} to build
 * @deprecated please use {@link io.coala.time.x.Instant}
 */
@Deprecated
public interface Clock<I extends Instant<I>> extends Identifiable<ClockID>
{

	/** @return a new {@link Instant} with this {@link Clock} as source */
	I newInstant();

	/**
	 * @param value the {@link Number} to return as {@link Instant}
	 * @return a new {@link Instant} with this {@link Clock} as source
	 */
	I newInstant(Number value);

	/**
	 * @param value the {@link Number} to return as {@link Instant}
	 * @param unit the {@link TimeUnit} to use for the {@link Instant}
	 * @return a new {@link Instant} with this {@link Clock} as source
	 */
	I newInstant(Number value, TimeUnit unit);

	/**
	 * @param date the {@link Date} to return as {@link Instant}
	 * @return a new {@link Instant} with this {@link Clock} as source
	 */
	I newInstant(Date date);

	/**
	 * @param calendar the {@link Calendar} to return as {@link Instant}
	 * @return a new {@link Instant} with this {@link Clock} as source
	 */
	I newInstant(Calendar calendar);

	/** @return a new {@link Instant} with this {@link Clock} as source */
	// I newInstant(DateTime dateTime);

}
