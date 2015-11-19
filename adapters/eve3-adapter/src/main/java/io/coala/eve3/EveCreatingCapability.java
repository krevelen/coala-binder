/* $Id: 9c2bf099c4653f4a94dc05a0c601d47a5589e822 $
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
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.eve3;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.agent.AgentStatusUpdate;
import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.admin.CreatingCapability;
import io.coala.log.InjectLogger;

import javax.inject.Inject;

import org.slf4j.Logger;

import rx.Observable;

/**
 * {@link EveCreatingCapability}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class EveCreatingCapability extends BasicCapability implements
		CreatingCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link EveCreatingCapability} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	private EveCreatingCapability(final Binder binder)
	{
		super(binder);
	}

	@Override
	public Observable<AgentStatusUpdate> createAgent(final String agentID)
	{
		return createAgent(agentID, null);
	}

	@Override
	public <A extends Agent> Observable<AgentStatusUpdate> createAgent(
			final String agentID, final Class<A> agentType)
	{
		return EveAgentManager.getInstance(getBinder())
				.boot(agentID, agentType);
	}

	@Override
	public Observable<AgentStatusUpdate> createAgent(final AgentID agentID)
	{
		return createAgent(agentID, null);
	}

	@Override
	public <A extends Agent> Observable<AgentStatusUpdate> createAgent(
			final AgentID agentID, final Class<A> type)
	{
		return EveAgentManager.getInstance(getBinder()).boot(agentID, type);
	}

	@Override
	public Observable<AgentID> getChildIDs(final boolean currentOnly)
	{
		return EveAgentManager.getInstance(getBinder()).getChildIDs(
				getID().getOwnerID(), currentOnly);
	}

}
