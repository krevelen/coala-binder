package io.coala.capability.plan;

import io.coala.name.AbstractIdentifiable;
import io.coala.time.ClockID;

/**
 * {@link ClockStatusUpdateImpl}
 */
@Deprecated
public class ClockStatusUpdateImpl extends
		AbstractIdentifiable<ClockStatusUpdateID> implements ClockStatusUpdate
{
	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private final ClockID clockID;

	/** */
	private final ClockStatus status;

	/**
	 * {@link ClockStatusUpdateImpl} constructor
	 * 
	 * @param clockID
	 * @param status
	 */
	public ClockStatusUpdateImpl(final ClockID clockID,
			final ClockStatus status)
	{
		super(new ClockStatusUpdateID());
		this.clockID = clockID;
		this.status = status;
	}

	@Override
	public ClockID getClockID()
	{
		return this.clockID;
	}

	@Override
	public ClockStatus getStatus()
	{
		return this.status;
	}

	@Override
	public String toString()
	{
		return String.format("[%04d] %s -> %s", getID().getValue(),
				getClockID(), getStatus());
	}

}