package io.coala.time;

import javax.measure.Quantity;

import io.coala.json.Wrapper;
import io.coala.time.Proactive.FutureSelf;
import io.reactivex.disposables.Disposable;

/**
 * {@link Expectation} or anticipation confirms that an event is scheduled to
 * occur at some future {@link Instant}
 * 
 * @version $Id: 3bf794ad6be84b466cf248d1360b0ed96172e295 $
 * @author Rick van Krevelen
 */
public class Expectation extends Wrapper.SimpleOrdinal<Instant>
{
	public static Expectation of( final Proactive self, final Instant when,
		final Disposable subscription )
	{
		return new Expectation( self, when, subscription );
	}

	Proactive self;

	Disposable subscription;

	public Expectation()
	{

	}

	public Expectation( final Proactive self, final Instant when,
		final Disposable subscription )
	{
		wrap( when );
		this.self = self;
		this.subscription = subscription;
	}

	/** @return the {@link Instant} when the event is scheduled to occur */
	public Instant due()
	{
		return unwrap();
	}

	/** cancels the scheduled event */
	public void remove()
	{
		this.subscription.dispose();
	}

//	/** @return {@code true} iff the event was cancelled or has occurred */
//	public boolean isRemoved()
//	{
//		return this.subscription.isUnsubscribed();
//	}

	public FutureSelf thenAfter( final Duration delay )
	{
		return FutureSelf.of( this.self, unwrap().add( delay.unwrap() ) );
	}

	public FutureSelf thenAfter( final Number delay )
	{
		return FutureSelf.of( this.self, unwrap().add( delay ) );
	}

	public FutureSelf thenAfter( final Quantity<?> delay )
	{
		return FutureSelf.of( this.self, unwrap().add( delay ) );
	}

}