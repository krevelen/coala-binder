/* $Id: 1887ca37caeee0492a6f9db1125983a28081836e $
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
package io.coala.exception;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link Thrower}
 * 
 * @version $Id: 1887ca37caeee0492a6f9db1125983a28081836e $
 * @author Rick van Krevelen
 */
public class Thrower
{
	/**
	 * {@link Thrower} singleton constructor
	 */
	private Thrower()
	{

	}

	/**
	 * @param <R> the phantom return type (determined at runtime)
	 * @param <E> the type of {@link Throwable} (determined at runtime)
	 * @param exception the (checked) {@link Throwable} to sneak by unchecked
	 * @return phantom value, never returns
	 * @throws E the sneaked {@link Throwable}
	 * @see rx.exceptions.Exceptions#propagate(Throwable)
	 * @see net.jodah.concurrentunit.Waiter#rethrow(Throwable)
	 */
	@SuppressWarnings( "unchecked" )
	public static <R, E extends Throwable> R
		rethrowUnchecked( final Throwable exception ) throws E
	{
		throw (E) exception;
	}

	/**
	 * @param exceptionFactory the {@link Function} to produce the actual
	 *            Exception
	 * @param messageFactory {@link Callable} message supplier
	 * @param <R> the dynamic (void) return type
	 * @param <E> the {@link Exception} type thrown
	 */
	public static <R, E extends Exception> R throwNew(
		final Function<String, E> exceptionFactory,
		final Supplier<String> messageFactory ) throws E
	{
		return rethrowUnchecked(
				(E) exceptionFactory.apply( messageFactory.get() ) );
	}

	/**
	 * @param exceptionFactory the {@link Function} to produce the actual
	 *            Exception
	 * @param messageFactory the first argument (e.g. message) supplier
	 * @param arg2 the second argument, e.g. Throwable cause
	 * @param <R> the run-time (void) return type
	 * @param <E> the {@link Exception} type thrown
	 */
	public static <R, T, E extends Exception> R throwNew(
		final BiFunction<String, T, E> exceptionFactory,
		final Supplier<String> messageFactory, final T arg2 ) throws E
	{
		return rethrowUnchecked(
				(E) exceptionFactory.apply( messageFactory.get(), arg2 ) );
	}

	/**
	 * @param exceptionFactory the {@link Function} to produce the actual
	 *            Exception
	 * @param messageFactory the first argument (e.g. message) supplier
	 * @param arg2 the second argument, e.g. Throwable cause
	 * @param <R> the run-time (void) return type
	 * @param <E> the {@link Exception} type thrown
	 */
	public static <R, E extends Exception> R throwNew(
		final BiFunction<String, Integer, E> exceptionFactory,
		final Supplier<String> messageFactory, final int arg2 ) throws E
	{
		return rethrowUnchecked(
				(E) exceptionFactory.apply( messageFactory.get(), arg2 ) );
	}

	/**
	 * @param type the {@link Exception} to throw using its {@code (String)}
	 *            {@link Constructor}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @param <R> the dynamic return type
	 * @param <E> the {@link Exception} type thrown
	 * @deprecated please consider using the {@link E}::new function on
	 *             {@link #throwNew(Function, Supplier)}
	 */
	@Deprecated
	public static <R, E extends Exception> R throwNew( final Class<E> type,
		final String messageFormat, final Object... args ) throws E
	{
		final String message = ExceptionBuilder.format( messageFormat, args )
				.getFormattedMessage();
		try
		{
			throw type.getConstructor( String.class ).newInstance( message );
		} catch( final InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e )
		{
			return rethrowUnchecked( new IllegalStateException( message, e ) );
		}
	}

	/**
	 * @param type the {@link Exception} to throw using its {@code (String)}
	 *            {@link Constructor}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @param <R> the dynamic return type
	 * @param <E> the {@link Exception} type thrown
	 * @deprecated please consider using the {@link E}::new function on
	 *             {@link #throwNew(Function, Supplier)}
	 */
	@Deprecated
	public static <R, E extends Exception> R throwNew( final Class<E> type,
		final Throwable cause, final String messageFormat,
		final Object... args ) throws E
	{
		final String message = ExceptionBuilder.format( messageFormat, args )
				.getFormattedMessage();
		try
		{
			throw type.getConstructor( String.class, Throwable.class )
					.newInstance( message, cause );
		} catch( final InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e )
		{
			return rethrowUnchecked( new IllegalStateException( message, e ) );
		}
	}

}