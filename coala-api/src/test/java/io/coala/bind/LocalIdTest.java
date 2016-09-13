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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.eaio.uuid.UUID;

import io.coala.log.LogUtil;
import io.coala.persist.JPAUtil;

/**
 * {@link LocalIdTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class LocalIdTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( LocalIdTest.class );

	@Test
	public void testLocalId()
	{
		LOG.info( "Starting test of: {}", LocalId.class.getSimpleName() );

		final LocalId id = LocalId.of( "role", LocalId.of( "org",
				LocalId.of( "agent", LocalId.of( new UUID() ) ) ) );

		final EntityManagerFactory emf = HibHikHypConfig.createEMF();
		JPAUtil.transact( emf, em ->
		{
			id.persist( em );
			LOG.trace( "Persisted: {}", id );
		} );
		final List<LocalId> list = new ArrayList<>();
		JPAUtil.transact( emf, em ->
		{
			id.streamAll( em ).forEach( list::add );
		} );
		LOG.trace( "SELECT all: {}", list );
	}
}
