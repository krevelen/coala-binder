/* $Id: 1985d4f78badb6ddf5ae4821a1131efbc50e0895 $
 *  
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.coala.rx;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import io.coala.exception.ExceptionFactory;
import io.coala.log.LogUtil;
import io.coala.util.Util;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

/**
 * {@link RxUtil} provides some
 * <a href="https://github.com/Netflix/RxJava/wiki">RxJava</a>-related utilities
 * 
 * @version $Id: 1985d4f78badb6ddf5ae4821a1131efbc50e0895 $
 * @author Rick van Krevelen
 */
public class RxUtil implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( RxUtil.class );

	/**
	 * {@link RxUtil} constructor
	 */
	private RxUtil()
	{
		// empty
	}

	/**
	 * {@link ThrowingFunc1} TODO apply a native way to map throwing functions
	 * in rxJava
	 * 
	 * @param <T> the input value type
	 * @param <R> the result type
	 * @version $Id: 1985d4f78badb6ddf5ae4821a1131efbc50e0895 $
	 * @author Rick van Krevelen
	 */
	public static interface ThrowingFunc1<T, R> extends Function<T, R>
	{

		/**
		 * @param t1
		 * @return
		 * @throws Throwable
		 */
		R call( T t1 ) throws Throwable;
	}

	/**
	 * @param source
	 * @param throwingFunc1
	 * @return
	 */
	public static <S, T> Observable<T> map( final Observable<S> source,
		final ThrowingFunc1<S, T> func )
	{
		return Observable.create( sub ->
		{
			source.subscribe( new Observer<S>()
			{
				@Override
				public void onComplete()
				{
					sub.onComplete();
				}

				@Override
				public void onError( final Throwable e )
				{
					sub.onError( e );
				}

				@Override
				public void onNext( final S s )
				{
					try
					{
						sub.onNext( func.call( s ) );
					} catch( final Throwable e )
					{
						sub.onError( e );
					}
				}

				@Override
				public void onSubscribe( final Disposable d )
				{
					// FIXME what to do here?
				}
			} );
		} );
	}

	/**
	 * @param source the {@link Observable} of which the first emitted object
	 *            will be returned, blocking the current thread until it does
	 * @return the first observed/emitted object
	 * @throws Throwable the first error that occurred before the first object
	 *             was emitted
	 */
	public static <T> T awaitFirst( final Observable<T> source )
	{
		return awaitFirst( source, 0, null );
	}

	/**
	 * @param source the {@link Observable} of which the first emitted object
	 *            will be returned, blocking the current thread until it does or
	 *            timeout occurs
	 * @param timeout the maximum time to wait, or <=0 for indefinite
	 * @param unit the {@link TimeUnit} of the {@code timeout} value
	 * @return the first observed/emitted object
	 * @throws Throwable the first error that was occurred before the first
	 *             object was observed
	 */
	public static <T> T awaitFirst( final Observable<T> source,
		final long timeout, final TimeUnit unit )
	{
		final List<T> list = awaitAll( source.take( 1 ), timeout, unit );
		if( list.isEmpty() ) throw new NullPointerException(
				"No first element: nothing emitted" );
		return list.get( 0 );
	}

	/**
	 * @param source the {@link Observable} of which all emitted objects will be
	 *            returned, blocking the current thread until it completes
	 * @return the @link List} of observed/emitted objects
	 * @throws Throwable the first error that occurred while observing the
	 *             objects
	 */
	public static <T> List<T> awaitAll( final Observable<T> source )
	{
		return awaitAll( source, 0, null );
	}

	/**
	 * @param source the {@link Observable} of which all emitted objects will be
	 *            returned, blocking the current thread until it completes or a
	 *            timeout occurs
	 * @param timeout the maximum time to wait, or {@code <=0} for never
	 * @param unit the {@link TimeUnit} of the {@code timeout} value
	 * @return the @link List} of observed/emitted objects
	 * @throws Throwable the first error that occurred while observing the
	 *             objects
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> List<T> awaitAll( final Observable<T> source,
		final long timeout, final TimeUnit unit )
	{
		// the container object that will lock the current thread
		final Object[] container = new Object[] { null };
		final CountDownLatch latch = new CountDownLatch( 1 );
		final long startTime = System.currentTimeMillis();
		final long maxDuration = unit == null ? 0
				: TimeUnit.MILLISECONDS.convert( timeout, unit );
		source.toList().subscribe( ( list, e ) ->
		{
			synchronized( container )
			{
				container[0] = list == null ? list : e;
				latch.countDown();
			}
		} );

		int i = 0;
		long duration = 0;
		while( container[0] == null
				&& (maxDuration <= 0 || duration < maxDuration) )
		{
			duration = System.currentTimeMillis() - startTime;
			try
			{
				if( maxDuration <= 0 ) // wait indefinitely
				{
					if( i++ > 0 ) LOG.trace( String.format(
							"awaiting first emitted item (t+%.3fs)...",
							(double) duration / 1000 ) );
					latch.await();
				} else
				{
					if( i++ > 0 ) LOG.trace( String.format(
							"awaiting first emitted item (remaining: %.3fs)...",
							(double) (maxDuration - duration) / 1000 ) );
					latch.await( timeout, unit );
				}
			} catch( final InterruptedException e )
			{
				container[0] = e;
			}
		}

		if( container[0] instanceof RuntimeException )
			throw (RuntimeException) container[0];

		if( container[0] instanceof Throwable ) throw ExceptionFactory
				.createUnchecked( (Throwable) container[0], "awaitAll" );

		if( container[0] == null )
			throw ExceptionFactory.createUnchecked( (Throwable) container[0],
					String.format( "Timeout occured at %.3fs",
							(double) duration / 1000 ) );

		return (List<T>) container[0];
	}

}
