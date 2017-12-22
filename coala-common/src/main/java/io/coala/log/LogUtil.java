/* $Id: 4f23dfdf61f2dda530b5b2588f296f5889748fd4 $
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
package io.coala.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.StringFormattedMessage;

import io.coala.util.Util;

/**
 * {@link LogUtil} provides some utility methods for Log4j (v2)
 * MessageFormatMessage
 * 
 * @version $Id: 4f23dfdf61f2dda530b5b2588f296f5889748fd4 $
 * @author Rick van Krevelen
 */
public class LogUtil implements Util
{

	static
	{
		final LogConfig conf = LogConfig.getOrCreate();
		if( !LogConfig.JUL_MANAGER_DEFAULT
				.equals( System.getProperty( LogConfig.JUL_MANAGER_KEY ) ) )
			getLogger( LogUtil.class ).trace(
					"java.util.logging not bridged to Log4j2; "
							+ "set JVM system property (-D) manually: {}={}",
					LogConfig.JUL_MANAGER_KEY, LogConfig.JUL_MANAGER_DEFAULT );

		Locale.setDefault( conf.locale() );
	}

	/**
	 * @param clazz the object type generating the log messages
	 * @return the {@link Logger} instance for specified {@code clazz}
	 */
	public static Logger getLogger( final Class<?> clz )
	{
		return getLogger( clz, clz );
	}

	/**
	 * @param source the {@link Object} requesting log messages
	 * @return the {@link Logger} instance for specified {@code clazz}
	 */
	public static Logger getLogger( final Object source )
	{
		return getLogger( source.getClass(), source );
	}

	/**
	 * @param clazz the object type generating the log messages
	 * @param source the {@link Object} requesting log messages
	 * @return the {@link Logger} instance for specified {@code clazz}
	 */
	public static Logger getLogger( final Class<?> clz, final Object source )
	{
		return getLogger( clz.getName(), source );
	}

	/**
	 * @param clazz the client logger's (fully qualified class) name
	 * @param source the {@link Object} requesting log messages
	 * @return the {@link Logger} instance for specified {@code FQCN}
	 */
	public static Logger getLogger( final String fqcn, final Object source )
	{
		if( source == null )
		{
			LogManager.getRootLogger()
					.warn( "Omitting null source for logger: {}", fqcn );
			return getLogger( fqcn );
		}
//		else if(source instanceof Identified)
//		{
//			LogManager.getFactory().g
//		}

		return LogManager.getLogger( fqcn );
		/* TODO add time/id from source to message, using log4j2 formatters ? */
	}

	/**
	 * this method is preferred over {@link Logger#getLogger} so as to
	 * initialize the Log4j2 system correctly via this {@link LogUtil} class
	 * 
	 * @param name
	 * @return
	 */
	public static Logger getLogger( final String name )
	{
		return LogManager.getLogger( name );
	}

	/**
	 * The returned {@link FormattedMessage} will try
	 * <ol>
	 * <li>{@link MessageFormat}, e.g. '{0}' or '{1,number,$,##}';
	 * <li>{@link String#format}, e.g. '%s' or '%04.2f'; and finally
	 * <li>{@link ParameterizedMessage} standard '{}' formatting.
	 * </ol>
	 * 
	 * @param pattern
	 * @param arg
	 * @return a {@link FormattedMessage} for (delayed) parameterized formatting
	 *         in e.g. {@link Logger#trace(Message,Throwable)}
	 * @see MessageFormatMessage
	 * @see StringFormattedMessage
	 * @see ParameterizedMessage
	 */
	public static FormattedMessage messageOf( final String pattern,
		final Object arg )
	{
		return new FormattedMessage( pattern, arg );
	}

	/**
	 * The returned {@link FormattedMessage} will try
	 * <ol>
	 * <li>{@link MessageFormat}, e.g. '{0}' or '{1,number,$,##}';
	 * <li>{@link String#format}, e.g. '%s' or '%04.2f'; and finally
	 * <li>{@link ParameterizedMessage} standard '{}' formatting.
	 * </ol>
	 * 
	 * @param pattern
	 * @param arg1
	 * @param arg2
	 * @return a {@link FormattedMessage} for delayed parameterized formatting
	 *         in e.g. {@link Logger#trace(Message,Throwable)}
	 * @see MessageFormatMessage
	 * @see StringFormattedMessage
	 * @see ParameterizedMessage
	 */
	public static FormattedMessage messageOf( final String pattern,
		final Object arg1, Object arg2 )
	{
		return new FormattedMessage( pattern, arg1, arg2 );
	}

	/**
	 * The returned {@link FormattedMessage} will try
	 * <ol>
	 * <li>{@link MessageFormat}, e.g. '{0}' or '{1,number,$,##}';
	 * <li>{@link String#format}, e.g. '%s' or '%04.2f'; and finally
	 * <li>{@link ParameterizedMessage} standard '{}' formatting.
	 * </ol>
	 * 
	 * @param pattern
	 * @param args
	 * @return a {@link FormattedMessage} for delayed parameterized formatting
	 *         in e.g. {@link Logger#trace(Message,Throwable)}
	 * @see MessageFormatMessage
	 * @see StringFormattedMessage
	 * @see ParameterizedMessage
	 */
	public static FormattedMessage messageOf( final String pattern,
		final Object... args )
	{
		return new FormattedMessage( pattern, args );
	}

	/**
	 * The returned {@link FormattedMessage} will try
	 * <ol>
	 * <li>{@link MessageFormat}, e.g. '{0}' or '{1,number,$,##}';
	 * <li>{@link String#format}, e.g. '%s' or '%04.2f'; and finally
	 * <li>{@link ParameterizedMessage} standard '{}' formatting.
	 * </ol>
	 * 
	 * @param throwable
	 * @param pattern
	 * @param args
	 * @return a {@link FormattedMessage} for delayed parameterized formatting
	 *         in e.g. {@link Logger#trace(Message,Throwable)}
	 * @see MessageFormatMessage
	 * @see StringFormattedMessage
	 * @see ParameterizedMessage
	 */
	public static FormattedMessage messageOf( final Throwable throwable,
		final String pattern, final Object... args )
	{
		return new FormattedMessage( pattern, args, throwable );
	}

	/**
	 * @param clazz the object type generating the log messages
	 * @return the {@link java.util.logging.Logger} instance for specified
	 *         {@code clazz}
	 */
	public static java.util.logging.Logger getJavaLogger( final Class<?> clazz )
	{
		return getJavaLogger( clazz.getName() );
	}

	/**
	 * @param clazz the object type generating the log messages
	 * @param level the level up to which messages should be logged, if allowed
	 *            by the bound (e.g. root) logger's settings
	 * @return the {@link java.util.logging.Logger} instance for specified
	 *         {@code clazz}
	 */
	public static java.util.logging.Logger getJavaLogger( final Class<?> clazz,
		final java.util.logging.Level level )
	{
		return getJavaLogger( clazz.getName(), level );
	}

	/**
	 * @param name the logger's name
	 * @return the {@link java.util.logging.Logger} instance for specified
	 *         {@code name}
	 */
	public static java.util.logging.Logger getJavaLogger( final String name )
	{
		return getJavaLogger( name,
				java.util.logging.Logger.getGlobal().getLevel() );
	}

	/**
	 * @param name the logger's name
	 * @param level the level up to which messages should be logged, if allowed
	 *            by the bound (e.g. root) logger's settings
	 * @return the {@link java.util.logging.Logger} instance for specified
	 *         {@code name}
	 */
	public static java.util.logging.Logger getJavaLogger( final String name,
		final java.util.logging.Level level )
	{
		final java.util.logging.Logger result = java.util.logging.Logger
				.getLogger( name );
		result.setLevel( level );
		return result;
	}

	/**
	 * {@link Pretty}
	 * 
	 * @version $Id: 4f23dfdf61f2dda530b5b2588f296f5889748fd4 $
	 * @author Rick van Krevelen
	 */
	public static interface Pretty
	{
		/** @return (cached) result of some {@link String} {@link Supplier} */
		@Override
		String toString();

		/**
		 * @param supplier the function to decorate in {@link #toString()} calls
		 * @return a decorator {@link Object} to help delay {@link #toString()}
		 *         call until absolutely necessary (e.g. for logging at a
		 *         desired level)
		 */
		static Pretty of( final Supplier<?> supplier )
		{
			return new Pretty()
			{
				private String cache = null;

				@Override
				public String toString()
				{
					if( this.cache != null ) return this.cache;
					try
					{
						final Object obj = supplier.get();
						return (this.cache = obj == null ? null
								: obj.toString());
					} catch( final Exception e )
					{
						final StringWriter w = new StringWriter()
								.append( "ERROR in " + supplier + ": " );
						e.printStackTrace( new PrintWriter( w ) );
						return w.toString();
					}
				}
			};
		}
	}

	public static Pretty toCachedString( final Supplier<?> supplier )
	{
		return Pretty.of( supplier );
	}
}
