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
package io.coala.enterprise.dao;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.aeonbits.owner.ConfigCache;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.JsonNode;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CoordinationFact;
import io.coala.enterprise.CoordinationFactType;
import io.coala.enterprise.Transaction;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.persist.JsonNodeToStringConverter;
import io.coala.persist.UUIDToByteConverter;
import io.coala.time.Instant;
import io.coala.time.ReplicateConfig;

/**
 * {@link CoordinationFactDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity
@Table( name = CoordinationFactDao.TABLE_NAME )
public class CoordinationFactDao
	extends AbstractDao<CoordinationFact, CoordinationFactDao>
{
	public static final String TABLE_NAME = "FACTS";

	/**
	 * {@link PK} as inspired by http://stackoverflow.com/a/13033039
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Embeddable
	public static class PK extends AbstractDao<CoordinationFact.ID, PK>
	{

		@Column( name = "CONTEXT", nullable = true, updatable = false )
		protected String localID;

		@Column( name = "ID", unique = true, nullable = false,
			updatable = false,
			length = 16/* , columnDefinition = "BINARY(16)" */ )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID id;

		@Inject
		LocalBinder binder;

		@Override
		CoordinationFact.ID doRestore()
		{
			if( this.binder.id().equals( this.localID ) )
				LogUtil.getLogger( PK.class ).warn( "Context mismatch: {} v {}",
						this.localID, this.binder.id() );
			return CoordinationFact.ID.of( this.id );
		}

		@Override
		PK prePersist( final CoordinationFact.ID source )
		{
			this.id = source.unwrap();
			this.localID = this.binder.id();
			return this;
		}

	}

	/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "CREATED_TS", insertable = false, updatable = false
	/* , columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" */ )
	protected Date created;

	/** time stamp of last update; should never change */
	@Version
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "UPDATED_TS", nullable = false, insertable = false,
		updatable = false )
	protected Date updated;

	@EmbeddedId
	protected PK id;

	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected TransactionDao transaction;

	@Column( name = "KIND", nullable = false, updatable = false )
	protected CompositeActorDao creator;

	@Column( name = "KIND", nullable = false, updatable = false )
	protected CoordinationFactType kind;

	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "OCCURRENCE", nullable = true, updatable = false )
	protected Date occurrence;

	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "EXPIRATION", nullable = true, updatable = false )
	protected Date expiration;

	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected CoordinationFactDao cause;

	@Column( name = "PROPERTIES", nullable = true, updatable = false )
	@Convert( converter = JsonNodeToStringConverter.class )
	protected JsonNode properties;

	@Inject
	LocalBinder binder;

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	CoordinationFact doRestore()
	{
		final Transaction tran = this.transaction.doRestore();
		final Date offset = null;
		return this.binder.inject( CoordinationFact.Factory.class ).create(
				tran.kind(), this.id.doRestore(), tran,
				this.creator.doRestore(), this.kind,
				Instant.of( this.expiration, offset ), this.cause.doRestore(),
				(Map<?, ?>) JsonUtil.valueOf( this.properties, Map.class ) );
	}

	@Override
	CoordinationFactDao prePersist( final CoordinationFact fact )
	{
		final Date offset = Date.from(
				ConfigCache.getOrCreate( ReplicateConfig.class ).offset() );
		this.id = this.binder.inject( PK.class ).prePersist( fact.id() );
		this.kind = fact.kind();
		this.transaction = this.binder.inject( TransactionDao.class )
				.prePersist( fact.transaction() );
		this.creator = this.binder.inject( CompositeActorDao.class )
				.prePersist( fact.creator() );
		this.cause = this.binder.inject( CoordinationFactDao.class )
				.prePersist( fact.cause() );
		this.occurrence = fact.occurrence().toDate( offset );
		this.expiration = fact.expiration().toDate( offset );
		this.properties = JsonUtil.toTree( fact.properties() );
		return this;
	}

}
