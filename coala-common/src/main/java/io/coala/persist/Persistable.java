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

import java.util.stream.Stream;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.coala.json.JsonUtil;
import io.coala.util.TypeArguments;

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
	DAO persist( EntityManager em );

	default String stringify()
	{
		return getClass().getSimpleName() + JsonUtil.stringify( this );
	}

	@SuppressWarnings( "unchecked" )
	default Stream<DAO> find( final EntityManager em, final String query )
	{
		return em
				.createQuery( query,
						(Class<DAO>) TypeArguments
								.of( Persistable.class, getClass() ).get( 0 ) )
				.getResultList().stream();
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
}
