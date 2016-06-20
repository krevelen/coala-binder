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
 * {@link FleetID}
 */
public class FleetID extends ModelComponentID<String>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link FleetID} zero-arg bean constructor
	 */
	protected FleetID()
	{
	}

	/**
	 * {@link FleetID} constructor
	 * 
	 * @param companyID
	 * @param fleetName
	 */
	public FleetID( final ModelID companyID, final String fleetName )
	{
		super( companyID, fleetName );
	}
}