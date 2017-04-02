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

import java.net.URI;

import javax.sql.DataSource;

/**
 * {@link HikariHibernateJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface HikariHibernateJPAConfig extends HibernateJPAConfig
{

	String HIKARI_DATASOURCE_DRIVER_KEY = "hibernate.hikari.dataSourceClassName";

	String HIKARI_DATASOURCE_URL_KEY = "hibernate.hikari.jdbcUrl";

	String HIKARI_DATASOURCE_USERNAME_KEY = "hibernate.hikari.dataSource.user"; // TODO check
	String HIKARI_DATASOURCE_PASSWORD_KEY = "hibernate.hikari.dataSource.password"; // TODO check
	String HIKARI_DATASOURCE_DATABASE_KEY = "hibernate.hikari.dataSource.databaseName"; // TODO check
	String HIKARI_DATASOURCE_SERVER_KEY = "hibernate.hikari.dataSource.serverName"; // TODO check

	@Key( HIBERNATE_CONNECTION_PROVIDER_KEY )
	@DefaultValue( "org.hibernate.hikaricp.internal.HikariCPConnectionProvider" )
		Class<?> // extends org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
		hibernateConnectionProviderClass();

	/**
	 * see
	 * https://github.com/brettwooldridge/HikariCP#popular-datasource-class-names
	 */
	@Key( HIKARI_DATASOURCE_DRIVER_KEY )
//	@DefaultValue( "org.hsqldb.jdbc.JDBCDataSource" )
	Class<? extends DataSource> hikariDataSourceDriver();

	@Key( HIKARI_DATASOURCE_URL_KEY )
//	@DefaultValue( "jdbc:hsqldb:mem:mymemdb" )
	URI jdbcUrl();

	@Key( HIKARI_DATASOURCE_USERNAME_KEY )
//	@DefaultValue( "sa" )
	String jdbcUsername();

	@Key( HIKARI_DATASOURCE_PASSWORD_KEY )
//	@DefaultValue( "" )
	String jdbcPassword();

	@Key( HIKARI_DATASOURCE_DATABASE_KEY )
	String jdbcDatabase();

	@Key( HIKARI_DATASOURCE_SERVER_KEY )
	String jdbcServer();

}