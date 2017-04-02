package io.coala.time;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.inject.Singleton;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalBinding;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.log.LogUtil;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Scheduler}
 * 
 * @version $Id: d60e554b08ebba3a7b59f0924ecbdd8910988d7a $
 * @author Rick van Krevelen
 */
public interface Scheduler extends Proactive, Runnable
{

	@Override
	default Scheduler scheduler()
	{
		return this;
	}

	/** @return an {@link Observable} stream of {@link Instant}s */
	Observable<Instant> time();

	default Subscription onReset( ThrowingRunnable<?> runnable )
	{
		return onReset( s -> runnable.run() );
	}

	Subscription onReset( ThrowingConsumer<Scheduler, ?> consumer );

	/** continue executing scheduled events until completion */
	void resume();

	/** block Thread until completion */
	@Override
	default void run()
	{
		resume();
		try
		{
			time().toBlocking().last();
		} catch( final NoSuchElementException e )
		{
			// ignore
		}
	}

	/** continue executing scheduled events until completion */
	default void run( final ThrowingConsumer<Scheduler, ?> onReset )
	{
		onReset( onReset );
		run();
	}

	/**
	 * @param when the {@link Instant} of execution
	 * @param what the {@link Runnable}
	 * @return the occurrence {@link Expectation}, for optional cancellation, or
	 *         {@code null} if event is instantaneous
	 */
	Expectation schedule( Instant when, ThrowingConsumer<Instant, ?> what );

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
	default <R> Observable<R> schedule( final Instant when,
		final Callable<R> what )
	{
		return schedule( Observable.just( when ), what );
	}

	/**
	 * Delay a stream of {@link Instant}s scheduled on this {@link Scheduler}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed lazily in
	 * {@link #schedule(Iterable)} and eagerly in {@link #schedule(Observable)}
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @return an {@link Observable} stream of delayed {@link Instant}s
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default Observable<Instant> schedule( final Iterable<Instant> when )
	{
		return schedule( when, (Observer) null );
	}

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed lazily in
	 * {@link #schedule(Iterable, ThrowingRunnable)} and eagerly in
	 * {@link #schedule(Observable, ThrowingRunnable)}
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @param what the {@link Runnable} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of {@link Expectation}s for each
	 *         next {@link Instant}, until completion of simulation time or
	 *         observed instants or an error occurs
	 */
	default <T> Observable<Expectation> schedule( final Iterable<Instant> when,
		final ThrowingRunnable<?> what )
	{
		return schedule( when, t ->
		{
			what.run();
		} );
	}

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed lazily in
	 * {@link #schedule(Iterable, ThrowingConsumer)} and eagerly in
	 * {@link #schedule(Observable, ThrowingConsumer)}
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @param what the {@link Consumer} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of {@link Expectation}s for each
	 *         next {@link Instant}, until completion of simulation time or
	 *         observed instants or an error occurs
	 */
	default <T> Observable<Expectation> schedule( final Iterable<Instant> when,
		final ThrowingConsumer<Instant, ?> what )
	{
		final Subject<Expectation, Expectation> result = PublishSubject
				.create();
		schedule( when, result ).subscribe( t ->
		{
			try
			{
				what.accept( t );
			} catch( final Throwable e )
			{
				LogUtil.getLogger( Scheduler.class )
						.error( "Problem in " + what, e );
//				Thrower.rethrowUnchecked( e );
			}
		}, e ->
		{
			// ignore errors, already passed to result Observable
		} );
		return result.asObservable();
	}

	/**
	 * Schedule a stream of values resulting from executing a {@link Callable}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed lazily in
	 * {@link #schedule(Iterable, Callable)} and eagerly in
	 * {@link #schedule(Observable, Callable)}
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @param what the {@link Callable} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of results, until completion of
	 *         simulation time or observed instants or an error occurs
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default <R> Observable<R> schedule( final Iterable<Instant> when,
		final Callable<R> what )
	{
		return schedule( when ).map( t ->
		{
			try
			{
				return what.call();
			} catch( final Throwable e )
			{
				return Thrower.rethrowUnchecked( e );
			}
		} );
	}

	/**
	 * Delay a stream of {@link Instant}s scheduled on this {@link Scheduler}
	 * and optionally observe each {@link Expectation}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed lazily in
	 * {@link #schedule(Iterable, Observer)} and eagerly in
	 * {@link #schedule(Observable, Observer)}
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @param what (optional) {@link Observer} of {@link Expectation}s for each
	 *            upcoming {@link Instant}
	 * @return transformed {@link Observable} stream of delayed {@link Instant}s
	 */
	default Observable<Instant> schedule( final Iterable<Instant> when,
		final Observer<Expectation> what )
	{
		final Subject<Instant, Instant> delayedCopy = PublishSubject.create();
		// schedule first element from iterator
		final Iterator<Instant> it = when.iterator();
		if( !it.hasNext() ) return Observable.empty();
		final Expectation exp0 = schedule( it.next(), delayedCopy::onNext );
		if( what != null ) what.onNext( exp0 );
		// schedule each following element upon merge with delayed previous
		return delayedCopy.zipWith( () -> it, ( t, t_next ) ->
		{
			final Expectation exp = schedule( t_next, delayedCopy::onNext );
			if( what != null ) what.onNext( exp );
			return t;
		} );
	}

	/**
	 * Delay a stream of {@link Instant}s scheduled on this {@link Scheduler}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed eagerly in
	 * {@link #schedule(Observable, Observer)} and lazily in
	 * {@link #schedule(Iterable, Observer)}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @return transformed {@link Observable} stream of delayed {@link Instant}s
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default Observable<Instant> schedule( final Observable<Instant> when )
	{
		return schedule( when, (Observer) null );
	}

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed eagerly in
	 * {@link #schedule(Observer, Observer)} and lazily in
	 * {@link #schedule(Iterable, Observer)}
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
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed eagerly in
	 * {@link #schedule(Observable, ThrowingConsumer)} and lazily in
	 * {@link #schedule(Iterable, ThrowingConsumer)}
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
		final Subject<Expectation, Expectation> result = PublishSubject
				.create();
		schedule( when, result ).subscribe( t ->
		{
			try
			{
				what.accept( t );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			}
		}, e ->
		{
			// ignore errors, already passed to result Observable
		} );
		return result.asObservable();
	}

	/**
	 * Schedule a stream of values resulting from executing a {@link Callable}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed eagerly in
	 * {@link #schedule(Observable, Callable)} and lazily in
	 * {@link #schedule(Iterable, Callable)}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @param what the {@link Callable} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of results, until completion of
	 *         simulation time or observed of instants or an error occurs
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default <R> Observable<R> schedule( final Observable<Instant> when,
		final Callable<R> what )
	{
		return schedule( when ).map( t ->
		{
			try
			{
				return what.call();
			} catch( final Throwable e )
			{
				return Thrower.rethrowUnchecked( e );
			}
		} );
	}

	/**
	 * Delay a stream of {@link Instant}s scheduled on this {@link Scheduler}
	 * and optionally observe each {@link Expectation}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed eagerly in
	 * {@link #schedule(Observable, Observer)} and lazily in
	 * {@link #schedule(Iterable, Observer)}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @param what (optional) {@link Observer} of {@link Expectation}s for each
	 *            upcoming {@link Instant}
	 * @return transformed {@link Observable} stream of delayed {@link Instant}s
	 */
	default Observable<Instant> schedule( final Observable<Instant> when,
		final Observer<Expectation> what )
	{
		final Subject<Instant, Instant> delayedCopy = PublishSubject.create();
		return when.map( t ->
		{
			final Expectation exp = schedule( t, delayedCopy::onNext );
			if( what != null ) what.onNext( exp );
			return t;
		} ).zipWith( delayedCopy, ( t, t0 ) ->
		{
			// merge "when" (observed eagerly) with "delayed" by scheduler
			return t;
		} );
	}

	interface Factory
	{

		Scheduler create( ReplicateConfig config );

		default Scheduler create( final Map<?, ?>... imports )
		{
			return create( ReplicateConfig.getOrCreate( imports ) );
		}

		default Scheduler create( final String rawId,
			final Map<?, ?>... imports )
		{
			return create( ReplicateConfig.getOrCreate( rawId, imports ) );
		}

		default Observable<Scheduler> createAndRun(
			final Observable<String> ids, final Map<?, ?>... imports )
			throws Throwable
		{
			return Observable.create( sub -> ids.subscribe(
					id -> create( id, imports ).run( sub::onNext ),
					sub::onError, sub::onCompleted ) );
		}

		@Singleton
		class Inject implements Factory, LocalBinding
		{
			private LocalBinder binder;

			@Override
			public Scheduler create( final ReplicateConfig config )
			{
				binder().reset( ReplicateConfig.class, config );
				return binder().inject( config.schedulerType() );
			}

			@Override
			public LocalBinder binder()
			{
				return this.binder;
			}
		}
	}
}