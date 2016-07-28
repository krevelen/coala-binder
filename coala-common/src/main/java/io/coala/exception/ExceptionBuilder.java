/* $Id: 159ef9e618c1d99cb8ef0ad10548596a3f3afff6 $
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
import java.util.Arrays;
import java.util.Collections;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;

import com.eaio.uuid.UUID;

import io.coala.exception.ExceptionBuilder.CheckedException;
import io.coala.json.Contextual;
import io.coala.json.Contextual.Context;
import io.coala.log.LogUtil;
import io.coala.name.Identified;

/**
 * {@link ExceptionBuilder} creates {@link CheckedException.Builder}s and
 * {@link UncheckedException.Builder}s, each of which emit the Exceptions upon
 * {@link #build()} via the {@link ExceptionStream}
 * 
 * @param <THIS>
 * @version $Id$
 * @author Rick van Krevelen
 */
public abstract class ExceptionBuilder<THIS extends ExceptionBuilder<THIS>>
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( ExceptionBuilder.class );

	/**
	 * @param message the message this {@link CheckedException}
	 * @return a {@link CheckedException.Builder}
	 */
	public static CheckedException.Builder checked( final Object message )
	{
		return new CheckedException.Builder( format( message ), null );
	}

	/**
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link CheckedException.Builder}
	 */
	public static CheckedException.Builder checked( final String messageFormat,
		final Object... args )
	{
		return new CheckedException.Builder( format( messageFormat, args ),
				null );
	}

	/**
	 * @param message the message this {@link CheckedException}
	 * @param cause the cause of this {@link CheckedException}
	 * @return a {@link CheckedException.Builder}
	 */
	public static CheckedException.Builder checked( final String message,
		final Throwable cause )
	{
		return new CheckedException.Builder( format( message ), cause );
	}

	/**
	 * @param cause the cause of this {@link CheckedException}
	 * @param message the message this {@link CheckedException}
	 * @return a {@link CheckedException.Builder}
	 */
	public static CheckedException.Builder checked( final Throwable cause,
		final Object message )
	{
		return new CheckedException.Builder( format( message ), cause );
	}

	/**
	 * @param cause the cause of this {@link CheckedException}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link CheckedException.Builder}
	 */
	public static CheckedException.Builder checked( final Throwable cause,
		final String messageFormat, final Object... args )
	{
		return new CheckedException.Builder( format( messageFormat, args ),
				cause );
	}

	/**
	 * @param message the message this {@link UncheckedException}
	 * @return a {@link UncheckedException.Builder}
	 */
	public static UncheckedException.Builder unchecked( final Object message )
	{
		return new UncheckedException.Builder( format( message ), null );
	}

	/**
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link UncheckedException.Builder}
	 */
	public static UncheckedException.Builder
		unchecked( final String messageFormat, final Object... args )
	{
		return new UncheckedException.Builder( format( messageFormat, args ),
				null );
	}

	/**
	 * @param message the message this {@link UncheckedException}
	 * @param cause the cause of this {@link UncheckedException}
	 * @return a {@link UncheckedException.Builder}
	 */
	public static UncheckedException.Builder unchecked( final String message,
		final Throwable cause )
	{
		return new UncheckedException.Builder( format( message ), cause );
	}

	/**
	 * @param cause the cause of this {@link UncheckedException}
	 * @param message the message this {@link UncheckedException}
	 * @return a {@link UncheckedException.Builder}
	 */
	public static UncheckedException.Builder unchecked( final Throwable cause,
		final Object message )
	{
		return new UncheckedException.Builder( format( message ), cause );
	}

	/**
	 * @param cause the cause of this {@link UncheckedException}
	 * @param messageFormat following {@link MessageFormat} syntax
	 * @param args stringifiable arguments as referenced in
	 *            {@code messageFormat}
	 * @return a {@link UncheckedException.Builder}
	 */
	public static UncheckedException.Builder unchecked( final Throwable cause,
		final String messageFormat, final Object... args )
	{
		return new UncheckedException.Builder( format( messageFormat, args ),
				cause );
	}

	/** FIXME from config? */
	private static final MessageFactory MESSAGE_FACTORY = new ParameterizedMessageFactory();

	protected static Message format( final Object message )
	{
		try
		{
			return MESSAGE_FACTORY.newMessage( message );
		} catch( final Throwable t )
		{
			LOG.warn( new ParameterizedMessage( "Problem with message \"{}\"",
					message ), t );
			return format( message.toString() );
		}
	}

	protected static Message format( final String message )
	{
		try
		{
			return MESSAGE_FACTORY.newMessage( message );
		} catch( final Throwable t )
		{
			LOG.warn( new ParameterizedMessage( "Problem with message \"{}\"",
					message ), t );
			return new StringFormattedMessage( message.toString() );
		}
	}

	protected static Message format( final String messageFormat,
		final Object... params )
	{
		try
		{
			return MESSAGE_FACTORY.newMessage( messageFormat, params );
		} catch( final Throwable t )
		{
			LOG.warn( new ParameterizedMessage(
					"Problem with message format \"{}\" and params: {}",
					messageFormat, params == null ? Collections.emptyList()
							: Arrays.asList( params ) ),
					t );
			// attempt the standard String Formatter
			return new StringFormattedMessage( messageFormat, params );
		}
	}

	/**
	 * {@link CheckedException}
	 * 
	 * @version $Id: 9c42376aeeee3ec696a8eccb1de02b006d538881 $
	 * @author Rick van Krevelen
	 */
	public static class CheckedException extends Exception
		implements Contextual, Identified<UUID>
	{

		/**
		 * {@link CheckedException} factory method
		 * 
		 * @param context the {@link Context} or {@code null}
		 * @param message the {@link Message} or {@code null}
		 * @param cause the cause {@link Throwable} or {@code null}
		 * @return a new {@link CheckedException}
		 */
		public static CheckedException of( final Context context,
			final Message message, final Throwable cause )
		{
			final CheckedException result = cause == null
					? new CheckedException() : new CheckedException( cause );
			result.id = new UUID();
			result.message = message;
			result.context = context;
			return result;
		}

		/** */
		private static final long serialVersionUID = 1L;

		/** */
		private UUID id = null;

		/** */
		private Message message = null;

		/** */
		private Context context = null;

		/**
		 * {@link CheckedException} zero-arg bean constructor for JSON-RPC
		 */
		protected CheckedException()
		{
			// empty
		}

		/**
		 * {@link CheckedException} constructor
		 */
		protected CheckedException( final Throwable cause )
		{
			super( cause );
		}

		@Override
		public UUID id()
		{
			return this.id;
		}

		@Override
		public Context context()
		{
			return this.context;
		}

		@Override
		public String getMessage()
		{
			return message.getFormattedMessage();
		}

		/**
		 * {@link Builder}
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		public static class Builder extends ExceptionBuilder<Builder>
		{

			/**
			 * {@link Builder} constructor
			 * 
			 * @param message the detailed description of the
			 *            {@link CheckedException}
			 * @param cause the {@link Throwable} causing the new
			 *            {@link CheckedException}, or {@code null} if none
			 */
			public Builder( final Message message, final Throwable cause )
			{
				super( message, cause );
			}

			@Override
			public CheckedException build()
			{
				return ExceptionStream.toPublished( CheckedException.of(
						this.context == null ? null : this.context,
						this.message, this.cause ) );
			}
		}
	}

	/**
	 * {@link UncheckedException}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class UncheckedException extends RuntimeException
		implements Contextual, Identified<UUID>
	{

		/**
		 * {@link UncheckedException} factory method
		 * 
		 * @param context the {@link Context} or {@code null}
		 * @param message the {@link Message} or {@code null}
		 * @param cause the cause {@link Throwable} or {@code null}
		 * @return a new {@link UncheckedException}
		 */
		public static UncheckedException of( final Context context,
			final Message message, final Throwable cause )
		{
			final UncheckedException result = cause == null
					? new UncheckedException()
					: new UncheckedException( cause );
			result.id = new UUID();
			result.message = message;
			result.context = context;
			return result;
		}

		/** */
		private static final long serialVersionUID = 1L;

		/** */
		private UUID id;

		/** */
		private Message message = null;

		/** */
		private Context context = null;

		/**
		 * {@link UncheckedException} zero-arg bean constructor for JSON-RPC
		 */
		protected UncheckedException()
		{
			super();
		}

		/**
		 * {@link UncheckedException} constructor
		 * 
		 * @param cause
		 */
		protected UncheckedException( final Throwable cause )
		{
			super( cause );
		}

		@Override
		public UUID id()
		{
			return this.id;
		}

		@Override
		public Context context()
		{
			return this.context;
		}

		@Override
		public String getMessage()
		{
			try
			{
				return this.message == null ? null
						: this.message.getFormattedMessage();
			} catch( final Exception e )
			{
				e.printStackTrace();
				return this.message.toString();
			}
		}

		/**
		 * {@link CheckedExceptionBuilder}
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		public static class Builder extends ExceptionBuilder<Builder>
		{

			/**
			 * {@link CheckedExceptionBuilder} constructor
			 * 
			 * @param message the detailed description of the
			 *            {@link UncheckedException}
			 * @param cause the {@link Throwable} causing the new
			 *            {@link UncheckedException}, or {@code null} if none
			 */
			public Builder( final Message message, final Throwable cause )
			{
				super( message, cause );
			}

			@Override
			public UncheckedException build()
			{
				return ExceptionStream.toPublished( UncheckedException.of(
						this.context == null ? null : this.context.locked(),
						this.message, this.cause ) );
			}
		}
	}

	public static <T extends Identified<?> & Contextual> String
		format( final T self, final String message )
	{
		return self.context() == null
				? String.format( "%s [%s] %s", message, self.id() )
				: String.format( "%s [%s] %s", message, self.id(),
						self.context() );
	}

	/** */
	protected Context context = null;

	/** */
	protected final Throwable cause;

	/** */
	protected final Message message;

	/**
	 * {@link ExceptionBuilder} constructor
	 * 
	 * @param message the detailed description of the {@link CheckedException}
	 * @param cause the {@link Throwable} causing the new
	 *            {@link CheckedException}, or {@code null} if none
	 */
	protected ExceptionBuilder( final Message message, final Throwable cause )
	{
		this.message = message;
		this.cause = cause;
	}

	/**
	 * @param key the context entry key {@link String}
	 * @param value the context entry value {@link Object}
	 * @return this {@link CheckedException.Builder}
	 */
	@SuppressWarnings( "unchecked" )
	public THIS with( final String key, final Object value )
	{
		if( this.context == null ) this.context = new Context();
		this.context.set( key, value );
		return (THIS) this;
	}

	/**
	 * @return the new immutable {@link Contextual} {@link Exception}
	 */
	public abstract Contextual build();

}
