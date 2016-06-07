package io.coala.eve3;

import java.net.URI;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.joda.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.interact.SendingCapability;
import io.coala.exception.ExceptionFactory;
import io.coala.json.JsonUtil;
import io.coala.log.InjectLogger;
import io.coala.message.Message;
import rx.Observer;

/**
 * {@link EveSendingCapability}
 * 
 * @version $Id: 288edfd6500c6985c85605d9e941c65f4a510340 $
 * @author Rick van Krevelen
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
	protected EveSendingCapability( final Binder binder )
	{
		super( binder );
	}

	@Override
	public void send( final Message<?> msg ) throws Exception
	{
		final EveWrapperAgent ag = EveUtil.getWrapperAgent( msg.getSenderID(),
				true );
		if( ag == null ) throw new IllegalStateException(
				"No Eve agent exists for " + msg.getSenderID() );
		final JsonNode payload = JsonUtil.toTree( msg );
		final URI receiverURI = EveUtil.getAddress( msg.getReceiverID() );
		ag.doSend( payload, receiverURI );
	}

	@Override
	public void send( final Message<?> msg, final Duration timeout )
		throws Exception
	{
		if( !getID().getOwnerID().equals( msg.getSenderID() ) )
			throw ExceptionFactory.createChecked( "Wrong senderID {} != {}",
					msg.getSenderID(), getID().getOwnerID() );
		final long timeoutMS = timeout == null ? 0 : timeout.getMillis();
		if( timeoutMS <= 0 )
		{
			send( msg );
			return;
		}
		// timeout specified
		RuntimeException lastError = null;
		int attempts = 0;
		long elapsedMS = 0;
		for( long startMS = System
				.currentTimeMillis(); elapsedMS < timeoutMS; elapsedMS = System
						.currentTimeMillis() - startMS )
		{
			if( lastError != null ) LOG.trace( "Attempt " + (++attempts) + " t-"
					+ (timeoutMS - elapsedMS) + "ms to: " + msg.getReceiverID()
					+ ", ignored error: " + lastError.getMessage() );
			try
			{
				send( msg );
				if( attempts != 0 )
					LOG.trace( "Attempt " + (++attempts) + " @" + elapsedMS
							+ "ms succeeded to: " + msg.getReceiverID() );
				return;
			} catch( final RuntimeException t )
			{
				lastError = t;
				Thread.sleep( 100L );
			}
		}
		throw new IllegalStateException( "Attempt " + (++attempts) + " @"
				+ elapsedMS + "ms failed to: " + msg.getReceiverID(),
				lastError );
	}

	@Override
	public <T extends Message<?>> Observer<T> outgoing( final Duration timeout )
	{
		return new Observer<T>()
		{

			@Override
			public void onCompleted()
			{
				// no more messages
			}

			@Override
			public void onError( final Throwable e )
			{
				LOG.error( "Problem in outgoing message source from "
						+ getID().getOwnerID(), e );
			}

			@Override
			public void onNext( final T t )
			{
				try
				{
					send( t, timeout );
				} catch( final Throwable e )
				{
					LOG.warn( "Problem sending to " + t.getReceiverID(), e );
					e.printStackTrace();
				}
			}
		};
	}
}
