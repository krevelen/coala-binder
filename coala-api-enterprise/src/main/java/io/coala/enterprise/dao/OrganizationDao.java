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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.Organization;

/**
 * {@link OrganizationDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity( name = OrganizationDao.TABLE_NAME )
public class OrganizationDao
	extends AbstractLocalEntity<Organization, OrganizationDao>
{
	public static final String TABLE_NAME = "ORGANIZATIONS";

	@Column( name = "ID", nullable = true, updatable = false )
	protected String id;

	@Override
	Organization doRestore( final LocalBinder binder )
	{
		return binder.inject( Organization.Factory.class ).create( this.id );
	}

	@Override
	void prepare( final EntityManager em, final Organization organization )
	{
		this.id = organization.id().unwrap();
	}
}
