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

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CoordinationFact.Dao;
import io.coala.log.LogUtil;
import io.coala.math.Range;
import io.coala.persist.JPAUtil;
import io.coala.time.Instant;
import rx.Observable;

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
	Observable<Dao> save( Observable<F> fact );

	/**
	 * @param fact
	 */
	default void save( final F fact )
	{
		save( Observable.just( fact ) )/* .toBlocking() */.subscribe( dao ->
		{
		}, e ->
		{
			LogUtil.getLogger( CoordinationFactBank.class )
					.error( "Problem while saving: " + fact, e );
		} );
	}

	/**
	 * @param fact
	 */
	@SuppressWarnings( "unchecked" )
	default void save( final F... facts )
	{
		save( Observable.from( facts ) )/* .toBlocking() */.subscribe( dao ->
		{
		}, e ->
		{
			LogUtil.getLogger( CoordinationFactBank.class )
					.error( "Problem while saving facts", e );
		} );
	}

	/**
	 * @param fact
	 */
	default void save( final Iterable<F> facts )
	{
		save( Observable.from( facts ) )/* .toBlocking() */.subscribe( dao ->
		{
		}, e ->
		{
			LogUtil.getLogger( CoordinationFactBank.class )
					.error( "Problem while saving facts", e );
		} );
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
		public Observable<Dao> save( final Observable<T> fact )
		{
			return this.bank.save( (Observable<?>) fact );
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
		public Observable<Dao> save( final Observable<CoordinationFact> facts )
		{
			return facts//.observeOn( Schedulers.io() ) 
					// FIXME rejoin at sim onCompleted
					.withLatestFrom(
					JPAUtil.transact( this.emf ),
					( fact, em ) -> fact.persist( em ) );
		}

		@Override
		public CoordinationFact find( final CoordinationFact.ID id )
		{
			return JPAUtil.transact( this.emf ).toBlocking().first()
					.createQuery(
							"SELECT f FROM " + CoordinationFact.Dao.ENTITY_NAME
									+ " AS f WHERE f.ID=?1",
							CoordinationFact.Dao.class )
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
				JPAUtil.transact( this.emf ).subscribe( em ->
				{
					final CriteriaBuilder cb = em.getCriteriaBuilder();
					final CriteriaQuery<Dao> q = cb.createQuery( Dao.class );
					final Root<Dao> d = q.from( Dao.class );
					q.select( d );
					// TODO first (1) fetch id's sorted by time-stamp, 
					// then (2) fetch full Dao upon (chunked) traversal
					@SuppressWarnings( "rawtypes" )
					final ParameterExpression<Class> typeParam = cb
							.parameter( Class.class );
					if( type != null ) q.where( cb
							.equal( d.get( "tranKind" ), typeParam ) );
					final ParameterExpression<CoordinationFactKind> kindParam = cb
							.parameter( CoordinationFactKind.class );
					if( kind != null ) q.where( cb
							.equal( d.get( Dao.KIND_ATTR_NAME ), kindParam ) );

					final TypedQuery<Dao> query = em.createQuery( q );

					if( type != null ) query.setParameter( typeParam, type );
					if( kind != null ) query.setParameter( kindParam, kind );

					query.getResultList().stream().map( dao ->
					{
						return dao.restore( this.binder );
					} ).forEach( sub::onNext );
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