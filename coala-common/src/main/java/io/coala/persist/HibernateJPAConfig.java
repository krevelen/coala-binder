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

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * {@link HibernateJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface HibernateJPAConfig extends JPAConfig
{

	@Key( AvailableSettings.DATASOURCE )
	String jdbcDatasourceJNDI();

	@Key( AvailableSettings.DRIVER )
	Class<? extends Driver> jdbcDriver();

	@Key( AvailableSettings.URL )
	URI jdbcUrl();

	@Key( AvailableSettings.USER )
	String jdbcUsername();

	@Key( AvailableSettings.PASS )
	String jdbcPassword();

	@Override
	default String jdbcPasswordKey()
	{
		return AvailableSettings.PASS;
	}

//	@Key( AvailableSettings.DIALECT ) // is resolved per JDBC driver or provider
//	Class<?> hibernateDialect();

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
	@Key( AvailableSettings.HBM2DDL_AUTO )
	@DefaultValue( "update" )
	HibernateSchemaPolicy hibernateSchemaPolicy();

	@Key( AvailableSettings.DEFAULT_SCHEMA )
	String hibernateDefaultSchema();

	@Key( AvailableSettings.SHOW_SQL )
	boolean hibernateShowSQL();

	@Key( AvailableSettings.USE_SQL_COMMENTS )
	boolean hibernateUseSQLComments();

	@Key( AvailableSettings.FORMAT_SQL )
	boolean hibernateFormatSQL();

	@Key( AvailableSettings.CONNECTION_PROVIDER )
//	@DefaultValue( "org.hibernate.connection.C3P0ConnectionProvider" )
//	@DefaultValue( "org.hibernate.hikaricp.internal.HikariCPConnectionProvider" )
	Class<? extends ConnectionProvider> hibernateConnectionProvider();

}