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

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.measure.unit.Unit;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.TreeNode;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.persist.LocalIdDao;
import io.coala.enterprise.Actor;
import io.coala.enterprise.Fact;
import io.coala.enterprise.Fact.ID;
import io.coala.enterprise.FactKind;
import io.coala.enterprise.Transaction;
import io.coala.json.JsonUtil;
import io.coala.persist.JsonToStringConverter;
import io.coala.persist.UUIDToByteConverter;
import io.coala.time.Instant;
import io.coala.time.persist.InstantDao;

/**
 * {@link FactDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity( name = FactDao.ENTITY_NAME )
public class FactDao implements BindableDao<Fact, FactDao>
{
	public static final String ENTITY_NAME = "FACTS";

	public static final String TYPE_COL_NAME = "TYPE";

	public static final String KIND_COL_NAME = "KIND";

	public static boolean exists( final EntityManager em, final ID id )
	{
		final UUID uuid = Objects.requireNonNull( id.unwrap() );
		try
		{
//			final Integer pk = 
			em.createQuery(
					"SELECT d.pk FROM " + ENTITY_NAME + " d WHERE d.id=?1",
					Integer.class ).setParameter( 1, uuid ).getSingleResult();
			return true;
		} catch( final NoResultException ignore )
		{
			return false;
		}
	}

	public static FactDao find( final EntityManager em, final ID id )
	{
		final UUID uuid = Objects.requireNonNull( id.unwrap() );
		try
		{
			return em
					.createQuery(
							"SELECT d FROM " + ENTITY_NAME + " d WHERE d.id=?1",
							FactDao.class )
					.setParameter( 1, uuid ).getSingleResult();
		} catch( final NoResultException ignore )
		{
			return null;
		}
	}

	public static FactDao create( final EntityManager em, final Fact fact )
	{
		final Transaction<?> tx = Objects.requireNonNull( fact.transaction() );
		final Date offset = Date.from( tx.offset() );
		final Unit<?> unit = tx.timeUnit();
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
		dao.cause = causeRef == null ? null : find( em, causeRef );
		dao.causeRef = causeRef == null ? null
				: Objects.requireNonNull( causeRef.unwrap() );
		dao.causeTranRef = causeRef == null ? null
				: Objects.requireNonNull( causeRef.parentRef() ).unwrap();
		dao.occur = InstantDao.of( occur, offset, unit );
		dao.expire = InstantDao.of( expire, offset, unit );
		dao.properties = JsonUtil.toTree( fact.properties() );
//			em.persist( result );
		return dao;
	}

	/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "CREATED_TS", insertable = false, updatable = false,
		columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
	@JsonIgnore
	protected Date created;

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	@Column( name = "PK", nullable = false, updatable = false )
	protected Integer pk;

	@Column( name = "ID", nullable = false, updatable = false, length = 16,
		columnDefinition = "BINARY(16)", unique = true )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID id;

	@ManyToOne( optional = false, fetch = FetchType.LAZY, cascade = {} )
	@JoinColumn( name = "CREATOR_ID", updatable = false )
	protected LocalIdDao creatorRef;

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

	@Column( name = KIND_COL_NAME, nullable = false, updatable = false )
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

	/** to construct cause reference, e.g. if cause is persisted remotely */
	@Column( name = "CAUSE_REF", nullable = true, updatable = false,
		length = 16, columnDefinition = "BINARY(16)" )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID causeRef;

	/** to construct cause reference, e.g. if cause is persisted remotely */
	@Column( name = "CAUSE_TID", nullable = true, updatable = false,
		length = 16, columnDefinition = "BINARY(16)" )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID causeTranRef;

	@Column( name = "PROPERTIES", nullable = true, updatable = false )
	@Convert( converter = JsonToStringConverter.class )
	protected TreeNode properties;

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
				Fact.fromJSON( JsonUtil.getJOM(), this.properties, tx.kind(),
						properties ) );
	}
}