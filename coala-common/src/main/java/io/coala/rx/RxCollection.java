/* $Id$
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
 */
package io.coala.rx;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Iterator;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link RxCollection} is a {@link Collection} that emits events on content
 * changes
 * 
 * TODO create RxMap extend {@link Map} that emits {@link PropertyChangeEvent}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface RxCollection<T> extends Collection<T>
{

	Observable<T> onAdd();

	Observable<T> onRemove();

	Observable<Integer> onSize();

	static <T> RxCollection<T> of( final Collection<T> source )
	{
		return new RxCollection<T>()
		{

			private final Subject<T> add = PublishSubject.create();

			private final Subject<T> remove = PublishSubject.create();

			private final Subject<Integer> size = PublishSubject.create();

			@Override
			public Observable<T> onAdd()
			{
				return this.add;
			}

			@Override
			public Observable<T> onRemove()
			{
				return this.remove;
			}

			@Override
			public Observable<Integer> onSize()
			{
				return this.size;
			}

			@Override
			public int size()
			{
				return source.size();
			}

			@Override
			public boolean isEmpty()
			{
				return source.isEmpty();
			}

			@Override
			public boolean contains( final Object o )
			{
				return source.contains( o );
			}

			@Override
			public Iterator<T> iterator()
			{
				final Iterator<T> result = source.iterator();
				return new Iterator<T>()
				{
					private T current = null;

					@Override
					public boolean hasNext()
					{
						return result.hasNext();
					}

					@Override
					public void remove()
					{
						result.remove();
						remove.onNext( this.current );
					}

					@Override
					public T next()
					{
						return (this.current = result.next());
					}
				};
			}

			@Override
			public Object[] toArray()
			{
				return source.toArray();
			}

			@SuppressWarnings( "hiding" )
			@Override
			public <T> T[] toArray( final T[] a )
			{
				return source.toArray( a );
			}

			@Override
			public boolean add( final T e )
			{
				final boolean result = source.add( e );
				if( result )
				{
					this.add.onNext( e );
					this.size.onNext( source.size() );
				}
				return result;
			}

			@SuppressWarnings( "unchecked" )
			@Override
			public boolean remove( final Object e )
			{
				final boolean result = source.remove( e );
				if( result )
				{
					this.remove.onNext( (T) e );
					this.size.onNext( source.size() );
				}
				return result;
			}

			@Override
			public boolean containsAll( final Collection<?> c )
			{
				return source.containsAll( c );
			}

			@Override
			public boolean addAll( final Collection<? extends T> c )
			{
				boolean result = false;
				for( T t : c )
					result |= add( t );
				return result;
			}

			@Override
			public boolean removeAll( final Collection<?> c )
			{
				boolean result = false;
				for( Object t : c )
					result |= remove( t );
				return result;
			}

			@Override
			public boolean retainAll( final Collection<?> c )
			{
				return removeIf( e ->
				{
					return !c.contains( e );
				} );
			}

			@Override
			public void clear()
			{
				// calls iterator().remove()
				if( size() != 0 )
				{
					removeIf( e ->
					{
						return true;
					} );
					this.size.onNext( 0 );
				}
			}
		};
	}
}
