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
package io.coala.message;

import java.io.Serializable;

import io.coala.event.TimedEventID;
import io.coala.model.ModelID;
import io.coala.time.AbstractInstant;
import io.coala.time.Instant;

/**
 * {@link MessageID} sets the identifier value type of a {@link Message}
 * 
 * @version $Id$
 * 
 * @param <ID> the {@link Serializable} and {@link Comparable} value type
 * @param <I> the type of {@link Instant}
 */
@Deprecated
public class MessageID<ID extends Serializable & Comparable<ID>, I extends AbstractInstant<I>>
	extends TimedEventID<ID, I>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link MessageID} zero-arg bean constructor
	 */
	protected MessageID()
	{

	}

	/**
	 * {@link MessageID} constructor
	 * 
	 * @param modelID
	 * @param value
	 * @param instant
	 */
	public MessageID( final ModelID modelID, final ID value, final I instant )
	{
		super( modelID, value, instant );
	}

	/**
	 * {@link MessageID} factory utility method
	 * 
	 * @param modelID
	 * @param value
	 * @param instant
	 */
	public static <
		ID extends Serializable & Comparable<ID>, I extends AbstractInstant<I>>
		MessageID<ID, I>
		of( final ModelID modelID, final ID value, final I instant )
	{
		return new MessageID<>( modelID, value, instant );
	}

}
