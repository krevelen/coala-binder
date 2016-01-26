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
 * 
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.event;

import java.io.Serializable;

import io.coala.model.ModelID;
import io.coala.time.Instant;
import io.coala.time.Timed;

/**
 * {@link TimedEventID}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <T> the {@link Serializable} and {@link Comparable} value type
 * @param <I> the {@link Instant} type
 */
public class TimedEventID<T extends Serializable & Comparable<T>, I extends Instant<I>>
		extends EventID<T> implements Timed<I>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** the {@link Instant} the identified {@link Event} occurs */
	private I time;

	/**
	 * {@link TimedEventID} zero-arg bean constructor
	 */
	protected TimedEventID()
	{
		super();
	}

	/**
	 * {@link TimedEventID} constructor
	 * 
	 * @param modelID the {@link ModelID}
	 * @param value the (unique) value of this identifier
	 * @param instant the {@link Instant} the identified {@link Event} occurs
	 */
	public TimedEventID(final ModelID modelID, final T value, final I instant)
	{
		super(modelID, value);
		this.time = instant;
	}

	@Override
	public I getTime()
	{
		return this.time;
	}

	@Override
	public String toString()
	{
		return String.format("%s t=%s", super.toString(),
				getTime() == null ? "?" : getTime().toString());
	}

}
