package io.coala.time;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Unit;
import javax.measure.quantity.Time;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalBinding;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.log.LogUtil;
import io.coala.util.Instantiator;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Scheduler}
 * 
 * @version $Id: b2709bf0fc58dd79a849baf71d699e8f475355b9 $
 * @author Rick van Krevelen
 */
public interface Scheduler extends Proactive, Runnable
{

	/** @return the {@link ZonedDateTime} offset of virtual time */
	ZonedDateTime offset();

	/** @return the {@link Unit} of virtual time */
	Unit<?> timeUnit();

	default ZonedDateTime nowZonedDateTime()
	{
		if( !timeUnit().getDimension().equals( Time.class ) ) return Thrower
				.throwNew( IllegalArgumentException::new, () -> "Time unit "
						+ timeUnit() + " incompatible, abstract steps?" );
		return now().toJava8( offset() );
	}

	@Override
	default Scheduler scheduler()
	{
		return this;
	}

	/** @return the current {@link SchedulerConfig} */
	SchedulerConfig config();

	@Override
	Instant now();

	/** @return an {@link Observable} stream of {@link Instant}s */
	Observable<Instant> time();

	Disposable onReset( ThrowingConsumer<Scheduler, ?> consumer );

	default Disposable onReset( final ThrowingRunnable<?> runnable )
	{
		return onReset( s -> runnable.run() );
	}

	/** continue executing scheduled events until completion */
	void resume();

	/** block Thread until completion */
	@Override
	default void run()
	{
		resume();
		try
		{
			time().blockingLast();
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
		return schedule( when, t -> what.run() );
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
	 * @return an {@link Observable} stream of delayed {@link Instant}s pushed
	 *         to any {@link Observable#subscribe} caller
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
	 *         source instants or an error occurs
	 */
	default <T> Observable<Expectation> schedule( final Iterable<Instant> when,
		final ThrowingRunnable<?> what )
	{
		return schedule( when, t -> what.run() );
	}

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * <p>
	 * NOTE that the {@link Instant} stream is consumed lazily in
	 * {@link #schedule(Iterable, ThrowingConsumer)} and IRREPRODUCIBLY parallel
	 * in {@link #schedule(Observable, ThrowingConsumer)}
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @param what the {@link Consumer} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of {@link Expectation}s for each
	 *         next {@link Instant} pushed to any {@link Observable#subscribe}
	 *         caller until completion of simulation time or source instants or
	 *         an error occurs
	 */
	default <T> Observable<Expectation> schedule( final Iterable<Instant> when,
		final ThrowingConsumer<Instant, ?> what )
	{
		final Subject<Expectation> result = PublishSubject.create();
		schedule( when, result ).subscribe( t ->
		{
			try
			{
				what.accept( t );
			} catch( final Throwable e )
			{
				LogUtil.getLogger( Scheduler.class )
						.error( "Problem in " + what, e );
				result.onError( e );
			}
		}, e ->
		{
			// ignore errors, already passed to result Observable by catch(){..}
		} );
		return result;
	}

	/**
	 * Schedule a stream of values resulting from executing a {@link Callable}
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @param what the {@link Callable} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of results pushed to any
	 *         {@link Observable#subscribe} caller until completion of
	 *         simulation time or observed instants or an error occurs
	 */
//	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default <R> Observable<R> schedule( final Iterable<Instant> when,
		final Callable<R> what )
	{
		return schedule( when ).map( t -> what.call() );
	}

	/**
	 * Delay a stream of {@link Instant}s scheduled on this {@link Scheduler}
	 * and optionally observe each prior {@link Expectation}, e.g. to cancel one
	 * 
	 * @param when the {@link Iterable} stream of {@link Instant}s
	 * @param what (optional) {@link Observer} of {@link Expectation}s for each
	 *            upcoming {@link Instant}
	 * @return transformed {@link Observable} stream of delayed {@link Instant}s
	 *         pushed to any {@link Observable#subscribe} caller
	 */
	default Observable<Instant> schedule( final Iterable<Instant> when,
		final Observer<Expectation> what )
	{
		final Subject<Instant> delayedCopy = PublishSubject.create();
		// schedule first element from iterator
		final Iterator<Instant> it = when.iterator();
		if( !it.hasNext() ) return Observable.empty();
		final Instant t0 = it.next();
		final Expectation exp0 = schedule( t0, delayedCopy::onNext );
		if( what != null ) what.onNext( exp0 );
		// schedule each following element upon merge with delayed previous
		return delayedCopy.zipWith( () -> it, ( t, t_next ) ->
		{
			final Expectation exp = schedule( t_next, delayedCopy::onNext );
			if( what != null ) what.onNext( exp );
			return t;
		} )/* .serialize() */;
	}

	/**
	 * Delay a stream of {@link Instant}s scheduled on this {@link Scheduler}
	 * <p>
	 * NOTE ensure REPRODUCIBLE scheduling with {@link Observable#serialize()}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @return transformed {@link Observable} stream of delayed {@link Instant}s
	 *         pushed to any {@link Observable#subscribe} caller
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default Observable<Instant> schedule( final Observable<Instant> when )
	{
		return schedule( when, (Observer) null );
	}

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * <p>
	 * NOTE ensure REPRODUCIBLE scheduling with {@link Observable#serialize()}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @param what the {@link Runnable} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of {@link Expectation}s for each
	 *         next {@link Instant} pushed to any {@link Observable#subscribe}
	 *         caller until completion of simulation time or source instants or
	 *         an error occurs
	 */
	default <T> Observable<Expectation> schedule(
		final Observable<Instant> when, final ThrowingRunnable<?> what )
	{
		return schedule( when, t -> what.run() );
	}

	/**
	 * Schedule a stream of {@link Expectation}s for execution of {@code what}
	 * <p>
	 * NOTE ensure REPRODUCIBLE scheduling with {@link Observable#serialize()}
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s
	 * @param what the {@link Consumer} to execute upon each {@link Instant}
	 * @return an {@link Observable} stream of {@link Expectation}s for each
	 *         next {@link Instant} pushed to any {@link Observable#subscribe}
	 *         caller, until completion of simulation time or source instants or
	 *         an error occurs
	 */
	default <T> Observable<Expectation> schedule(
		final Observable<Instant> when,
		final ThrowingConsumer<Instant, ?> what )
	{
		final Subject<Expectation> result = PublishSubject.create();
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
			// consume errors, as they are already passed to result Observable
		} );
		return result;
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
	 * @return an {@link Observable} stream of results, pushed to any
	 *         {@link Observable#subscribe} caller until completion of
	 *         simulation time or observed of instants or an error occurs
	 */
//	@SuppressWarnings( { "unchecked", "rawtypes" } )
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
	 *         pushed to any {@link Observable#subscribe} caller
	 */
	default Observable<Instant> schedule( final Observable<Instant> when,
		final Observer<Expectation> what )
	{
		final Subject<Instant> delayedCopy = PublishSubject.create();
		return when.map( t ->
		{
			final Expectation exp = schedule( t, delayedCopy::onNext );
			if( what != null ) what.onNext( exp );
			return t;
		} ).zipWith(
				// merge "when" (observed eagerly) with "delayed" by scheduler
				delayedCopy, ( t, t0 ) -> t );
	}

	/**
	 * {@link Factory}
	 */
	interface Factory
	{

		default Scheduler create( SchedulerConfig config )
		{
			return Instantiator.instantiate( config.implementation() );
		}

		default Scheduler create( final Map<?, ?>... imports )
		{
			return create( SchedulerConfig.getOrCreate( imports ) );
		}

		default Scheduler create( final String rawId,
			final Map<?, ?>... imports )
		{
			return create( SchedulerConfig.getOrCreate( rawId, imports ) );
		}

		default Observable<Scheduler> createAndRun(
			final Observable<String> ids, final Map<?, ?>... imports )
			throws Throwable
		{
			return Observable.create( sub -> ids.subscribe(
					id -> create( id, imports ).run( sub::onNext ),
					sub::onError, sub::onComplete ) );
		}

		@Singleton
		class Rebinder implements Factory, LocalBinding
		{
			@Inject
			private LocalBinder binder;

			@Override
			public Scheduler create( final SchedulerConfig config )
			{
				// FIXME use some configuration mechanism during injection, rather than resetting 
				binder().reset( SchedulerConfig.class, config );
				final Scheduler scheduler = binder()
						.inject( config.implementation() );
				binder().reset( Scheduler.class, scheduler );
				return scheduler;
			}

			@Override
			public LocalBinder binder()
			{
				return this.binder;
			}
		}
	}
}