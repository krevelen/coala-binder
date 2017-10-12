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
package io.coala.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.coala.data.Table.Change;
import io.coala.data.Table.Property;
import io.coala.data.Table.Tuple;
import io.coala.exception.Thrower;
import io.reactivex.Observable;

/**
 * {@link MapLayer} provides a layer for maps with structure:<br/>
 * { tupleKey -> { propertyType -> propertyValue } }
 * 
 * @param <ID>
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public class MapLayer<ID> implements DataLayer
{
	private final Map<ID, Map<Class<? extends Property>, Object>> data;

	private final List<Class<? extends Property>> properties;

	private final Supplier<ID> indexer;

	private final List<Observable<Change>> changes = new ArrayList<>();

	public MapLayer( final Map<ID, Map<Class<? extends Property>, Object>> data,
		final List<Class<? extends Property>> properties,
		final Supplier<ID> indexer )
	{
		this.data = data;
		this.properties = properties;
		this.indexer = indexer;
	}

	private <T extends Tuple> T createTuple( final Class<T> type )
	{
		try
		{
			return (T) type.newInstance();
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	private Map<Class<? extends Property>, Object> validValues( final ID key,
		final Class<? extends Property> prop )
	{
		return this.properties != null && this.properties.contains( prop )
				? this.data.computeIfAbsent( key, k -> new HashMap<>() )
				: Thrower.throwNew( IllegalArgumentException::new,
						() -> "Illegal property: " + prop );
	}

	@SuppressWarnings( "unchecked" )
	private Object get( final ID key, final Class<? extends Property> k )
	{
		return validValues( key, k ).get( k );
	}

	@SuppressWarnings( "unchecked" )
	private Object put( final ID key, final Class<? extends Property> k,
		final Object v )
	{
		return validValues( key, k ).put( k, v );
	}

	private String stringify( final ID key )
	{
		final Map<Class<? extends Property>, Object> values = this.data
				.get( key );
		return key + (values == null ? "{}"
				: "{" + this.properties.stream()
						.map( p -> p.getSimpleName() + "="
								+ (values.containsKey( p ) ? values.get( p )
										: "") )
						.reduce( ( s1, s2 ) -> String.join( ", ", s1, s2 ) )
						+ "}");
	}

	@Override
	public Observable<Change> changes()
	{
		return Observable.fromIterable( this.changes ).flatMap( rx -> rx );
	}

	@Override
	public <T extends Tuple> Table<T> createTable( final Class<T> tupleType )
	{
		@SuppressWarnings( "unchecked" )
		final Table<T> result = new Table.Simple<>( this.properties::stream,
				this.indexer::get, this.data::remove,
				this.data.keySet()::stream,
				( key, changes ) -> (T) createTuple( tupleType ).reset( key,
						changes, k -> get( key, k ),
						( k, v ) -> put( key, k, v ), () -> stringify( key ) ),
				this.data::size, this.data::toString, this.data::clear );
		this.changes.add( result.changes() );
		return result;
	}
}