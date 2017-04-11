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
import java.util.Map;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.SharedCacheMode;
import javax.persistence.spi.PersistenceUnitInfo;

import org.aeonbits.owner.Config.Sources;

import io.coala.config.GlobalConfig;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;

/**
 * {@link JPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Sources( "classpath:jpa.properties" )
public interface JPAConfig extends GlobalConfig, JDBCConfig
{
	String NAME_DELIMITER = ",";

	/** @see PersistenceUnitInfo#getPersistenceUnitName() */
	String JPA_UNIT_NAMES_KEY = "javax.persistence.unit-names";

	/**
	 * @deprecated not a persistence property (anymore)
	 * @see Persistence#PERSISTENCE_PROVIDER
	 * @see PersistenceUnitInfo#getPersistenceProviderClassName()
	 */
	String JPA_PROVIDER_KEY = Persistence.PERSISTENCE_PROVIDER;

	/**
	 * not a persistence property
	 * 
	 * @see javax.sql.DataSource
	 */
	String JPA_JTA_DATASOURCE_KEY = "javax.persistence.jta-data-source";

	/**
	 * not a persistence property
	 * 
	 * @see javax.sql.DataSource
	 */
	String JPA_NON_JTA_DATASOURCE_KEY = "javax.persistence.non-jta-data-source";

	/** @see java.sql.DriverManager */
	String JPA_JDBC_DRIVER_KEY = "javax.persistence.jdbc.driver";

	/** @see java.sql.DriverManager */
	String JPA_JDBC_URL_KEY = "javax.persistence.jdbc.url";

	/** @see java.sql.DriverManager */
	String JPA_JDBC_USER_KEY = "javax.persistence.jdbc.user";

	/** @see java.sql.DriverManager */
	String JPA_JDBC_PASSWORD_KEY = "javax.persistence.jdbc.password";

	/**
	 * the SHARED_CACHE_MODE_KEY as per {@link SharedCacheMode}
	 * 
	 * @see PersistenceUnitInfo#getSharedCacheMode()
	 */
	String JPA_SHARED_CACHE_MODE_KEY = "javax.persistence.sharedCache.mode";

	/** the CACHE_RETRIEVE_MODE_KEY as per {@link CacheRetrieveMode} */
	String JPA_CACHE_RETRIEVE_MODE_KEY = "javax.persistence.cache.retrieveMode";

	/**
	 * see also http://stackoverflow.com/a/3218191/1418999
	 * 
	 * @see PersistenceUnitInfo#getNonJtaDataSource()
	 */
	@Key( JPA_NON_JTA_DATASOURCE_KEY )
	String jdbcDatasourceNonJtaJNDI();

	/**
	 * see also http://stackoverflow.com/a/3218191/1418999
	 * 
	 * @see PersistenceUnitInfo#getJtaDataSource()
	 */
	@Key( JPA_JTA_DATASOURCE_KEY )
	String jdbcDatasourceJNDI();

	@Key( JPA_JDBC_DRIVER_KEY )
	Class<? extends Driver> jdbcDriver();

	@Key( JPA_JDBC_URL_KEY )
	URI jdbcUrl();

	@Key( JPA_JDBC_USER_KEY )
	String jdbcUsername();

	@Key( JPA_JDBC_PASSWORD_KEY )
	String jdbcPassword();

	@Override
	default String jdbcPasswordKey()
	{
		return JPA_JDBC_PASSWORD_KEY;
	}

	/** @see PersistenceUnitInfo#getPersistenceUnitName() */
	@Key( JPA_UNIT_NAMES_KEY )
	@Separator( NAME_DELIMITER )
	String[] jpaUnitNames();

	/**
	 * @see SharedCacheMode#ENABLE_SELECTIVE
	 * @see PersistenceUnitInfo#getSharedCacheMode()
	 */
	@Key( JPA_SHARED_CACHE_MODE_KEY )
	@DefaultValue( "ENABLE_SELECTIVE" )
	SharedCacheMode jpaSharedCacheMode();

	/**
	 * <dl>
	 * <dt>{@link CacheRetrieveMode#USE USE}</dt>
	 * <dd>Read entity data from the cache</dd>
	 * <dt>{@link CacheRetrieveMode#BYPASS BYPASS}</dt>
	 * <dd>Bypass the cache: get data directly from the database</dd>
	 * <dt>{@code REFRESH}</dt>
	 * <dd>update from query?</dd>
	 * </dl>
	 */
	@Key( JPA_CACHE_RETRIEVE_MODE_KEY )
	@DefaultValue( "USE" ) // 
	CacheRetrieveMode jpaCacheRetrieveMode();

	/**
	 * @param imports additional configuration
	 * @return the (expensive) {@link EntityManagerFactory}
	 */
	default EntityManagerFactory createEMF( final Map<?, ?>... imports )
	{
		return createEMF( String.join( NAME_DELIMITER, jpaUnitNames() ),
				imports );
	}

	/**
	 * @param jpaUnitNames the entity persistence unit name(s)
	 * @param imports additional configuration
	 * @return the (expensive) {@link EntityManagerFactory}
	 */
	default EntityManagerFactory createEMF( final String jpaUnitNames,
		final Map<?, ?>... imports )
	{
		final Map<String, Object> config = export( imports );
		config.put( JPA_SHARED_CACHE_MODE_KEY, jpaSharedCacheMode() ); // deser
		config.put( JPA_CACHE_RETRIEVE_MODE_KEY, jpaCacheRetrieveMode() ); // deser
		LogUtil.getLogger( JPAConfig.class ).trace(
				"Creating persistence units {} with config: {}", jpaUnitNames,
				JsonUtil.toJSON( config ) );
		config.put( jdbcPasswordKey(), jdbcPassword() ); // prompt
		return Persistence.createEntityManagerFactory( jpaUnitNames, config );
	}

}