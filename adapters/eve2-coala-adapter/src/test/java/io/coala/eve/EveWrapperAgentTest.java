/* $Id$
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
package io.coala.eve;

import io.coala.agent.AgentStatusUpdate;
import io.coala.eve.EveAgentManager;
import io.coala.log.LogUtil;

import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import rx.Observer;

/**
 * {@link EveWrapperAgentTest}
 */
@Ignore
public class EveWrapperAgentTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( EveWrapperAgentTest.class );

	@Test
	public void testConfig() throws Exception
	{
		LOG.trace( "Starting EveAgentFactory..." );
		final EveAgentManager eve = EveAgentManager.getInstance();

		final CountDownLatch latch = new CountDownLatch( 1 );

		final String agentID = "_testAgent_";
		eve.boot( agentID, TestAgent.class )
				.subscribe( new Observer<AgentStatusUpdate>()
				{
					@Override
					public void onNext( final AgentStatusUpdate update )
					{
						if( update.getStatus().isInitializedStatus() )
							latch.countDown();
					}

					@Override
					public void onError( final Throwable t )
					{
						LOG.warn( "Problem with wrapper status update", t );
					}

					@Override
					public void onCompleted()
					{
						// TODO cleanup?
					}
				} );

		latch.await();
		LOG.trace( "Eve wrapper for agent " + agentID + " finalized!" );
	}

}
