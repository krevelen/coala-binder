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

import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.model.ModelComponentID;
import io.coala.model.ModelID;

/**
 * {@link ClockID} contains a value to identify some {@link Clock}
 */
// @Embeddable
@Deprecated
public class ClockID extends ModelComponentID<String>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link ClockID} zero-arg bean constructor
	 */
	protected ClockID()
	{
		super();
	}

	/**
	 * {@link ClockID} CDI constructor
	 * 
	 * @param modelID
	 * @param value
	 */
	@Inject
	public ClockID(final ModelID modelID, final String value)
	{
		super(modelID, value);
		if (modelID.getValue() == null)
			throw new NullPointerException("No modelID value");
	}

	@JsonIgnore
	public ModelID getModelID()
	{
		return super.getModelID();
	}

}
