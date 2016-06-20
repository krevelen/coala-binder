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
package io.coala.experimental.locate;

import io.coala.name.AbstractIdentifier;

import java.io.Serializable;

/**
 * {@link LocationID} fixes the identifier type of {@link Location} to
 * {@link String}
 * 
 * @param <T> the type of value for this {@link AbstractIdentifier}
 */
public class LocationID<T extends Serializable & Comparable<T>>
	extends AbstractIdentifier<T>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * @param value
	 */
	public LocationID( final T value )
	{
		super( value );
	}

}
