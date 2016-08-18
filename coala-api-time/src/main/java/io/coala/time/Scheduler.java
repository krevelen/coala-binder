package io.coala.time;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.coala.function.ThrowableUtil;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.log.LogUtil;
import rx.Observable;
import rx.Observer;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

/**
 * {@link Scheduler}
 * 
 * @version $Id: d60e554b08ebba3a7b59f0924ecbdd8910988d7a $
 * @author Rick van Krevelen
 */
public interface Scheduler extends Proactive
{

	@Override
	default Scheduler scheduler()
	{
		return this;
	}

	/** @return an {@link Observable} stream of {@link Instant}s */
	Observable<Instant> time();

	/** */
	void resume();

	/**
	 * @param when the {@link Instant} of execution
	 * @param what the {@link Runnable}
	 * @return the occurrence {@link Expectation}, for optional cancellation
	 */
	default Expectation schedule( final Instant when,
		final ThrowingRunnable<?> what )
	{
		return schedule( when, t ->
		{
			what.run();
		} );
	}

	/**
	 * @param when the {@link Instant} of execution
	 * @param what the {@link Runnable}
	 * @return the occurrence {@link Expectation}, for optional cancellation, or
	 *         {@code null} if event is instantaneous
	 */
	Expectation schedule( Instant when, ThrowingConsumer<Instant, ?> what );

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @param what the {@link Runnable} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of {@link Expectation}s for each
	 *         next {@link Instant}, until completion of simulation time or
	 *         observed instants or an error occurs
	 */
	default <T> Observable<Expectation> schedule(
		final Observable<Instant> when, final ThrowingRunnable<?> what )
	{
		return schedule( when, t ->
		{
			what.run();
		} );
	}

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @param what the {@link Consumer} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of {@link Expectation}s for each
	 *         next {@link Instant}, until completion of simulation time or
	 *         observed instants or an error occurs
	 */
	default <T> Observable<Expectation> schedule(
		final Observable<Instant> when,
		final ThrowingConsumer<Instant, ?> what )
	{
		return schedule( when, new Observer<Instant>()
		{
			@Override
			public void onNext( final Instant t )
			{
				try
				{
					what.accept( t );
				} catch( final Throwable e )
				{
					ThrowableUtil.throwAsUnchecked( e );
				}
			}

			@Override
			public void onError( final Throwable e )
			{
				// ignore errors, already passed to result Observable
			}

			@Override
			public void onCompleted()
			{
				// ignore complete, result Observable also completes
			}
		} );
	}

	/**
	 * Schedule a stream of values resulting from executing a {@link Callable}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @param what the {@link Callable} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of results, until completion of
	 *         simulation time or observed instants or an error occurs
	 */
	default <R> Observable<R> schedule( final Observable<Instant> when,
		final Callable<R> what )
	{
		final Subject<R, R> result = BehaviorSubject.create();
		schedule( when, new Observer<Instant>()
		{
			@Override
			public void onNext( final Instant t )
			{
				try
				{
					result.onNext( what.call() );
				} catch( final Throwable e )
				{
					result.onError( e );
				}
			}

			@Override
			public void onError( final Throwable e )
			{
				result.onError( e );
			}

			@Override
			public void onCompleted()
			{
				result.onCompleted();
			}
		} );
		return result.asObservable();
	}

	/**
	 * Schedule a stream of {@link Instant}s and their {@link Expectation}s
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s, to be
	 *            scheduled immediately
	 * @param what the {@link Observer} of the same {@link Instant}s but delayed
	 *            until they occur in simulation time
	 * @return an {@link Observable} stream of {@link Expectation}s, until
	 *         completion of simulation time or of observed instants or an error
	 *         occurs
	 */
	default <T> Observable<Expectation>
		schedule( final Observable<Instant> when, final Observer<Instant> what )
	{
		final Subject<Expectation, Expectation> result = BehaviorSubject
				.create();

		time().subscribe( t ->
		{
			// ignore passage of time
		}, e ->
		{
			result.onError( e );
		}, () ->
		{
			result.onCompleted();
		} );
		when.first().subscribe( t ->
		{
			final Expectation exp = scheduler().schedule( t, () ->
			{
				try
				{
					what.onNext( t );
					// completed first() Instant, recurse remaining: skip(1)
					schedule( when.skip( 1 ), what ).subscribe( result );
				} catch( final Throwable e )
				{
					// failed first() Instant, interrupt recursion
					LogUtil.getLogger( Scheduler.class )
							.error( "Problem in event, canceled remaining" );
					throw e;
				}
			} );
			result.onNext( exp );
		}, e ->
		{
			// recursion complete
			if( e instanceof NoSuchElementException )
			{
				// no elements remain
				result.onCompleted();
				what.onCompleted();
			} else
			{
				// problem observing Instants
				result.onError( e );
				what.onError( e );
			}
		} );
		return result.asObservable();
	}

}