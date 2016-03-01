/* $Id: ab999c34cf71781b83fca79971ba0bb3a50748b8 $
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
package io.coala.enterprise.test.impl;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.agent.AgentID;
import io.coala.bind.Binder;
import io.coala.capability.interact.SendingCapability;
import io.coala.enterprise.fact.CoordinationFact;
import io.coala.enterprise.role.AbstractInitiator;
import io.coala.enterprise.role.Initiator;
import io.coala.enterprise.test.TestFact;
import io.coala.enterprise.test.TestFact.Request;
import io.coala.log.InjectLogger;

/**
 * {@link MyTestFactInitiatorRole}
 * 
 * @version $Id: ab999c34cf71781b83fca79971ba0bb3a50748b8 $
 * @author Rick van Krevelen
 */
public class MyTestFactInitiatorRole
	extends AbstractInitiator<TestFact.Response> implements TestFact.Initiator
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link TestFactInitiatorRole} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected MyTestFactInitiatorRole( final Binder binder )
	{
		super( binder );
	}

	/** @see TestFact.InitiatorRole#initiate(AgentID) */
	@Override
	public Request initiate( final AgentID executorID ) throws Exception
	{
		LOG.info( "Initiating " + TestFact.class.getSimpleName()
				+ " for owner type: " + getOwnerType().getSimpleName() );
		final Request result = Request.Builder.forProducer( this )
				.withReceiverID( executorID ).build();
		getBinder().inject( SendingCapability.class ).send( result );
		return result;
	}

	/** @see Initiator#onStated(CoordinationFact) */
	@Override
	public void onStated( final TestFact.Response result )
	{
		LOG.trace( "Got result: " + result );
	}

}