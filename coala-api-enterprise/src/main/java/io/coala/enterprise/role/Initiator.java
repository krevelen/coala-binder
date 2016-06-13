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
 * 
 * Copyright (c) 2010-2014 Almende B.V. 
 */
package io.coala.enterprise.role;

import io.coala.enterprise.fact.CoordinationFact;

/**
 * {@link Initiator}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <F> the {@link CoordinationFact} type being handled
 */
public interface Initiator<F extends CoordinationFact> extends ActorRole<F>
{
	
	// marker interface

//	/** @param request */
//	void onExpiredRequest(F request);
//
//	/** @param cancel */
//	void onExpiredRequestCancellation(F cancel);
//	
//	/** @param allow */
//	void onAllowedRequestCancellation(F allow);
//
//	/** @param refuse */
//	void onRefusedRequestCancellation(F refuse);
//
//	/** @param promise */
//	void onPromised(F promise);
//
//	/** @param cancel */
//	void onCancelledPromise(F cancel);
//
//	/** @param decline */
//	void onDeclined(F decline);
//
//	/** @param state */
//	void onStated(F result);
//
//	/** @param cancel */
//	void onCancelledState(F cancel);
//
//	/** @param cancel */
//	void onExpiredAcceptCancellation(F cancel);
//
//	/** @param refuse */
//	void onRefusedAcceptCancellation(F refuse);
//
//	/** @param allow */
//	void onAllowedAcceptCancellation(F allow);

}