/* $Id$
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

import java.text.MessageFormat;

/**
 * {@link Thrower}
 * 
 * @version $Id$
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
	 * @param type the {@link Exception} to throw using its {@code (String)}
	 *            {@link Constructor}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @param <R> the dynamic return type
	 * @param <E> the {@link Exception} type thrown
	 */
	public static <R, E extends Exception> R throwNew( final Class<E> type,
		final String messageFormat, final Object... args ) throws E
	{
		return throwNew( type, null, messageFormat, args );
	}

	/**
	 * @param type the {@link Exception} to throw using its {@code (String)}
	 *            {@link Constructor}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @param <R> the dynamic return type
	 * @param <E> the {@link Exception} type thrown
	 */
	public static <R, E extends Exception> R throwNew( final Class<E> type,
		final Throwable cause, final String messageFormat,
		final Object... args ) throws E
	{
		try
		{
			final String message = ExceptionBuilder
					.format( messageFormat, args ).getFormattedMessage();
			throw cause == null
					? type.getConstructor( String.class ).newInstance( message )
					: type.getConstructor( String.class, Throwable.class )
							.newInstance( message, cause );
		} catch( final Throwable e )
		{
			return rethrowUnchecked( e );
		}
	}

}
