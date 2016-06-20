package io.coala.capability.interact;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.logging.log4j.Logger;

import io.coala.agent.Agent;
import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.log.InjectLogger;
import io.coala.message.Message;
import io.coala.message.MessageHandler;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

/**
 * {@link BasicReceivingCapability}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class BasicReceivingCapability extends BasicCapability
	implements ReceivingCapability, MessageHandler
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/** type of {@link Agent} owning the {@link Binder} */
	@SuppressWarnings( "rawtypes" )
	@Inject
	@Named( Binder.AGENT_TYPE )
	private Class ownerType;

	/** */
	private Subject<Message<?>, Message<?>> incoming = ReplaySubject.create();

	/**
	 * {@link BasicReceivingCapability} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected <T extends Message<?>> BasicReceivingCapability(
		final Binder binder )
	{
		super( binder );
	}

	/** orphan owners are always ready to receive, child owners are managed */
	private boolean ownerReady = getID().getOwnerID().isOrphan();

	@Override
	public final void initialize()
	{
		if( !Agent.class.isAssignableFrom( this.ownerType ) )
		{
			LOG.warn( "BEWARE! " + getID() + " has no transport layer and is "
					+ "reachable only via pointer within the same JVM" );
		}
	}

	@Override
	public final void activate()
	{
		synchronized( this.backlog )
		{
			this.ownerReady = true;
			handleBacklog();
		}
	}

	@Override
	public final void finish()
	{
		// synchronized (this.backlog)
		// {
		// this.ownerReady = false;
		// }
	}

	private void handleBacklog()
	{
		final Deque<Message<?>> queue;
		synchronized( this.backlog )
		{
			// System.err.println(getID().getOwnerID()
			// + " EMPTYING BACKLOG, total: " + this.backlog.size());
			if( this.backlog.isEmpty() ) return;
			queue = new LinkedList<>( this.backlog );
			this.backlog.clear();
		}
		try
		{
			while( !queue.isEmpty() )
				this.incoming.onNext( queue.removeFirst() );
		} catch( final Throwable t )
		{
			this.incoming.onError( t );
		}
	}

	private final List<Message<?>> backlog = Collections
			.synchronizedList( new LinkedList<Message<?>>() );

	@Override
	public void onMessage( final Message<?> message )
	{
		synchronized( this.backlog )
		{
			if( this.ownerReady )
				try
				{
					// LOG.info("DELIVERING: " + message);
					this.incoming.onNext( message );
					// LOG.info("DELIVERED: " + message);
				} catch( final Throwable t )
				{
					t.printStackTrace();
					this.incoming.onError( t );
					LOG.error( "Problem delivering: " + message, t );
				}
			else
			{
				this.backlog.add( message );
				// System.err.println(getID().getOwnerID()
				// + " not ready, backlog at: " + this.backlog.size());
			}
		}
	}

	@Override
	public Observable<Message<?>> getIncoming()
	{
		return this.incoming.asObservable();
	}
}