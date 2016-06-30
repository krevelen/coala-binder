package io.coala.eve3;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.agent.BasicAgentStatus;
import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.admin.DestroyingCapability;
import io.coala.lifecycle.MachineUtil;
import io.coala.log.InjectLogger;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * {@link EveDestroyingCapability}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Deprecated
public class EveDestroyingCapability extends BasicCapability
	implements DestroyingCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link EveDestroyingCapability} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	private EveDestroyingCapability( final Binder binder )
	{
		super( binder );
	}

	@Override
	public AgentID destroy()
	{
		return destroy( getID().getOwnerID() );
	}

	@Override
	public AgentID destroy( final AgentID agentID )
	{
		final Agent agent = EveAgentManager.getInstance().getAgent( agentID,
				false );

		if( agent == null )
		{
			// TODO try other/distributed/connected VMs?
			LOG.warn( "Could not kill agent, not available in this VM" );
			return agentID;
		}

		Schedulers.trampoline().createWorker().schedule( new Action0()
		{
			@Override
			public void call()
			{
				final BasicAgentStatus status = BasicAgentStatus
						.determineKillStatus( agent );
				LOG.info( "Going for the kill: " + status );
				MachineUtil.setStatus( agent, status, true );
			}
		} );

		return agentID;
	}

}
