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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

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
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface CompositeActor
	extends Proactive, Identified.Ordinal<CompositeActor.ID>
{
	/** @return the ownew {@link Organization.ID} */
//	Organization.ID ownerID();

	/** @return an {@link Observable} of outgoing {@link CoordinationFact}s */
	Observable<CoordinationFact> outgoing();

	/** @return an {@link Observable} of expired {@link CoordinationFact}s */
	Observable<CoordinationFact> expired();

	/**
	 * @param factKind the type of {@link CoordinationFact} to filter for
	 * @param creatorID the origin {@link Organization.ID} to filter for
	 * @return an {@link Observable} of incoming {@link CoordinationFact}s
	 */
	<F extends CoordinationFact> Observable<F> on( Class<F> factKind,
		Organization.ID creatorID );

	/**
	 * @param factKind the type of {@link CoordinationFact} to filter for
	 * @param creatorID the origin {@link Organization.ID} to filter for
	 * @param handler a {@link Consumer} for handling fact instances
	 * @return the handler's {@link Subscription} for future cancellation
	 */
	default <F extends CoordinationFact> Subscription on( Class<F> factKind,
		Organization.ID creatorID, final Consumer<F> handler )
	{
		return on( factKind, creatorID ).subscribe( fact ->
		{
			handler.accept( fact );
		}, error ->
		{
		} );
	}

	/**
	 * @param factKind the type of {@link CoordinationFact} to transact
	 * @param tranID the context {@link Transaction.ID}
	 * @param initiatorID the initiator {@link Organization.ID}
	 * @param executorID the executor {@link Organization.ID}
	 * @return the {@link Transaction} context
	 */
	<F extends CoordinationFact> Transaction<F> transact( Class<F> factKind,
		Transaction.ID tranID, CompositeActor.ID initiatorID,
		CompositeActor.ID executorID );

	/**
	 * @param factKind the type of {@link CoordinationFact} to transact
	 * @param executorID the executor {@link Organization.ID}
	 * @return the {@link Transaction} context
	 */
	default <F extends CoordinationFact> Transaction<F> asInitiator(
		final Class<F> factKind, final CompositeActor.ID executorID )
	{
		return transact( factKind, Transaction.ID.create(), id(), executorID );
	}

	/**
	 * @param fact the {@link CoordinationFact} to respond to
	 * @return the {@link Transaction} context
	 */
	@SuppressWarnings( "unchecked" )
	default <F extends CoordinationFact> Transaction<F>
		asResponder( final F fact )
	{
		return transact( (Class<F>) fact.getClass(), fact.tranID(),
				fact.creatorID(), id() );
	}

	/**
	 * initiate a new transaction
	 * 
	 * @param factKind
	 * @param executorID
	 * @param cause
	 * @param expiration
	 * @param params
	 * @return
	 */
	default <F extends CoordinationFact> F createRequest(
		final Class<F> factKind, final CompositeActor.ID executorID,
		final CoordinationFact cause, final Instant expiration,
		final Map<?, ?>... params )
	{
		return asInitiator( factKind, executorID ).createFact(
				CoordinationFactType.REQUESTED, cause, false, expiration,
				params );
	}

	/**
	 * @param cause
	 * @param type
	 * @param terminal
	 * @param expiration
	 * @param params
	 * @return
	 */
	default <F extends CoordinationFact> F createResponse( final F cause,
		final CoordinationFactType type, final boolean terminal,
		final Instant expiration, final Map<?, ?>... params )
	{
		return (F) asResponder( cause ).createFact( type, cause, terminal,
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

	static CompositeActor of( final String name, final Organization org,
		final CoordinationFact.Factory factFactory )
	{
		final ID id = ID.of( name, org.id() );
		final Subject<CoordinationFact, CoordinationFact> outgoing = PublishSubject
				.create();
		final Subject<CoordinationFact, CoordinationFact> expiring = PublishSubject
				.create();
		final Map<Transaction.ID, Transaction<?>> txs = new HashMap<>();

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
				on( final Class<F> factKind, final Organization.ID creatorID )
			{
				return org.incoming().ofType( factKind ).filter( fact ->
				{
					return fact.creatorID().equals( creatorID );
				} );
			}

			@Override
			@SuppressWarnings( "unchecked" )
			public <F extends CoordinationFact> Transaction<F> transact(
				final Class<F> factKind, final Transaction.ID tranID,
				final CompositeActor.ID initiatorID,
				final CompositeActor.ID executorID )
			{
				return (Transaction<F>) txs.computeIfAbsent( tranID, type ->
				{
					final Transaction<F> result = Transaction.of( factKind,
							scheduler(), tranID, initiatorID, executorID,
							factFactory );
					org.incoming().ofType( factKind ).subscribe( incoming ->
					{
						result.on( incoming );
					}, e ->
					{
						// transport failed?
					}, () ->
					{
						// simulation done?
					} );
					result.outgoing().subscribe( fact ->
					{
						outgoing.onNext( fact );
					}, e ->
					{
						outgoing.onError( e );
					}, () ->
					{
						txs.remove( tranID );
					} );
					result.expiring().subscribe( fact ->
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
			}
		};
	}
}