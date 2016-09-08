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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import com.eaio.uuid.UUID;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CoordinationFact;
import io.coala.enterprise.Transaction;
import io.coala.persist.UUIDToByteConverter;

/**
 * {@link TransactionDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity( name = TransactionDao.TABLE_NAME )
public class TransactionDao
	extends AbstractLocalEntity<Transaction<?>, TransactionDao>
{
	public static final String TABLE_NAME = "TRANSACTIONS";

	@Column( name = "ID", unique = true, nullable = false, updatable = false,
		length = 16/* , columnDefinition = "BINARY(16)" */ )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID id;

	@Column( name = "KIND", nullable = true, updatable = false )
	protected Class<? extends CoordinationFact> kind;

	@Column( name = "INITIATOR", nullable = true, updatable = false )
	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected CompositeActorDao initiator;

	@Column( name = "EXECUTOR", nullable = true, updatable = false )
	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected CompositeActorDao executor;

	@Override
	Transaction<?> doRestore( final LocalBinder binder )
	{
		return Transaction.of( binder, Transaction.ID.of( this.id ), this.kind,
				this.initiator.restore( binder ).id(),
				this.executor.restore( binder ).id() );
	}

	@Override
	void prepare( final EntityManager em, final Transaction<?> t )
	{
		this.id = t.id().unwrap();
		this.kind = t.kind();
//		this.initiator = t.initiatorID();
//		this.executor = t.executorID();
	}

}
