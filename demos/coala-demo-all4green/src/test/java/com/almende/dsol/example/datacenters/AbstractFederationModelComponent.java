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
package com.almende.dsol.example.datacenters;

import io.coala.dsol.util.AbstractDsolModelComponent;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;

/**
 * {@link AbstractFederationModelComponent}
 */
public abstract class AbstractFederationModelComponent
	extends AbstractDsolModelComponent<DEVSSimulatorInterface, FederationModel>
	implements FederationModelComponent
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link AbstractFederationModelComponent} constructor
	 * 
	 * @param model
	 * @param name
	 */
	public AbstractFederationModelComponent( final FederationModel model,
		final String name )
	{
		super( model, name );
	}

}
