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

import com.eaio.uuid.UUID;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CoordinationFact;
import io.coala.enterprise.Transaction;
import io.coala.log.LogUtil;
import io.coala.persist.UUIDToByteConverter;

/**
 * {@link TransactionDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity
@Table( name = TransactionDao.TABLE_NAME )
public class TransactionDao extends AbstractDao<Transaction<?>, TransactionDao>
{
	public static final String TABLE_NAME = "TRANSACTIONS";

	/**
	 * {@link PK} as inspired by http://stackoverflow.com/a/13033039
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Embeddable
	public static class PK extends AbstractDao<Transaction.ID, PK>
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
		Transaction.ID doRestore()
		{
			if( this.binder.id().equals( this.localID ) )
				LogUtil.getLogger( PK.class ).warn( "Context mismatch: {} v {}",
						this.localID, this.binder.id() );
			return Transaction.ID.of( this.id );
		}

		@Override
		PK prePersist( final Transaction.ID source )
		{
			this.id = source.unwrap();
			this.localID = this.binder.id();
			return this;
		}

	}

	@EmbeddedId
	protected PK id;

	@Column( name = "KIND", nullable = true, updatable = false )
	protected Class<? extends CoordinationFact> kind;

//	@Column( name = "INITIATOR", nullable = true, updatable = false )
	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected CompositeActorDao initiator;

//	@Column( name = "EXECUTOR", nullable = true, updatable = false )
	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected CompositeActorDao executor;

	@Inject
	LocalBinder binder;

	@Override
	Transaction<?> doRestore()
	{
		return this.binder.inject( Transaction.Factory.class ).create(
				this.id.restore(), this.kind, this.initiator.doRestore(),
				this.executor.doRestore() );
	}

	@Override
	TransactionDao prePersist( final Transaction<?> tran )
	{
		this.id = this.binder.inject( PK.class ).prePersist( tran.id() );
		this.kind = tran.kind();
		this.initiator = this.binder.inject( CompositeActorDao.class )
				.prePersist( tran.initiator() );
		this.executor = this.binder.inject( CompositeActorDao.class )
				.prePersist( tran.executor() );
		return this;
	}

}
