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
package io.coala.persist;

import java.net.URI;
import java.sql.Driver;

/**
 * {@link HibernateJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface HibernateJPAConfig extends JPAConfig
{

	String HIBERNATE_SCHEMA_POLICY_KEY = "hibernate.hbm2ddl.auto";

	String HIBERNATE_DEFAULT_SCHEMA_KEY = "hibernate.default_schema";

	String HIBERNATE_SQL_SHOW_KEY = "hibernate.show_sql";

	String HIBERNATE_SQL_USE_COMMENTS_KEY = "hibernate.use_sql_comments";

	String HIBERNATE_SQL_FORMAT_KEY = "hibernate.format_sql";

	String HIBERNATE_CONNECTION_PROVIDER_KEY = "hibernate.connection.provider_class";

	String HIBERNATE_CONNECTION_DRIVER_KEY = "hibernate.connection.driver_class";

	String HIBERNATE_CONNECTION_ISOLATION_KEY = "hibernate.connection.isolation";

	String HIBERNATE_CONNECTION_AUTOCOMMIT_KEY = "hibernate.connection.autocommit";

	String HIBERNATE_CONNECTION_URL_KEY = "hibernate.connection.url";

	String HIBERNATE_CONNECTION_USERNAME_KEY = "hibernate.connection.username";

	String HIBERNATE_CONNECTION_PASSWORD_KEY = "hibernate.connection.password";

	String HIBERNATE_DATASOURCE_CLASS_KEY = "hibernate.hikari.dataSourceClassName";

	String HIBERNATE_DATASOURCE_URL_KEY = "hibernate.hikari.dataSource.url";

	String HIBERNATE_DATASOURCE_USERNAME_KEY = "hibernate.hikari.dataSource.user";

	String HIBERNATE_DATASOURCE_PASSWORD_KEY = "hibernate.hikari.dataSource.password";

	String HIBERNATE_DIALECT_KEY = "hibernate.dialect";

//	@Key("hibernate.search.default.directory_provider")
//	@DefaultValue("ram")
//	String hibernateSearchDirectoryProvider();

	// ignored by EMF, only checked in persistence.xml or static JPA resolution
//	@Key( JPA_PROVIDER_KEY )
//	@DefaultValue( "org.hibernate.jpa.HibernatePersistenceProvider" )
//	Class<? extends PersistenceProvider> jpaProvider();

	@Key( HIBERNATE_CONNECTION_DRIVER_KEY )
	Class<? extends Driver> jdbcDriver();

	@Key( HIBERNATE_CONNECTION_URL_KEY )
	URI jdbcUrl();

	@Key( HIBERNATE_CONNECTION_USERNAME_KEY )
	String jdbcUsername();

	@Key( HIBERNATE_CONNECTION_PASSWORD_KEY )
	String jdbcPassword();
	
	@Override
	default String jdbcPasswordKey()
	{
		return HIBERNATE_CONNECTION_PASSWORD_KEY;
	}

//	@Key( HIBERNATE_DIALECT_KEY ) // FIXME determined by JDBC driver or provider
//		Class<?> // extends org.hibernate.dialect.Dialect
//		hibernateDialect();

	/**
	 * Policy for database schema validation or export upon SessionFactory
	 * creation, see
	 * https://docs.jboss.org/hibernate/orm/5.0/manual/en-US/html/ch03.html
	 * <dl>
	 * <dt>{@link HibernateSchemaPolicy#none none}</dt>
	 * <dd></dd>
	 * <dt>{@link HibernateSchemaPolicy#validate validate}</dt>
	 * <dd></dd>
	 * <dt>{@link HibernateSchemaPolicy#create create}</dt>
	 * <dd></dd>
	 * <dt>{@link HibernateSchemaPolicy#create_drop create-drop}</dt>
	 * <dd></dd>
	 * <dt>{@link HibernateSchemaPolicy#update update}</dt>
	 * <dd>DON'T USE {@code update} in production, see
	 * http://stackoverflow.com/a/221422</dd>
	 * </dl>
	 */
	@Key( HIBERNATE_SCHEMA_POLICY_KEY )
	@DefaultValue( "update" )
	HibernateSchemaPolicy hibernateSchemaPolicy();

	@Key( HIBERNATE_DEFAULT_SCHEMA_KEY )
//	@DefaultValue( "PUBLIC" )
	String hibernateDefaultSchema();

	@Key( HIBERNATE_SQL_SHOW_KEY )
	@DefaultValue( "" + false )
	boolean hibernateShowSQL();

	@Key( HIBERNATE_SQL_USE_COMMENTS_KEY )
	@DefaultValue( "" + false )
	boolean hibernateUseSQLComments();

	@Key( HIBERNATE_SQL_FORMAT_KEY )
	@DefaultValue( "" + false )
	boolean hibernateFormatSQL();

	@Key( HIBERNATE_CONNECTION_PROVIDER_KEY )
//	@DefaultValue( "org.hibernate.connection.C3P0ConnectionProvider" )
		Class<?> // extends org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
		hibernateConnectionProviderClass();

}