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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.persist.LocalIdDao;
import io.coala.exception.Thrower;
import io.coala.name.Id;
import io.coala.persist.JPAUtil;
import io.coala.persist.Persistable;
import io.reactivex.Observable;

/**
 * {@link LocalId} is a recursive child-parent id pair with a {@link UUID} as
 * its root context ID and {@link Comparable} nodes/leaves, persisted as
 * {@link String}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
@JsonSerialize( using = LocalId.JsonSerializer.class ) // override Wrapper tooling
@JsonDeserialize( converter = LocalId.FromStringConverter.class )
public class LocalId extends Id.OrdinalChild<Comparable, LocalId>
	implements Persistable<LocalIdDao>
{
	/**
	 * {@link JsonExport} tells Jackson how to write a {@link LocalId} as JSON
	 */
	public static class JsonSerializer
		extends com.fasterxml.jackson.databind.JsonSerializer<LocalId>
	{
		@Override
		public void serialize( final LocalId value, final JsonGenerator jgen,
			final SerializerProvider provider ) throws IOException
		{
			jgen.writeString( value.toJSON() );
		}
	}

	/**
	 * {@link FromStringConverter} tells Jackson how to read a {@link LocalId}
	 */
	public static class FromStringConverter
		extends StdConverter<String, LocalId>
	{
		@Override
		public LocalId convert( final String value )
		{
			return valueOf( value );
		}
	}

	/** @return a (root) context {@link LocalId} */
	public static LocalId create()
	{
		return of( new UUID() );
	}

	/** @return a (root) context {@link LocalId} */
	public static LocalId of( final UUID contextId )
	{
		return Id.of( new LocalId(), contextId, null );
	}

	/** @return a (sub) context {@link LocalId} */
	public static LocalId of( final Comparable<?> value, final LocalId parent )
	{
		return Id.of( new LocalId(), value, parent );
	}

	/** @param value the {@link String} representation to deserialize */
	public static LocalId valueOf( final String value )
	{
		String[] pair = value.split( "@" );
		if( pair.length != 2 || pair[1].isEmpty() ) Thrower
				.throwNew( IllegalArgumentException::new, () -> "Can't parse "
						+ LocalId.class + " from: '" + value + "'" );
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
			if( id.unwrap() instanceof UUID == false )
				result = id.unwrap() + ID_SEP_REGEX + result;
//			else
//				result += "@" + Integer.toHexString( id.unwrap().hashCode() );
		return result;
	}

	public UUID contextRef()
	{
		for( LocalId i = this; i != null; i = i.parentRef() )
			if( i.parentRef() == null ) return (UUID) i.unwrap();
		throw new IllegalArgumentException();
	}

	private transient Integer pk = null;

	@Override
	public LocalIdDao persist( final EntityManager em )
	{
		if( this.pk != null ) return em.find( LocalIdDao.class, this.pk ); // cached?
		final LocalIdDao result = JPAUtil.<LocalIdDao>findOrCreate( em,
				() -> LocalIdDao.find( em, this ),
				() -> LocalIdDao.create( em, this ) );
		this.pk = result.pk;
		return result;
	}

	/**
	 * restore all {@link LocalId}s in the context of given {@link LocalBinder}
	 * synchronously
	 * 
	 * @param em the session or {@link EntityManager}
	 * @param binder the {@link LocalBinder} to help instantiate
	 * @return a {@link Stream} of {@link LocalId} instances, if any
	 */
	public static Stream<LocalId> findAllSync( final EntityManager em,
		final UUID contextRef )
	{
		return LocalIdDao.findAll( em, contextRef ).stream()
				.map( dao -> dao.restore( null ) );
	}

	/**
	 * restore all {@link LocalId}s in the context of given {@link LocalBinder}
	 * asynchronously (using page buffers)
	 * 
	 * @param em the session or {@link EntityManager}
	 * @param contextRef the {@link UUID} of the relevant context
	 * @param pageSize the page buffer size
	 * @return an {@link Observable} stream of {@link LocalId} instances, if any
	 */
	@Deprecated
	public static Observable<List<LocalId>> findAllAsync(
		final EntityManager em, final UUID contextRef, final int pageSize )
	{
		return LocalIdDao.findAllAsync( em, contextRef, pageSize )
				.map( page -> page.stream().map( dao -> dao.restore( null ) )
						.collect( Collectors.toList() ) );
	}
}