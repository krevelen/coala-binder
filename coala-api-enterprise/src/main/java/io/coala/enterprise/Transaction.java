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

import com.eaio.uuid.UUID;

import io.coala.enterprise.fact.CoordinationFactType;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import io.coala.time.Timed;
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
	extends Timed, Identified.Ordinal<Transaction.ID>
{
	/** @return */
	Organization.ID initiatorID();

	/** @return */
	Organization.ID executorID();

	/**
	 * @param type
	 * @param cause
	 * @param terminal
	 * @param expiration
	 * @param params
	 * @return
	 */
	F createFact( CoordinationFactType type, CoordinationFact cause,
		boolean terminal, Instant expiration, Map<?, ?>... params );

	/** @param incoming */
	void on( F incoming );

	/** @return */
	Observable<F> outgoing();

	/** @return */
	Observable<F> expiring();

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends Id.Ordinal<UUID>
	{
		/** @return */
		public static Transaction.ID create()
		{
			return Util.of( new UUID(), new ID() );
		}
	}

	/**
	 * @param factKind
	 * @param initiator
	 * @param executorID
	 * @param factFactory
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of(
		final Class<F> factKind, final Organization initiator,
		final Organization.ID executorID,
		final CoordinationFact.Factory factFactory )
	{
		return of( factKind, initiator.scheduler(), ID.create(), initiator.id(),
				executorID, factFactory );
	}

	/**
	 * @param factKind
	 * @param initiatorID
	 * @param executor
	 * @param factFactory
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of(
		final Class<F> factKind, final Organization.ID initiatorID,
		final Organization executor,
		final CoordinationFact.Factory factFactory )
	{
		return of( factKind, executor.scheduler(), ID.create(), initiatorID,
				executor.id(), factFactory );
	}

	/**
	 * @param factKind
	 * @param scheduler
	 * @param id
	 * @param initiatorID
	 * @param executorID
	 * @param factFactory
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of(
		final Class<F> factKind, final Scheduler scheduler,
		final Transaction.ID id, final Organization.ID initiatorID,
		final Organization.ID executorID,
		final CoordinationFact.Factory factFactory )
	{
		final Subject<F, F> outgoing = PublishSubject.create();
		final Map<CoordinationFact.ID, Expectation> pending = new HashMap<>();
		final Subject<F, F> expiring = PublishSubject.create();
		return new Transaction<F>()
		{
			@Override
			public Scheduler scheduler()
			{
				return scheduler;
			}

			@Override
			public Transaction.ID id()
			{
				return id;
			}

			@Override
			public Organization.ID initiatorID()
			{
				return initiatorID;
			}

			@Override
			public Organization.ID executorID()
			{
				return executorID;
			}

			@Override
			public F createFact( final CoordinationFactType type,
				final CoordinationFact cause, final boolean terminal,
				final Instant expiration, final Map<?, ?>... params )
			{
				try
				{
					final F result = factFactory.create( scheduler(), factKind,
							CoordinationFact.ID.create(), id(),
							type.isFromInitiator() ? initiatorID : executorID,
							type, expiration, cause == null ? null : cause.id(),
							params );
					if( cause != null ) pending.remove( cause.id() );
					if( expiration != null )
					{
						pending.put( result.id(), at( expiration ).call( () ->
						{
							pending.remove( expiring );
							expiring.onNext( result );
						} ) );
					}
					outgoing.onNext( result );
					if( terminal ) outgoing.onCompleted();
					return result;
				} catch( final Throwable e )
				{
					outgoing.onError( e );
					throw e;
				}
			}

			@Override
			public Observable<F> outgoing()
			{
				return outgoing.asObservable();
			}

			@Override
			public Observable<F> expiring()
			{
				return expiring.asObservable();
			}

			@Override
			public void on( final F fact )
			{
				pending.remove( fact.causeID() );
				if( fact.expiration() != null ) pending.put( fact.id(),
						scheduler.at( fact.expiration() ).call( () ->
						{
							pending.remove( expiring );
							expiring.onNext( fact );
						} ) );
			}
		};
	}
}