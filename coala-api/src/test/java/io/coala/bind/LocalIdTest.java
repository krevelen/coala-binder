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

import io.coala.json.JsonUtil;
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

	@Test //( expected = PersistenceException.class )
	public void testLocalId()
	{
		LOG.info( "Starting test of: {}", LocalId.class.getSimpleName() );

		final LocalId id1 = LocalId.of( "role", LocalId.of( "org",
				LocalId.of( "agent", LocalId.of( new UUID() ) ) ) );

		final LocalId id2 = LocalId.of( "role", LocalId.of( "org",
				LocalId.of( "agent", LocalId.of( new UUID() ) ) ) );

		final LocalId id3 = LocalId.of( "role", LocalId.of( "org",
				LocalId.of( "", LocalId.of( new UUID() ) ) ) );

		final EntityManagerFactory emf = HibHikHypConfig.createEMF();
		JPAUtil.session( emf, em ->
		{
			id1.persist( em );
			LOG.trace( "a Persisted: {}", id1 );
		} );
		JPAUtil.session( emf, em ->
		{
			id1.persist( em );
			LOG.trace( "b Persisted: {}", id1 );
		} );
		JPAUtil.session( emf, em ->
		{
			id2.persist( em );
			LOG.trace( "c Persisted: {}", id2 );
		} );
		JPAUtil.session( emf, em ->
		{
			id1.persist( em );
			LOG.trace( "d Persisted: {}", id1 );
			id2.persist( em );
			LOG.trace( "e Persisted: {}", id2 );
			id3.persist( em );
			LOG.trace( "f Persisted: {}", id3 );
		} );
		final List<LocalId> list = new ArrayList<>();
		JPAUtil.session( emf,
				em -> id1.findAll( em, null ).forEach( list::add ) );
		LOG.trace( "SELECT all: {}", list );
		list.forEach( id -> LOG.trace( "stringify {} -> {} -> {}", id,
				JsonUtil.stringify( id ),
				JsonUtil.valueOf( JsonUtil.stringify( id ), LocalId.class ) ) );
	}
}
