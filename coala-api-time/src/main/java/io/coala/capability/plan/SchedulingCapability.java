/* $Id: 3910462899309fbce4d82fb12041417f0f755501 $
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
package io.coala.capability.plan;

import io.coala.capability.CapabilityFactory;
import io.coala.process.Job;
import io.coala.time.Instant;
import io.coala.time.Trigger;

/**
 * {@link SchedulingCapability}
 * 
 * @param <I>
 */
@Deprecated
public interface SchedulingCapability<I extends Instant<I>> extends TimingCapability<I>
{

	/**
	 * {@link Factory}
	 * 
	 * @version $Id: 3910462899309fbce4d82fb12041417f0f755501 $
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings("rawtypes")
	interface Factory extends CapabilityFactory<SchedulingCapability>
	{
		// empty
	}

	/** */
	void schedule(Job<?> job, Trigger<?> trigger);

	/**
	 * Cancel the specified {@link Job}
	 * 
	 * @param job the command to unschedule
	 * @return {@code true} if cancelled, {@code false} if not found (e.g. never
	 *         scheduled, already cancelled, or already executed)
	 */
	boolean unschedule(Job<?> job);

}
