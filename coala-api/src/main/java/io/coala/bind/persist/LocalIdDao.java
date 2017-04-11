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

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.persist.Persistable;
import io.coala.persist.UUIDToByteConverter;
import rx.Observable;

/**
 * {@link LocalIdDao} is a data access object for {@link LocalId} values with
 * JPA attributes available in generated {@link LocalIdDao_} meta-model entity.
 * <p>
 * Note: {@link #restore(LocalBinder)} ignores the {@link LocalBinder} argument
 * so it may be left {@code null}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity
@Cacheable
@Table( name = "LOCAL_IDS", uniqueConstraints =
// NOTE: multi-column constraints unsupported in Neo4J
{ @UniqueConstraint( columnNames = { LocalIdDao.CONTEXT_COLUMN_NAME,
				LocalIdDao.VALUE_COLUMN_NAME } ),
		@UniqueConstraint( columnNames =
		{ LocalIdDao.CONTEXT_COLUMN_NAME, LocalIdDao.PARENT_COLUMN_NAME } ) } )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
public class LocalIdDao implements BindableDao<LocalId, LocalIdDao>
{
	/** constant used in {@link Table @Table} column constraint specification */
	public static final String VALUE_COLUMN_NAME = "VALUE";

	/** constant used in {@link Table @Table} column constraint specification */
	public static final String PARENT_COLUMN_NAME = "PARENT_ID";

	/** constant used in {@link Table @Table} column constraint specification */
	public static final String CONTEXT_COLUMN_NAME = "CONTEXT";

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	// GenerationType.IDENTITY strategy unsupported in Neo4J
	@Column( name = "PK", nullable = false, updatable = false,
		insertable = false )
	public Integer pk;

	/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
	@JsonIgnore
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "CREATED_TS", insertable = false, updatable = false,
		columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
	protected Date created;

	@Column( name = VALUE_COLUMN_NAME, nullable = false, updatable = false )
	protected String value;

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn( name = PARENT_COLUMN_NAME, nullable = true, updatable = false )
	protected LocalIdDao parentRef;

	@JsonIgnore
	@Basic // for meta-model, see http://stackoverflow.com/q/27333779/1418999
	@Column( name = CONTEXT_COLUMN_NAME, nullable = false, updatable = false,
		length = 16, columnDefinition = "BINARY(16)" )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID contextRef;

	/**
	 * retrieve all {@link LocalIdDao}s in the context of given
	 * {@link LocalBinder} synchronously
	 * 
	 * @param em the session or {@link EntityManager}
	 * @param binder the {@link LocalBinder} to help instantiate
	 * @return a {@link Stream} of {@link LocalId} instances, if any
	 */
	@Transactional
	public static List<LocalIdDao> findAllSync( final EntityManager em,
		final LocalBinder binder )
	{
		return findAll( em, binder.id().contextRef() );
	}

	/**
	 * retrieve all {@link LocalIdDao}s in the context of given
	 * {@link LocalBinder} synchronously
	 * 
	 * @param em the session or {@link EntityManager}
	 * @param binder the {@link LocalBinder} to help instantiate
	 * @return a {@link Stream} of {@link LocalId} instances, if any
	 */
	@Transactional
	public static List<LocalIdDao> findAll( final EntityManager em,
		final UUID contextRef )
	{
//		em.createQuery( "SELECT d FROM " + LocalIdDao.ENTITY_NAME
//				+ " d WHERE d.contextRef=?1", LocalIdDao.class )
//				.setParameter( 1, binder.id().contextRef() );

		final CriteriaBuilder cb = em.getCriteriaBuilder();
		final CriteriaQuery<LocalIdDao> qry = cb
				.createQuery( LocalIdDao.class );
		final Root<LocalIdDao> root = qry.from( qry.getResultType() );
//		final ParameterExpression<UUID> param = cb.parameter( UUID.class );
		final Predicate pred = cb.equal( root.get( LocalIdDao_.contextRef ),
				contextRef );

		return em.createQuery( qry.select( root ).where( pred ) )
				/* .setParameter( param, contextRef ) */.getResultList();
	}

	/**
	 * retrieve all {@link LocalIdDao}s in the context of given
	 * {@link LocalBinder} asynchronously (using page buffers)
	 * 
	 * @param em the session or {@link EntityManager}
	 * @param contextRef the {@link UUID} of the relevant context
	 * @param pageSize the page buffer size
	 * @return a {@link Stream} of {@link LocalId} instances, if any
	 */
	@Transactional
	@Deprecated
	public static Observable<LocalIdDao> findAllAsync( final EntityManager em,
		final UUID contextRef, final int pageSize )
	{
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		return Persistable
				.findAsync( em, LocalIdDao.class, pageSize, LocalIdDao_.pk,
						qry -> em.createQuery( qry.where( cb.equal(
								qry.from( LocalIdDao.class )
										.get( LocalIdDao_.contextRef ),
								contextRef ) ) ) );
	}

	@Transactional
	public static LocalIdDao find( final EntityManager em, final LocalId id )
	{
		final Comparable<?> value = Objects.requireNonNull( id.unwrap() );
		final LocalIdDao parentRef = id.parentRef().parentRef() == null ? null
				: find( em, id.parentRef() );
		final UUID contextRef = Objects.requireNonNull( id.contextRef() );
		try
		{
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<LocalIdDao> qry = cb
					.createQuery( LocalIdDao.class );
			final Root<LocalIdDao> root = qry.from( LocalIdDao.class );
			return em
					.createQuery( qry.select( root ).where( cb.and( cb.equal(
							root.get( LocalIdDao_.contextRef ), contextRef ),
							cb.equal( root.get( LocalIdDao_.value ), value ),
							cb.equal( root.get( LocalIdDao_.parentRef ),
									parentRef ) ) ) )
					.getSingleResult();
		} catch( final NoResultException ignore )
		{
			return null;
		}
	}

	@Transactional // not really
	public static LocalIdDao create( final EntityManager em, final LocalId id )
	{
		final Comparable<?> value = Objects.requireNonNull( id.unwrap() );
		final LocalIdDao parentRef = Objects.requireNonNull( id.parentRef() )
				.parentRef() == null ? null : id.parentRef().persist( em );
		final UUID contextRef = Objects.requireNonNull( id.contextRef() );

		final LocalIdDao result = new LocalIdDao();
		result.contextRef = contextRef;
		result.parentRef = parentRef;
		result.value = value.toString();
//			em.persist( result );
		return result;
	}

	@Override
	public LocalId restore( final LocalBinder ignore )
	{
		final String value = Objects.requireNonNull( this.value );
		return LocalId.of( value,
				this.parentRef == null
						? LocalId
								.of( Objects.requireNonNull( this.contextRef ) )
						: this.parentRef.restore( ignore ) );
	}
}