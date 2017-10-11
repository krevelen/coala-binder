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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.coala.util.TypeArguments;
import io.reactivex.Observer;

/**
 * {@link Property}
 * 
 * @param <T> the value return type
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Property<T>
{
	T get();

	void set( T newValue );

	@SuppressWarnings( "unchecked" )
	default <THIS extends Property<T>> THIS with( T newValue )
	{
		set( newValue );
		return (THIS) this;
	}

	@SuppressWarnings( "unchecked" )
	default Class<? extends T> valueType()
	{
		final T value = get();
		return value == null ? null : (Class<? extends T>) value.getClass();
	}

	Map<Class<?>, Class<?>> RETURN_TYPE_CACHE = new HashMap<>();

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	static Class<?> returnType( final Class<? extends Property> propertyType )
	{
		return RETURN_TYPE_CACHE.computeIfAbsent( propertyType,
				k -> TypeArguments.of( Property.class, propertyType )
						.get( 0 ) );
	}

	/**
	 * {@link Change}
	 */
	class Change
	{
		private final Object sourceRef;

		private final Class<?> key;

		private final Object oldValue;

		private final Object newValue;

		public <K extends Property<V>, V> Change( final Object sourceRef,
			final Class<K> key, final V oldValue, final V newValue )
		{
			this.sourceRef = sourceRef;
			this.key = key;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		@Override
		public String toString()
		{
			return sourceRef() + "::" + key().getSimpleName() + "::"
					+ oldValue() + "->" + newValue();
		}

		public Object sourceRef()
		{
			return this.sourceRef;
		}

		public Class<?> key()
		{
			return this.key;
		}

		public Object oldValue()
		{
			return this.oldValue;
		}

		public Object newValue()
		{
			return this.newValue;
		}
	}

	@SuppressWarnings( "rawtypes" )
	class Tuple
	{

		private Object ownerRef;

		private Observer<Property.Change> emitter;

		private Function<Class<? extends Property>, Object> getter;

		private BiConsumer<Class<? extends Property>, Object> setter;

		private Supplier<String> stringifier;

		@Override
		public String toString()
		{
			return this.stringifier.get();
		}

		public Object key()
		{
			return this.ownerRef;
		}

		@SuppressWarnings( "unchecked" )
		public void set( final Property<?> property )
		{
			final Object oldValue = this.getter.apply( property.getClass() );
			final Property.Change chg = new Property.Change( key(),
					property.getClass(), oldValue, property.get() );
			this.setter.accept( property.getClass(), property.get() );
			this.emitter.onNext( chg );
		}

		@SuppressWarnings( "unchecked" )
		public <K extends Property<V>, V> V get( final Class<K> key )
		{
			return (V) this.getter.apply( key );
		}

		public Property.Tuple reset( final Object ownerRef,
			final Observer<Property.Change> emitter,
			final Function<Class<? extends Property>, Object> getter,
			final BiConsumer<Class<? extends Property>, Object> setter,
			final Supplier<String> stringifier )
		{
			this.ownerRef = ownerRef;
			this.emitter = emitter;
			this.getter = getter;
			this.setter = setter;
			this.stringifier = stringifier;
			return this;
		}

		@SuppressWarnings( "rawtypes" )
		public void set( final Property... properties )
		{
			if( properties == null || properties.length == 0 ) return;
			for( int i = 0; i < properties.length; i++ )
				set( properties[i] );
		}

		@SuppressWarnings( "unchecked" )
		public boolean match( final Property<?> property )
		{
			return get( property.getClass() ).equals( property.get() );
		}

		public <K extends Property<V>, V> V put( final K property )
		{
			@SuppressWarnings( "unchecked" )
			final V old = (V) get( (Class<K>) property.getClass() );
			set( property );
			return old;
		}
	}

}