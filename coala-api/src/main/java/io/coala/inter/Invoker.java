/* $Id: 8a4a05908ddbd1ff11538f952eeb93cca85a6a98 $
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.inter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.coala.exception.Thrower;
import io.reactivex.Observable;

/**
 * {@link Invoker}
 * 
 * @version $Id: 8a4a05908ddbd1ff11538f952eeb93cca85a6a98 $
 * @author Rick van Krevelen
 */
public interface Invoker
{

	/**
	 * @param method the target {@link Method} to call
	 * @param args the invocation arguments, or {@code null)
	 * @return an {@link Observable} stream of just zero or one return value, or
	 *         possibly some {@link InvocationTargetException}
	 */
	<R> Observable<R> invoke( URI endpoint, Method method, Object... args );

	String SYNC_TIMEOUT_DEFAULT = "PT30S";

	Duration SYNC_TIMEOUT = Duration.parse( SYNC_TIMEOUT_DEFAULT );

	/**
	 * @param abstractType the interface to proxy
	 * @param address the exposed {@link URI}
	 * @return a {@link Proxy} implementation
	 */
//	@SuppressWarnings( "unchecked" )
	default <T> T createProxy( final Class<T> abstractType, final URI address )
	{
		return createProxy( abstractType, address, SYNC_TIMEOUT );
	}

	/**
	 * @param abstractType the interface to proxy
	 * @param address the exposed {@link URI}
	 * @param timeout the timeout {@link Duration}
	 * @return a {@link Proxy} implementation
	 */
//	@SuppressWarnings( "unchecked" )
	default <T> T createProxy( final Class<T> abstractType, final URI address,
		final Duration timeout )
	{
		return createProxy( abstractType, address, timeout, () ->
		this );
	}

	/**
	 * @param abstractType the interface to proxy
	 * @param address the exposed {@link URI}
	 * @param timeout the timeout {@link Duration}
	 * @return a {@link Proxy} implementation
	 */
	@SuppressWarnings( "unchecked" )
	static <T> T createProxy( final Class<T> abstractType, final URI address,
		final Duration timeout, final Supplier<Invoker> invoker )
	{
		return (T) Proxy.newProxyInstance( abstractType.getClassLoader(),
				new Class[]
		{ abstractType }, ( proxy, method, args ) ->
		{
			// shortcut native Object#..() methods to (local) Invoker
			if( (method.getName() == "toString"
					&& (args == null || args.length == 0))
					|| (method.getName() == "hashCode"
							&& (args == null || args.length == 0))
					|| (method.getName() == "equals"
							&& (args != null && args.length == 1)) )
				return method.invoke( invoker.get(), args );

			final Object[] result = { null };
			final CountDownLatch latch = new CountDownLatch( 1 );
			invoker.get().invoke( address, method, args ).subscribe( t ->
			{
				result[0] = t;
				latch.countDown();
			}, e ->
			{
				result[0] = e;
				e.printStackTrace();
				latch.countDown();
			}, () -> latch.countDown() );
			final long nanos = timeout.getSeconds() * 1000000000
					+ timeout.getNano();
			latch.await( nanos, TimeUnit.NANOSECONDS );
			if( latch.getCount() != 0 ) return Thrower.throwNew(
					TimeoutException::new,
					() -> "Response from " + address + " took >" + timeout );
			if( result[0] instanceof Throwable )
				return Thrower.rethrowUnchecked( (Throwable) result[0] );
			return result[0];
		} );
	}
}