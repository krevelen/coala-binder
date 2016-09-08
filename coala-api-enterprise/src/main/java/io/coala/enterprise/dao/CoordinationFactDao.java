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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.databind.JsonNode;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CoordinationFact;
import io.coala.enterprise.CoordinationFactType;
import io.coala.enterprise.Transaction;
import io.coala.json.JsonUtil;
import io.coala.persist.JsonNodeToStringConverter;
import io.coala.persist.UUIDToByteConverter;
import io.coala.time.Instant;

/**
 * {@link CoordinationFactDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity( name = CoordinationFactDao.TABLE_NAME )
public class CoordinationFactDao
	extends AbstractLocalEntity<CoordinationFact, CoordinationFactDao>
{
	public static final String TABLE_NAME = "FACTS";

	@Column( name = "ID", unique = true, nullable = false, updatable = false,
		length = 16/* , columnDefinition = "BINARY(16)" */ )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID id;

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

	@Column( name = "CAUSE", nullable = true, updatable = false )
	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected CoordinationFactDao cause;

	@Column( name = "PROPERTIES", nullable = true, updatable = false )
	@Convert( converter = JsonNodeToStringConverter.class )
	protected JsonNode properties;

	@Override
	CoordinationFact doRestore( final LocalBinder binder )
	{
		final Transaction<?> tran = this.transaction.doRestore( binder );
		final Date offset = null;
		return binder.inject( CoordinationFact.Factory.class ).create(
				tran.kind(), CoordinationFact.ID.of( this.id ), tran.id(),
				this.creator.restore( binder ).id(), this.kind,
				Instant.of( this.expiration, offset ),
				CoordinationFact.ID.of( this.cause.id ),
				(Map<?, ?>) JsonUtil.valueOf( this.properties, Map.class ) );
	}

	@Override
	void prepare( final EntityManager em, final CoordinationFact t )
	{
		this.id = t.id().unwrap();
//		this.transaction = t.tranID();
//		this.creator = t.creatorID();
//		this.kind = t.type();
//		this.occurrence = t.occurrence();
//		this.expiration = t.expiration();
//		this.cause = t.causeID();
//		this.properties = t.properties();
	}

}
