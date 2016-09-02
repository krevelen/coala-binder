/* $Id$
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
package io.coala.exception;

import java.text.MessageFormat;

import io.coala.exception.ExceptionBuilder.CheckedException;
import io.coala.exception.ExceptionBuilder.UncheckedException;

/**
 * {@link ExceptionFactory} has shorthand utility methods to create
 * {@link CheckedException}s and {@link UncheckedException}s which are emitted
 * via the {@link ExceptionStream}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public abstract class ExceptionFactory
{

	/**
	 * @param message the message this {@link CheckedException}
	 * @return a {@link CheckedException}
	 */
	public static CheckedException createChecked( final Object message )
	{
		return ExceptionBuilder.checked( message ).build();
	}

	/**
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link CheckedException}
	 */
	public static Exception createChecked( final String messageFormat,
		final Object... args )
	{
		return ExceptionBuilder.checked( messageFormat, args ).build();
	}

	/**
	 * @param message the message this {@link CheckedException}
	 * @param cause the cause of this {@link CheckedException}
	 * @return a {@link CheckedException}
	 */
	public static Exception createChecked( final String message,
		final Throwable cause )
	{
		return ExceptionBuilder.checked( message, cause ).build();
	}

	/**
	 * @param cause the cause of this {@link CheckedException}
	 * @param message the message this {@link CheckedException}
	 * @return a {@link CheckedException}
	 */
	public static Exception createChecked( final Throwable cause,
		final Object message )
	{
		return ExceptionBuilder.checked( cause, message ).build();
	}

	/**
	 * @param cause the cause of this {@link CheckedException}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link CheckedException}
	 */
	public static Exception createChecked( final Throwable cause,
		final String messageFormat, final Object... args )
	{
		return ExceptionBuilder.checked( cause, messageFormat, args ).build();
	}

	/**
	 * @param message the message this {@link UncheckedException}
	 * @return a {@link UncheckedException}
	 */
	public static UncheckedException createUnchecked( final Object message )
	{
		return ExceptionBuilder.unchecked( message ).build();
	}

	/**
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link UncheckedException}
	 */
	public static UncheckedException
		createUnchecked( final String messageFormat, final Object... args )
	{
		return ExceptionBuilder.unchecked( messageFormat, args ).build();
	}

	/**
	 * @param message the message this {@link UncheckedException}
	 * @param cause the cause of this {@link UncheckedException}
	 * @return a {@link UncheckedException}
	 */
	public static UncheckedException createUnchecked( final String message,
		final Throwable cause )
	{
		return ExceptionBuilder.unchecked( message, cause ).build();
	}

	/**
	 * @param cause the cause of this {@link UncheckedException}
	 * @param message the message this {@link UncheckedException}
	 * @return a {@link UncheckedException}
	 */
	public static UncheckedException createUnchecked( final Throwable cause,
		final Object message )
	{
		return ExceptionBuilder.unchecked( cause, message ).build();
	}

	/**
	 * @param cause the cause of this {@link UncheckedException}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link UncheckedException}
	 */
	public static UncheckedException createUnchecked( final Throwable cause,
		final String messageFormat, final Object... args )
	{
		return ExceptionBuilder.unchecked( cause, messageFormat, args ).build();
	}
}
