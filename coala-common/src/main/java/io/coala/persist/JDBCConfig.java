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
package io.coala.persist;

import static org.aeonbits.owner.util.Collections.entry;
import static org.aeonbits.owner.util.Collections.map;

import java.lang.reflect.Method;
import java.net.URI;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Converter;

import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;

/**
 * {@link JDBCConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Sources( "classpath:jdbc.properties" )
public interface JDBCConfig extends GlobalConfig
{
	/** no specification */
	String DATASOURCE_KEY = "datasource";

	/** no specification */
	String DATASOURCE_DRIVER_KEY = "datasource.driver";

	/** no specification */
	String JDBC_DRIVER_KEY = "jdbc.driver";

	/** no specification */
	String JDBC_URL_KEY = "jdbc.url";

	/** no specification */
	String JDBC_USERNAME_KEY = "jdbc.username";

	/** no specification */
	String JDBC_PASSWORD_KEY = "jdbc.password";

	/** no specification */
	String JDBC_DATABASE_KEY = "jdbc.database";

	/** no specification */
	String JDBC_SERVER_KEY = "jdbc.server";

	/** @see javax.sql.DataSource */
	@Key( DATASOURCE_KEY )
	String jdbcDatasourceJNDI();

	/**
	 * https://github.com/brettwooldridge/HikariCP#popular-datasource-class-names
	 * 
	 * @see javax.sql.DataSource
	 */
	@Key( DATASOURCE_DRIVER_KEY )
	Class<? extends DataSource> jdbcDatasourceDriver();

	/** @see java.sql.DriverManager */
	@Key( JDBC_DRIVER_KEY )
	Class<? extends Driver> jdbcDriver();

	/** may be used as part of the {@link #jdbcUrl()} */
	@Key( JDBC_DATABASE_KEY )
	String jdbcDatabase();

	/** may be used as part of the {@link #jdbcUrl()} */
	@Key( JDBC_SERVER_KEY )
	String jdbcServer();

	/** @see java.sql.DriverManager */
	@Key( JDBC_URL_KEY )
	URI jdbcUrl();

	/** @see java.sql.DriverManager */
	@Key( JDBC_USERNAME_KEY )
	String jdbcUsername();

//	@DefaultValue( PASSWORD_PROMPT_VALUE )
	@Key( JDBC_PASSWORD_KEY )
	@ConverterClass( PasswordPromptConverter.class )
	String jdbcPassword();

	/**
	 * @return hides password value
	 */
	@SuppressWarnings( "unchecked" )
	default Map<String, Object> export( final Map<?, ?>... imports )
	{
		return ConfigUtil.export( this, ConfigUtil.join(
				map( entry( jdbcPasswordKey(), "<hidden>" ) ), imports ) );
	}

	/**
	 * @param sql the SQL statement
	 * @param consumer a {@link ResultSet} handler
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	default void execute( final String sql, final Consumer<ResultSet> consumer )
		throws SQLException, ClassNotFoundException
	{
		jdbcDriver();
		JDBCUtil.execute( jdbcUrl(), jdbcUsername(), jdbcPassword(), sql,
				consumer );
	}

	/**
	 * @return name used in {@link Key @Key} annotation of effective
	 *         {@link #jdbcPassword()} override, needed to mask password export
	 */
	default String jdbcPasswordKey()
	{
		return JDBC_PASSWORD_KEY;
	}

	String PASSWORD_PROMPT_VALUE = "prompt";

	/**
	 * {@link PasswordPromptConverter}
	 */
	class PasswordPromptConverter implements Converter<String>
	{
		@Override
		public String convert( final Method method, final String input )
		{
			if( input != null
					&& !input.toLowerCase().contains( PASSWORD_PROMPT_VALUE ) )
				return input;

			final String message = "Enter password (now: " + input + ")";
			if( System.console() != null ) // terminal console
				return new String(
						System.console().readPassword( "%s> ", message ) );

			// inside IDE console
			final JPasswordField pf = new JPasswordField();
			return JOptionPane.showConfirmDialog( null, pf, message,
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE ) == JOptionPane.OK_OPTION
							? new String( pf.getPassword() ) : input;
		}
	}

	static void exec( final String sql, final Consumer<ResultSet> consumer )
		throws ClassNotFoundException, SQLException
	{
		getOrCreate().execute( sql, consumer );
	}

	static JDBCConfig getOrCreate( final Map<?, ?>... imports )
	{
		return ConfigCache.getOrCreate( JDBCConfig.class, imports );
	}
}