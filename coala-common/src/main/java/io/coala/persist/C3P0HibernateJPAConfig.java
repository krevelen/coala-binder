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

/**
 * {@link C3P0HibernateJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface C3P0HibernateJPAConfig extends HibernateJPAConfig
{

	String HIBERNATE_C3P0_MIN_SIZE_KEY = "hibernate.c3p0.min_size";

	String HIBERNATE_C3P0_MAX_SIZE_KEY = "hibernate.c3p0.max_size";

	String HIBERNATE_C3P0_TIMEOUT_KEY = "hibernate.c3p0.timeout";

	String HIBERNATE_C3P0_MAX_STATEMENTS_KEY = "hibernate.c3p0.max_statements";

	String HIBERNATE_C3P0_IDLE_TEST_PERIOD_KEY = "hibernate.c3p0.idle_test_period";

	@Key( HIBERNATE_CONNECTION_PROVIDER_KEY )
	@DefaultValue( "org.hibernate.connection.C3P0ConnectionProvider" )
	Class<? // extends org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
			> hibernateConnectionProviderClass();

	@Key( HIBERNATE_C3P0_MIN_SIZE_KEY )
	@DefaultValue( "" + 5 )
	int hibernateConnectionPoolMinimumSize();

	@Key( HIBERNATE_C3P0_MAX_SIZE_KEY )
	@DefaultValue( "" + 20 )
	int hibernateConnectionPoolMaximumSize();

	@Key( HIBERNATE_C3P0_TIMEOUT_KEY )
	@DefaultValue( "" + 500 )
	int hibernateConnectionPoolTimeoutMillis();

	@Key( HIBERNATE_C3P0_MAX_STATEMENTS_KEY )
	@DefaultValue( "" + 50 )
	int hibernateConnectionPoolMaximumStatements();

	@Key( HIBERNATE_C3P0_IDLE_TEST_PERIOD_KEY )
	@DefaultValue( "" + 2000 )
	int hibernateConnectionPoolIdleTestPeriodMillis();

}