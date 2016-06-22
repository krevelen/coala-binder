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

/**
 * {@link TimedEvent}
 * 
 * @param <ID> the type of {@link TimedEventID} for time-ordered event identity
 */
@Deprecated
public interface TimedEvent<ID extends TimedEventID<?, ?>> extends Event<ID>
{

	/**
	 * {@link Builder}
	 * 
	 * @param <ID>
	 * @param <E>
	 * @param <THIS>
	 */
	public interface Builder<ID extends TimedEventID<?, ?>, E extends TimedEvent<ID>, THIS extends Builder<ID, E, THIS>>
		extends Event.Builder<ID, E, THIS>
	{
		// just a tag
	}
}
