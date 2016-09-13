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

import java.util.Date;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import com.eaio.uuid.UUID;

import io.coala.exception.Thrower;
import io.coala.name.Id;
import io.coala.persist.UUIDToByteConverter;

/**
 * {@link LocalId} is a recursive child-parent id pair with a {@link UUID} as
 * its root context ID and {@link Comparable} nodes/leaves, persisted as
 * {@link String}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public class LocalId extends Id.OrdinalChild<Comparable, LocalId>
{
	/**
	 * FIXME reverse direction, handle UUID, handle multiple ancestors...
	 * 
	 * @param localId the "natural" {@link String} representation
	 * @return the parsed/deserialized {@link LocalId}
	 */
//	public static LocalId valueOf( final String localId )
//	{
//		if( localId == null ) return null;
//		final String[] split = localId.split( ID_SEP_REGEX, 2 );
//		return of( split[0], valueOf( split[1] ) );
//	}

	public static LocalId create()
	{
		return of( new UUID() );
	}

	public static LocalId of( final UUID contextId )
	{
		return of( contextId, null );
	}

	public static LocalId of( final Comparable value, final LocalId parentId )
	{
		return Id.of( new LocalId(), value, parentId );
	}

	public Stream<LocalId> find( final EntityManager em,
		final LocalBinder binder, final String query )
	{
		return em.createQuery( query, Dao.class ).getResultList().stream()
				.map( dao ->
				{
					return dao.restore( binder );
				} );
	}

	public Stream<LocalId> findAll( final EntityManager em,
		final LocalBinder binder )
	{
		return find( em, binder,
				"SELECT dao FROM " + Dao.ENTITY_NAME + " dao" );
	}

	public Dao persist( final EntityManager em )
	{
		final Dao result = new Dao().prePersist( this );
		em.persist( result );
		return result;
	}

	/**
	 * Although this {@link Dao} implements the {@link BindableDao}, it does not
	 * require to be @{@link Inject}ed e.g. by a {@link LocalBinder}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Entity( name = Dao.ENTITY_NAME )
	@Cacheable
	@Table( name = "LOCAL_IDS", uniqueConstraints = @UniqueConstraint(
		columnNames =
	{ "MY_ID", "PARENT_ID", "CONTEXT_ID" } ) )
	public static class Dao extends BindableDao<LocalId, Dao>
	{
		public static final String ENTITY_NAME = "LocalIdDao";

		/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
		@Temporal( TemporalType.TIMESTAMP )
		@Column( name = "CREATED_TS", insertable = false, updatable = false,
			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
		protected Date created;

		/** automated time stamp of last update (typically never changes) */
		@Version
		@Temporal( TemporalType.TIMESTAMP )
		@Column( name = "UPDATED_TS", insertable = false, updatable = false,
			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
		protected Date updated;

		@javax.persistence.Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		@Column( name = "ID", nullable = false, updatable = false )
		protected Integer id;

		@Column( name = "MY_ID", nullable = false, updatable = false )
		protected String myId;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		@JoinColumn( name = "PARENT_ID", nullable = true, updatable = false )
		protected Dao parentId;

		@Column( name = "CONTEXT_ID", nullable = false, updatable = false,
			length = 16 /* , columnDefinition = "BINARY(16)" */ )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID contextId;

		@Override
		public LocalId restore( final LocalBinder binder )
		{
			return LocalId.of( this.myId,
					this.parentId == null ? LocalId.of( this.contextId )
							: this.parentId.restore( binder ) );
		}

		@Override
		protected Dao prePersist( final LocalId source )
		{
			// exit recursion if no parent (i.e. source == root context UUID)
			if( source == null || source.parent() == null ) return null;
			for( LocalId id = source; id.parent() != null; id = id.parent() )
				if( id.parent() != null && id.parent().parent() == null )
					this.contextId = (UUID) id.parent().unwrap();
			if( this.contextId == null )
				Thrower.throwNew( IllegalArgumentException.class,
						"Unable to resolve context UUID for {}: {}",
						LocalId.class.getSimpleName(), source );
			this.myId = source.unwrap().toString();
			this.parentId = new Dao().prePersist( source.parent() );
			return this;
		}
	}
}