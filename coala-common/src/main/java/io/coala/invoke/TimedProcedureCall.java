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
package io.coala.invoke;

import io.coala.event.TimedEvent;
import io.coala.event.TimedEventID;

/**
 * {@link TimedProcedureCall}
 * 
 * @param <ID> the type of {@link TimedEventID} for time-ordered event identity
 */
public abstract class TimedProcedureCall<ID extends TimedEventID<?, ?>>
	extends ProcedureCall<ID> implements TimedEvent<ID>
{

	/** */
	private static final long serialVersionUID = 1L;

}
