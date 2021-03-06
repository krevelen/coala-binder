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

import io.coala.model.ModelID;

/**
 * {@link Vehicle}
 */
public class Vehicle // agent prototype, monitors status/position at
// DEAL
{
	/** */
	public final VehicleID id;

	/** */
	public final LocationID homeID;

	/** */
	public boolean flexible;

	/** */
	public FleetID assignedFleetID = null;

	/** */
	public VehicleStatus status = null;

	/** */
	public LocationID origin = null;

	/** */
	public Coordinate position = null;

	/** */
	public LocationID destination = null;

	/**
	 * {@link Vehicle} constructor
	 * 
	 * @param companyName
	 * @param name
	 * @param homeID
	 * @param fleetID
	 * @param flexible
	 */
	public Vehicle( final ModelID companyName, final String name,
		final LocationID homeID, final FleetID fleetID, final boolean flexible )
	{
		this.id = new VehicleID( companyName, name );
		this.homeID = homeID;
		this.assignedFleetID = fleetID;
		this.flexible = flexible;
	}
}