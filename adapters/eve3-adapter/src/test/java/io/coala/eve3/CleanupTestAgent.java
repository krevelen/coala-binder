/* $Id: 6b2aa6282208757bc058922543d8c3c0a2190b59 $
 * $URL: https://dev.almende.com/svn/abms/eve-util/src/test/java/com/almende/coala/eve/TestAgent.java $
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

import javax.inject.Inject;

import org.apache.log4j.Logger;

import io.coala.agent.AgentID;
import io.coala.agent.BasicAgent;
import io.coala.bind.Binder;
import io.coala.capability.admin.DestroyingCapability;
import io.coala.capability.interact.ReceivingCapability;
import io.coala.lifecycle.MachineUtil;
import io.coala.log.InjectLogger;
import io.coala.message.AbstractMessage;
import io.coala.message.MessageID;
import io.coala.process.BasicProcessStatus;
import io.coala.time.NanoInstant;
import rx.Observer;
import rx.schedulers.Schedulers;

/**
 * {@link CleanupTestAgent}
 * 
 * @date $Date: 2014-06-20 12:27:58 +0200 (Fri, 20 Jun 2014) $
 * @version $Revision: 312 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 */
@SuppressWarnings("serial")
public class CleanupTestAgent extends BasicAgent
{

	public static class Harakiri extends AbstractMessage<MessageID<?, ?>>
	{
		/** */
		private static long count = 0L;

		public Harakiri(final AgentID ownerID)
		{
			super(MessageID.of(ownerID.getModelID(), count++, NanoInstant.ZERO),
					ownerID, ownerID, ownerID);
		}
	}

	@InjectLogger
	private Logger LOG;

	/**
	 * {@link CleanupTestAgent} constructor
	 * 
	 * @param binder
	 */
	@Inject
	public CleanupTestAgent(final Binder binder)
	{
		super(binder);

		getBinder().inject(ReceivingCapability.class).getIncoming()
				.ofType(Harakiri.class)//.observeOn(Schedulers.trampoline())
				.subscribe(new Observer<Harakiri>()
				{
					@Override
					public void onCompleted()
					{
						LOG.trace(
								"Completed receiving messages, agent destroyed?");
					}

					@Override
					public void onError(final Throwable e)
					{
						LOG.error("Error", e);
						e.printStackTrace();
					}

					@Override
					public void onNext(final Harakiri kill)
					{
						System.err.println(
								"Received message for Harakiri, destroying now...");

						try
						{
							getBinder().inject(DestroyingCapability.class).destroy();
						} catch (final Exception e)
						{
							LOG.error("Problem committing Hara Kiri", e);
							e.printStackTrace();
						}
					}
				});
	}

	/**
	 * @see BasicAgent#initialize()
	 */
	@Override
	public void initialize()
	{
		LOG.trace(getID() + " is initialized");
	}

	@Override
	public void finish()
	{
		LOG.trace(getID() + " is done");
	}

}
