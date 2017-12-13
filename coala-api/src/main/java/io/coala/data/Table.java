/* $Id: 8c9473f02ba2ddb5a321b89c76cbfb26bcd149f6 $
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.Logger;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import io.coala.exception.Thrower;
import io.coala.function.TriConsumer;
import io.coala.log.LogUtil;
import io.coala.log.LogUtil.Pretty;
import io.coala.math.DecimalUtil;
import io.coala.util.MapBuilder;
import io.coala.util.TypeArguments;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Table} is an {@link Iterable} layer producing
 * {@link Property}-{@link Tuple}s for manipulating values at some (remote) data
 * source using basic persistence {@link Operation}s
 * 
 * @param <T>
 */
public interface Table<T extends Table.Tuple>
	extends Map<Object, T>, Iterable<T>
{
	/** return an {@link Observable} stream of {@link Change}s */
	Observable<Change> changes();

	default Observable<Change> changes( final Object keyFilter )
	{
		return changes().filter( chg -> chg.sourceRef().equals( keyFilter ) );
	}

	@SuppressWarnings( "unchecked" )
	default Table<T> onCreate( final Consumer<T> onCreate,
		final Consumer<? super Throwable> onError )
	{
		changes().filter( chg -> chg.crud() == Operation.CREATE ).subscribe(
				chg -> onCreate.accept( (T) chg.newValue() ), onError::accept );
		return this;
	}

	@SuppressWarnings( "unchecked" )
	default Table<T> onDelete( final Consumer<T> onDelete,
		final Consumer<? super Throwable> onError )
	{
		changes().filter( chg -> chg.crud() == Operation.DELETE ).subscribe(
				chg -> onDelete.accept( (T) chg.oldValue() ), onError::accept );
		return this;
	}

	@SuppressWarnings( "unchecked" )
	default <P extends Property<V>, V> Table<T> onUpdate(
		final Class<P> propertyType, final TriConsumer<Object, V, V> onUpdate,
		final Consumer<? super Throwable> onError )
	{
		changes()
				.filter( chg -> chg.crud() == Operation.UPDATE
						&& chg.changedType() == propertyType )
				.subscribe(
						chg -> onUpdate.accept( chg.sourceRef(),
								(V) chg.oldValue(), (V) chg.newValue() ),
						onError::accept );
		return this;
	}

	@SuppressWarnings( "rawtypes" )
	Stream<Class<? extends Property>> properties();

	/** @return a new tuple */ // TODO hide this method to avoid type errors?
	@SuppressWarnings( "rawtypes" )
	T insertValues( Map<Class<? extends Property>, Object> values );

	@SuppressWarnings( "rawtypes" )
	default T insertValues( final UnaryOperator<PropertySetter> mapOp )
	{
		final MapBuilder<Class<? extends Property>, Object, ?> map = MapBuilder
				.unordered();
		mapOp.apply( PropertySetter.ofMapBuilder( map ) );
		return insertValues( map.build() );
	}

	/** @return a new tuple */
	@SuppressWarnings( "rawtypes" )
	default T insertValues( final Stream<Property> values )
	{
		return insertValues( values.collect(
				Collectors.<Property, Class<? extends Property>, Object>toMap(
						Property::getClass, Property::get ) ) );
	}

	/** @return a new tuple */
	default T insert()
	{
		return insertValues( Collections.emptyMap() );
	}

	/** @return a new tuple */
	@SuppressWarnings( "rawtypes" )
	default <P extends Property> T insert( final P property )
	{
		return insertValues( Collections.singletonMap( property.getClass(),
				property.get() ) );
	}

	/** @return a new tuple */
	@SuppressWarnings( "rawtypes" )
	default T insert( final Property... properties )
	{
		return properties == null ? insert()
				: insertValues( Arrays.stream( properties ) );
	}

	<P extends Property<V>, V> boolean equals( T tuple, Class<P> property,
		V value );

	/** @param key */
	boolean delete( Object key );

	/** @return a {@link Stream} of available keys */
	Stream<?> keys();

	/**
	 * @param key
	 * @return a {@link Tuple}
	 */
	T select( Object key );

	default Stream<T> select( final Object... keys )
	{
		return Arrays.stream( keys ).map( this::select );
	}

	@SuppressWarnings( "unchecked" )
	default Observable<T> creation()
	{
		return changes().filter( chg -> chg.crud() == Operation.CREATE )
				.map( chg -> (T) chg.newValue() );
	}

	@SuppressWarnings( "unchecked" )
	default Observable<T> deletion()
	{
		return changes().filter( chg -> chg.crud() == Operation.DELETE )
				.map( chg -> (T) chg.oldValue() );
	}

	@Override
	default boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	default boolean containsKey( final Object key )
	{
		return select( key ) != null;
	}

	@Override
	default boolean containsValue( final Object value )
	{
		return stream().anyMatch( v -> v.equals( value ) );
	}

	@Override
	default T get( final Object key )
	{
		return select( key );
	}

	/** @deprecated use {@link Change#delete(Object)} instead */
	@Deprecated
	@Override
	default T remove( final Object key )
	{
		final T old = select( key );
		if( old == null ) return null;
		delete( key );
		return old; // FIXME return detached tuple with cloned state
	}

	@SuppressWarnings( "unchecked" )
	@Override
	default T put( final Object toKey, final T fromTuple )
	{
		if( fromTuple.key().equals( toKey ) ) return fromTuple;
		final T oldTuple = select( toKey ),
				toTuple = oldTuple == null ? insert() : oldTuple;
		properties().forEach( p ->
		{
			// swap non-null values
			final Object oldValue = toTuple.get( p );
			if( oldValue == null ) return;
			final Object newValue = fromTuple.get( p );
			toTuple.set( p, newValue );
			fromTuple.set( p, oldValue );
		} );
		return fromTuple; // FIXME return detached tuple with cloned state?
	}

	@Override
	default void putAll( final Map<? extends Object, ? extends T> m )
	{
		m.forEach( this::put );
	}

	/** @deprecated use {@link #keys()} instead */
	@Deprecated
	@Override
	default public Set<Object> keySet()
	{
		return keys().collect( Collectors.toSet() );
	}

	/** @deprecated use {@link #stream()} instead */
	@Deprecated
	@Override
	default Collection<T> values()
	{
		return stream().collect( Collectors.toList() );
	}

	/** @deprecated use {@link #forEach(BiConsumer)} instead */
	@Deprecated
	@Override
	default Set<Map.Entry<Object, T>> entrySet()
	{
		return keys()
				.map( key -> org.aeonbits.owner.util.Collections
						.<Object, T>entry( key, select( key ) ) )
				.collect( Collectors.toSet() );
	}

	@Override
	default void
		forEach( final BiConsumer<? super Object, ? super T> keyTupleVisitor )
	{
		keys().forEach( key -> keyTupleVisitor.accept( key, select( key ) ) );
	}

	default void forEachLazy(
		final BiConsumer<? super Object, Supplier<T>> keyTupleVisitor )
	{
		keys().forEach(
				key -> keyTupleVisitor.accept( key, () -> select( key ) ) );
	}

	default <K extends Property<V>, V> void forEachValue(
		final Class<K> propertyType,
		final BiConsumer<Object, V> keyValueVisitor )
	{
		keys().forEach( key -> keyValueVisitor.accept( key,
				select( key ).get( propertyType ) ) );
	}

	default Stream<T> stream()
	{
		return keys().map( this::select );
	}

	@SuppressWarnings( { "rawtypes" } )
	default Map<Class<? extends Property>, Object>
		selectAsMap( final Object key )
	{
		final T t = select( key );
		if( t == null ) return null;
		return t.toMap( properties() );
	}

	default <K extends Property<V>, V> Stream<V>
		values( final Class<K> property )
	{
		return stream().map( t -> t.get( property ) );
	}

	@Override
	default Iterator<T> iterator()
	{
		return stream().iterator();
	}

	default <K extends Property<V>, V> V selectValue( final Object key,
		final Class<K> property )
	{
		return select( key ).get( property );
	}

	default Stream<T> selectWhere( final Predicate<? super T> filter )
	{
		return stream().filter( filter );
	}

	default Stream<T> selectWhere( final Property<?> property )
	{
		return selectWhere( t -> t.isEqual( property ) );
	}

	@SuppressWarnings( { "rawtypes" } )
	default Stream<T> selectAnd( final Property... filter )
	{
		if( filter == null || filter.length == 0 ) return Stream.empty();
		if( filter.length == 1 ) return selectWhere( filter[0] );
		return selectWhere( t ->
		{
			for( int i = 0; i < filter.length; i++ )
				if( !t.isEqual( filter[i] ) ) return false;
			return true;
		} );
	}

	/**
	 * {@link PropertySetter} enables type-safe property setting for
	 * {@link Table#insertValues(UnaryOperator)}
	 */
	interface PropertySetter
	{
		<P extends Property<? super V>, V> PropertySetter
			set( Class<P> propertyType, V value );

		static PropertySetter ofMapBuilder(
			@SuppressWarnings( "rawtypes" ) final MapBuilder<Class<? extends Property>, Object, ?> map )
		{
			return new PropertySetter()
			{
				@Override
				public <P extends Property<? super V>, V> PropertySetter
					set( final Class<P> propertyType, final V value )
				{
					map.put( propertyType, value );
					return this;
				}
			};
		}
	}

	@SuppressWarnings( { "rawtypes" } )
	default Stream<T> selectOr( final Property... filter )
	{
		if( filter == null || filter.length == 0 ) return Stream.empty();
		if( filter.length == 1 ) return selectWhere( filter[0] );
		return selectWhere( t ->
		{
			for( int i = 0; i < filter.length; i++ )
				if( t.isEqual( filter[i] ) ) return true;
			return false;
		} );
	}

	/**
	 * {@link Operation} enumerates four persistence operations, see <a href=
	 * "https://www.wikiwand.com/en/Create,_read,_update_and_delete">CRUD</a>
	 */
	enum Operation
	{
		CREATE, READ, UPDATE, DELETE;
	}

	/**
	 * {@link Property} wraps a value of a {@link Tuple} in some {@link Table}
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

		@SuppressWarnings( "unchecked" )
		default Class<T> returnType()
		{
			return (Class<T>) returnType( this );
		}

		Map<Class<?>, Class<?>> RETURN_TYPE_CACHE = new HashMap<>();

		static Class<?> returnType( final Property<?> property )
		{
			return returnType( Objects.requireNonNull( property ).getClass() );
		}

		/**
		 * @param propertyType the concrete (run-time) type to reflect on
		 * @return the (cached) type of the (run-time) type argument
		 */
		@SuppressWarnings( { "rawtypes" } )
		static Class<?>
			returnType( final Class<? extends Property> propertyType )
		{
			return RETURN_TYPE_CACHE.computeIfAbsent( propertyType,
					k -> TypeArguments.of( Property.class, propertyType )
							.get( 0 ) );
		}

	}

	/**
	 * {@link Change} notifies modifications made to e.g. a {@link Property},
	 * {@link Tuple}, {@link Table} or their sub-types
	 */
	class Change
	{
		private final Operation crud;

		private final Object sourceRef;

		private final Class<?> changedType;

		private final Object oldValue;

		private final Object newValue;

		public Change( final Operation crud, final Object sourceRef,
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

		public Operation crud()
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

	/**
	 * {@link Tuple} represents a {@link Property} set from a row in a
	 * {@link Table}
	 */
	@SuppressWarnings( "rawtypes" )
	class Tuple
	{

		private static final char start = '[', end = ']', eq = ':';
		private static final String delim = "; ";

		private Object key;

		private Observer<Change> emitter;

		private Function<Class<? extends Property>, Object> getter;

		private BiConsumer<Class<? extends Property>, Object> setter;

		private Supplier<String> stringifier;

		public Tuple reset( final Object key, final Observer<Change> emitter,
			final Function<Class<? extends Property>, Object> getter,
			final BiConsumer<Class<? extends Property>, Object> setter,
			final Supplier<String> stringifier )
		{
			this.key = key;
			this.emitter = emitter;
			this.getter = getter;
			this.setter = setter;
			this.stringifier = stringifier;
			return this;
		}

		public List<Class<? extends Property>> properties()
		{
			return Collections.emptyList();
		}

		@Override
		public String toString()
		{
			return this.stringifier.get();
		}

		@SuppressWarnings( "unchecked" )
		public Pretty pretty( final Class<? extends Property>... properties )
		{
			return pretty( properties == null || properties.length == 0
					|| properties[0] == null ? null
							: Arrays.stream( properties ) );
		}

		public Pretty
			pretty( final Iterable<Class<? extends Property>> properties )
		{
			return pretty( properties == null ? null
					: StreamSupport.stream( properties.spliterator(), false ) );
		}

		@SuppressWarnings( "unchecked" )
		public Pretty
			pretty( final Stream<Class<? extends Property>> properties )
		{
			return Pretty.of( () -> properties == null ? toString()
					: "#" + key() + properties
							.<StringBuilder>map(
									p -> new StringBuilder( p.getSimpleName() )
											.append( eq )
											.append( String.valueOf(
													(Object) get( p ) ) ) )
							.collect( () -> new StringBuilder(), ( s,
								v ) -> (s.length() == 0 ? s.append( start )
										: s.append( delim )).append( v ),
									( s1, s2 ) -> s1.append( delim )
											.append( s2 ) )
							.append( end ) );
		}

		public Object key()
		{
			return this.key;
		}

		@SuppressWarnings( "unchecked" )
		public Map<Class<? extends Property>, Object>
			toMap( final Class<? extends Property>... properties )
		{
			return properties == null || properties.length == 0
					|| properties[0] == null ? Collections.emptyMap()
							: toMap( Arrays.stream( properties ) );
		}

		@SuppressWarnings( "unchecked" )
		public Map<Class<? extends Property>, Object>
			toMap( final Iterable<Class<? extends Property>> properties )
		{
			return properties == null ? Collections.emptyMap()
					: toMap( StreamSupport.stream( properties.spliterator(),
							false ) );
		}

		@SuppressWarnings( "unchecked" )
		public Map<Class<? extends Property>, Object>
			toMap( final Stream<Class<? extends Property>> properties )
		{
			return properties.collect( Collectors.toMap( p -> p, this::get ) );
		}

		@SuppressWarnings( "unchecked" )
		public <K extends Property<V>, V> V put( final K property )
		{
			return getAndUpdate( (Class<K>) property.getClass(),
					v -> property.get() );
		}

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

		public <K extends Property<V>, V> void set( final Class<K> propertyType,
			final V value )
		{
			this.setter.accept( propertyType, value );
		}

		public <K extends Property<V>, V> V updateAndGet(
			final Class<K> propertyType, final UnaryOperator<V> op )
		{
			final V oldValue = this.get( propertyType ),
					updated = op.apply( oldValue );
			if( updated != oldValue )
			{
				set( propertyType, updated );
				if( this.emitter != null )
					this.emitter.onNext( new Change( Operation.UPDATE, key(),
							propertyType, oldValue, updated ) );
			}
			return updated;
		}

		public <K extends Property<V>, V> V getAndUpdate(
			final Class<K> propertyType, final UnaryOperator<V> op )
		{
			final V oldValue = get( propertyType ),
					updated = op.apply( oldValue );
			if( updated != oldValue )
			{
				set( propertyType, updated );
				if( this.emitter != null )
					this.emitter.onNext( new Change( Operation.UPDATE, key(),
							propertyType, oldValue, updated ) );
			}
			return oldValue;
		}

		@SuppressWarnings( "unchecked" )
		public <K extends Property<V>, V> V get( final Class<K> key )
		{
			return (V) this.getter.apply( key );
		}

		public <K extends Property<V>, V> V get( final Class<K> key,
			final V defaultValue )
		{
			final V value = get( key );
			return value == null ? defaultValue : value;
		}

		public <K extends Property<V>, V> V getNonNull( final Class<K> key )
		{
			final V value = get( key );
			return value == null ? Thrower.throwNew( IllegalStateException::new,
					() -> "Missing " + key() + "::" + key.getSimpleName() )
					: value;
		}

		@SuppressWarnings( "unchecked" )
		public <THIS extends Tuple, P extends Property<V>, V> THIS
			with( final P property )
		{
			return (THIS) with( property.getClass(), property.get() );
		}

		@SuppressWarnings( "unchecked" )
		public <THIS extends Tuple, P extends Property<V>, V> THIS
			with( final Class<P> propertyType, final V value )
		{
			while( !match( propertyType, Matchers.equalTo( value ) ) )
				this.setter.accept( propertyType, value );
			return (THIS) this;
		}

		public <P extends Property<V>, V> boolean
			match( final Class<P> propertyType, final Matcher<V> test )
		{
			return test.matches( get( propertyType ) );
		}

		public <P extends Property<V>, V> boolean
			isEqual( final Class<P> propertyType, final V equals )
		{
			return match( propertyType, Matchers.equalTo( equals ) );
		}

		@SuppressWarnings( "unchecked" )
		public boolean isEqual( final Property<?> property )
		{
			return isEqual( property.getClass(), property.get() );
		}

		public <P extends Property<W>, W> Tuple
			override( final Class<P> property, final W override )
		{
			final Tuple self = this;
			return new Tuple()
			{
				@Override
				@SuppressWarnings( "unchecked" )
				public <K extends Property<V>, V> V get( final Class<K> key )
				{
					return key == property ? (V) override : super.get( key );
				}

				@Override
				public List<Class<? extends Property>> properties()
				{
					return self.properties();
				}
			}.reset( this.key, this.emitter, this.getter, this.setter,
					this.stringifier );
		}

	}

	class Simple<PK, T extends Tuple> implements Table<T>
	{
		/** */
		private static final Logger LOG = LogUtil
				.getLogger( Table.Simple.class );

		@SuppressWarnings( "rawtypes" )
		private final Supplier<Stream<Class<? extends Property>>> properties;

		private final Supplier<PK> adder;

		private final Consumer<PK> deleter;

		private final Supplier<Stream<PK>> indexer;

		private final BiFunction<PK, Observer<Change>, T> retriever;

		private final IntSupplier counter;

		private final Supplier<String> printer;

		private final Runnable cleaner;

		private final Subject<Change> emitter = PublishSubject.create();

		public Simple(
			@SuppressWarnings( "rawtypes" ) final Supplier<Stream<Class<? extends Property>>> properties,
			final Supplier<PK> adder, final Consumer<PK> remover,
			final Supplier<Stream<PK>> indexer,
			final BiFunction<PK, Observer<Change>, T> retriever,
			final IntSupplier counter, final Supplier<String> printer,
			final Runnable cleaner )
		{
			this.properties = properties;
			this.adder = adder;
			this.deleter = remover;
			this.indexer = indexer;
			this.retriever = retriever;
			this.counter = counter;
			this.printer = printer;
			this.cleaner = cleaner;
		}

		@Override
		public String toString()
		{
			return this.printer.get();
		}

		@Override
		public Observable<Change> changes()
		{
			return this.emitter;
		}

		@Override
		public Stream<?> keys()
		{
			return this.indexer.get();
		}

		private static final int MAX_REPAIRS = 5;

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public T insertValues(
			final Map<Class<? extends Property>, Object> properties )
		{
			final T result = this.retriever.apply( this.adder.get(), null );
			if( properties != null && !properties.isEmpty() )
			{
				int tries = 0;
				repair: while( tries++ < MAX_REPAIRS )
				{
					properties.forEach( result::set );

					for( Map.Entry<Class<? extends Property>, Object> e : properties
							.entrySet() )
						if( !equals( result, e.getKey(), e.getValue() ) )
						{
							LOG.warn(
									"Inconsistent, try: {}: {}<-{}, stored: {}",
									tries, e.getKey().getSimpleName(),
									e.getValue(), result.get( e.getKey() ) );
							continue repair;
						}

					break repair;
				}
				if( tries == MAX_REPAIRS )
					Thrower.throwNew( IllegalStateException::new,
							() -> "Failed to repair #" + result.key() + ": "
									+ result + " <- " + properties.values() );
			}
			this.emitter.onNext( new Change( Operation.CREATE, result.key(),
					result.getClass(), null, result ) );
			return result;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public T select( final Object key )
		{
			return this.retriever.apply( (PK) key, this.emitter );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public boolean delete( final Object key )
		{
			final T old = select( key );
			if( old == null ) return false;
			// FIXME publish detached tuple of cloned state? use #override(Map)
			this.emitter.onNext( new Change( Operation.DELETE, key,
					old.getClass(), old, null ) );
			this.deleter.accept( (PK) key );
			return true;
		}

		@Override
		public int size()
		{
			return this.counter.getAsInt();
		}

		@Override
		public void clear()
		{
			this.cleaner.run();
		}

		@SuppressWarnings( "rawtypes" )
		@Override
		public Stream<Class<? extends Property>> properties()
		{
			return this.properties.get();
		}

		@Override
		public <P extends Property<V>, V> boolean equals( final T tuple,
			final Class<P> property, final V value )
		{
			final V v = tuple.get( property );
			return v == null ? (value == null || (value instanceof Number
					&& DecimalUtil.valueOf( (Number) value ).signum() == 0))
					: v.equals( value );
		}
	}
}