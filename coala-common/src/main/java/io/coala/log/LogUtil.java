/* $Id: ea08c5561c228e73b61d6868506fcd5150e897c0 $
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

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.slf4j.LoggerFactory;

import io.coala.exception.Thrower;
import io.coala.name.Identified;
import io.coala.util.Util;

/**
 * {@link LogUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
/**
 * {@link LogUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class LogUtil implements Util
{

	static
	{
		final LogConfig conf = LogConfig.getOrCreate();
		System.setProperty( LogConfig.JUL_MANAGER_KEY,
				conf.julManagerType().getName() );
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
	 * @param pattern
	 * @param arg
	 * @return a {@link ParameterizedMessage} for delayed parameterized
	 *         formatting in e.g. {@link Logger#trace(Message,Throwable)}
	 */
	public static ParameterizedMessage messageOf( final String pattern,
		final Object arg )
	{
		return new ParameterizedMessage( pattern, arg );
	}

	/**
	 * @param pattern
	 * @param arg1
	 * @param arg2
	 * @return a {@link ParameterizedMessage} for delayed parameterized
	 *         formatting in e.g. {@link Logger#trace(Message,Throwable)}
	 */
	public static ParameterizedMessage messageOf( final String pattern,
		final Object arg1, Object arg2 )
	{
		return new ParameterizedMessage( pattern, arg1, arg2 );
	}

	/**
	 * @param pattern
	 * @param args
	 * @return a {@link ParameterizedMessage} for delayed parameterized
	 *         formatting in e.g. {@link Logger#trace(Message,Throwable)}
	 */
	public static ParameterizedMessage messageOf( final String pattern,
		final Object... args )
	{
		return new ParameterizedMessage( pattern, args );
	}

	/**
	 * @param throwable
	 * @param pattern
	 * @param args
	 * @return a {@link ParameterizedMessage} for delayed parameterized
	 *         formatting in e.g. {@link Logger#trace(Message,Throwable)}
	 */
	public static ParameterizedMessage messageOf( final Throwable throwable,
		final String pattern, final Object... args )
	{
		return new ParameterizedMessage( pattern, args, throwable );
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

	public static void injectLogger( final Object encloser, final Field field )
	{
		final String postfix = encloser instanceof Identified
				? "." + ((Identified<?>) encloser).id() : "";
		Object logger = null;
		try
		{
			// Log4j2
			if( field.getType() == Logger.class )
				logger = getLogger( encloser.getClass(), encloser );
			else // SLF4J
			if( field.getType() == org.slf4j.Logger.class )
			{
				logger = LoggerFactory
						.getLogger( encloser.getClass().getName() + postfix );
			} else // java.util.logging
			if( field.getType() == java.util.logging.Logger.class )
			{
				logger = LogUtil.getJavaLogger(
						encloser.getClass().getName() + postfix );
			} else
				Thrower.throwNew( UnsupportedOperationException.class,
						"@{} only injects {}, {} or {}",
						InjectLogger.class.getSimpleName(),
						Logger.class.getName(),
						org.slf4j.Logger.class.getName(),
						java.util.logging.Logger.class.getName() );

			field.setAccessible( true );
			field.set( encloser, logger );
		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param supplier the function to decorate in {@link #toString()} calls
	 * @return a decorator {@link Object} to help delay {@link #toString()} call
	 *         until absolutely necessary (e.g. for logging at a desired level)
	 */
	public static Object wrapToString( final Supplier<String> supplier )
	{
		return new Object()
		{
			@Override
			public String toString()
			{
				return supplier.get();
			}
		};
	}
}
