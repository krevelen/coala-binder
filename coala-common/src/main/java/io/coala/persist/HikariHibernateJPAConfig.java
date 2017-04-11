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

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.hikaricp.internal.HikariConfigurationUtil;

/**
 * {@link HikariHibernateJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 * @see HikariConfigurationUtil
 */
public interface HikariHibernateJPAConfig extends HibernateJPAConfig
{
	@Key( AvailableSettings.CONNECTION_PROVIDER )
	@DefaultValue( "org.hibernate.hikaricp.internal.HikariCPConnectionProvider" )
	Class<? extends ConnectionProvider> hibernateConnectionProvider();
}