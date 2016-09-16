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

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.name.Id;
import io.coala.persist.JPAUtil;
import io.coala.persist.Persistable;
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
@JsonSerialize( using = LocalId.JsonExport.class ) // override Wrapper tooling
@JsonDeserialize( converter = LocalId.JsonStringToLocalIdConverter.class )
public class LocalId extends Id.OrdinalChild<Comparable, LocalId>
	implements Persistable<LocalId.Dao>
{
	public static class JsonExport extends JsonSerializer<LocalId>
	{
		@Override
		public void serialize( final LocalId value, final JsonGenerator jgen,
			final SerializerProvider provider ) throws IOException
		{
			jgen.writeString( value.toJSON() );
		}
	}

	public static class JsonStringToLocalIdConverter
		extends StdConverter<String, LocalId>
	{
		@Override
		public LocalId convert( final String value )
		{
			return valueOf( value );
		}
	}

	public static LocalId create()
	{
		return of( new UUID() );
	}

	public static LocalId of( final UUID contextId )
	{
		return Id.of( new LocalId(), contextId, null );
	}

	public static LocalId of( final String value, final LocalId parent )
	{
		return Id.of( new LocalId(), value, parent );
	}

	public static LocalId valueOf( final String value )
	{
		String[] pair = value.split( "@" );
		if( pair.length != 2 || pair[1].isEmpty() )
			throw new IllegalArgumentException( "Can't parse " + value );
		LocalId parent = of( new UUID( pair[1] ) );
		for( pair = pair[0].split( ID_SEP_REGEX,
				2 ); pair.length == 2; pair = pair[1].split( ID_SEP_REGEX, 2 ) )
			parent = of( pair[0], parent );
		return of( pair[0], parent );
	}

	@JsonValue
	public String toJSON()
	{
		String result = unwrap().toString();
		for( LocalId id = parent(); id != null; id = id.parent() )
			if( id.unwrap() instanceof UUID )
				result += "@" + id.unwrap();
			else
				result = id.unwrap() + ID_SEP_REGEX + result;
		return result;
	}

	@Override
	public String toString()
	{
		String result = unwrap().toString();
		for( LocalId id = parent(); id != null; id = id.parent() )
			if( id.unwrap() instanceof UUID )
				result = '['
						+ Integer.toHexString( ((UUID) id.unwrap()).hashCode() )
						+ ']' + result;
			else
				result = id.unwrap() + ID_SEP_REGEX + result;
		return result;
	}

	public UUID contextId()
	{
		for( LocalId i = this; i.parent() != null; i = i.parent() )
			if( i.parent() != null && i.parent().parent() == null )
				return (UUID) i.parent().unwrap();
		throw new IllegalArgumentException();
	}

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

	@Override
	public Dao persist( final EntityManager em )
	{
		final Dao parent = Objects.requireNonNull( parent() ).parent() == null
				? null : parent().persist( em ); // recurse
		final String id = Objects.requireNonNull( unwrap() ).toString();
		final UUID context = Objects.requireNonNull( contextId() );
		return JPAUtil.<Dao> findOrCreate( em,
				() -> Dao.find( em, id, parent, context ),
				() -> Dao.of( id, parent, context ) );
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
	@Table( name = "LOCAL_IDS", uniqueConstraints =
	// FIXME this constraint can be violated, using Hibernate 5
	{ @UniqueConstraint(
		columnNames =
			{ Dao.CONTEXT_COLUMN_NAME, Dao.ID_COLUMN_NAME } ),
			@UniqueConstraint( columnNames =
			{ Dao.CONTEXT_COLUMN_NAME, Dao.PARENT_COLUMN_NAME } ) } )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
//	@DiscriminatorColumn( name = "ID_TYPE" )
	public static class Dao implements BindableDao
	{
		public static final String ENTITY_NAME = "LOCAL_ID";

		public static final String ID_COLUMN_NAME = "ID";

		public static final String PARENT_COLUMN_NAME = "PARENT";

		public static final String CONTEXT_COLUMN_NAME = "CONTEXT";

		protected static Dao find( final EntityManager em, final String id,
			final Dao parent, final UUID context )
		{
			try
			{
				final Dao result = em
						.createQuery(
								"SELECT d FROM " + ENTITY_NAME
										+ " d WHERE d.id=?1  AND d.contextId=?2",
								Dao.class )
						.setParameter( 1, Objects.requireNonNull( id ) )
						.setParameter( 2, Objects.requireNonNull( context ) )
						.getSingleResult();
				return result;
			} catch( final NoResultException ignore )
			{
				return null;
			}
		}

		protected static Dao of( final String id, final Dao parent,
			final UUID context )
		{
			final Dao result = new Dao();
			result.contextId = context;
			result.parent = parent;
			result.id = id;
			return result;
		}

		@javax.persistence.Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		@Column( name = "PK", nullable = false, updatable = false,
			insertable = false )
		protected Integer pk;

//		/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
//		@Temporal( TemporalType.TIMESTAMP )
//		@Column( name = "CREATED_TS", insertable = false, updatable = false,
//			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
//		@JsonIgnore
//		protected Date created;

//		/** automated time stamp of last update (typically never changes) */
//		@Version
//		@Temporal( TemporalType.TIMESTAMP )
//		@Column( name = "UPDATED_TS", insertable = false, updatable = false,
//			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
//		@JsonIgnore
//		protected Date updated;

		@Column( name = ID_COLUMN_NAME, nullable = false, updatable = false )
		protected String id;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = PARENT_COLUMN_NAME, nullable = true,
			updatable = false )
		protected Dao parent;

		@JsonIgnore
		@Column( name = CONTEXT_COLUMN_NAME, nullable = false,
			updatable = false, length = 16, columnDefinition = "BINARY(16)" )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID contextId;

		@Override
		public LocalId restore( final LocalBinder binder )
		{
			return LocalId.of( Objects.requireNonNull( this.id ),
					this.parent == null
							? LocalId.of(
									Objects.requireNonNull( this.contextId ) )
							: this.parent.restore( binder ) );
		}
	}
}