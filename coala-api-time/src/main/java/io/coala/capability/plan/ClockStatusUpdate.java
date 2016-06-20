package io.coala.capability.plan;

import io.coala.time.ClockID;

/**
 * {@link ClockStatusUpdate}
 */
@Deprecated
public interface ClockStatusUpdate
{

	/**
	 * @return
	 */
	ClockID getClockID();

	/**
	 * @return
	 */
	ClockStatus getStatus();

}