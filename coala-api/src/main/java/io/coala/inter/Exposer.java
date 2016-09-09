/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.inter;

import java.net.URI;
import java.util.List;

/**
 * {@link Exposer} provides end-points to the real world
 * 
 * TODO handle permissions, provide security?
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Exposer
{
	/**
	 * @param serviceIntf the public (abstract) service interface to expose
	 * @param serviceImpl the concrete service implementation to handle calls
	 * @return a non-empty {@link List} of exposed end-point {@link URI}s
	 */
	default <T> List<URI> exposeAs( Class<T> serviceIntf, T serviceImpl )
	{
		return exposeAs(null, serviceIntf,serviceImpl);
	}

	/**
	 * @param serviceIntf the public (abstract) service interface to expose
	 * @param serviceImpl the concrete service implementation to handle calls
	 * @return a non-empty {@link List} of exposed end-point {@link URI}s
	 */
	<T> List<URI> exposeAs( String id, Class<T> serviceIntf, T serviceImpl );
}
