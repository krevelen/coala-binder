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
	 * @param initiator the initiator {@link CompositeActor}
	 * @param executor the executor {@link CompositeActor}
	 * @return the {@link Transaction} context
	 */
	<F extends CoordinationFact> Transaction<F> transact( Class<F> tranKind,
		Transaction<F> transaction, CompositeActor initiator,
		CompositeActor executor );

	/**
	 * @param tranKind the type of {@link CoordinationFact} to transact
	 * @param executorID the executor {@link Organization.ID}
	 * @return the {@link Transaction} context
	 */
	default <F extends CoordinationFact> Transaction<F>
		asInitiator( final Class<F> tranKind, final CompositeActor executor )
	{
		return transact( tranKind, null, this, executor );
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
				(Transaction<F>) fact.transaction(), fact.creator(), this );
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
		final Class<F> tranKind, final CompositeActor executor,
		final CoordinationFact cause, final Instant expiration,
		final Map<?, ?>... params )
	{
		return (F) asInitiator( tranKind, executor ).createFact(
				CoordinationFactType.REQUESTED, cause, false, expiration,
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
		final CoordinationFactType factKind, final boolean terminal,
		final Instant expiration, final Map<?, ?>... params )
	{
		return (F) asResponder( cause ).createFact( factKind, cause, terminal,
				expiration, params );
	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends Id.OrdinalChild<String, Organization.ID>
	{
		/**
		 * @param name
		 * @param orgId
		 * @return
		 */
		public static CompositeActor.ID of( final String name,
			final Organization.ID orgId )
		{
			return (CompositeActor.ID) Util.of( name, new ID() )
					.parent( orgId );
		}
	}

	static CompositeActor of( final ID id, final Organization org,
		final CoordinationFact.Factory factFactory )
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
					return fact.creator().id().equals( creatorID );
				} );
			}

			@Override
			@SuppressWarnings( "unchecked" )
			public <F extends CoordinationFact> Transaction<F> transact(
				final Class<F> factKind, final Transaction<F> transaction,
				final CompositeActor initiator, final CompositeActor executor )
			{
				try
				{
					return (Transaction<F>) txs.computeIfAbsent(
							transaction == null ? Transaction.ID.create()
									: transaction.id(),
							tid ->
							{
								final Transaction<F> result = transaction != null
										? transaction
										: Transaction.of( factKind, scheduler(),
												tid, initiator, executor,
												factFactory );
								org.incoming().ofType( factKind )
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

			@Override
			public CompositeActor create( final ID id, final Organization org )
			{
				return this.localCache.computeIfAbsent( id, k ->
				{
					return CompositeActor.of( id, org, this.factFactory );
				} );
			}
		}
	}
}