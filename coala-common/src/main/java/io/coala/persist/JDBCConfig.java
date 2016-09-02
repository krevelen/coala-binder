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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import io.coala.config.GlobalConfig;

/**
 * {@link JDBCConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface JDBCConfig extends GlobalConfig
{
	@Key( "jdbc.driver" )
	String driver();

	@Key( "jdbc.url" )
	String url();

	@Key( "jdbc.username" )
	String username();

	@Key( "jdbc.password" )
	String password();

	default void execute( final String sql, final Consumer<ResultSet> consumer )
		throws SQLException
	{
		JDBCUtil.execute( url(), username(), password(), sql, consumer );
	}
}