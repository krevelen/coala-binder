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
package io.coala.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link ModelID}
 */
public class ModelID extends ModelComponentID<String>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link ModelID} zero-arg bean constructor
	 */
	protected ModelID()
	{
		super();
	}

	/**
	 * {@link ModelID} constructor
	 * 
	 * @param value
	 */
	public ModelID( final String value )
	{
		super( null, value );
		setModelID( this );
	}

	@Override
	@JsonIgnore
	// prevent circular references
	public ModelID getModelID()
	{
		return super.getModelID(); // == this
	}

}
