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
package io.coala.bind;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.util.TypeArguments;

/**
 * {@link Injectable} work in progress
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Injectable
{

	@JsonIgnore
	LocalBinder binder();

//	default EntityManager

	/**
	 * {@link Dao} links ({@link Entity} or {@link Embeddable}) data access
	 * objects to their referent types and provides some JSON de/serialization
	 * features
	 * <p>
	 * TODO: use guice-persist, DAO auto-mapping, {@link TypedQuery} utility,
	 * etc.
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
	abstract class Dao<T, THIS extends Dao<T, ?>> implements Injectable
	{

		@SuppressWarnings( "unchecked" )
		public static <R extends Dao<T, ?>, T> R persist(
			final LocalBinder binder, final T source, final Class<R> daoType )
		{
			return (R) binder.inject( daoType ).persist( source );
		}

		@Inject
		private LocalBinder binder;

		@Inject
		private EntityManager em;

		@Override
		public String toString()
		{
			return getClass().getSimpleName() + JsonUtil.stringify( this );
		}

		@Override
		public LocalBinder binder()
		{
			return this.binder;
		}

		@Transactional
		public TypedQuery<THIS> query( final String query )
		{
			return this.em.createQuery( query, daoType() );
		}

		public Stream<T> find( final String query )
		{
			return query( query ).getResultList().stream().map( Dao::restore );
		}

		public Stream<T> findAll()
		{
			return find( "SELECT dao FROM " + daoName() + " dao" );
		}

		@SuppressWarnings( "unchecked" )
		@Transactional
		public THIS persist( final T source )
		{
			prePersist( source );
			this.em.persist( this );
			return (THIS) this;
		}

		@SuppressWarnings( "unchecked" )
		public Class<THIS> daoType()
		{
			// caching?
			return (Class<THIS>) TypeArguments.of( Dao.class, getClass() )
					.get( 1 );
		}

		public String daoName()
		{
			// caching?
			Entity annot;
			for( Class<?> cls = getClass(); cls != BindableDao.class; cls = cls
					.getSuperclass() )
				if( (annot = cls.getAnnotation( Entity.class )) != null )
					return annot.name() == null ? cls.getSimpleName()
							: annot.name();
			return Thrower.throwNew( IllegalStateException::new,
					() -> getClass() + " is not an @" + Entity.class );
		}

		protected abstract THIS prePersist( T source );

		public abstract T restore();
	}

}
