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
package io.coala.json;

import java.beans.PropertyChangeEvent;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.reactivex.Observable;

/**
 * {@link Attributed} tags JSON compatible java beans
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Attributed
{

	/**
	 * @return the bean properties {@link Map} as used for extended
	 *         getters/setters
	 */
	@JsonAnyGetter
	Map<String, Object> properties();

	Observable<PropertyChangeEvent> emitChanges();

	/**
	 * Useful as {@link JsonAnySetter}
	 * 
	 * @param property the property (or bean attribute) to change
	 * @param value the new value
	 * @return the previous value, as per {@link Map#put(Object, Object)}
	 */
	@JsonAnySetter
	default Object set( final String property, final Object value )
	{
		return properties().put( property, value );
	}

	/**
	 * Builder-style bean property setter
	 * 
	 * @param property the property (or bean attribute) to change
	 * @param value the new value
	 * @param THIS the concrete sub-type
	 * @return {@link THIS} type of object to allow chaining
	 */
	default <THIS extends Attributed> THIS with( final String property,
		final Object value, final Class<THIS> type )
	{
		set( property, value );
		return type.cast( this );
	}

	default <T> Observable<T> emitChanges( final String propertyName,
		final Class<T> propertyType )
	{
		return emitChanges()
				.filter( e -> propertyName.equals( e.getPropertyName() ) )
				.map( e -> e.getNewValue() ).cast( propertyType );
	}

}