package io.coala.exception;

import io.coala.json.Contextualized;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link ExceptionStream} wraps a {@link Subject} for {@link Contextualized}
 * {@link Throwable}s via static {@link #toPublished(Throwable)} and
 * {@link #asObservable()} methods.
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class ExceptionStream<T extends Throwable & Contextualized>
{

	/** */
	private static volatile ExceptionStream<?> INSTANCE = null;

	/**
	 * @return the singleton instance
	 */
	@SuppressWarnings( "unchecked" )
	public static <T extends Throwable & Contextualized> ExceptionStream<T>
		getInstance()
	{
		if( INSTANCE == null ) INSTANCE = new ExceptionStream<T>();
		return (ExceptionStream<T>) INSTANCE;
	}

	/**
	 * Helper-method
	 * 
	 * @param e the {@link Contextualized} {@link Throwable} to publish
	 * @return the same {@link Contextualized} to allow chaining
	 */
	public static <T extends Throwable & Contextualized> T
		toPublished( final T e )
	{
		getInstance().subject.onNext( e );
		return e;
	}

	/**
	 * @return an {@link Observable} of {@link Contextualized} {@link Throwable}
	 *         s
	 */
	public static Observable<? extends Throwable> asObservable()
	{
		return getInstance().subject.asObservable();
	}

	/** */
	private final Subject<T, T> subject = PublishSubject.create();

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
				setName( ExceptionStream.class.getSimpleName() );
				subject.onCompleted();
			}
		} );
	}
}