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

import javax.inject.Inject;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CompositeActor;
import io.coala.enterprise.Organization;
import io.coala.log.LogUtil;

/**
 * {@link CompositeActorDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity
@Table( name = CompositeActorDao.TABLE_NAME )
public class CompositeActorDao
	extends AbstractDao<CompositeActor, CompositeActorDao>
{
	public static final String TABLE_NAME = "ACTORS";

	/**
	 * {@link PK} as inspired by http://stackoverflow.com/a/13033039
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Embeddable
	public static class PK extends AbstractDao<CompositeActor.ID, PK>
	{

		@Column( name = "CONTEXT", nullable = true, updatable = false )
		protected String localID;

		@Column( name = "ID", nullable = true, updatable = false )
		protected String id;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		protected PK parent;

		@Inject
		LocalBinder binder;

		@Override
		CompositeActor.ID doRestore()
		{
			if( this.binder.id().equals( this.localID ) )
				LogUtil.getLogger( PK.class ).warn( "Context mismatch: {} v {}",
						this.localID, this.binder.id() );
			return CompositeActor.ID.of( this.id,
					Organization.ID.of( this.parent.restore().unwrap() ) );
		}

		@Override
		PK prePersist( final CompositeActor.ID source )
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
	protected OrganizationDao organization;

	@Inject
	LocalBinder binder;

	@Override
	CompositeActor doRestore()
	{
		return this.binder.inject( CompositeActor.Factory.class )
				.create( this.id.doRestore(), this.organization.doRestore() );
	}

	@Override
	CompositeActorDao prePersist( final CompositeActor source )
	{
		this.id = this.binder.inject( PK.class ).prePersist( source.id() );
		return this;
	}

}
