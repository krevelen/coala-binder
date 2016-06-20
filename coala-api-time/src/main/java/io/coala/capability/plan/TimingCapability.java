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
package io.coala.capability.plan;

import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import io.coala.time.ClockID;
import io.coala.time.Instant;
import io.coala.time.Timed;
import rx.Observable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link TimingCapability} provides the time
 * 
 * @param <I> the type of {@link Instant} to provide
 */
@Deprecated
public interface TimingCapability<I extends Instant<I>> extends
		Capability<BasicCapabilityStatus>, Timed<I>
{

	/**
	 * {@link Factory}
	 */
	@SuppressWarnings("rawtypes")
	interface Factory extends CapabilityFactory<TimingCapability>
	{
		// empty
	}

	/** @return the identifier of the source clock */
	ClockID getClockID();
	
	/** @return the offset from which to translate actual {@link Instant}s */
	I getVirtualOffset();

	/**
	 * @param virtualTime the virtual {@link Instant}
	 * @return the wall-clock {@link Instant}
	 */
	I toActualTime(I virtualTime);

	/** @return the offset from which to translate virtual {@link Instant}s */
	I getActualOffset();

	/**
	 * @param actualTime the wall-clock {@link Instant}
	 * @return the virtual {@link Instant}
	 */
	I toVirtualTime(I actualTime);
	
	/** @return the virtual clock speed factor */
	Number getApproximateSpeedFactor();

	/** @return an {@link Observable} of the {@link ClockStatusUpdate}s */
	@JsonIgnore
	Observable<ClockStatusUpdate> getStatusUpdates();
	
	/** @return an {@link Observable} of the {@link Instant}s */
	@JsonIgnore
	Observable<I> getTimeUpdates();

}
