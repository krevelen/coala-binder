/* $Id: 6b2aa6282208757bc058922543d8c3c0a2190b59 $
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
import io.coala.agent.BasicAgentStatus;
import io.coala.bind.Binder;
import io.coala.capability.interact.ReceivingCapability;
import io.coala.capability.interact.SendingCapability;
import io.coala.log.InjectLogger;
import io.coala.message.AbstractMessage;
import io.coala.message.MessageID;
import io.coala.model.ModelID;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import rx.Observer;

/**
 * {@link MessagingTestAgent}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class MessagingTestAgent extends BasicAgent
{

	/**
	 * {@link MyMessageID}
	 * 
	 * @version $Id$
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	public static class MyMessageID extends MessageID<Long, SimTime>
	{

		/** */
		private static final long serialVersionUID = 1L;

		/** */
		private static long msgID = 0;

		/**
		 * {@link MyMessageID} zero-arg bean constructor
		 */
		protected MyMessageID()
		{
		}

		/**
		 * {@link MyMessageID} constructor
		 * 
		 * @param modelID the {@link ModelID}
		 * @param instant the {@link SimTime}
		 */
		public MyMessageID(final ModelID modelID, final SimTime instant)
		{
			super(modelID, msgID++, instant);
		}

	}

	/**
	 * {@link MyMessage}
	 * 
	 * @version $Id$
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	public static class MyMessage extends AbstractMessage<MyMessageID>
	{

		/** */
		private static final long serialVersionUID = 1L;

		/** */
		public String content = null;

		/**
		 * {@link MyMessage} zero-arg bean constructor
		 */
		protected MyMessage()
		{
		}

		/**
		 * {@link MyMessage} constructor
		 * 
		 * @param time
		 * @param senderID
		 * @param receiverID
		 * @param content
		 */
		public MyMessage(final SimTime time, final AgentID senderID,
				final AgentID receiverID, final String content)
		{
			super(new MyMessageID(senderID.getModelID(), time),
					senderID.getModelID(), senderID, receiverID);
			this.content = content;
		}

	}

	/** */
	private static final long serialVersionUID = 1L;

	@InjectLogger
	private Logger LOG;

	/**
	 * {@link MessagingTestAgent} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	public MessagingTestAgent(final Binder binder)
	{
		super(binder);
	}

	/**
	 * @see BasicAgent#initialize()
	 */
	@Override
	public void initialize()
	{
		// subscribe message handler
		getBinder().inject(ReceivingCapability.class).getIncoming()
				.ofType(MyMessage.class).subscribe(new Observer<MyMessage>()
				{
					@Override
					public void onNext(final MyMessage msg)
					{
						try
						{
							handle(msg);
						} catch (Exception e)
						{
							onError(e);
						}
					}

					@Override
					public void onCompleted()
					{

					}

					@Override
					public void onError(final Throwable t)
					{
						t.printStackTrace();
					}
				});

		if (getID().equals(MessagingTest.senderAgentID))
		{
			try
			{
				LOG.info("About to ping...");
				ping();
			} catch (final Exception e)
			{
				LOG.error("No ping today!", e);
			}
		}
	}

	/**
	 * @param t
	 * @return
	 */
	protected SimTime createTick(int t)
	{
		return getBinder().inject(SimTime.Factory.class).create(t,
				TimeUnit.TICKS);
	}

	/**
	 * @throws Exception
	 */
	public void ping() throws Exception
	{
		final MyMessage msg = new MyMessage(createTick(0), getID(),
				MessagingTest.receiverAgentID, "ping");
		LOG.trace("About to send ping: " + msg);
		getBinder().inject(SendingCapability.class).send(msg);
	}

	/**
	 * @throws Exception
	 */
	public void pong() throws Exception
	{
		final MyMessage msg = new MyMessage(createTick(1), getID(),
				MessagingTest.senderAgentID, "pong");

		LOG.trace("About to send pong: " + msg);
		getBinder().inject(SendingCapability.class).send(msg);
	}

	@Override
	public void finish()
	{
		LOG.trace(getID() + " is done");
	}

	/**
	 * @param message
	 * @throws Exception
	 */
	public void handle(final MyMessage message) throws Exception
	{
		LOG.trace(getID().getValue() + " handling " + message.content + "...");
		if (getID().equals(MessagingTest.receiverAgentID))
		{
			pong();
			forceStatus(BasicAgentStatus.COMPLETE);
		} else if (getID().equals(MessagingTest.senderAgentID))
		{
			forceStatus(BasicAgentStatus.COMPLETE);
		}
	}

}
