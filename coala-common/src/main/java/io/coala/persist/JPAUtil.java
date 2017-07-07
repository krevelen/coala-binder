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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.Transactional;

import org.apache.logging.log4j.Logger;

import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
import io.coala.log.LogUtil;
import io.reactivex.Observable;

/**
 * {@link JPAUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class JPAUtil
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( JPAUtil.class );

	private JPAUtil()
	{
		// singleton
	}

	/**
	 * @param emf the (expensive) {@link EntityManagerFactory}
	 * @param consumer the transaction's {@link EntityManager} {@link Consumer}
	 */
	public static void session( final EntityManagerFactory emf,
		final ThrowingConsumer<EntityManager, ?> consumer )
	{
		final List<Throwable> error = new ArrayList<>( 1 );
		final CountDownLatch latch = new CountDownLatch( 1 );
		session( emf ).subscribe( em ->
		{
			try
			{
				consumer.accept( em );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			} finally
			{
				latch.countDown();
			}
		}, e ->
		{
			error.add( e );
			latch.countDown();
		} );
		int secs = 0;
		while( latch.getCount() > 0 )
			try
			{
				latch.await( 1, TimeUnit.SECONDS );
				if( error.isEmpty() )
					LOG.trace( "JPA session taking >{} seconds", ++secs );
			} catch( final InterruptedException ignore )
			{
			}
		if( !error.isEmpty() ) Thrower.rethrowUnchecked( error.get( 0 ) );
	}

	/**
	 * @param emf
	 * @return
	 */
	public static Observable<EntityManager>
		session( final EntityManagerFactory emf )
	{
		return Observable.using( emf::createEntityManager, em ->
		{
			return Observable.using( em::getTransaction, tx ->
			{
				return Observable.create( sub ->
				{
					tx.begin();
					try
					{
						sub.onNext( em );
						if( tx.isActive() ) // tx may have been committed already
							tx.commit();
						sub.onComplete();
					} catch( final Throwable e )
					{
						sub.onError( e );
					}
				} );
			}, tx ->
			{
				if( tx.isActive() ) tx.rollback();
			} );
		}, EntityManager::close );

//		return Observable.create( sub ->
//		{
//			final EntityManager em = emf.createEntityManager();
//			final EntityTransaction tran = em.getTransaction();
//			try
//			{
//				tran.begin();
//				sub.onNext( em );
//				if( tran.isActive() )
//				{
//					tran.commit();
//				}
//				sub.onComplete();
//			} catch( final Throwable e )
//			{
//				if( tran.isActive() )
//				{
//					tran.rollback();
//				}
//				sub.onError( e );
//			} finally
//			{
//				em.close();
//			}
//		} );
	}

	/**
	 * see http://stackoverflow.com/a/35587856/1418999, but with half the calls
	 * to {@code finder}
	 * 
	 * @param em
	 * @param keyFinder
	 * @param factory
	 * @return
	 */
	@Transactional // not really
	public static <T> void existsOrCreate( final EntityManager outer,
		final Supplier<Boolean> keyFinder, final Supplier<T> factory )
	{
		if( keyFinder.get() ) return;
		final EntityManager inner = outer.getEntityManagerFactory()
				.createEntityManager();
		final T created = factory.get();
		final EntityTransaction tx = inner.getTransaction();
		try
		{
			tx.begin();
			inner.persist( created );
			tx.commit();
			outer.merge( created );
		} catch( final PersistenceException ex )
		{
			tx.rollback(); // unique constraint violation ?
			if( !keyFinder.get() ) throw ex; // retry failed otherwise -> fail
		} catch( final Throwable t )
		{
			tx.rollback();
			throw t;
		} finally
		{
			inner.close();
		}
	}

	/**
	 * see http://stackoverflow.com/a/35587856/1418999, but with half the calls
	 * to {@code finder}
	 * 
	 * @param em
	 * @param finder
	 * @param factory
	 * @return
	 */
	@Transactional // not really
	public static <T> T findOrCreate( final EntityManager outer,
		final Supplier<T> finder, final Supplier<T> factory )
	{
		final EntityManager inner = outer.getEntityManagerFactory()
				.createEntityManager();
		final T attempt1 = finder.get();
		if( attempt1 != null ) return attempt1;
		final T created = factory.get();
		final EntityTransaction tx = inner.getTransaction();
		try
		{
			tx.begin();
			inner.persist( created );
			tx.commit();
			return outer.merge( created );
		} catch( final PersistenceException ex )
		{
			if( tx.isActive() ) tx.rollback(); // unique constraint violation ?
			final T result = finder.get(); // retry
			if( result == null ) throw ex; // other issue -> fail
			return result; // ok now
		} catch( final Throwable t )
		{
			if( tx.isActive() ) tx.rollback();
			throw t;
		} finally
		{
			inner.close();
		}
	}

	/**
	 * @param em the session or {@link EntityManager}
	 * @param daoType the type of entity to return
	 * @param query the JPQL match query {@link String} to execute
	 * @return a synchronous {@link Stream} of match results, if any
	 */
	@Transactional // not really
	public static <DAO> Stream<DAO> findSync( final EntityManager em,
		final Class<DAO> daoType, final String query )
	{
		return em.createQuery( query, daoType ).getResultList().stream();
	}

	/**
	 * utility method
	 * 
	 * @param em the session or {@link EntityManager}
	 * @param query the JPQL match query {@link String} to execute
	 * @param pageSize the buffer size (small: more SQL, large: more heap)
	 * @param pkType the type of the primary key attribute/field
	 * @param pkAtt the name of the primary key attribute/field
	 * @return a buffered {@link Observable} stream of match results, if any
	 */
	@Transactional // not really
	@SuppressWarnings( "unchecked" )
	public static <DAO, PK> Observable<List<DAO>> findAsync(
		final EntityManager em, final int pageSize,
		final SingularAttribute<? super DAO, PK> pkAttr,
		final BiFunction<CriteriaQuery<PK>, Root<DAO>, CriteriaQuery<PK>> restrictor )
	{
		final Class<DAO> entityType = (Class<DAO>) Objects.requireNonNull(
				pkAttr.getDeclaringType().getJavaType(),
				"No declaring entity type" );
		final Class<PK> pkType = Objects.requireNonNull(
				pkAttr.getBindableJavaType(), "No bindable attribute type" );
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		return Observable.using( () ->
		{
			final CriteriaQuery<PK> pkQry = cb.createQuery( pkType );
			final Root<DAO> pkRoot = pkQry.from( entityType );
			pkQry.select( pkRoot.get( pkAttr ) );
			return em
					.createQuery( restrictor == null ? pkQry
							: restrictor.apply( pkQry, pkRoot ) )
					.getResultList();
		}, pks ->
		{
			return Observable.fromIterable( pks )
					.buffer( Math.max( 1, pageSize ) ).map( page ->
					{
						final CriteriaQuery<DAO> pgQry = cb
								.createQuery( entityType );
						final Root<DAO> pgRoot = pgQry.from( entityType );
						// query filtering primary keys in current page only
						final Predicate pkFilter = cb.disjunction();
						for( Object pk : page )
							pkFilter.getExpressions().add(
									cb.equal( pgRoot.get( pkAttr ), pk ) );
//								cb.equal( pgRoot.get( pkAttr ), pk );
						return //Observable.fromIterable( 
						em.createQuery(
								pgQry.select( pgRoot ).where( pkFilter ) )
								.getResultList()
//									)
						;
					} );
		}, list ->
		{
		} );
	}
}
