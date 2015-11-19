package io.coala.eve3;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.joda.time.Duration;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.interact.SendingCapability;
import io.coala.exception.CoalaExceptionFactory;
import io.coala.log.InjectLogger;
import io.coala.message.Message;
import rx.Observer;

/**
 * {@link EveSendingCapability}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class EveSendingCapability extends BasicCapability
		implements SendingCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link EveSendingCapability} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected EveSendingCapability(final Binder binder)
	{
		super(binder);
	}

	@Override
	public void send(final Message<?> msg) throws Exception
	{
		// @SuppressWarnings("deprecation")
		// EveUtil.receiveMessageByPointer(msg);
		final EveWrapperAgent ag = EveUtil.getWrapperAgent(msg.getSenderID(),
				true);
		if (ag == null)
			throw new IllegalStateException(
					"No Eve agent exists for " + msg.getSenderID());
		ag.doSend(msg);
	}

	@Override
	public void send(final Message<?> msg, final Duration timeout)
			throws Exception
	{
		if (!getID().getOwnerID().equals(msg.getSenderID()))
			throw CoalaExceptionFactory.VALUE_NOT_ALLOWED.create("senderID",
					msg.getSenderID());
		if (msg.getReceiverID() == null)
			throw CoalaExceptionFactory.VALUE_NOT_SET.create("receiverID", msg);
		// if (EveUtil.getWrapperAgent(msg.getSenderID(), false) == null)
		// throw CoalaExceptionFactory.AGENT_NOT_ALLOWED
		// .createRuntime(msg.getReceiverID());
		final long timeoutMS = timeout == null ? 0 : timeout.getMillis();
		if (timeoutMS <= 0)
		{
			send(msg);
			return;
		}
		// timeout specified
		RuntimeException lastError = null;
		int attempts = 0;
		long elapsedMS = 0;
		for (long startMS = System
				.currentTimeMillis(); elapsedMS < timeoutMS; elapsedMS = System
						.currentTimeMillis() - startMS)
		{
			if (lastError != null)
				LOG.trace("Attempt " + (++attempts) + " t-"
						+ (timeoutMS - elapsedMS) + "ms to: "
						+ msg.getReceiverID() + ", ignored error: "
						+ lastError.getMessage());
			try
			{
				send(msg);
				if (attempts != 0)
					LOG.trace("Attempt " + (++attempts) + " @" + elapsedMS
							+ "ms sucessful to " + msg.getReceiverID());
				return;
			} catch (final RuntimeException t)
			{
				lastError = t;
				Thread.sleep(100L);
			}
		}
		throw new IllegalStateException("Attempt " + (++attempts) + " @"
				+ elapsedMS + "ms failed to: " + msg.getReceiverID(),
				lastError);
	}

	@Override
	public <T extends Message<?>> Observer<T> outgoing(final Duration timeout)
	{
		return new Observer<T>()
		{

			@Override
			public void onCompleted()
			{
				// no more messages
			}

			@Override
			public void onError(final Throwable e)
			{
				LOG.error("Problem in outgoing message source from "
						+ getID().getOwnerID(), e);
			}

			@Override
			public void onNext(final T t)
			{
				try
				{
					send(t, timeout);
				} catch (final Throwable e)
				{
					LOG.warn("Problem sending message to " + t.getReceiverID(),
							e);
				}
			}
		};
	}

}
