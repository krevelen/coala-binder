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
package io.coala.enterprise.transaction;

import io.coala.model.ModelComponentID;
import io.coala.model.ModelID;

import com.eaio.uuid.UUID;

/**
 * {@link TransactionID}
 */
public class TransactionID extends ModelComponentID<UUID>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link TransactionID} constructor
	 * 
	 * @param modelID
	 */
	public TransactionID( final ModelID modelID )
	{
		super( modelID, new UUID() );
	}

	/**
	 * {@link TransactionID} zero-arg bean constructor
	 */
	protected TransactionID()
	{
		super();
	}

}
