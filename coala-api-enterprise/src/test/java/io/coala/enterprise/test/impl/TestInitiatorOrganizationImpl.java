/* $Id: 0d4b49b590a20e0127818cdc7e333b076b04d399 $
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
import io.coala.capability.configure.ConfiguringCapability;
import io.coala.enterprise.organization.AbstractOrganization;
import io.coala.enterprise.test.TestFact;
import io.coala.enterprise.test.TestInitiatorOrganization;
import io.coala.invoke.ProcedureCall;
import io.coala.invoke.Schedulable;
import io.coala.log.InjectLogger;
import io.coala.model.ModelComponentIDFactory;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import io.coala.time.Trigger;

/**
 * {@link TestInitiatorOrganizationImpl}
 * 
 * @version $Id: 0d4b49b590a20e0127818cdc7e333b076b04d399 $
 * @author Rick van Krevelen
 */
public class TestInitiatorOrganizationImpl extends AbstractOrganization
	implements TestInitiatorOrganization
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link TestInitiatorOrganizationImpl} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected TestInitiatorOrganizationImpl( final Binder binder )
	{
		super( binder );
	}

	@Override
	public TestFact.Initiator getTestFactInitiator()
	{
		return getBinder().inject( TestFact.Initiator.class );
	}

	@Override
	public void initialize() throws Exception
	{
		super.initialize();

		LOG.trace( "Initializing organization..." );
		getScheduler().schedule(
				ProcedureCall.create( this, this, DO_REQUEST_METHOD_ID, 0 ),
				Trigger.createAbsolute( getTime() ) );
		LOG.trace( "Initialized!" );

		getSimulator().start();
	}

	/** */
	private static final String DO_REQUEST_METHOD_ID = "newRequest/1";

	protected ConfiguringCapability getConfig()
	{
		return getBinder().inject( ConfiguringCapability.class );
	}

	protected AgentID newAgentID( final String value )
	{
		return getBinder().inject( ModelComponentIDFactory.class )
				.createAgentID( value );
	}

	/**
	 * @param number
	 * @throws Exception
	 */
	@Schedulable( DO_REQUEST_METHOD_ID )
	protected void newRequest( final long number ) throws Exception
	{
		LOG.trace( "Initiating request " + number );
		final TestFact.Request req = getTestFactInitiator().initiate(
				newAgentID( getConfig().getProperty( EXECUTOR_NAME_KEY )
						.get( EXECUTOR_NAME_DEFAULT ) ) );

		if( number >= 2 )
		{
			LOG.trace( "Initiated request " + number + ": " + req.getID()
					+ ", dying..." );
			// die();
			return;
		}

		final SimTime then = getTime().plus( 1, TimeUnit.DAYS );
		final long next = number + 1;
		LOG.trace( "Repeating initiation @ " + then );

		getScheduler().schedule(
				ProcedureCall.create( this, this, DO_REQUEST_METHOD_ID, next ),
				Trigger.createAbsolute( then ) );
	}

}
