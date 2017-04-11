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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

import java.net.URI;

import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.Logger;
import org.hibernate.cfg.AvailableSettings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.eaio.uuid.UUID;

import io.coala.bind.LocalId;
import io.coala.log.LogUtil;
import io.coala.persist.HikariHibernateJPAConfig;
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

	public interface MyJPAConfig extends HikariHibernateJPAConfig
	{
		@DefaultValue( "localid_test_pu" ) // match persistence.xml
		@Key( JPA_UNIT_NAMES_KEY )
		String[] jpaUnitNames();

//		@DefaultValue( "jdbc:mysql://localhost/testdb" )
//		@DefaultValue( "jdbc:hsqldb:mem:mymemdb" )
//		@DefaultValue( "jdbc:neo4j:bolt://192.168.99.100:7687/db/data" )
		@DefaultValue( "jdbc:hsqldb:file:target/testdb" )
		@Key( AvailableSettings.URL )
		URI jdbcUrl();
	}

	@BeforeClass
	public static void createEMF()
	{
		emf = ConfigFactory.create( MyJPAConfig.class ).createEMF();
	}

	@AfterClass
	public static void closeEMF()
	{
		if( emf != null ) emf.close();
	}

	@Test( expected = RollbackException.class )
	public void testSyncConstraint1()
	{
		final String role = "role", org = "org", agent = "agent";
		final UUID contextRef = new UUID();
		final LocalId id1 = LocalId.of( role, LocalId.of( org,
				LocalId.of( agent, LocalId.of( contextRef ) ) ) );
		final LocalId id2 = LocalId.of( role, LocalId.of( org,
				LocalId.of( agent, LocalId.of( new UUID() ) ) ) );
		try
		{
			JPAUtil.session( emf, em ->
			{
				id1.persist( em );
				id2.persist( em );
				LocalId.findAllSync( em, contextRef ).forEach( id ->
				{
					LOG.info( "Sync retrieved: {}", id );
					assertThat( "contextRef match", id.contextRef(),
							equalTo( contextRef ) );
				} );
			} );
		} catch( final Exception e )
		{
			fail( e.getMessage() );
		}

		JPAUtil.session( emf, em ->
		{
			// persist new wrapper instances to avoid matching cached entities
			LocalId.of( role,
					LocalId.of( org,
							LocalId.of( agent, LocalId.of( contextRef ) ) ) )
					.persist( em );
			fail( "Throw constraint violation" );
		} );
	}

	@SuppressWarnings( "deprecation" )
	@Ignore
	@Test( expected = RollbackException.class )
	public void testAsyncConstraint2()
	{
		final String role = "role", org = "org", agent = "agent";
		final UUID contextRef = new UUID();
		final LocalId id3 = LocalId.of( role, LocalId.of( org,
				LocalId.of( agent, LocalId.of( contextRef ) ) ) );
		JPAUtil.session( emf, em ->
		{
			id3.persist( em );
			LocalId.findAllAsync( em, contextRef, 2 ).subscribe( id ->
			{
				LOG.info( "Async retrieved: {}", id );
				assertThat( "contextRef match", id.contextRef(),
						equalTo( contextRef ) );
			}, e -> fail( e.getMessage() ) );
		} );

		JPAUtil.session( emf, em ->
		{
			// persist new wrapper instances to avoid matching cached entities
			LocalId.of( role,
					LocalId.of( org,
							LocalId.of( agent, LocalId.of( contextRef ) ) ) )
					.persist( em );
			fail( "Throw constraint violation" );
		} );
	}
}
