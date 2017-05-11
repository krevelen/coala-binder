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
package io.coala.enterprise.persist;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.measure.Unit;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.TreeNode;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.persist.LocalIdDao;
import io.coala.bind.persist.LocalIdDao_;
import io.coala.enterprise.Actor;
import io.coala.enterprise.Fact;
import io.coala.enterprise.Fact.ID;
import io.coala.enterprise.FactKind;
import io.coala.enterprise.Transaction;
import io.coala.json.JsonUtil;
import io.coala.math.Range;
import io.coala.persist.JsonToStringConverter;
import io.coala.persist.UUIDToByteConverter;
import io.coala.time.Instant;
import io.coala.time.persist.InstantDao;
import io.coala.time.persist.InstantDao_;

/**
 * {@link FactDao} with JPA MetaModel in {@link FactDao_}, {@link LocalIdDao_}
 * and {@link InstantDao_}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity
@Table( name = "FACTS" )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
// SINGLE_TABLE preferred, see https://en.wikibooks.org/wiki/Java_Persistence/Inheritance
public class FactDao implements BindableDao<Fact, FactDao>
{
	/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "CREATED_TS", insertable = false, updatable = false,
		columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
	@JsonIgnore
	protected Date created;

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	// GenerationType.IDENTITY strategy unsupported in Neo4J
	@Column( name = "PK", nullable = false, updatable = false )
	protected Integer pk;

	@Basic // for meta-model, see http://stackoverflow.com/q/27333779/1418999
	@Column( name = "ID", nullable = false, updatable = false, length = 16,
		columnDefinition = "BINARY(16)", unique = true )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID id;

	/** redundant, derived from transaction */
	@ManyToOne( optional = false, fetch = FetchType.LAZY, cascade = {} )
	@JoinColumn( name = "CREATOR_ID", updatable = false )
	protected LocalIdDao creatorRef;

	/** redundant, derived from transaction */
	@ManyToOne( optional = false, fetch = FetchType.LAZY, cascade = {} )
	@JoinColumn( name = "RESPONDER_ID", updatable = false )
	protected LocalIdDao responderRef;

	@Basic // for meta-model, see http://stackoverflow.com/q/27333779/1418999
	@Column( name = "TID", nullable = false, updatable = false, length = 16,
		columnDefinition = "BINARY(16)" )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID tid;

	@Column( name = "TYPE", nullable = false, updatable = false )
	protected Class<? extends Fact> type;

	@ManyToOne( optional = false, fetch = FetchType.LAZY, cascade = {} )
	@JoinColumn( name = "INITIATOR_ID", updatable = false )
	protected LocalIdDao initiatorRef;

	@ManyToOne( optional = false, fetch = FetchType.LAZY, cascade = {} )
	@JoinColumn( name = "EXECUTOR_ID", updatable = false )
	protected LocalIdDao executorRef;

	@Column( name = "KIND", nullable = false, updatable = false )
	protected FactKind kind;

	@AttributeOverrides( {
			@AttributeOverride( name = InstantDao.POSIX_ATTR_NAME,
				column = @Column( name = "OCCUR_POSIX", nullable = false,
					updatable = false ) ),
			@AttributeOverride( name = InstantDao.NUM_ATTR_NAME,
				column = @Column( name = "OCCUR", nullable = false,
					updatable = false ) ),
			@AttributeOverride( name = InstantDao.STR_ATTR_NAME,
				column = @Column( name = "OCCUR_STR", nullable = false,
					updatable = false ) ), } )
	@Embedded
	protected InstantDao occur;

	@AttributeOverrides( {
			@AttributeOverride( name = InstantDao.POSIX_ATTR_NAME,
				column = @Column( name = "EXPIRE_POSIX", updatable = false ) ),
			@AttributeOverride( name = InstantDao.NUM_ATTR_NAME,
				column = @Column( name = "EXPIRE", updatable = false ) ),
			@AttributeOverride( name = InstantDao.STR_ATTR_NAME,
				column = @Column( name = "EXPIRE_STR",
					updatable = false ) ), } )
	@Embedded
	protected InstantDao expire;

	/** links to complete cause (only if persisted locally) */
	@ManyToOne( optional = true, fetch = FetchType.LAZY, cascade = {} )
	@JoinColumn( name = "CAUSE_ID", updatable = false )
	protected FactDao cause;

	/** to reconstruct cause reference, e.g. if cause is persisted remotely */
	@Basic // for meta-model, see http://stackoverflow.com/q/27333779/1418999
	@Column( name = "CAUSE_REF", nullable = true, updatable = false,
		length = 16, columnDefinition = "BINARY(16)" )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID causeRef;

	/** to reconstruct cause reference, e.g. if cause is persisted remotely */
	@Basic // for meta-model, see http://stackoverflow.com/q/27333779/1418999
	@Column( name = "CAUSE_TID", nullable = true, updatable = false,
		length = 16, columnDefinition = "BINARY(16)" )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID causeTranRef;

	@Column( name = "PROPERTIES", nullable = true, updatable = false )
	@Convert( converter = JsonToStringConverter.class )
	protected TreeNode properties;

	public static boolean exists( final EntityManager em, final ID id )
	{
		final UUID uuid = Objects.requireNonNull( id.unwrap() );
		try
		{
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<FactDao> qry = cb.createQuery( FactDao.class );
			final Root<FactDao> root = qry.from( FactDao.class );
			em.createQuery( qry.select( root )
					.where( cb.equal( root.get( FactDao_.id ), uuid ) ) )
					.getSingleResult();
			return true;
		} catch( final NoResultException ignore )
		{
			return false;
		}
	}

	@Transactional // not really
	public static FactDao find( final EntityManager em, final Fact.ID id )
	{
		final UUID uuid = Objects.requireNonNull( id.unwrap() );
		try
		{
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<FactDao> qry = cb.createQuery( FactDao.class );
			final Root<FactDao> root = qry.from( FactDao.class );
			return em
					.createQuery( qry.select( root ).where(
							cb.equal( root.get( FactDao_.id ), uuid ) ) )
					.getSingleResult();
		} catch( final NoResultException empty )
		{
			return null;
		}
	}

	// @Transactional // not really
	public static List<FactDao> find( final EntityManager em,
		final UUID contextRef, final Class<?> typeFilter,
		final Actor.ID initiatorFilter, final Actor.ID executorFilter,
		final FactKind kindFilter, final Fact.ID causeFilter,
		final Actor.ID creatorFilter, final Actor.ID responderFilter,
		final Range<BigDecimal> occurrenceFilter,
		final Range<BigDecimal> expirationFilter,
		final Map<String, Object> propertiesFilter )
	{
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		final CriteriaQuery<FactDao> qry = cb.createQuery( FactDao.class );
		final Root<FactDao> root = qry.from( FactDao.class );
		final Predicate and = cb.and( cb.equal(
				// any Actor.ID, e.g. responder, initiator, executor
				root.get( FactDao_.creatorRef ).get( LocalIdDao_.contextRef ),
				contextRef ) );
		qry.select( root ).where( and );

		if( typeFilter != null ) and.getExpressions()
				.add( cb.equal( root.get( FactDao_.type ), typeFilter ) );
		if( initiatorFilter != null ) and.getExpressions()
				.add( cb.equal( root.get( FactDao_.initiatorRef ),
						initiatorFilter.persist( em ) ) );
		if( executorFilter != null ) and.getExpressions()
				.add( cb.equal( root.get( FactDao_.executorRef ),
						executorFilter.persist( em ) ) );
		if( creatorFilter != null )
			and.getExpressions().add( cb.equal( root.get( FactDao_.creatorRef ),
					creatorFilter.persist( em ) ) );
		if( responderFilter != null ) and.getExpressions()
				.add( cb.equal( root.get( FactDao_.responderRef ),
						initiatorFilter.persist( em ) ) );
		if( kindFilter != null ) and.getExpressions()
				.add( cb.equal( root.get( FactDao_.kind ), kindFilter ) );
		if( causeFilter != null ) and.getExpressions().add( cb
				.equal( root.get( FactDao_.causeRef ), causeFilter.unwrap() ) );
		if( occurrenceFilter != null ) InstantDao.addRangeCriteria( and, cb,
				root.get( FactDao_.occur ), occurrenceFilter );
		if( expirationFilter != null ) InstantDao.addRangeCriteria( and, cb,
				root.get( FactDao_.expire ), expirationFilter );
		if( propertiesFilter != null )
		{
			// FIXME add %like% '"key":"value"' statements for each entry
		}

		// FIXME: chunking, two-step streaming (sorted by creation ts?)
		// as already implemented in ... LocalId JPAUtil?
		return em.createQuery( qry ).getResultList();
	}

	/**
	 * helper method for {@link Fact#persist(EntityManager)}
	 * 
	 * @param em
	 * @param fact
	 * @return
	 */
	@Transactional // not really
	public static FactDao create( final EntityManager em, final Fact fact )
	{
		final Transaction<?> tx = Objects.requireNonNull( fact.transaction() );
		final Date offset = Date.from( tx.scheduler().offset().toInstant() );
		final Unit<?> unit = tx.scheduler().timeUnit();
		final Instant occur = Objects.requireNonNull( fact.occur() );
		final Instant expire = fact.expire();
		final ID causeRef = fact.causeRef();

		final FactDao dao = new FactDao();
		dao.id = Objects.requireNonNull( fact.id().unwrap() );
		dao.tid = Objects.requireNonNull( tx.id().unwrap() );
		dao.type = Objects.requireNonNull( tx.kind() );
		dao.kind = Objects.requireNonNull( fact.kind() );
		dao.initiatorRef = Objects.requireNonNull( tx.initiatorRef() )
				.persist( em );
		dao.executorRef = Objects.requireNonNull( tx.executorRef() )
				.persist( em );
		dao.creatorRef = Objects.requireNonNull( fact.creatorRef() )
				.persist( em );
		dao.responderRef = Objects.requireNonNull( fact.responderRef() )
				.persist( em );
		dao.cause = causeRef == null ? null : find( em, causeRef );
		dao.causeRef = causeRef == null ? null
				: Objects.requireNonNull( causeRef.unwrap() );
		dao.causeTranRef = causeRef == null ? null
				: Objects.requireNonNull( causeRef.parentRef() ).unwrap();
		dao.occur = InstantDao.of( occur, offset, unit );
		dao.expire = InstantDao.of( expire, offset, unit );
		dao.properties = JsonUtil.toTree( fact.properties() );
		// DONT PERSIST YET, taken care of by Fact#persist(EntityManager)
		return dao;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public Fact restore( final LocalBinder binder )
	{
		Objects.requireNonNull( this.creatorRef );
		if( this.causeRef != null ) Objects.requireNonNull( this.causeTranRef );

		final ID id = ID.of( Objects.requireNonNull( this.id ),
				Transaction.ID.of( this.tid, binder.id() ) );
		final Transaction tx = binder.inject( Transaction.Factory.class )
				.create( Transaction.ID.of( this.tid, binder.id() ), this.type,
						Actor.ID.of( this.initiatorRef.restore( binder ) ),
						Actor.ID.of( this.executorRef.restore( binder ) ) );
		final ID cause = this.causeRef == null ? null
				: ID.of( this.causeRef,
						Transaction.ID.of( this.causeTranRef, binder.id() ) );
		final Instant occur = Objects.requireNonNull( this.occur )
				.restore( binder );
		final Instant expire = this.expire == null ? null
				: this.expire.restore( binder );

		final Map<String, Object> properties = JsonUtil
				.valueOf( this.properties, Map.class );
		return binder.inject( Fact.Factory.class ).create( tx.kind(), id, tx,
				this.kind, occur, expire, cause,
				Fact.treeToMap( JsonUtil.getJOM(), this.properties, tx.kind(),
						properties ) );
	}
}