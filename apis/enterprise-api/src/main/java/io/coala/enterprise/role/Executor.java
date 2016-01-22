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
 * {@link Executor}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <F> the type of {@link CoordinationFact} being handled
 */
public interface Executor<F extends CoordinationFact> extends ActorRole<F>
{

	// marker interface

	/**
	 * Triggers this {@link Executor} to handle a request (i.e. type
	 * {@link CoordinationFactType#REQUESTED}) responding with either
	 * <ul>
	 * <li>a promise (i.e. type {@link CoordinationFactType#PROMISED}),</li>
	 * <li>a decline (i.e. type {@link CoordinationFactType#DECLINED}), or</li>
	 * <li>not at all (i.e. a time-out, resulting in a decline).</li>
	 * </ul>
	 * 
	 * @param request
	 */
	// void onRequested(F request);
	//
	// /** @param cancel */
	// void onCancelledRequest(F cancel);
	//
	// /** @param promise */
	// void onExpiredPromise(F promise);
	//
	// /** @param cancel */
	// void onExpiredPromiseCancellation(F cancel);
	//
	// /** @param allow */
	// void onAllowedPromiseCancellation(F allow);
	//
	// /** @param refuse */
	// void onRefusedPromiseCancellation(F refuse);
	//
	// /** @param state */
	// void onExpiredState(F state);
	//
	// /** @param cancel */
	// void onExpiredStateCancellation(F cancel);
	//
	// /** @param allow */
	// void onAllowedStateCancellation(F allow);
	//
	// /** @param refuse */
	// void onRefusedStateCancellation(F refuse);
	//
	// /** @param accept */
	// void onAccepted(F accept);
	//
	// /** @param cancel */
	// void onCancelledAccept(F cancel);
	//
	// /** @param reject */
	// void onRejected(F reject);

}
