/* $Id: e18ce506411a6728e42f316b48bc079101b4e237 $
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
package io.coala.eve3;

import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import io.coala.agent.AgentStatusUpdate;
import io.coala.config.ConfigUtil;
import io.coala.log.LogUtil;
import rx.Observer;

/**
 * {@link CleanupTest}
 * 
 * @version $Id: e18ce506411a6728e42f316b48bc079101b4e237 $
 * @author Rick van Krevelen
 */
public class CleanupTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( CleanupTest.class );

	@Ignore // FIXME !
	@Test
	public void testConfig() throws Exception
	{
		LOG.trace( "Starting EveAgentFactory..." );
		System.setProperty( ConfigUtil.CONFIG_FILE_PROPERTY,
				ConfigUtil.CONFIG_FILE_DEFAULT );
		final EveAgentManager eve = EveAgentManager.getInstance();

		final int max = 100;

		final CountDownLatch latch = new CountDownLatch( max );
		for( int i = 0; i < max; i++ )
		{
			final String agentID = "_testAgent" + i;
			eve.boot( agentID, CleanupTestAgent.class )
					.subscribe( new Observer<AgentStatusUpdate>()
					{
						@SuppressWarnings( "deprecation" )
						@Override
						public void onNext( final AgentStatusUpdate update )
						{
							LOG.info(
									"Handling agent status update: " + update );
							if( update.getStatus().isPassiveStatus() )
								EveUtil.receiveMessageByPointer(
										new CleanupTestAgent.Harakiri(
												update.getAgentID() ) );
							else if( update.getStatus().isCompleteStatus() )
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
							LOG.trace( "Eve wrapper for agent " + agentID
									+ " finalized, pending: "
									+ latch.getCount() );

						}
					} );
		}

		latch.await();
		EveAgentManager.getInstance().shutdown();
		LOG.trace( "Eve wrapper for agents finalized!" );
	}

}
