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
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.eaio.uuid.UUID;

import io.coala.bind.LocalBinder;
import io.coala.name.Id;
import io.coala.name.Identified;
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
	CompositeActor.ID initiatorID();

	/** @return */
	CompositeActor.ID executorID();

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
		public static Transaction.ID of( final UUID value )
		{
			return Util.of( value, new ID() );
		}

		/** @return */
		public static Transaction.ID create()
		{
			return of( new UUID() );
		}
	}

	/**
	 * @param kind
	 * @param initiator
	 * @param executorID
	 * @param factFactory
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of( final Class<F> kind,
		final CompositeActor initiator, final CompositeActor.ID executorID,
		final CoordinationFact.Factory factFactory )
	{
		return of( kind, initiator.scheduler(), ID.create(), initiator.id(),
				executorID, factFactory );
	}

	/**
	 * @param kind
	 * @param initiatorID
	 * @param executor
	 * @param factFactory
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of( final Class<F> kind,
		final CompositeActor.ID initiatorID, final CompositeActor executor,
		final CoordinationFact.Factory factFactory )
	{
		return of( kind, executor.scheduler(), ID.create(), initiatorID,
				executor.id(), factFactory );
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
	static <F extends CoordinationFact> Transaction<F> of(
		final LocalBinder binder, final Transaction.ID id, final Class<F> kind,
		final CompositeActor.ID initiatorID,
		final CompositeActor.ID executorID )
	{
		return of( kind, binder.inject( Scheduler.class ), id, initiatorID,
				executorID, binder.inject( CoordinationFact.Factory.class ) );
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
		final CompositeActor.ID initiatorID, final CompositeActor.ID executorID,
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
			public CompositeActor.ID initiatorID()
			{
				return initiatorID;
			}

			@Override
			public CompositeActor.ID executorID()
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
					final F result = factFactory
							.create( kind, CoordinationFact.ID.create(), id(),
									type.isFromInitiator() ? initiatorID
											: executorID,
									type, expiration,
									cause == null ? null : cause.id(), params );
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

	interface Factory
	{
		<F extends CoordinationFact> Transaction<F> create( Transaction.ID id,
			Class<F> factType, CompositeActor.ID initiatorID,
			CompositeActor.ID executorID );

		@Singleton
		class Simple implements Factory
		{
			private final Map<Class<?>, Transaction<?>> localCache = new ConcurrentHashMap<>();

			@Inject
			private LocalBinder binder;

			@SuppressWarnings( "unchecked" )
			@Override
			public <F extends CoordinationFact> Transaction<F> create(
				final ID id, final Class<F> kind,
				final CompositeActor.ID initiatorID,
				final CompositeActor.ID executorID )
			{
				return (Transaction<F>) this.localCache.computeIfAbsent( kind,
						k ->
						{
							return Transaction.of( this.binder, id, kind,
									initiatorID, executorID );
						} );
			}

		}
	}
}