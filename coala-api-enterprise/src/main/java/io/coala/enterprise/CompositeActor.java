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
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Entity;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import rx.Observable;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link CompositeActor} can handle multiple {@link Transaction} types or kinds
 * <p>
 * TODO make recursive/holonic
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface CompositeActor
	extends Proactive, Identified.Ordinal<CompositeActor.ID>
{
	/** @return an {@link Observable} of outgoing {@link CoordinationFact}s */
	Observable<CoordinationFact> outgoing();

	/** @return an {@link Observable} of expired {@link CoordinationFact}s */
	Observable<CoordinationFact> expired();

	/**
	 * @param factKind the type of {@link CoordinationFact} to filter for
	 * @param creatorID the origin {@link CompositeActor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link CoordinationFact}s
	 */
	<F extends CoordinationFact> Observable<F> on( Class<F> factKind,
		CompositeActor.ID creatorID );

	/**
	 * @param factKind the type of {@link CoordinationFact} to filter for
	 * @param creatorID the origin {@link CompositeActor.ID} to filter for
	 * @param handler a {@link Consumer} for handling fact instances
	 * @return the handler's {@link Subscription} for future cancellation
	 */
	default <F extends CoordinationFact> Subscription on( Class<F> factKind,
		CompositeActor.ID creatorID, final Consumer<F> handler )
	{
		return on( factKind, creatorID ).subscribe( fact ->
		{
			handler.accept( fact );
		}, e ->
		{
			LogUtil.getLogger( CompositeActor.class ).warn( "Ignore error", e );
		} );
	}

	/**
	 * @param tranKind the type of {@link CoordinationFact} to transact
	 * @param transaction the context {@link Transaction}, or {@code null}
	 * @param initiator the initiator {@link CompositeActor.ID}
	 * @param executor the executor {@link CompositeActor.ID}
	 * @param source the source {@link CompositeActor}
	 * @return the {@link Transaction} context
	 */
	<F extends CoordinationFact> Transaction<F> transact( Class<F> tranKind,
		Transaction<F> transaction, CompositeActor.ID initiator,
		CompositeActor.ID executor, CompositeActor source );

	/**
	 * @param tranKind the type of {@link CoordinationFact} to transact
	 * @param executorID the executor {@link Organization.ID}
	 * @return the {@link Transaction} context
	 */
	default <F extends CoordinationFact> Transaction<F>
		asInitiator( final Class<F> tranKind, final CompositeActor.ID executor )
	{
		return transact( tranKind, null, id(), executor, this );
	}

	/**
	 * @param fact the {@link CoordinationFact} to respond to
	 * @return the {@link Transaction} context
	 */
	@SuppressWarnings( "unchecked" )
	default <F extends CoordinationFact> Transaction<F>
		asResponder( final F fact )
	{
		return transact( (Class<F>) fact.getClass(),
				(Transaction<F>) fact.transaction(), fact.creator(), id(),
				this );
	}

	/**
	 * initiate a new transaction
	 * 
	 * @param tranKind
	 * @param executor
	 * @param cause
	 * @param expiration
	 * @param params
	 * @return
	 */
	default <F extends CoordinationFact> F createRequest(
		final Class<F> tranKind, final CompositeActor.ID executor,
		final CoordinationFact.ID cause, final Instant expiration,
		final Map<?, ?>... params )
	{
		return (F) asInitiator( tranKind, executor ).createFact(
				CoordinationFactKind.REQUESTED, cause, false, expiration,
				params );
	}

	/**
	 * @param cause
	 * @param factKind
	 * @param terminal
	 * @param expiration
	 * @param params
	 * @return
	 */
	default <F extends CoordinationFact> F createResponse( final F cause,
		final CoordinationFactKind factKind, final boolean terminal,
		final Instant expiration, final Map<?, ?>... params )
	{
		return (F) asResponder( cause ).createFact( factKind, cause.id(),
				terminal, expiration, params );
	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends LocalId
	{
		/**
		 * @param name
		 * @param orgId
		 * @return
		 */
		public static ID of( final String name, final Organization.ID ctx )
		{
			return Id.of( new ID(), name, ctx );
		}

		protected static ID of( final LocalId ctx )
		{
			return of( (String) ctx.unwrap(),
					Organization.ID.of( ctx.parent() ) );
		}

		@Entity( name = Dao.ENTITY_NAME )
		public static class Dao extends LocalId.Dao
		{
			public static final String ENTITY_NAME = "ACTOR_IDS";

			@Override
			public ID restore( final LocalBinder binder )
			{
				return CompositeActor.ID.of( this.myId,
						Organization.ID.of( this.parentId.restore( binder ) ) );
			}

			@Override
			public Dao prePersist( final LocalId source )
			{
				super.prePersist( source );
				return this;
			}
		}
	}

	static CompositeActor of( final ID id, final Organization org,
		final CoordinationFact.Factory factFactory,
		final CoordinationFactBank.Factory bankFactory )
	{
		final Subject<CoordinationFact, CoordinationFact> outgoing = PublishSubject
				.create();
		final Subject<CoordinationFact, CoordinationFact> expiring = PublishSubject
				.create();
		final Map<Transaction.ID, Transaction<?>> txs = new ConcurrentHashMap<>();

		return new CompositeActor()
		{
			@Override
			public Scheduler scheduler()
			{
				return org.scheduler();
			}

			@Override
			public ID id()
			{
				return id;
			}

			@Override
			public Observable<CoordinationFact> outgoing()
			{
				return outgoing.asObservable();
			}

			@Override
			public Observable<CoordinationFact> expired()
			{
				return expiring.asObservable();
			}

			@Override
			public <F extends CoordinationFact> Observable<F>
				on( final Class<F> factKind, final CompositeActor.ID creatorID )
			{
				return org.incoming().ofType( factKind ).filter( fact ->
				{
					return fact.creator().equals( creatorID );
				} );
			}

			@Override
			@SuppressWarnings( "unchecked" )
			public <F extends CoordinationFact> Transaction<F> transact(
				final Class<F> tranKind, final Transaction<F> transaction,
				final CompositeActor.ID initiator,
				final CompositeActor.ID executor, final CompositeActor source )
			{
				try
				{
					return (Transaction<F>) txs.computeIfAbsent(
							transaction == null ? Transaction.ID.create( id() )
									: transaction.id(),
							tid ->
							{
								final Transaction<F> result = transaction != null
										? transaction
										: Transaction.of( tranKind, scheduler(),
												tid, initiator, executor,
												factFactory, bankFactory );
								org.incoming().ofType( tranKind )
										.subscribe( incoming ->
										{
											result.on( incoming );
										}, e ->
										{
											// transport failed?
										}, () ->
										{
											// simulation done?
										} );
								result.performed().subscribe( fact ->
								{
									outgoing.onNext( fact );
								}, e ->
								{
									outgoing.onError( e );
								}, () ->
								{
									txs.remove( result.id() );
								} );
								result.expired().subscribe( fact ->
								{
									expiring.onNext( fact );
								}, e ->
								{
									expiring.onError( e );
								}, () ->
								{
									// nothing to do
								} );
								return result;
							} );
				} catch( final Throwable e )
				{
//					outgoing.onError( e );
					return Thrower.rethrowUnchecked( e );
				}
			}
		};
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Factory
	{

		default CompositeActor create( String name, Organization org )
		{
			return create( ID.of( name, org.id() ), org );
		}

		CompositeActor create( ID id, Organization org );

		@Singleton
		class Simple implements Factory
		{
			private final Map<ID, CompositeActor> localCache = new ConcurrentHashMap<>();

			@Inject
			private CoordinationFact.Factory factFactory;

			@Inject
			private CoordinationFactBank.Factory bankFactory;

			@Override
			public CompositeActor create( final ID id, final Organization org )
			{
				return this.localCache.computeIfAbsent( id, k ->
				{
					return CompositeActor.of( id, org, this.factFactory,
							this.bankFactory );
				} );
			}
		}
	}
}