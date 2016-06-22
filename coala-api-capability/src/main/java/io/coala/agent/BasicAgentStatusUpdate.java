package io.coala.agent;

import io.coala.name.AbstractIdentifiable;

/**
 * {@link BasicAgentStatusUpdate}
 */
@Deprecated
public class BasicAgentStatusUpdate extends
		AbstractIdentifiable<AgentStatusUpdateID> implements AgentStatusUpdate
{
	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private final AgentID agentID;

	/** */
	private final AgentStatus<?> status;

	/**
	 * {@link BasicAgentStatusUpdate} constructor
	 * 
	 * @param agentID
	 * @param status
	 */
	protected BasicAgentStatusUpdate(final AgentID agentID,
			final AgentStatus<?> status)
	{
		super(new AgentStatusUpdateID());
		this.agentID = agentID;
		this.status = status;
	}

	@Override
	public AgentID getAgentID()
	{
		return this.agentID;
	}

	@Override
	public AgentStatus<?> getStatus()
	{
		return this.status;
	}

	@Override
	public String toString()
	{
		return String.format("[%04d] %s -> %s", getID().getValue(),
				getAgentID(), getStatus());
	}

}