package io.coala.time;

import io.coala.json.Wrapper;
import io.coala.time.Timed.FutureSelf;
import rx.Subscription;

/**
 * {@link Expectation} or anticipation confirms that an event is scheduled
 * to occur at some future {@link Instant}
 * 
 * @version $Id: 3d3e16811192f8231b89032b459eab02371a877f $
 * @author Rick van Krevelen
 */
public class Expectation extends Wrapper.SimpleOrdinal<Instant>
{
	Timed self;

	Subscription subscription;

	public Expectation()
	{

	}

	public Expectation( final Timed self, final Instant when,
		final Subscription subscription )
	{
		wrap( when );
		this.self = self;
		this.subscription = subscription;
	}

	/** cancels the scheduled event */
	public void remove()
	{
		this.subscription.unsubscribe();
	}

	/** @return {@code true} iff the event was cancelled or has occurred */
	public boolean isRemoved()
	{
		return this.subscription.isUnsubscribed();
	}

	public FutureSelf thenAfter( final Duration delay )
	{
		return FutureSelf.of( this.self, unwrap().add( delay.unwrap() ) );
	}

	public FutureSelf thenAfter( final TimeSpan delay )
	{
		return FutureSelf.of( this.self, unwrap().add( delay ) );
	}

	public static Expectation of( final Timed self, final Instant when,
		final Subscription subscription )
	{
		return new Expectation( self, when, subscription );
	}
}