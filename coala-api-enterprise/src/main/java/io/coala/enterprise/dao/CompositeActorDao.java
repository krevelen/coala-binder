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
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CompositeActor;
import io.coala.enterprise.CoordinationFact;
import io.coala.enterprise.Organization;

/**
 * {@link CompositeActorDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity( name = CompositeActorDao.TABLE_NAME )
public class CompositeActorDao
	extends AbstractLocalEntity<CompositeActor, CompositeActorDao>
{
	public static final String TABLE_NAME = "ACTORS";

	@Column( name = "ID", nullable = true, updatable = false )
	protected String id;

	@Column( name = "ORGANIZATION", nullable = true, updatable = false )
	@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
	protected OrganizationDao organization;

	@Override
	CompositeActor doRestore( final LocalBinder binder )
	{
		final Organization organization = this.organization.doRestore( binder );
		return CompositeActor.of( this.id, organization,
				binder.inject( CoordinationFact.Factory.class ) );
	}

	@Override
	void prepare( final EntityManager em, final CompositeActor t )
	{
		this.id = t.id().unwrap();
//		this.organization = em.createQuery( "", OrganizationDao.class )
//				.getSingleResult();
	}

}
