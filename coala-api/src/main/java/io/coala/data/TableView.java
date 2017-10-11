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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.coala.data.Property.Change;
import io.coala.data.Property.Tuple;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link TableView}
 * 
 * @param <ID>
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class TableView<ID, T extends Tuple>
{
	private final Subject<Property.Change> emitter = PublishSubject.create();

	private final Supplier<ID> adder;

	private final Consumer<ID> remover;

	private final Supplier<Stream<ID>> keyStreamer;

	private final BiFunction<ID, Observer<Property.Change>, T> retriever;

	public TableView( final Supplier<ID> adder, final Consumer<ID> remover,
		final Supplier<Stream<ID>> keyStreamer,
		final BiFunction<ID, Observer<Property.Change>, T> retriever )
	{
		this.adder = adder;
		this.remover = remover;
		this.keyStreamer = keyStreamer;
		this.retriever = retriever;
	}

	public Observable<Change> emit()
	{
		return this.emitter;
	}

	public Stream<ID> keys()
	{
		return this.keyStreamer.get();
	}

	public T select( final ID ownerRef )
	{
		return this.retriever.apply( ownerRef, this.emitter );
	}

	@SuppressWarnings( "unchecked" )
	public void remove( final T view )
	{
		this.remover.accept( (ID) view.key() );
	}

	public Stream<T> selectAll()
	{
		return keys().map( this::select );
	}

	public Stream<T> selectWhere( final Predicate<? super T> filter )
	{
		return selectAll().filter( filter );
	}

	public Stream<T> selectWhere( final Property<?> property )
	{
		return selectWhere( t -> t.match( property ) );
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public Stream<T> selectAnd( final Property... filter )
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
	public Stream<T> selectOr( final Property... filter )
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

	public Observable<Property.Change> emit( final Class<?> key )
	{
		return emit().filter( chg -> key.isAssignableFrom( chg.key() ) );
	}

	public ID create()
	{
		return this.adder.get();
	}

	public boolean contains( final ID ownerRef )
	{
		return select( ownerRef ) != null;
	}

	public <K extends Property<V>, V> void forEach( final Class<K> key,
		final BiConsumer<ID, Object> visitor )
	{
		keys().forEach( ownerRef -> visitor.accept( ownerRef,
				select( ownerRef ).get( key ) ) );
	}

	public Observable<Property.Change> emit( final ID sourceRef )
	{
		return emit().filter( chg -> chg.sourceRef().equals( sourceRef ) );
	}

	public <K extends Property<?>> Observable<Property.Change>
		emit( final ID ownerRef, final Class<? extends Property<?>> key )
	{
		return emit( ownerRef ).filter( chg -> chg.key().equals( key ) );
	}

	@SuppressWarnings( "unchecked" )
	public <K extends Property<V>, V> Observable<V>
		emitValues( final ID ownerRef, final Class<K> key )
	{
		return emit( ownerRef, key ).map( chg -> (V) chg.newValue() );
	}
}