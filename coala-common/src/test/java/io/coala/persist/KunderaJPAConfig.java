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

import com.impetus.kundera.Constants;

/**
 * {@link KunderaJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface KunderaJPAConfig extends HibernateJPAConfig
{

	@Key( "javax.persistence.provider" )
	@DefaultValue( "com.impetus.kundera.KunderaPersistence" )
	Class<?> provider();

	@Key( "kundera.client.lookup.class" )
	@DefaultValue( Constants.RDBMS_CLIENT_FACTORY )
	// TODO add defaults for Neo4J, Redis, Spark, Mongo, etc.
	String kunderaClientLookupClass();

}