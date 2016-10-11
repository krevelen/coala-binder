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
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.persist.LocalIdDao;
import io.coala.exception.Thrower;
import io.coala.name.Id;
import io.coala.persist.JPAUtil;
import io.coala.persist.Persistable;

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
@JsonDeserialize( converter = LocalId.FromStringConverter.class )
public class LocalId extends Id.OrdinalChild<Comparable, LocalId>
	implements Persistable<LocalIdDao>
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

	public static class FromStringConverter
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
			Thrower.throwNew( IllegalArgumentException.class,
					"Can't parse {} from: '{}'", LocalId.class, value );
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
		for( LocalId id = parentRef(); id != null; id = id.parentRef() )
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
		for( LocalId id = parentRef(); id != null; id = id.parentRef() )
			if( id.unwrap() instanceof UUID )
				result += "@" + Integer.toHexString( id.unwrap().hashCode() );
			else
				result = id.unwrap() + ID_SEP_REGEX + result;
		return result;
	}

	public UUID contextRef()
	{
		for( LocalId i = this; i != null; i = i.parentRef() )
			if( i.parentRef() == null ) return (UUID) i.unwrap();
		throw new IllegalArgumentException();
	}

	public Stream<LocalId> find( final EntityManager em,
		final LocalBinder binder, final String query )
	{
		return em.createQuery( query, LocalIdDao.class ).getResultList()
				.stream().map( dao ->
				{
					return dao.restore( binder );
				} );
	}

	public Stream<LocalId> findAll( final EntityManager em,
		final LocalBinder binder )
	{
		return find( em, binder,
				"SELECT dao FROM " + LocalIdDao.ENTITY_NAME + " dao" );
	}

	private transient Integer pk = null;

	@Override
	public LocalIdDao persist( final EntityManager em )
	{
		if( this.pk != null ) return em.find( LocalIdDao.class, this.pk ); // cached?
		final LocalIdDao result = JPAUtil.<LocalIdDao> findOrCreate( em,
				() -> LocalIdDao.find( em, this ),
				() -> LocalIdDao.create( em, this ) );
		this.pk = result.pk;
		return result;
	}
}