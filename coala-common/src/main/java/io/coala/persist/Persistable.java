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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.coala.json.JsonUtil;
import io.coala.util.TypeArguments;
import rx.Observable;

/**
 * {@link Persistable}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
@JsonInclude( Include.NON_NULL )
public interface Persistable<DAO extends Persistable.Dao>
{

	default String stringify()
	{
		return getClass().getSimpleName() + JsonUtil.stringify( this );
	}

	@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
	@JsonInclude( Include.NON_NULL )
	interface Dao
	{
		default String stringify()
		{
			return getClass().getSimpleName() + JsonUtil.stringify( this );
		}
	}

	@Transactional
	DAO persist( EntityManager em );

	@SuppressWarnings( "unchecked" )
	default Class<DAO> daoType()
	{
		return (Class<DAO>) TypeArguments.of( Persistable.class, getClass() )
				.get( 0 );

	}

	/**
	 * @param em the session or {@link EntityManager}
	 * @param query the JPQL match query {@link String} to execute
	 * @param daoType the type of entity to return
	 * @return a synchronous {@link Stream} of match results, if any
	 */
	@Transactional
	static <DAO> Stream<DAO> findSync( final EntityManager em,
		final String query, final Class<DAO> daoType )
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
	@Transactional
	static <DAO> Observable<DAO> findAsync( final EntityManager em,
		final Class<DAO> entityType, final int pageSize, final String pkName,
		final Function<CriteriaQuery<?>, TypedQuery<?>> restrictor )
	{
		try
		{
			// select only primary keys first
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<Object> pkQry = cb.createQuery();
			final Path<Object> pkPath = pkQry.from( entityType ).get( pkName );
			pkQry.select( pkPath ).distinct( true );
			final List<?> pks = (restrictor == null ? em.createQuery( pkQry )
					: restrictor.apply( pkQry )).getResultList();

			System.out.println( "Buffering " + pks );
			// FIXME why are pages handled twice????
			// stream full results in page buffers of specified pageSize>=1
			return Observable.from( pks ).buffer( Math.max( 1, pageSize ) )
					.flatMap( page ->
					{
						System.out.println( "Page " + page );
						try
						{
							// query filtering primary keys in current page only
							final Predicate pkFilter = cb.disjunction();
							for( Object pk : page )
								pkFilter.getExpressions()
										.add( cb.equal( pkPath, pk ) );
							final CriteriaQuery<DAO> pgQry = cb
									.createQuery( entityType );
							return Observable
									.from( em
											.createQuery( pgQry
													.select( pgQry
															.from( entityType ) )
													.where( pkFilter ) )
											.getResultList() );
						} catch( final NoResultException e )
						{
							return Observable.empty();
						} catch( final Exception e )
						{
							return Observable.error( e );
						}
					} );
		} catch( final NoResultException e )
		{
			return Observable.empty();
		} catch( final Exception e )
		{
			return Observable.error( e );
		}
	}
}
