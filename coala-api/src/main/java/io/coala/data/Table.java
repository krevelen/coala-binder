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
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.coala.exception.Thrower;
import io.coala.util.TypeArguments;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Table}
 * 
 * @param <ID> key type, for consistency
 * @param <T>
 */
public interface Table<T extends Table.Tuple> extends Iterable<T>
{
	/** return an {@link Observable} stream of {@link Change}s */
	Observable<Change> changes();

	/** @return a new tuple */
	T create( Property<?>... properties );

	/** @param key */
	void remove( Object key );

	/**
	 * @param key
	 * @return a {@link Tuple}
	 */
	T select( Object key );

	/** @return a {@link Stream} of available keys */
	Stream<?> keys();

	default Stream<T> stream()
	{
		return keys().map( this::select );
	}

	default <K extends Property<V>, V> Stream<V>
		values( final Class<K> property )
	{
		return stream().map( t -> t.get( property ) );
	}

	@SuppressWarnings( "unchecked" )
	default <K extends Property<V>, V> Observable<V>
		valueChanges( final Object keyFilter, final Class<K> property )
	{
		return changes( keyFilter, property ).map( chg -> (V) chg.newValue() );
	}

	@Override
	default Iterator<T> iterator()
	{
		return stream().iterator();
	}

	default Stream<T> selectWhere( final Predicate<? super T> filter )
	{
		return stream().filter( filter );
	}

	default Stream<T> selectWhere( final Property<?> property )
	{
		return selectWhere( t -> t.match( property ) );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	default Stream<T> selectAnd( final Property... filter )
	{
		if( filter == null || filter.length == 0 ) return Stream.empty();
		if( filter.length == 1 ) return selectWhere( filter[0] );
		return selectWhere( t ->
		{
			for( int i = 0; i < filter.length; i++ )
				if( !t.match( filter[i] ) ) return false;
			return true;
		} );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	default Stream<T> selectOr( final Property... filter )
	{
		if( filter == null || filter.length == 0 ) return Stream.empty();
		if( filter.length == 1 ) return selectWhere( filter[0] );
		return selectWhere( t ->
		{
			for( int i = 0; i < filter.length; i++ )
				if( t.match( filter[i] ) ) return true;
			return false;
		} );
	}

	@SuppressWarnings( "unchecked" )
	default boolean contains( final Object key )
	{
		return select( key ) != null;
	}

	default void forEach( final BiConsumer<Object, T> keyTupleVisitor )
	{
		keys().forEach( key -> keyTupleVisitor.accept( key, select( key ) ) );
	}

	default <K extends Property<V>, V> void forEach(
		final Class<K> propertyType,
		final BiConsumer<Object, V> keyValueVisitor )
	{
		keys().forEach( ownerRef -> keyValueVisitor.accept( ownerRef,
				select( ownerRef ).get( propertyType ) ) );
	}

	default Observable<Change> changes( final Object keyFilter )
	{
		return changes().filter( chg -> chg.sourceRef().equals( keyFilter ) );
	}

	default <K extends Property<?>> Observable<Change> changes(
		final Object keyFilter, final Class<? extends Property<?>> key )
	{
		return changes( keyFilter )
				.filter( chg -> chg.changedType().equals( key ) );
	}

	@SuppressWarnings( "unchecked" )
	default Observable<T> creation()
	{
		return changes().filter( chg -> chg.crud() == CRUD.CREATE )
				.map( chg -> (T) chg.newValue() );
	}

	@SuppressWarnings( "unchecked" )
	default Observable<T> deletion()
	{
		return changes().filter( chg -> chg.crud() == CRUD.DELETE )
				.map( chg -> (T) chg.oldValue() );
	}

	enum CRUD
	{
		CREATE, READ, UPDATE, DELETE;
	}

	/**
	 * {@link Property} of a {@link Tuple} in a {@link Table}
	 * 
	 * @param <T> the value return type
	 */
	interface Property<T>
	{
		T get();

		void set( T newValue );

		@SuppressWarnings( "unchecked" )
		default <THIS extends Property<T>> THIS with( T newValue )
		{
			set( newValue );
			return (THIS) this;
		}

		Map<Class<?>, Class<?>> RETURN_TYPE_CACHE = new HashMap<>();

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		static Class<?>
			returnType( final Class<? extends Property> propertyType )
		{
			return RETURN_TYPE_CACHE.computeIfAbsent( propertyType,
					k -> TypeArguments.of( Property.class, propertyType )
							.get( 0 ) );
		}

	}

	/**
	 * {@link Change}
	 */
	class Change
	{
		private final CRUD crud;

		private final Object sourceRef;

		private final Class<?> changedType;

		private final Object oldValue;

		private final Object newValue;

		public Change( final CRUD crud, final Object sourceRef,
			final Class<?> changedType, final Object oldValue,
			final Object newValue )
		{
			this.crud = crud;
			this.sourceRef = sourceRef;
			this.changedType = changedType;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		@Override
		public String toString()
		{
			return crud() + " #" + sourceRef() + " "
					+ changedType().getSimpleName() + ": " + oldValue() + " -> "
					+ newValue();
		}

		public CRUD crud()
		{
			return this.crud;
		}

		public Object sourceRef()
		{
			return this.sourceRef;
		}

		public Class<?> changedType()
		{
			return this.changedType;
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

		private Object key;

		private Observer<Change> emitter;

		private Function<Class<? extends Property>, Object> getter;

		private BiConsumer<Class<? extends Property>, Object> setter;

		private Supplier<String> stringifier;

		public Tuple reset( final Object ownerRef,
			final Observer<Change> emitter,
			final Function<Class<? extends Property>, Object> getter,
			final BiConsumer<Class<? extends Property>, Object> setter,
			final Supplier<String> stringifier )
		{
			this.key = ownerRef;
			this.emitter = emitter;
			this.getter = getter;
			this.setter = setter;
			this.stringifier = stringifier;
			return this;
		}

		@Override
		public String toString()
		{
			return this.stringifier.get();
		}

		public Object key()
		{
			return this.key;
		}

		public <K extends Property<V>, V> V put( final K property )
		{
			@SuppressWarnings( "unchecked" )
			final V old = (V) get( (Class<K>) property.getClass() );
			set( property );
			return old;
		}

		@SuppressWarnings( "rawtypes" )
		public void set( final Property... properties )
		{
			if( properties == null || properties.length == 0 ) return;
			for( int i = 0; i < properties.length; i++ )
				set( properties[i] );
		}

		@SuppressWarnings( "unchecked" )
		public void set( final Property<?> property )
		{
			set( property.getClass(), property.get() );
		}

		@SuppressWarnings( "unchecked" )
		public void set( final Class<? extends Property> propertyType,
			final Object value )
		{
			final Object oldValue = this.getter.apply( propertyType );
			this.setter.accept( propertyType, value );
			if( this.emitter != null && value != oldValue )
				this.emitter.onNext( new Change( CRUD.UPDATE, key(),
						propertyType, oldValue, value ) );
		}

		@SuppressWarnings( "unchecked" )
		public <K extends Property<V>, V> V get( final Class<K> key )
		{
			return (V) this.getter.apply( key );
		}

		@SuppressWarnings( "unchecked" )
		public <K extends Property<V>, V> V getNonNull( final Class<K> key )
		{
			final V value = get( key );
			return value == null ? Thrower.throwNew( IllegalStateException::new,
					() -> "Missing " + key() + "::" + key.getSimpleName() )
					: value;
		}

		@SuppressWarnings( "unchecked" )
		public <THIS extends Tuple> THIS with( final Property<?> property )
		{
			set( property );
			return (THIS) this;
		}

		@SuppressWarnings( "unchecked" )
		public <THIS extends Tuple, P extends Property<V>, V> THIS
			with( final Class<P> propertyType, final V value )
		{
			set( propertyType, value );
			return (THIS) this;
		}

		@SuppressWarnings( "unchecked" )
		public boolean match( final Property<?> property )
		{
			return get( property.getClass() ).equals( property.get() );
		}

	}

	class Simple<PK, T extends Tuple> implements Table<T>
	{
		private final Subject<Change> emitter = PublishSubject.create();

		private final Supplier<PK> adder;

		private final Consumer<PK> remover;

		private final Supplier<Stream<PK>> keyStreamer;

		private final BiFunction<PK, Observer<Change>, T> retriever;

		private final Supplier<String> stringifier;

		public Simple( final Supplier<PK> adder, final Consumer<PK> remover,
			final Supplier<Stream<PK>> keyStreamer,
			final BiFunction<PK, Observer<Change>, T> retriever,
			final Supplier<String> stringifier )
		{
			this.adder = adder;
			this.remover = remover;
			this.keyStreamer = keyStreamer;
			this.retriever = retriever;
			this.stringifier = stringifier;
		}

		@Override
		public String toString()
		{
			return this.stringifier.get();
		}

		@Override
		public Observable<Change> changes()
		{
			return this.emitter;
		}

		@Override
		public Stream<?> keys()
		{
			return this.keyStreamer.get();
		}

		@SuppressWarnings( "unchecked" )
		public T create( final Property<?>... properties )
		{
			final T result = this.retriever.apply( this.adder.get(), null );
			if( properties != null && properties.length > 0 )
				for( Property<?> property : properties )
				result.set( property );
//			if( !Long.valueOf( 0 ).equals( result.key() ) )
			this.emitter.onNext( new Change( CRUD.CREATE, result.key(),
					result.getClass(), null, result ) );
			return result;
		}

		@SuppressWarnings( "unchecked" )
		public T select( final Object key )
		{
			return this.retriever.apply( (PK) key, this.emitter );
		}

		@SuppressWarnings( "unchecked" )
		public void remove( final Object key )
		{
			final T old = select( key );
			this.remover.accept( (PK) key );
			this.emitter.onNext(
					new Change( CRUD.DELETE, key, old.getClass(), old, null ) );
		}
	}

}