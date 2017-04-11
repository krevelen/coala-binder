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

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * {@link C3P0HibernateJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface C3P0HibernateJPAConfig extends HibernateJPAConfig
{

	@Key( AvailableSettings.CONNECTION_PROVIDER )
	@DefaultValue( "org.hibernate.connection.C3P0ConnectionProvider" )
	Class<? extends ConnectionProvider> hibernateConnectionProvider();

	@Key( AvailableSettings.C3P0_MIN_SIZE )
	@DefaultValue( "" + 5 )
	int hibernateConnectionPoolMinimumSize();

	@Key( AvailableSettings.C3P0_MAX_SIZE )
	@DefaultValue( "" + 20 )
	int hibernateConnectionPoolMaximumSize();

	@Key( AvailableSettings.C3P0_TIMEOUT )
	@DefaultValue( "" + 500 )
	int hibernateConnectionPoolTimeoutMillis();

	@Key( AvailableSettings.C3P0_MAX_STATEMENTS )
	@DefaultValue( "" + 50 )
	int hibernateConnectionPoolMaximumStatements();

	@Key( AvailableSettings.C3P0_IDLE_TEST_PERIOD )
	@DefaultValue( "" + 2000 )
	int hibernateConnectionPoolIdleTestPeriodMillis();

}