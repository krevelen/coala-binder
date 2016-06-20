package io.coala.eve;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.agent.BasicAgentStatus;
import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.admin.DestroyingCapability;
import io.coala.lifecycle.MachineUtil;
import io.coala.log.InjectLogger;

import javax.inject.Inject;

import org.apache.log4j.Logger;

/**
 * {@link EveDestroyingCapability}
 */
public class EveDestroyingCapability extends BasicCapability implements
		DestroyingCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link EveDestroyingCapability} constructor
	 * 
	 * @param binder
	 */
	@Inject
	private EveDestroyingCapability(final Binder binder)
	{
		super(binder);
	}

	/** @see DestroyingCapability#destroy(AgentID) */
	@Override
	public AgentID destroy()
	{
		return destroy(getID().getOwnerID());
	}

	/** @see DestroyingCapability#destroy(AgentID) */
	@Override
	public AgentID destroy(final AgentID agentID)
	{
		final Agent agent = EveAgentManager.getInstance().getAgent(agentID,
				false);

		if (agent == null)
		{
			LOG.warn("Could not kill agent, not available in this VM");
			return agentID;
		}

		final BasicAgentStatus status = BasicAgentStatus
				.determineKillStatus(agent);
		LOG.info("Going for the kill: " + status);
		MachineUtil.setStatus(agent, status, status.isFinishedStatus()
				|| status.isFailedStatus());

		return agentID;
	}
}
