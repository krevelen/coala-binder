/* $Id: 80e4eafd9724cda8737180c653b37962fbdb72f0 $
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
package io.coala.dsol3;

import java.util.HashMap;
import java.util.Map;

import io.coala.json.Wrapper;
import nl.tudelft.simulation.event.EventInterface;
import nl.tudelft.simulation.event.EventType;

/**
 * {@link DsolEvent}
 * 
 * @version $Id: 80e4eafd9724cda8737180c653b37962fbdb72f0 $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class DsolEvent<T> extends Wrapper.Simple<T> implements EventInterface
{

	private static final Map<Class<?>, EventType> TYPE_CACHE = new HashMap<>();

	@SuppressWarnings( "rawtypes" )
	public static EventType resolveType( final Class<? extends DsolEvent> type )
	{
		EventType result = TYPE_CACHE.get( type );
		if( result == null )
		{
			result = new EventType( type.getName() );
			TYPE_CACHE.put( type, result );
		}
		return result;
	}

	public static <T> DsolEvent<T> valueOf( final T value )
	{
		return new DsolEvent<T>().withValue( value );
	}

	private transient Object source = null;

	public DsolEvent<T> withValue( final T value )
	{
		wrap( value );
		return this;
	}

	@Override
	public Object getContent()
	{
		return unwrap();
	}

	public DsolEvent<T> withSource( final Object source )
	{
		this.source = source;
		return this;
	}

	@Override
	public Object getSource()
	{
		return this.source;
	}

	@Override
	public EventType getType()
	{
		return resolveType( getClass() );
	}
}