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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.Logger;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CoordinationFact.Dao;
import io.coala.log.LogUtil;
import io.coala.math.Range;
import io.coala.persist.JPAUtil;
import io.coala.time.Instant;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * {@link CoordinationFactBank}
 * 
 * @param <F>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface CoordinationFactBank<F extends CoordinationFact>
	extends AutoCloseable
{

	/**
	 * @param fact
	 */
	Observable<Dao> saveAsync( Observable<F> fact );

	/**
	 * @param fact
	 */
	default Iterable<Dao> saveSync( Observable<F> fact )
	{
		return saveAsync( fact ).toBlocking().toIterable();
	}

	static Logger logger()
	{
		return LogUtil.getLogger( CoordinationFactBank.class );
	}

	/**
	 * @param fact
	 */
	default void save( final F fact )
	{
		for( Dao dao : saveSync( Observable.just( fact ) ) )
			logger().trace( "saved: {}", dao );
	}

	/**
	 * @param fact
	 */
	@SuppressWarnings( "unchecked" )
	default void save( final F... facts )
	{
		for( Dao dao : saveSync( Observable.from( facts ) ) )
			logger().trace( "saved: {}", dao );
	}

	/**
	 * @param fact
	 */
	default void save( final Iterable<F> facts )
	{
		for( Dao dao : saveSync( Observable.from( facts ) ) )
			logger().trace( "saved: {}", dao );
	}

	/**
	 * @param fact
	 */
	default void save( final Stream<F> facts )
	{
		save( () -> facts.iterator() );
	}

	default Class<F> transactionKindFilter()
	{
		return null;
	}

	default CoordinationFactKind kindFilter()
	{
		return null;
	}

	default CoordinationFact.ID causeFilter()
	{
		return null;
	}

	default CompositeActor.ID initiatorFilter()
	{
		return null;
	}

	default CompositeActor.ID executorFilter()
	{
		return null;
	}

	default CompositeActor.ID creatorFilter()
	{
		return null;
	}

	default Range<Instant> occurrenceFilter()
	{
		return null;
	}

	default Range<Instant> expirationFilter()
	{
		return null;
	}

	default Map<String, Object> propertiesFilter()
	{
		return null;
	}

	/**
	 * @param id the {@link CoordinationFact.ID} to match
	 * @return the {@link CoordinationFact} or {@code null} if not found
	 */
	F find( CoordinationFact.ID id );

	/**
	 * @return an {@link Iterable} of the matching {@link CoordinationFact}s
	 */
	Observable<F> find();

	default Iterable<F> iterable()
	{
		return find().toBlocking().toIterable();
	}

	/**
	 * @param factKind the {@link CoordinationFactKind} to match
	 * @return an {@link Iterable} of the matching {@link CoordinationFact}s
	 */
	default CoordinationFactBank<F>
		matchKind( final CoordinationFactKind factKind )
	{
		return new CoordinationFactBank.Wrapper<F>( this )
		{
			@Override
			public CoordinationFactKind kindFilter()
			{
				return factKind;
			}
		};
	}

	/**
	 * @param causeId the cause {@link CoordinationFact.ID} to match
	 * @return an {@link Iterable} of the matching {@link CoordinationFact}s
	 */
	default <T extends CoordinationFact> CoordinationFactBank<T>
		matchTransactionKind( final Class<T> tranKind )
	{
		final CoordinationFactBank<?> self = this;
		return new CoordinationFactBank.Wrapper<T>( self )
		{

			@Override
			public T find( final CoordinationFact.ID id )
			{
				final CoordinationFact result = self.find( id );
				return result == null ? null : result.proxyAs( tranKind, null );
			}

			@Override
			public Observable<T> find()
			{
				return self.find()
						.map( fact -> fact.proxyAs( tranKind, null ) );
			}

			@Override
			public Class<T> transactionKindFilter()
			{
				return tranKind;
			}
		};
	}

	/**
	 * @param causeId the cause {@link CoordinationFact.ID} to match
	 * @return an {@link Iterable} of the matching {@link CoordinationFact}s
	 */
	default CoordinationFactBank<F>
		matchCause( final CoordinationFact.ID causeId )
	{
		return new CoordinationFactBank.Wrapper<F>( this )
		{
			@Override
			public CoordinationFact.ID causeFilter()
			{
				return causeId;
			}
		};
	}

	/**
	 * {@link Wrapper} simple decorator for custom overrides
	 * 
	 * @param <T>
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	class Wrapper<T extends CoordinationFact> implements CoordinationFactBank<T>
	{
		protected final CoordinationFactBank bank;

		Wrapper( final CoordinationFactBank bank )
		{
			this.bank = bank;
		}

		@Override
		public void close() throws Exception
		{
			this.bank.close();
		}

		@Override
		public Observable<Dao> saveAsync( final Observable<T> fact )
		{
			return this.bank.saveAsync( (Observable<?>) fact );
		}

		@Override
		public T find( final CoordinationFact.ID id )
		{
			return (T) this.bank.find( id );
		}

		@Override
		public Observable<T> find()
		{
			return (Observable<T>) this.bank.find();
		}

		@Override
		public Class<T> transactionKindFilter()
		{
			return (Class<T>) this.bank.transactionKindFilter();
		}
	}

	/**
	 * {@link SimpleJPA}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Singleton
	public class SimpleJPA implements CoordinationFactBank<CoordinationFact>
	{

		@Inject
		private LocalBinder binder;

		@Inject
		private EntityManagerFactory emf;

		@Override
		public void close() throws Exception
		{
			this.emf.close();
		}

		@Override
		public Observable<Dao>
			saveAsync( final Observable<CoordinationFact> facts )
		{
			// TODO defer: facts.observeOn( ... ) & rejoin at sim::onCompleted
			return PublishSubject.<Dao> create( sub ->
			{
				// One session for each fact
				facts.subscribe( fact ->
				{
					JPAUtil.session( this.emf, em -> fact.persist( em ) );
				}, e -> sub.onError( e ), () -> sub.onCompleted() );
				
				// One session for all facts
//				JPAUtil.session( this.emf ).subscribe( em ->
//				{
//					facts.subscribe( fact ->
//					{
//						em.flush();
//						sub.onNext( fact.persist( em ) );
//					}, e -> sub.onError( e ), () -> sub.onCompleted() );
//			}, e -> sub.onError( e ), () -> sub.onCompleted() );
			} ).asObservable();
		}

		@Override
		public CoordinationFact find( final CoordinationFact.ID id )
		{
			return JPAUtil.session( this.emf ).toBlocking().first()
					.createQuery( "SELECT f FROM " + Dao.ENTITY_NAME
							+ " AS f WHERE f.ID=?1", Dao.class )
					.setParameter( 1, id.unwrap() ).getSingleResult()
					.restore( this.binder );
		}

		@Override
		public Observable<CoordinationFact> find()
		{
			final Class<?> type = transactionKindFilter();
			final CoordinationFactKind kind = kindFilter();
//			final CoordinationFact.ID cause = causeFilter();
//			final CompositeActor.ID initiator = initiatorFilter();
//			final CompositeActor.ID executor = executorFilter();
//			final CompositeActor.ID creator = creatorFilter();
//			final Range<Instant> occurrence = occurrenceFilter();
//			final Range<Instant> expiration = expirationFilter();
//			final Map<String, Object> properties = propertiesFilter();

			// TODO: chunking, two-step streaming, sorted by creation time-stamp
			return Observable.create( sub ->
			{
				JPAUtil.session( this.emf ).subscribe( em ->
				{
					final CriteriaBuilder cb = em.getCriteriaBuilder();
					final CriteriaQuery<Dao> q = cb.createQuery( Dao.class );
					final Root<Dao> d = q.from( Dao.class );
					q.select( d );

					// TODO first (1) fetch id's sorted by time-stamp, 
					// then (2) fetch full Dao upon (chunked) traversal

//					@SuppressWarnings( "rawtypes" )
//					final ParameterExpression<Class> typeParam = cb
//							.parameter( Class.class );
//					if( type != null )
//						q.where( cb.equal( d.get( "transaction" ).get( "kind" ),
//								typeParam ) );
//					final ParameterExpression<CoordinationFactKind> kindParam = cb
//							.parameter( CoordinationFactKind.class );
//					if( kind != null ) q.where( cb
//							.equal( d.get( Dao.KIND_ATTR_NAME ), kindParam ) );

					final TypedQuery<Dao> query = em.createQuery( q );

//					if( type != null ) query.setParameter( typeParam, type );
//					if( kind != null ) query.setParameter( kindParam, kind );

					final List<Dao> list = query.getResultList();
					System.err.println( "Got: " + list );
					list.stream().map( dao -> dao.restore( this.binder ) )
							.forEach( sub::onNext );
					sub.onCompleted();
				}, sub::onError );
			} );
		}

		@Override
		public Class<CoordinationFact> transactionKindFilter()
		{
			return CoordinationFact.class;
		}
	}

	interface Factory
	{
		CoordinationFactBank<?> create();

		/**
		 * @param tranKind
		 * @return
		 */
		default <F extends CoordinationFact> CoordinationFactBank<F>
			create( final Class<F> tranKind )
		{
			return create().matchTransactionKind( tranKind );
		}

		@Singleton
		class LocalJPA implements Factory
		{

			@Inject
			private LocalBinder binder;

			@Override
			public CoordinationFactBank<?> create()
			{
				return this.binder.inject( SimpleJPA.class );
			}
		}
	}
}