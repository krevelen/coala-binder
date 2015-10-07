/* $Id: 4ba48af2bf764fc16c6794e4e8c7c6b8bafbffc8 $
 * $URL: https://dev.almende.com/svn/abms/eve-util/src/test/java/com/almende/coala/eve/EveWrapperAgentTest.java $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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

import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.junit.Test;

import io.coala.agent.AgentStatusUpdate;
import io.coala.log.LogUtil;
import rx.Observer;

/**
 * {@link CleanupTest}
 * 
 * @date $Date: 2014-06-19 12:25:20 +0200 (Thu, 19 Jun 2014) $
 * @version $Revision: 306 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
// @Ignore
public class CleanupTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger(CleanupTest.class);

	@Test
	public void testConfig() throws Exception
	{
		LOG.trace("Starting EveAgentFactory...");
		final EveAgentManager eve = EveAgentManager.getInstance();

		final int max = 100;

		final CountDownLatch latch = new CountDownLatch(max);
		for (int i = 0; i < max; i++)
		{
			final String agentID = "_testAgent" + i;
			eve.boot(agentID, CleanupTestAgent.class)
					.subscribe(new Observer<AgentStatusUpdate>()
					{
						@SuppressWarnings("deprecation")
						@Override
						public void onNext(final AgentStatusUpdate update)
						{
							LOG.info("Handling agent status update: "+update);
							if (update.getStatus().isPassiveStatus())
								EveUtil.receiveMessageByPointer(
										new CleanupTestAgent.Harakiri(update.getAgentID()));
							else if (update.getStatus().isCompleteStatus())
								latch.countDown();
						}

						@Override
						public void onError(final Throwable t)
						{
							LOG.warn("Problem with wrapper status update", t);
						}

						@Override
						public void onCompleted()
						{
							LOG.trace("Eve wrapper for agent " + agentID
									+ " finalized, pending: "
									+ latch.getCount());

						}
					});
		}

		latch.await();
		EveAgentManager.getInstance().shutdown();
		LOG.trace("Eve wrapper for agents finalized!");
	}

}
