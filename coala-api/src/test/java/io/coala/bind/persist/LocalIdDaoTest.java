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
package io.coala.bind.persist;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.eaio.uuid.UUID;

import io.coala.bind.LocalId;
import io.coala.log.LogUtil;
import io.coala.persist.JPAUtil;

/**
 * {@link LocalIdDaoTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class LocalIdDaoTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( LocalIdDaoTest.class );

	private static EntityManagerFactory emf = null;

	@BeforeClass
	public static void createEMF()
	{
		emf = HibHikHypConfig.createEMF();
	}

	@AfterClass
	public static void closeEMF()
	{
		if( emf != null ) emf.close();
	}

	@Test( expected = RollbackException.class )
	public void testSyncConstraint1()
	{
		final UUID contextRef = new UUID();
		final LocalId id1 = LocalId.of( "role", LocalId.of( "org",
				LocalId.of( "agent", LocalId.of( contextRef ) ) ) );
		try
		{
			JPAUtil.session( emf, em ->
			{
				id1.persist( em );
				final List<LocalIdDao> list = LocalIdDao.findAllSync( em,
						contextRef );
				LOG.info( "SELECT all: {}",
						list.stream().map( dao -> dao.restore( null ) )
								.collect( Collectors.toList() ) );
			} );
		} catch( final Exception e )
		{
			fail( e.getMessage() );
		}

		JPAUtil.session( emf, em ->
		{
			LocalId.of( "role",
					LocalId.of( "org",
							LocalId.of( "agent", LocalId.of( contextRef ) ) ) )
					.persist( em );
			fail( "Should have thrown constraint violation" );
		} );
	}

	@Test( expected = RollbackException.class )
	public void testAsyncConstraint2()
	{
		final UUID contextRef = new UUID();
		final LocalId id3 = LocalId.of( "role", LocalId.of( "org",
				LocalId.of( "", LocalId.of( contextRef ) ) ) );
		JPAUtil.session( emf, em ->
		{
			id3.persist( em );
			LocalIdDao.findAllAsync( em, contextRef, 2 ).subscribe(
					dao -> LOG.info( "Async retrieved: {}",
							LogUtil.toCachedString(
									() -> dao.restore( null ) ) ),
					e -> fail( e.getMessage() ) );
		} );

		JPAUtil.session( emf, em ->
		{
			LocalId.of( "role",
					LocalId.of( "org",
							LocalId.of( "", LocalId.of( contextRef ) ) ) )
					.persist( em );
			fail( "Should have thrown constraint violation" );
		} );
	}
}