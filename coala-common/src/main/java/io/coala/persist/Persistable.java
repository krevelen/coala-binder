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
package io.coala.persist;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.Transactional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.coala.json.JsonUtil;
import io.coala.util.TypeArguments;
import io.reactivex.Observable;

/**
 * {@link Persistable}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
@JsonInclude( Include.NON_NULL )
public interface Persistable<DAO extends Persistable.Dao>
{

	default String stringify()
	{
		return getClass().getSimpleName() + JsonUtil.stringify( this );
	}

	@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
	@JsonInclude( Include.NON_NULL )
	interface Dao
	{
		default String stringify()
		{
			return getClass().getSimpleName() + JsonUtil.stringify( this );
		}
	}

	@SuppressWarnings( "unchecked" )
	default Class<DAO> daoType()
	{
		return (Class<DAO>) TypeArguments.of( Persistable.class, getClass() )
				.get( 0 );
	}

	@Transactional // not really
	DAO persist( EntityManager em );

	@Transactional // not really
	default Stream<DAO> findSync( final EntityManager em, final String query )
	{
		return JPAUtil.findSync( em, daoType(), query );
	}

	@Transactional // not really
	default <PK> Observable<List<DAO>> findAsync( final EntityManager em,
		final int pageSize, final SingularAttribute<? super DAO, PK> pkAttr,
		final BiFunction<CriteriaQuery<PK>, Root<DAO>, CriteriaQuery<PK>> restrictor )
	{
		return JPAUtil.findAsync( em, pageSize, pkAttr, restrictor );
	}
}
