/* $Id: cccb3d9313ff65e159321e3a8283c101ef6a24b3 $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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
 * Copyright (c) 2015 Almende B.V. 
 */
package io.coala.exception.x;

import io.coala.exception.x.UncheckedException.Builder;

/**
 * {@link ExceptionBuilder} creates {@link CheckedException}s and
 * {@link UncheckedException}s and publishes them as {@link Contextualized}
 * via static {@link #getObservable()} method.
 * <p>
 * TODO add createError(...) methods?
 * 
 * @date $Date$
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public abstract class ExceptionBuilder<THIS extends ExceptionBuilder<THIS>>
{
	/** */
	protected final ExceptionContext context = new ExceptionContext();

	/** */
	protected final Throwable cause;

	/** */
	protected final String message;

	/**
	 * {@link ExceptionBuilder} constructor
	 * 
	 * @param message the detailed description of the {@link CheckedException}
	 * @param cause the {@link Throwable} causing the new
	 *            {@link CheckedException}, or {@code null} if none
	 */
	protected ExceptionBuilder(final String message, final Throwable cause)
	{
		this.message = message;
		this.cause = cause;
		// with("message", message);
		// with("cause", cause == null ? null : cause.getClass().getName());
	}

	/**
	 * @param key the context entry key
	 * @param value the context entry value
	 * @return this {@link Builder}
	 */
	@SuppressWarnings("unchecked")
	public THIS with(final String key, final Object value)
	{
		this.context.set(key, value);
		return (THIS) this;
	}

	/**
	 * @return the immutable {@link Contextualized}
	 */
	public abstract Contextualized build();

	/**
	 * Helper-method
	 * 
	 * @param message
	 * @return
	 */
	private static String toString(final Object message)
	{
		return message.toString();
	}

	/**
	 * Helper-method
	 * 
	 * @param messageFormat
	 * @param args
	 * @return
	 */
	private static String toString(final String messageFormat,
			final Object... args)
	{
		return String.format(messageFormat, args);
	}

	/**
	 * @param message
	 * @return
	 */
	public static CheckedException.Builder checked(final Object message)
	{
		return new CheckedException.Builder(toString(message), null);
	}

	/**
	 * @param messageFormat
	 * @param args
	 * @return
	 */
	public static CheckedException.Builder checked(final String messageFormat,
			final Object... args)
	{
		return new CheckedException.Builder(toString(messageFormat, args), null);
	}

	/**
	 * @param message
	 * @param args
	 * @return
	 */
	public static CheckedException.Builder checked(final String message,
			final Throwable cause)
	{
		return new CheckedException.Builder(toString(message), cause);
	}

	/**
	 * @param cause
	 * @param message
	 * @return
	 */
	public static CheckedException.Builder checked(final Throwable cause,
			final Object message)
	{
		return new CheckedException.Builder(toString(message), cause);
	}

	/**
	 * @param cause
	 * @param messageFormat
	 * @param args
	 * @return
	 */
	public static CheckedException.Builder checked(final Throwable cause,
			final String messageFormat, final Object... args)
	{
		return new CheckedException.Builder(toString(messageFormat, args),
				cause);
	}

	/**
	 * @param message
	 * @return
	 */
	public static UncheckedException.Builder unchecked(final Object message)
	{
		return new UncheckedException.Builder(toString(message), null);
	}

	/**
	 * @param messageFormat
	 * @param args
	 * @return
	 */
	public static UncheckedException.Builder unchecked(
			final String messageFormat, final Object... args)
	{
		return new UncheckedException.Builder(toString(messageFormat, args),
				null);
	}

	/**
	 * @param message
	 * @param args
	 * @return
	 */
	public static UncheckedException.Builder unchecked(final String message,
			final Throwable cause)
	{
		return new UncheckedException.Builder(toString(message), cause);
	}

	/**
	 * @param cause
	 * @param message
	 * @return
	 */
	public static UncheckedException.Builder unchecked(final Throwable cause,
			final Object message)
	{
		return new UncheckedException.Builder(toString(message), cause);
	}

	/**
	 * @param cause
	 * @param messageFormat
	 * @param args
	 * @return
	 */
	public static UncheckedException.Builder unchecked(final Throwable cause,
			final String messageFormat, final Object... args)
	{
		return new UncheckedException.Builder(toString(messageFormat, args),
				cause);
	}

}
