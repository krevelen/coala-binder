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

import java.util.Map;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.SharedCacheMode;

import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;
import io.coala.log.LogUtil;

/**
 * {@link JPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface JPAConfig extends GlobalConfig
{
	@Key( "javax.persistence.jdbc.driver" )
	String driver();

	@Key( "javax.persistence.jdbc.url" )
	String url();

	@Key( "javax.persistence.jdbc.user" )
	String username();

	@Key( "javax.persistence.jdbc.password" )
	String password();

	/** the SHARED_CACHE_MODE_KEY as per {@link SharedCacheMode} */
	String SHARED_CACHE_MODE_KEY = "javax.persistence.sharedCache.mode";

	/** the CACHE_RETRIEVE_MODE_KEY as per {@link CacheRetrieveMode} */
	String CACHE_RETRIEVE_MODE_KEY = "javax.persistence.cache.retrieveMode";

	String NAME_DELIMITER = ",";

	@Key( "javax.persistence.unit.names" )
	@Separator( NAME_DELIMITER )
	String[] persistenceUnitNames();

	@Key( SHARED_CACHE_MODE_KEY )
	@DefaultValue( "ENABLE_SELECTIVE" )
	SharedCacheMode sharedCacheMode();

	@Key( CACHE_RETRIEVE_MODE_KEY )
	@DefaultValue( "BYPASS" )
	CacheRetrieveMode cacheRetrieveMode();

//	@Key( "javax.persistence.provider" )
//	Class<?> provider();

	/**
	 * @param imports additional configuration
	 * @return the (expensive) {@link EntityManagerFactory}
	 */
	default EntityManagerFactory
		createEntityManagerFactory( final Map<?, ?>... imports )
	{
		return createEntityManagerFactory(
				String.join( NAME_DELIMITER, persistenceUnitNames() ),
				imports );
	}

	/**
	 * @param persistenceUnitNames the entity persistence unit name(s)
	 * @param imports additional configuration
	 * @return the (expensive) {@link EntityManagerFactory}
	 */
	default EntityManagerFactory createEntityManagerFactory(
		final String persistenceUnitNames, final Map<?, ?>... imports )
	{
		final Map<String, Object> config = ConfigUtil.export( this, imports );
		config.put( SHARED_CACHE_MODE_KEY, sharedCacheMode() ); // deser
		config.put( CACHE_RETRIEVE_MODE_KEY, cacheRetrieveMode() ); // deser
		LogUtil.getLogger( JPAConfig.class ).trace( "JPA config: {}", config );
		return Persistence.createEntityManagerFactory( persistenceUnitNames,
				config );
	}

}