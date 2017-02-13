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
package io.coala.bind;

import java.net.URI;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.aeonbits.owner.ConfigCache;

import io.coala.persist.HibernateJPAConfig;

/**
 * {@link HibHikHypConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
interface HibHikHypConfig extends HibernateJPAConfig
{
	String DATASOURCE_CLASS_KEY = "hibernate.hikari.dataSourceClassName";

	String DATASOURCE_URL_KEY = "hibernate.hikari.dataSource.url";

	String DATASOURCE_USERNAME_KEY = "hibernate.hikari.dataSource.user";

	String DATASOURCE_PASSWORD_KEY = "hibernate.hikari.dataSource.password";

	@DefaultValue( "hibernate_test_pu" )
	String[] persistenceUnitNames();

	@Key( SCHEMA_POLICY_KEY )
	@DefaultValue( "create-drop" )
	SchemaPolicy hibernateSchemaPolicy();

	@Key( DEFAULT_SCHEMA_KEY )
	@DefaultValue( "PUBLIC" )
	String hibernateDefaultSchema();

	@Key( CONNECTION_PROVIDER_CLASS_KEY )
	@DefaultValue( "org.hibernate.hikaricp.internal.HikariCPConnectionProvider" )
	String hibernateConnectionProviderClass();

	// see https://github.com/brettwooldridge/HikariCP/wiki/Configuration#popular-datasource-class-names
	@Key( DATASOURCE_CLASS_KEY )
	@DefaultValue( "org.hsqldb.jdbc.JDBCDataSource" )
	String hikariDataSourceClass();

	@Key( DATASOURCE_USERNAME_KEY )
	@DefaultValue( "sa" )
	String username();

	@Key( DATASOURCE_PASSWORD_KEY )
	@DefaultValue( "" )
	String password();

	@Key( DATASOURCE_URL_KEY )
	@DefaultValue( "jdbc:hsqldb:file:target/testdb2" )
//	@DefaultValue( "jdbc:hsqldb:mem:mymemdb" )
//	@DefaultValue( "jdbc:mysql://localhost/testdb" )
	URI url();

	/**
	 * @param imports additional {@link EntityManagerFactory} configuration
	 * @return the (expensive) {@link EntityManagerFactory}
	 */
	static EntityManagerFactory createEMF( final Map<?, ?>... imports )
	{
		return ConfigCache.getOrCreate( HibHikHypConfig.class, imports )
				.createEntityManagerFactory();
	}
}