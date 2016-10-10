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
package io.coala.bind.persist;

import java.util.Date;
import java.util.Objects;

import javax.inject.Inject;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.persist.UUIDToByteConverter;

/**
 * Although this {@link LocalIdDao} implements the {@link BindableDao}, it does
 * not require to be @{@link Inject}ed e.g. by a {@link LocalBinder}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Entity( name = LocalIdDao.ENTITY_NAME )
@Cacheable
@Table( name = "LOCAL_IDS",
	uniqueConstraints =
{ @UniqueConstraint( columnNames = { LocalIdDao.CONTEXT_COLUMN_NAME,
				LocalIdDao.VALUE_COLUMN_NAME } ),
		@UniqueConstraint( columnNames =
		{ LocalIdDao.CONTEXT_COLUMN_NAME, LocalIdDao.PARENT_COLUMN_NAME } ) } )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
public class LocalIdDao implements BindableDao<LocalId, LocalIdDao>
{
	public static final String ENTITY_NAME = "LOCAL_ID";

	public static final String VALUE_COLUMN_NAME = "VALUE";

	public static final String PARENT_COLUMN_NAME = "PARENT_ID";

	public static final String CONTEXT_COLUMN_NAME = "CONTEXT";

	public static LocalIdDao find( final EntityManager em, final LocalId id )
	{
		final Comparable<?> value = Objects.requireNonNull( id.unwrap() );
		final LocalIdDao parentRef = id.parentRef().parentRef() == null ? null
				: find( em, id.parentRef() );
		final UUID context = Objects.requireNonNull( id.contextRef() );
		try
		{
			return em
					.createQuery( "SELECT d FROM " + ENTITY_NAME
							+ " d WHERE d.value=?1 AND d.parentRef=?2"
							+ " AND d.contextRef=?3", LocalIdDao.class )
					.setParameter( 1, value ).setParameter( 2, parentRef )
					.setParameter( 3, context ).getSingleResult();
		} catch( final NoResultException ignore )
		{
			return null;
		}
	}

	public static LocalIdDao create( final EntityManager em, final LocalId id )
	{
		final Comparable<?> value = Objects.requireNonNull( id.unwrap() );
		final LocalIdDao parentRef = Objects.requireNonNull( id.parentRef() )
				.parentRef() == null ? null : id.parentRef().persist( em );
		final UUID contextRef = Objects.requireNonNull( id.contextRef() );

		final LocalIdDao result = new LocalIdDao();
		result.contextRef = contextRef;
		result.parentRef = parentRef;
		result.value = value.toString();
//			em.persist( result );
		return result;
	}

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	@Column( name = "PK", nullable = false, updatable = false,
		insertable = false )
	public Integer pk;

	/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "CREATED_TS", insertable = false, updatable = false,
		columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
	@JsonIgnore
	protected Date created;

//		/** automated time stamp of last update (typically never changes) */
//		@Version
//		@Temporal( TemporalType.TIMESTAMP )
//		@Column( name = "UPDATED_TS", insertable = false, updatable = false,
//			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
//		@JsonIgnore
//		protected Date updated;

	@Column( name = VALUE_COLUMN_NAME, nullable = false, updatable = false )
	protected String value;

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn( name = PARENT_COLUMN_NAME, nullable = true, updatable = false )
	protected LocalIdDao parentRef;

	@JsonIgnore
	@Column( name = CONTEXT_COLUMN_NAME, nullable = false, updatable = false,
		length = 16, columnDefinition = "BINARY(16)" )
	@Convert( converter = UUIDToByteConverter.class )
	protected UUID contextRef;

	@Override
	public LocalId restore( final LocalBinder binder )
	{
		final String value = Objects.requireNonNull( this.value );
		return LocalId.of( value,
				this.parentRef == null
						? LocalId
								.of( Objects.requireNonNull( this.contextRef ) )
						: this.parentRef.restore( binder ) );
	}
}