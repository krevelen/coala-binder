package io.coala.exception;

import io.coala.json.Contextual;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link ExceptionStream} wraps a {@link Subject} for {@link Contextual}
 * {@link Throwable}s via static {@link #toPublished(Throwable)} and
 * {@link #asObservable()} methods.
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class ExceptionStream<T extends Throwable & Contextual>
{

	/** the singleton {@link ExceptionStream} instance */
	private static volatile ExceptionStream<?> INSTANCE = null;

	/**
	 * @return the singleton {@link ExceptionStream} instance
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends Throwable & Contextual> ExceptionStream<T>
		getInstance()
	{
		return INSTANCE != null ? (ExceptionStream<T>) INSTANCE
				: (ExceptionStream<T>) (INSTANCE = new ExceptionStream<T>());
	}

	/**
	 * Helper-method
	 * 
	 * @param e the {@link Contextual} {@link Throwable} to publish
	 * @return the same {@link Contextual} to allow chaining
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends Throwable & Contextual> T toPublished( final T e )
	{
		((Observer<T>) getInstance().subject).onNext( e );
		return e;
	}

	/**
	 * @return an {@link Observable} of {@link Contextual} {@link Throwable} s
	 */
	public static Observable<? extends Throwable> asObservable()
	{
		return getInstance().subject;
	}

	/** */
	private final Subject<T> subject = PublishSubject.create();

	/**
	 * {@link ExceptionStream} singleton constructor
	 */
	private ExceptionStream()
	{
		Runtime.getRuntime().addShutdownHook( new Thread()
		{
			@Override
			public void run()
			{
				this.setName( ExceptionStream.class.getSimpleName() );
				subject.onComplete();
			}
		} );
	}
}