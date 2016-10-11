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

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@link HibernateJPAConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface HibernateJPAConfig extends JPAConfig
{

//	@DefaultValue( "org.hibernate.ejb.HibernatePersistence" )
//	Class<?> provider();

//	@Key( "hibernate.dialect" )
//	@DefaultValue( "org.hibernate.dialect.HSQLDialect" )
//	// TODO add defaults for MySQL, Oracle, PostgreSQL, etc.
//	String hibernateDialect();

	/**
	 * {@link SchemaPolicy} NOTE: don't use "update" in production, see
	 * http://stackoverflow.com/a/221422
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	enum SchemaPolicy
	{

		/** */
		create,

		/** */
		create_drop( "create-drop" ),

		/** */
		update,

		/** */
		none,

		/** */
		validate;

		private final String value;

		private SchemaPolicy()
		{
			this( null );
		}

		private SchemaPolicy( final String value )
		{
			this.value = value;
		}

		@Override
		@JsonValue
		public String toString()
		{
			return this.value == null ? name() : this.value;
		}
	}

	String SCHEMA_POLICY_KEY = "hibernate.hbm2ddl.auto";

	String DEFAULT_SCHEMA_KEY = "hibernate.default_schema";

	String SHOW_SQL_KEY = "hibernate.show_sql";
	
	String USE_SQL_COMMENTS_KEY = "hibernate.use_sql_comments";

	String FORMAT_SQL_KEY = "hibernate.format_sql";

	String CONNECTION_PROVIDER_CLASS_KEY = "hibernate.connection.provider_class";

	@Key( SCHEMA_POLICY_KEY )
	@DefaultValue( "update" )
	SchemaPolicy hibernateSchemaPolicy();

	@Key( DEFAULT_SCHEMA_KEY )
//	@DefaultValue( "MY_SCHEMA" )
	String hibernateDefaultSchema();

	@Key( SHOW_SQL_KEY )
	@DefaultValue( "false" )
	boolean hibernateShowSQL();

	@Key( USE_SQL_COMMENTS_KEY )
	@DefaultValue( "false" )
	boolean hibernateUseSQLComments();

	@Key( FORMAT_SQL_KEY )
	@DefaultValue( "false" )
	boolean hibernateFormatSQL();

	@Key( CONNECTION_PROVIDER_CLASS_KEY )
	@DefaultValue( "org.hibernate.connection.C3P0ConnectionProvider" )
	String hibernateConnectionProviderClass();

	@Key( "hibernate.c3p0.min_size" )
	@DefaultValue( "5" )
	int hibernateConnectionPoolMinimumSize();

	@Key( "hibernate.c3p0.max_size" )
	@DefaultValue( "20" )
	int hibernateConnectionPoolMaximumSize();

	@Key( "hibernate.c3p0.timeout" )
	@DefaultValue( "500" )
	int hibernateConnectionPoolTimeoutMillis();

	@Key( "hibernate.c3p0.max_statements" )
	@DefaultValue( "50" )
	int hibernateConnectionPoolMaximumStatements();

	@Key( "hibernate.c3p0.idle_test_period" )
	@DefaultValue( "2000" )
	int hibernateConnectionPoolIdleTestPeriodMillis();

}