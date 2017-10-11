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
package io.coala.enterprise;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.Fact.ID;
import io.coala.enterprise.persist.FactDao;
import io.coala.math.QuantityUtil;
import io.coala.math.Range;
import io.coala.persist.JPAUtil;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.reactivex.Observable;

/**
 * {@link FactBank} provides Fact persistence via {@link #saveAsync(Observable)}
 * 
 * <ul>
 * Reference implementations:
 * <li>{@link SimpleCache}
 * <li>{@link SimpleJPA} using the {@link FactDao}
 * <a href="https://www.wikiwand.com/en/Data_access_object">data access
 * object</a>
 * <li>SimpleORM (TODO, using e.g. <a href="http://ormlite.com/">ORMlite</a> or
 * <a href="https://empire-db.apache.org">Empire-DB</a>)
 * </ul>
 * 
 * @param <F> the root {@link Fact} type supported
 * @param <DAO> the type of data access object
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface FactBank<F extends Fact> extends AutoCloseable
{

	/** @return a type-argument free root instance, useful for casting */
	FactBank<Fact> root();

	/**
	 * @param facts the {@link Observable} stream of {@link Fact facts} to store
	 *            lazily (upon subscription)
	 * @return an asynchronous {@link Observable} stream of generated {@link DAO
	 *         data access objects}
	 */
	Observable<?> saveAsync( Observable<F> facts );

	/**
	 * @param facts the {@link Observable} stream of {@link Fact facts} to store
	 *            immediately
	 * @return a synchronous {@link Iterable} stream of generated {@link DAO
	 *         data access objects}
	 */
	default Iterable<?> saveSync( final Observable<F> facts )
	{
		return saveAsync( facts ).blockingIterable();
	}

	/**
	 * @param fact
	 */
	default Object save( final F fact )
	{
		for( Object dao : saveSync( Observable.just( fact ) ) )
			return dao;
		return null;
	}

	/**
	 * @param facts the {@link F[] fact} array to store immediately
	 */
	@SuppressWarnings( "unchecked" )
	default void save( final F... facts )
	{
		saveSync( Observable.fromArray( facts ) );
	}

	/**
	 * @param facts the {@link Iterable} stream of {@link Fact facts} to store
	 *            immediately
	 */
	default void save( final Iterable<F> facts )
	{
		saveSync( Observable.fromIterable( facts ) );
	}

	/**
	 * @param facts the {@link Stream} of {@link Fact facts} to store
	 *            immediately
	 */
	default void save( final Stream<F> facts )
	{
		save( (Iterable<F>) () -> facts.iterator() );
	}

	default Class<F> transactionKindFilter()
	{
		return null; // default: any
	}

	default FactKind kindFilter()
	{
		return null; // default: any
	}

	default Fact.ID causeFilter()
	{
		return null; // default: any
	}

	default Actor.ID initiatorFilter()
	{
		return null; // default: any
	}

	default Actor.ID executorFilter()
	{
		return null; // default: any
	}

	default Actor.ID creatorFilter()
	{
		return null; // default: any
	}

	default Actor.ID responderFilter()
	{
		return null; // default: any
	}

	default Range<Instant> occurrenceFilter()
	{
		return null; // default: any
	}

	default Range<Instant> expirationFilter()
	{
		return null; // default: any
	}

	default Map<String, Object> propertiesFilter()
	{
		return null; // default: any
	}

	/**
	 * @param id the {@link Fact.ID} to match
	 * @return the {@link Fact} or {@code null} if not found
	 */
	F find( Fact.ID id );

	/**
	 * @return an asynchronous {@link Observable} stream of matching
	 *         {@link Fact}s, re-created for each subscription
	 */
	Observable<F> find( Class<?> typeFilter, Actor.ID initiatorFilter,
		Actor.ID executorFilter, FactKind kindFilter, Fact.ID causeFilter,
		Actor.ID creatorFilter, Actor.ID responderFilter,
		Range<Instant> occurrenceFilter, Range<Instant> expirationFilter,
		Map<String, Object> propertiesFilter );

	/**
	 * @return an asynchronous {@link Observable} stream of matching
	 *         {@link Fact}s, re-created for each subscription
	 */
	default Observable<F> find()
	{
		return find( transactionKindFilter(), initiatorFilter(),
				executorFilter(), kindFilter(), causeFilter(), creatorFilter(),
				responderFilter(), occurrenceFilter(), expirationFilter(),
				propertiesFilter() );
	}

	/**
	 * @return a synchronous {@link Iterable} stream of matching {@link Fact}s
	 */
	default Iterable<F> findAsIterable()
	{
		return find().blockingIterable();
	}

	/**
	 * @return a synchronous (parallel) {@link Stream} of {@link Fact}s matching
	 *         the specified filters
	 */
	default Stream<F> findAsStream( final boolean parallel )
	{
		return StreamSupport.stream( findAsIterable().spliterator(), parallel );
	}

	/**
	 * @param factKind the {@link FactKind} to match
	 * @return an {@link Iterable} of the matching {@link Fact}s
	 */
	default FactBank<F> matchKind( final FactKind factKind )
	{
		return new FactBank.Filtered<F>( this )
		{
			@Override
			public FactKind kindFilter()
			{
				return factKind;
			}
		};
	}

	/**
	 * FIXME this override does not work
	 * 
	 * @param tranKind the transaction kind ({@link Fact} type) to match
	 * @return an {@link Iterable} of the matching {@link Fact}s
	 */
	default <T extends Fact> FactBank<T>
		matchTransactionKind( final Class<T> tranKind )
	{
		final FactBank<?> me = this;
		return new FactBank.Filtered<T>( me )
		{
			@Override
			public T find( final Fact.ID id )
			{
				final Fact result = me.find( id );
				return result == null ? null : result.proxyAs( tranKind, null );
			}

			@Override
			public Class<T> transactionKindFilter()
			{
				return tranKind;
			}
		};
	}

	/**
	 * @param causeRef the cause {@link Fact.ID} to match
	 * @return an {@link Iterable} of the matching {@link Fact}s
	 */
	default FactBank<F> matchCause( final Fact.ID causeRef )
	{
		return new FactBank.Filtered<F>( this )
		{
			@Override
			public Fact.ID causeFilter()
			{
				return causeRef;
			}
		};
	}

	/**
	 * @param initiatorRef the initiator {@link Actor.ID} to match
	 * @return an {@link Iterable} of the matching {@link Fact}s
	 */
	default FactBank<F> matchInitiator( final Actor.ID initiatorRef )
	{
		return new FactBank.Filtered<F>( this )
		{
			@Override
			public Actor.ID initiatorFilter()
			{
				return initiatorRef;
			}
		};
	}

	/**
	 * @param executorRef the executor {@link Actor.ID} to match
	 * @return an {@link Iterable} of the matching {@link Fact}s
	 */
	default FactBank<F> matchExecutor( final Actor.ID executorRef )
	{
		return new FactBank.Filtered<F>( this )
		{
			@Override
			public Actor.ID executorFilter()
			{
				return executorRef;
			}
		};
	}

	/**
	 * @param creatorRef the creator {@link Actor.ID} to match
	 * @return an {@link Iterable} of the matching {@link Fact}s
	 */
	default FactBank<F> matchCreator( final Actor.ID creatorRef )
	{
		return new FactBank.Filtered<F>( this )
		{
			@Override
			public Actor.ID creatorFilter()
			{
				return creatorRef;
			}
		};
	}

	/**
	 * @param responderRef the initiator {@link Actor.ID} to match
	 * @return an {@link Iterable} of the matching {@link Fact}s
	 */
	default FactBank<F> matchResponder( final Actor.ID responderRef )
	{
		return new FactBank.Filtered<F>( this )
		{
			@Override
			public Actor.ID responderFilter()
			{
				return responderRef;
			}
		};
	}

	/**
	 * {@link Filtered} simple decorator for custom filter overrides
	 * 
	 * @param <T>
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	class Filtered<T extends Fact> implements FactBank<T>
	{
		protected final FactBank self;

		Filtered( final FactBank self )
		{
			this.self = self;
		}

		@Override
		public FactBank<Fact> root()
		{
			return this.self.root();
		}

		@Override
		public void close() throws Exception
		{
			this.self.close();
		}

		@Override
		public Observable<?> saveAsync( final Observable<T> fact )
		{
			return this.self.saveAsync( (Observable<?>) fact );
		}

		@Override
		public T find( final Fact.ID id )
		{
			return (T) this.self.find( id );
		}

		@Override
		public Observable<T> find( final Class<?> typeFilter,
			final Actor.ID initiatorFilter, final Actor.ID executorFilter,
			final FactKind kindFilter, final Fact.ID causeFilter,
			final Actor.ID creatorFilter, final Actor.ID responderFilter,
			final Range<Instant> occurrenceFilter,
			final Range<Instant> expirationFilter,
			final Map<String, Object> propertiesFilter )
		{
			return (Observable<T>) this.self.find( typeFilter, initiatorFilter,
					executorFilter, kindFilter, causeFilter, creatorFilter,
					responderFilter, occurrenceFilter, expirationFilter,
					propertiesFilter );
		}
	}

	/**
	 * {@link SimpleCache}
	 */
	@Singleton
	public class SimpleDrain implements FactBank<Fact>
	{
		@Override
		public FactBank<Fact> root()
		{
			return this;
		}

		@Override
		public Observable<?> saveAsync( final Observable<Fact> facts )
		{
			return facts.map( f ->
			{
//				LogUtil.getLogger( SimpleDrain.class ).trace( "Drained {}", f );
				return f;
			} );
		}

		@Override
		public Fact find( final ID id )
		{
			return null;
		}

		@Override
		public Observable<Fact> find( final Class<?> typeFilter,
			final Actor.ID initiatorFilter, final Actor.ID executorFilter,
			final FactKind kindFilter, final Fact.ID causeFilter,
			final Actor.ID creatorFilter, final Actor.ID responderFilter,
			final Range<Instant> occurrenceFilter,
			final Range<Instant> expirationFilter,
			final Map<String, Object> propertiesFilter )
		{
			return Observable.empty();
		}

		@Override
		public void close() throws Exception
		{
			// empty
		}
	}

	/**
	 * {@link SimpleCache}
	 */
	@Singleton
	public class SimpleCache implements FactBank<Fact>
	{
		private final Map<ID, Fact> cache = new TreeMap<>();

		@Override
		public FactBank<Fact> root()
		{
			return this;
		}

		@Override
		public Observable<?> saveAsync( final Observable<Fact> facts )
		{
			return facts.map( fact ->
			{
				this.cache.put( fact.id(), fact );
				return fact;
			} );
		}

		@Override
		public Fact find( final ID id )
		{
			return this.cache.get( id );
		}

		@Override
		public Observable<Fact> find( final Class<?> typeFilter,
			final Actor.ID initiatorFilter, final Actor.ID executorFilter,
			final FactKind kindFilter, final Fact.ID causeFilter,
			final Actor.ID creatorFilter, final Actor.ID responderFilter,
			final Range<Instant> occurrenceFilter,
			final Range<Instant> expirationFilter,
			final Map<String, Object> propertiesFilter )
		{
			Observable<Fact> result = Observable
					.fromIterable( this.cache.values() );

			if( typeFilter != null ) result = result
					.filter( f -> typeFilter.equals( f.transaction().kind() ) );

			if( initiatorFilter != null )
				result = result.filter( f -> initiatorFilter
						.equals( f.transaction().initiatorRef() ) );
			if( executorFilter != null )
				result = result.filter( f -> executorFilter
						.equals( f.transaction().executorRef() ) );
			if( kindFilter != null )
				result = result.filter( f -> kindFilter.equals( f.kind() ) );
			if( causeFilter != null ) result = result
					.filter( f -> causeFilter.equals( f.causeRef() ) );
			if( creatorFilter != null ) result = result
					.filter( f -> creatorFilter.equals( f.creatorRef() ) );
			if( responderFilter != null ) result = result
					.filter( f -> responderFilter.equals( f.responderRef() ) );
			if( occurrenceFilter != null ) result = result
					.filter( f -> occurrenceFilter.contains( f.occur() ) );
			if( expirationFilter != null ) result = result
					.filter( f -> expirationFilter.contains( f.expire() ) );
			if( propertiesFilter != null ) result = result.filter( f ->
			{
				for( Map.Entry<?, ?> entry : propertiesFilter.entrySet() )
				{
					final Object value = f.properties().get( entry.getKey() );
					if( entry.getValue() == null )
					{
						if( value != null ) return false;
					} else if( !entry.getValue().equals( value ) ) return false;
				}
				return true;
			} );

			return result;
		}

		@Override
		public void close() throws Exception
		{
			this.cache.clear();
		}
	}

	/**
	 * {@link SimpleJPA}
	 */
	@Singleton
	public class SimpleJPA implements FactBank<Fact>, AutoCloseable
	{

		@Inject
		private LocalBinder binder;

		/** only needed for the timeunit */
		@Inject
		private Scheduler scheduler;

		// FIXME inject (global) JPAConfig, (re-)create expensive EMF on demand?

		@Inject
		private EntityManagerFactory emf;

		@Override
		public FactBank<Fact> root()
		{
			return this;
		}

		@Override
		public void close() throws Exception
		{
			this.emf.close();
		}

		@Override
		public Observable<?> saveAsync( final Observable<Fact> facts )
		{
			// TODO defer: facts.observeOn( ... ) & rejoin at sim::onCompleted ?
			return Observable.<FactDao>create( sub ->
			{
				// One session for each fact
				facts.subscribe(
						fact -> JPAUtil.session( this.emf, fact::persist ),
						sub::onError, sub::onComplete );

				// One session for all facts
//				JPAUtil.session( this.emf ).subscribe( em ->
//				{
//					facts.subscribe( fact ->
//					{
//						em.flush();
//						sub.onNext( fact.persist( em ) );
//					}, sub::onError, sub::onCompleted );
//			}, e -> sub.onError( e ), () -> sub.onCompleted() );
			} );
		}

		@Override
		public Fact find( final Fact.ID id )
		{
			try
			{
				return JPAUtil.session( this.emf ).map(
						em -> FactDao.find( em, id ).restore( this.binder ) )
						.blockingFirst();
			} catch( final NoResultException empty )
			{
				return null;
			}
		}

		private BigDecimal normalize( final Instant t )
		{
			return QuantityUtil.decimalValue( t.toQuantity(),
					this.scheduler.timeUnit() );
		}

		@Override
		public Observable<Fact> find( final Class<?> typeFilter,
			final Actor.ID initiatorFilter, final Actor.ID executorFilter,
			final FactKind kindFilter, final Fact.ID causeFilter,
			final Actor.ID creatorFilter, final Actor.ID responderFilter,
			final Range<Instant> occurrenceFilter,
			final Range<Instant> expirationFilter,
			final Map<String, Object> propertiesFilter )
		{
			// FIXME switch to safe rx patterns
			return Observable.unsafeCreate( sub ->
			{
				JPAUtil.session( this.emf ).subscribe( em ->
				{
					Observable
							.fromIterable( FactDao.find( em,
									this.binder.id().contextRef(), typeFilter,
									initiatorFilter, executorFilter, kindFilter,
									causeFilter, creatorFilter, responderFilter,
									occurrenceFilter.map( this::normalize ),
									expirationFilter.map( this::normalize ),
									propertiesFilter ) )
							.map( dao -> dao.restore( this.binder ) )
							.safeSubscribe( sub );
				}, sub::onError );
			} );
		}
	}
}