/* $Id: 1566b85bfea340fc753861e6a6413629269ecf14 $
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
package io.coala.experimental.grant;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.agent.AgentID;
import io.coala.agent.BasicAgent;
import io.coala.bind.Binder;
import io.coala.capability.replicate.ReplicatingCapability;
import io.coala.invoke.ProcedureCall;
import io.coala.invoke.Schedulable;
import io.coala.log.InjectLogger;
import io.coala.model.ModelComponent;
import io.coala.process.Job;
import io.coala.time.SimTime;
import io.coala.time.Trigger;

/**
 * {@link PacedActor}
 * 
 * @version $Id: 1566b85bfea340fc753861e6a6413629269ecf14 $
 * @author Rick van Krevelen
 */
public class PacedActor extends BasicAgent implements ModelComponent<AgentID>
{

	/** */
	private static final long serialVersionUID = 1L;

	@InjectLogger
	private Logger LOG;

	/**
	 * {@link PacedActor} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected PacedActor( final Binder binder )
	{
		super( binder );
	}

	protected ReplicatingCapability getSimulator()
	{
		return getBinder().inject( ReplicatingCapability.class );
	}

	@Override
	public SimTime getTime()
	{
		return getScheduler().getTime();
	}

	@Override
	public void initialize()
	{
		LOG.trace( "Initializing" );

		final Job<?> job = ProcedureCall.create( this, this, testMethodID );
		final Trigger<?> trigger = Trigger.createAbsolute( getTime() );
		getScheduler().schedule( job, trigger );
		die();
	}

	private static final String testMethodID = "";

	@Schedulable( testMethodID )
	protected void scheduleTestMethod( final SimTime time )
	{
		if( time.equals( getTime() ) )
			LOG.info( "Method executed ON TIME" );
		else
			LOG.info( "Method executed BUT NOT as scheduled" );
	}

	@Override
	public void activate()
	{
		LOG.trace( "Activating" );
	}

	@Override
	public void deactivate()
	{
		LOG.trace( "Deactivating" );
	}

	@Override
	public void finish()
	{
		LOG.trace( "Finishing" );
	}

	@Override
	public AgentID getOwnerID()
	{
		return getID();
	}

}
