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
package io.coala.event;

import java.io.Serializable;

import io.coala.model.ModelComponentID;
import io.coala.model.ModelID;

/**
 * {@link EventID}
 * 
 * @version $Id$
 * 
 * @param <T> the {@link Serializable} and {@link Comparable} value type
 */
@Deprecated
public class EventID<T extends Serializable & Comparable<T>>
	extends ModelComponentID<T>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link EventID} zero-arg bean constructor
	 */
	public EventID()
	{
		super();
	}

	/**
	 * {@link EventID} constructor
	 * 
	 * @param modelID
	 * @param value
	 */
	public EventID( final ModelID modelID, final T value )
	{
		super( modelID, value );
	}

}
