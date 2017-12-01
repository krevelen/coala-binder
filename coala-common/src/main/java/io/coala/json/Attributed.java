/* $Id: db5051646ed8357e0d1cb29bd6a47d5b78eab29b $
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
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.coala.util.Comparison;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Attributed} tags JSON compatible java beans
 * 
 * @version $Id: db5051646ed8357e0d1cb29bd6a47d5b78eab29b $
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

	/**
	 * Useful as {@link JsonAnySetter}
	 * 
	 * @param property the property (or bean attribute) to change
	 * @param value the new value
	 * @return the previous value, as per {@link Map#put(Object, Object)}
	 */
	@SuppressWarnings( "unchecked" )
	@JsonAnySetter
	default <T> T set( final String property, final T value )
	{
		return (T) properties().put( property, value );
	}

	/**
	 * Builder-style bean property setter
	 * 
	 * @param property the property (or bean attribute) to change
	 * @param value the new value
	 * @return this {@link Attributed} to allow chaining
	 */
	default Attributed with( final String property, final Object value )
	{
		set( property, value );
		return this;
	}

	interface Reactive extends Attributed
	{
		Observable<PropertyChangeEvent> changeEmitter();

		default <T> Observable<T> valueEmitter( final String propertyName,
			final Class<T> propertyType )
		{
			final T currentValue = propertyType
					.cast( properties().get( propertyName ) );
			return (currentValue == null ? BehaviorSubject.<T>create()
					: BehaviorSubject.createDefault( currentValue ))
							.mergeWith( changeEmitter()
									.filter( e -> e.getPropertyName()
											.equals( propertyName ) )
									.map( e -> e.getNewValue() )
									.cast( propertyType ) );
		}

		class Simple implements Reactive
		{

			private final TreeMap<String, Object> properties = new TreeMap<>();

			private final Subject<PropertyChangeEvent> changes = PublishSubject
					.create();

			@Override
			public String toString()
			{
				return getClass().getSimpleName() + properties();
			}

			@Override
			public TreeMap<String, Object> properties()
			{
				return this.properties;
			}

			@SuppressWarnings( "unchecked" )
			@Override
			public <T> T set( final String property, final T value )
			{
				final Object previous = properties().put( property, value );
				this.changes.onNext( new PropertyChangeEvent( this, property,
						previous, value ) );
				return (T) previous;
			}

			@Override
			public Observable<PropertyChangeEvent> changeEmitter()
			{
				return this.changes;
			}
		}

		@SuppressWarnings( "rawtypes" )
		class SimpleOrdinal<C> extends Simple implements Comparable<C>
		{
			@Override
			public int compareTo( final C o )
			{
				return Comparison.compare( properties(),
						((SimpleOrdinal) o).properties() );
			}
		}
	}
}