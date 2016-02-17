/* $Id: 79a68792095c1ae453e4f86411f96219ccd6fbe5 $
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

import io.coala.bind.Binder;
import io.coala.enterprise.role.AbstractExecutor;
import io.coala.enterprise.test.TestFact;
import io.coala.invoke.ProcedureCall;
import io.coala.invoke.Schedulable;
import io.coala.log.InjectLogger;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import io.coala.time.Trigger;

/**
 * {@link MyTestFactExecutorRole}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MyTestFactExecutorRole extends AbstractExecutor<TestFact.Request>
	implements TestFact.Executor
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link MyTestFactExecutorRole} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected MyTestFactExecutorRole( final Binder binder )
	{
		super( binder );
	}

	@Override
	public void onRequested( final TestFact.Request request )
	{
		LOG.trace( "Owner type: " + getOwnerType().getName() );
		final SimTime then = getTime().plus( 1, TimeUnit.HOURS );
		LOG.trace( "Sending response @ " + then + " for request " + request );
		getScheduler().schedule( ProcedureCall.create( this, this,
				DO_RESPONSE_METHOD_ID, request ),
				Trigger.createAbsolute( then ) );
	}

	private static final String DO_RESPONSE_METHOD_ID = "doResponse/1";

	@Schedulable( DO_RESPONSE_METHOD_ID )
	protected void doResponse( final TestFact.Request request ) throws Exception
	{
		send( TestFact.Response.Builder.forProducer( this, request ).build() );
	}
}