/* $Id: ea08c5561c228e73b61d6868506fcd5150e897c0 $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/log/LogUtil.java $
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
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;

import io.coala.util.FileUtil;
import io.coala.util.Util;

/**
 * {@link LogUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class LogUtil implements Util
{

	/** */
//	private static final String CONFIG_PROPERTY_KEY = "log4j.configuration";

	/** */
//	private static final String CONFIG_PROPERTY_DEFAULT = "log4j.properties";

	/** */
	private static final String LOCALE_PROPERTY_KEY = "locale";

	/** */
	private static final String LOCALE_PROPERTY_DEFAULT = "en";

	static
	{
		// redirect JUL to Log4J2
		System.setProperty( "java.util.logging.manager",
				org.apache.logging.log4j.jul.LogManager.class.getName() );

		// FIXME allow override from COALA config
		Locale.setDefault( Locale.forLanguageTag( System.getProperty(
				LOCALE_PROPERTY_KEY, LOCALE_PROPERTY_DEFAULT ) ) );
/*
 * // divert java.util.logging.Logger LogRecords to SLF4J
 * SLF4JBridgeHandler.removeHandlersForRootLogger();
 * SLF4JBridgeHandler.install();
 * 
 * // load log4j properties using #getFile() from file system or class path //
 * PropertyConfigurator.configure(loadProperties(System.getProperty( //
 * CONFIG_PROPERTY_KEY, CONFIG_PROPERTY_DEFAULT)));
 * 
 * LogManager .setRepositorySelector( new DefaultRepositorySelector( new
 * CoalaLog4jHierarchy( new RootLogger( Level.INFO ) ) ), LogUtil.class );
 * 
 * // FIXME allow override from COALA config new
 * CoalaLog4jPropertyConfigurator().doConfigure( loadProperties(
 * System.getProperty( CONFIG_PROPERTY_KEY, CONFIG_PROPERTY_DEFAULT ) ),
 * LogManager.getLoggerRepository() );
 */ }

	/**
	 * @param file the properties file location in the classpath
	 * @return the properties loaded from specified location
	 */
	public static Properties loadProperties( final String file )
	{

		final Properties result = new Properties();

		InputStream is = null;
		try
		{
			is = FileUtil.toInputStream( file );
			result.load( is );
		} catch( final Exception e )
		{
			System.err
					.println( "Problem loading properties from file: " + file );
			e.printStackTrace();
		} finally
		{
			if( is != null ) try
			{
				is.close();
			} catch( final IOException e )
			{
				System.err
						.println( "Problem closing properties file: " + file );
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * @param clazz the object type generating the log messages
	 * @return the {@link org.apache.logging.log4j.Logger} instance for
	 *         specified {@code clazz}
	 */
	public static org.apache.logging.log4j.Logger
		getLogger( final Class<?> clz )
	{
		return getLogger( clz, clz );
	}

	/**
	 * @param clazz the object type generating the log messages
	 * @return the {@link org.apache.logging.log4j.Logger} instance for
	 *         specified {@code clazz}
	 */
	public static org.apache.logging.log4j.Logger getLogger( final Class<?> clz,
		final Object source )
	{
		return getLogger( clz.getName(), source );
	}

	/**
	 * @param clazz the object type generating the log messages
	 * @return the {@link org.apache.logging.log4j.Logger} instance for
	 *         specified {@code FQCN}
	 */
	public static org.apache.logging.log4j.Logger getLogger( final String fqcn,
		final Object source )
	{
		if( source == null )
		{
			new NullPointerException( "Omitting logger for null object" )
					.printStackTrace();
			return getLogger( fqcn );
		}

		return LogManager//((CoalaLog4jHierarchy) LogManager.getLoggerRepository())
				.getLogger( fqcn );
		/* TODO add time/id from source to message */
	}

	/**
	 * this method is preferred over
	 * {@link org.apache.logging.log4j.Logger#getLogger} so as to initialize the
	 * Log4j2 system correctly via this {@link LogUtil} class
	 * 
	 * @param name
	 * @return
	 */
	public static org.apache.logging.log4j.Logger getLogger( final String name )
	{
		return LogManager//((CoalaLog4jHierarchy) LogManager.getLoggerRepository())
				.getLogger( name );
	}

	public ParameterizedMessage messageOf( final String pattern,
		final Object arg )
	{
		return new ParameterizedMessage( pattern, arg );
	}

	public ParameterizedMessage messageOf( final String pattern,
		final Object... arg )
	{
		return new ParameterizedMessage( pattern, arg );
	}

	public ParameterizedMessage messageOf( final String pattern,
		final Object arg1, Object arg2 )
	{
		return new ParameterizedMessage( pattern, arg1, arg2 );
	}

	public ParameterizedMessage messageOf( final Throwable throwable,
		final String pattern, final Object... args )
	{
		return new ParameterizedMessage( pattern, args, throwable );
	}

//	public static org.apache.logging.log4j.Logger getLogger2(final Class<?> type)
//	{
//		return org.apache.logging.log4j.LogManager.getLogger(type);
//	}

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

	public static Message toMessage( final String messagePattern,
		final Object... parameters )
	{
		return new ParameterizedMessage( messagePattern, parameters );
	}
}
