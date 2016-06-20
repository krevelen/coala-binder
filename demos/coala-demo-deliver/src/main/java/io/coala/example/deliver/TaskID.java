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
package io.coala.example.deliver;

import io.coala.model.ModelComponentID;
import io.coala.model.ModelID;

/**
 * {@link TaskID}
 */
public class TaskID extends ModelComponentID<String>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static long TASK_ID_COUNT = 0;

	/**
	 * {@link TaskID} zero-arg bean constructor
	 */
	protected TaskID()
	{
	}

	/**
	 * {@link TaskID} constructor
	 * 
	 * @param companyID
	 */
	public TaskID( final ModelID companyID )
	{
		super( companyID, "task" + TASK_ID_COUNT++ );
	}
}