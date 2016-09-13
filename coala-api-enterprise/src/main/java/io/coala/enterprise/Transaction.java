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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.persist.UUIDToByteConverter;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Transaction}
 * 
 * @param <F>
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Transaction<F extends CoordinationFact>
	extends Proactive, Identified.Ordinal<Transaction.ID>
{
	/** @return */
	Class<F> kind();

	/** @return */
	LocalId owner();

	/** @return */
	CompositeActor.ID initiator();

	/** @return */
	CompositeActor.ID executor();

	/**
	 * @param factKind
	 * @param cause
	 * @param terminal
	 * @param expiration
	 * @param params
	 * @return
	 */
	F createFact( CoordinationFactType factKind, CoordinationFact.ID cause,
		boolean terminal, Instant expiration, Map<?, ?>... params );

	/** @param incoming */
	void on( F incoming );

	/** @return */
	@JsonIgnore
	Observable<F> performed();

	/** @return */
	@JsonIgnore
	Observable<F> expired();

	default Stream<Transaction<?>> find( final EntityManager em,
		final LocalBinder binder, final String query )
	{
		return em.createQuery( query, Dao.class ).getResultList().stream()
				.map( dao ->
				{
					return dao.restore( binder );
				} );
	}

	default Stream<Transaction<?>> findAll( final EntityManager em,
		final LocalBinder binder )
	{
		return find( em, binder,
				"SELECT dao FROM " + Dao.ENTITY_NAME + " dao" );
	}

	default Dao persist( final EntityManager em )
	{
		final Dao result = new Dao().prePersist( this );
		em.persist( result );
		return result;
	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends LocalId
	{

		/** @return the wrapped value */
		@Override
		public UUID unwrap()
		{
			return (UUID) super.unwrap();
		}

		/** @return */
		public static ID of( final UUID value, final LocalId ctx )
		{
			return Id.of( new ID(), value, ctx );
		}

		/** @return */
		public static ID create( final LocalId ctx )
		{
			return of( new UUID(), ctx );
		}

		@Entity( name = Dao.ENTITY_NAME )
		public static class Dao extends LocalId.Dao
		{
			public static final String ENTITY_NAME = "TRANSACTION_IDS";

			@Override
			public ID restore( final LocalBinder binder )
			{
				return ID.of( new UUID( this.myId ), CompositeActor.ID
						.of( this.parentId.restore( binder ) ) );
			}

			@Override
			public Dao prePersist( final LocalId source )
			{
				super.prePersist( source );
				return this;
			}
		}
	}

	/**
	 * @param tranKind
	 * @param scheduler
	 * @param id
	 * @param initiator
	 * @param executor
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of(
		final LocalBinder binder, final Transaction.ID id,
		final Class<F> tranKind, final CompositeActor.ID initiator,
		final CompositeActor.ID executor )
	{
		return of( tranKind, binder.inject( Scheduler.class ), id, initiator,
				executor, binder.inject( CoordinationFact.Factory.class ) );
	}

	/**
	 * @param kind
	 * @param scheduler
	 * @param id
	 * @param initiatorID
	 * @param executorID
	 * @param factFactory
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of( final Class<F> kind,
		final Scheduler scheduler, final Transaction.ID id,
		final CompositeActor.ID initiator, final CompositeActor.ID executor,
		final CoordinationFact.Factory factFactory )
	{
		final Subject<F, F> performed = PublishSubject.create();
		final Map<CoordinationFact.ID, Expectation> pending = new ConcurrentHashMap<>();
		final Subject<F, F> expired = PublishSubject.create();
		return new Transaction<F>()
		{
			@Override
			public Scheduler scheduler()
			{
				return scheduler;
			}

			@Override
			public Class<F> kind()
			{
				return kind;
			}

			@Override
			public Transaction.ID id()
			{
				return id;
			}

			@Override
			public LocalId owner()
			{
				return factFactory.owner();
			}

			@Override
			public CompositeActor.ID initiator()
			{
				return initiator;
			}

			@Override
			public CompositeActor.ID executor()
			{
				return executor;
			}

			@Override
			public F createFact( final CoordinationFactType factKind,
				final CoordinationFact.ID cause, final boolean terminal,
				final Instant expiration, final Map<?, ?>... params )
			{
				try
				{
					final F result = factFactory.create( kind(),
							CoordinationFact.ID.create( factFactory.owner() ),
							this,
							factKind.isFromInitiator() ? initiator : executor,
							factKind, expiration, cause, params );
					if( cause != null ) pending.remove( cause );
					if( expiration != null )
					{
						pending.put( result.id(), at( expiration ).call( () ->
						{
							pending.remove( expired );
							expired.onNext( result );
						} ) );
					}
					performed.onNext( result );
					if( terminal ) performed.onCompleted();
					return result;
				} catch( final Throwable e )
				{
					performed.onError( e );
					throw e;
				}
			}

			@Override
			public Observable<F> performed()
			{
				return performed.asObservable();
			}

			@Override
			public Observable<F> expired()
			{
				return expired.asObservable();
			}

			@Override
			public void on( final F fact )
			{
				pending.remove( fact.cause() );
				if( fact.expiration() != null ) pending.put( fact.id(),
						scheduler.at( fact.expiration() ).call( () ->
						{
							pending.remove( expired );
							expired.onNext( fact );
						} ) );
			}
		};
	}

	/**
	 * {@link Dao}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Entity( name = Dao.ENTITY_NAME )
	public class Dao extends BindableDao<Transaction<?>, Dao>
	{
		public static final String ENTITY_NAME = "TRANSACTIONS";

		@javax.persistence.Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		@Column( name = "PK", nullable = false, updatable = false )
		protected Integer pk;

		@Column( name = "ID", nullable = false, updatable = false,
			length = 16 /* , columnDefinition = "BINARY(16)" */ )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID id;

		@Column( name = "KIND", nullable = true, updatable = false )
		protected Class<? extends CoordinationFact> kind;

		@JoinColumn( name = "INITIATOR_ID", nullable = true, updatable = false )
		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		protected CompositeActor.ID.Dao initiator;

		@JoinColumn( name = "EXECUTOR_ID", nullable = true, updatable = false )
		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		protected CompositeActor.ID.Dao executor;

		@Override
		public Transaction<?> restore( final LocalBinder binder )
		{
			return binder.inject( Transaction.Factory.class ).create(
					Transaction.ID.of( this.id, binder.id() ), this.kind,
					this.initiator.restore( binder ),
					this.executor.restore( binder ) );
		}

		@Override
		protected Dao prePersist( final Transaction<?> tran )
		{
			this.id = tran.id().unwrap();
			this.kind = tran.kind();
			this.initiator = new CompositeActor.ID.Dao()
					.prePersist( tran.initiator() );
			this.executor = new CompositeActor.ID.Dao()
					.prePersist( tran.executor() );
			return this;
		}

	}

	interface Factory
	{
		<F extends CoordinationFact> Transaction<F> create( Transaction.ID id,
			Class<F> factType, CompositeActor.ID initiator,
			CompositeActor.ID executor );

		@Singleton
		class LocalCaching implements Factory
		{
			private final Map<Class<?>, Transaction<?>> localCache = new ConcurrentHashMap<>();

			@Inject
			private LocalBinder binder;

			@SuppressWarnings( "unchecked" )
			@Override
			public <F extends CoordinationFact> Transaction<F> create(
				final ID id, final Class<F> kind,
				final CompositeActor.ID initiator,
				final CompositeActor.ID executor )
			{
				return (Transaction<F>) this.localCache.computeIfAbsent( kind,
						k ->
						{
							return Transaction.of( this.binder, id, kind,
									initiator, executor );
						} );
			}

		}
	}
}