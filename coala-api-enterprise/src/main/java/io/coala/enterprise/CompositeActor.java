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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.registry.infomodel.Organization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalContextual;
import io.coala.bind.LocalId;
import io.coala.exception.Thrower;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link CompositeActor} can handle multiple {@link Transaction} types or kinds
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface CompositeActor extends Proactive,
	Identified.Ordinal<CompositeActor.ID>, Observer<CoordinationFact>
{

	/**
	 * @param actorID
	 * @return the referenced child {@link CompositeActor} instance
	 */
	CompositeActor actor( ID actorID );

	/**
	 * @param actorID
	 * @return the referenced child {@link CompositeActor} instance
	 */
	default CompositeActor actor( final String actorID )
	{
		return actor( ID.of( actorID, id() ) );
	}

	/**
	 * @return an {@link Observable} stream of all generated or received
	 *         {@link CoordinationFact}s
	 */
	Observable<CoordinationFact> facts();

	/**
	 * @return an {@link Observable} merging {@link Transaction#expired()}
	 *         streams for all transactions involving this actor {@link #id()}
	 */
	Observable<CoordinationFact> expired();

	/**
	 * @param factKind the type of {@link CoordinationFact} to filter for
	 * @return an {@link Observable} of incoming {@link CoordinationFact}s
	 */
	default <F extends CoordinationFact> Observable<F>
		on( final Class<F> factKind )
	{
		return facts().ofType( factKind );
	}

	/**
	 * @param tranKind the type of {@link CoordinationFact} to filter for
	 * @param factKind the {@link CoordinationFactKind} to filter for
	 * @param creatorID the origin {@link CompositeActor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link CoordinationFact}s
	 */
	default <F extends CoordinationFact> Observable<F>
		on( final Class<F> tranKind, final CoordinationFactKind factKind )
	{
		return on( tranKind ).filter( f -> f.kind().equals( factKind ) );
	}

	/**
	 * @param tranKind the type of {@link CoordinationFact} to filter for
	 * @param creatorRef the origin {@link CompositeActor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link CoordinationFact}s
	 */
	default <F extends CoordinationFact> Observable<F>
		on( final Class<F> tranKind, final CompositeActor.ID creatorRef )
	{
		return on( tranKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/**
	 * @param tranKind the type of {@link CoordinationFact} to filter for
	 * @param factKind the {@link CoordinationFactKind} to filter for
	 * @param creatorRef the origin {@link CompositeActor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link CoordinationFact}s
	 */
	default <F extends CoordinationFact> Observable<F> on(
		final Class<F> tranKind, final CoordinationFactKind factKind,
		final CompositeActor.ID creatorRef )
	{
		return on( tranKind, factKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/** @return an {@link Observable} of outgoing {@link CoordinationFact}s */
	default Observable<CoordinationFact> outgoing()
	{
		return facts().filter( CoordinationFact::isOutgoing );
	}

	default <F extends CoordinationFact> Observable<F>
		outgoing( final Class<F> tranKind )
	{
		return outgoing().ofType( tranKind );
	}

	default <F extends CoordinationFact> Observable<F>
		outgoing( final Class<F> tranKind, final CoordinationFactKind kind )
	{
		return outgoing( tranKind ).filter( f ->
		{
			return f.kind() == kind;
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
				(Transaction<F>) fact.transaction(), fact.creatorRef(), id(),
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
	default <F extends CoordinationFact> F initiate( final Class<F> tranKind,
		final CompositeActor.ID executor, final CoordinationFact.ID cause,
		final Instant expiration, final Map<?, ?>... params )
	{
		return (F) asInitiator( tranKind, executor ).generate(
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
	default <F extends CoordinationFact> F respond( final F cause,
		final CoordinationFactKind factKind, final boolean terminal,
		final Instant expiration, final Map<?, ?>... params )
	{
		return (F) asResponder( cause ).generate( factKind, cause.id(),
				terminal, expiration, params );
	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@JsonDeserialize( converter = ID.FromStringConverter.class )
	class ID extends LocalId
	{
		public static class FromStringConverter extends StdConverter<String, ID>
		{
			@Override
			public ID convert( final String value )
			{
				return of( valueOf( value ) );
			}
		}

		/**
		 * @param name
		 * @param parent
		 * @return
		 */
		public static ID of( final String name, final LocalId parent )
		{
			return Id.of( new ID(), name, parent );
		}

		/**
		 * @param raw
		 * @return
		 */
		public static ID of( final LocalId raw )
		{
			return raw == null || raw.parentRef() == null ? null
					: of( raw.unwrap().toString(), raw.parentRef() );
		}

		/** @return the root (parent organization's) {@link ID} */
		// derived @JsonIgnore
		public ID organization()
		{
			for( LocalId id = this;; id = id.parentRef() )
				if( id.parentRef() instanceof ID == false ) return (ID) id;
		}
	}

	/**
	 * @param id
	 * @param scheduler
	 * @param actorFactory
	 * @param factFactory
	 * @param bankFactory
	 * @return
	 */
	static CompositeActor of( final ID id, final Scheduler scheduler,
		final CompositeActor.Factory actorFactory,
		final CoordinationFact.Factory factFactory,
		final CoordinationFactBank.Factory bankFactory )
	{
		return new CompositeActor()
		{
//			final Logger LOG = LogUtil.getLogger( CompositeActor.class );

			private final Subject<CoordinationFact, CoordinationFact> incoming = PublishSubject
					.create();
			/**
			 * the {@link Subject} that merges all {@link Transaction#expired()}
			 * streams for transactions involving this {@link CompositeActor}
			 */
			private final Subject<CoordinationFact, CoordinationFact> expiring = PublishSubject
					.create();
			private final Map<Transaction.ID, Transaction<?>> txs = new ConcurrentHashMap<>();
			private final Map<ID, CompositeActor> actorMap = new ConcurrentHashMap<>();

			@Override
			public Scheduler scheduler()
			{
				return scheduler;
			}

			@Override
			public ID id()
			{
				return id;
			}

			@Override
			public Observable<CoordinationFact> expired()
			{
				return this.expiring.asObservable();
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
					return (Transaction<F>) this.txs.computeIfAbsent(
							transaction == null ? Transaction.ID.create( id() )
									: transaction.id(),
							tid ->
							{
								final Transaction<F> result = transaction != null
										? transaction
										: Transaction.of( tranKind, scheduler(),
												tid, initiator, executor,
												factFactory, bankFactory );
								this.on( tranKind ).subscribe( result::on,
										this::onError );
								result.generated().subscribe(
										this.incoming::onNext,
										this.incoming::onError,
										() -> this.txs.remove( result.id() ) );
								result.expired().subscribe(
										this.expiring::onNext,
										this.expiring::onError );
								return result;
							} );
				} catch( final Throwable e )
				{
//					outgoing.onError( e );
					return Thrower.rethrowUnchecked( e );
				}
			}

			@Override
			public CompositeActor actor( final ID actorID )
			{
				return this.actorMap.computeIfAbsent( actorID,
						id -> actorFactory.create( actorID, facts() ) );
			}

			@Override
			public Observable<CoordinationFact> facts()
			{
				return this.incoming.asObservable();
			}

			@Override
			public void onCompleted()
			{
				this.incoming.onCompleted();
			}

			@Override
			public void onError( final Throwable e )
			{
				this.incoming.onError( e );
			}

			@Override
			public void onNext( final CoordinationFact fact )
			{
				this.incoming.onNext( fact );
			}
		};
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Factory extends LocalContextual
	{

		CompositeActor create( ID id );

		default CompositeActor create( final String name )
		{
			return create( ID.of( name, id() ) );
		}

		default CompositeActor create( final String name,
			final CompositeActor parent )
		{
			return create( ID.of( name, parent.id() ), parent.facts() );
		}

		default CompositeActor create( final ID id,
			final Observable<CoordinationFact> incoming )
		{
			final CompositeActor result = create( id );
//			result.outgoing().subscribe( outgoing );
			incoming.subscribe( result );
			return result;
		}

		@Singleton
		class LocalCaching implements Factory
		{
			private final Map<ID, CompositeActor> localCache = new ConcurrentHashMap<>();

			@Inject
			private LocalBinder binder;

			@Inject
			private Scheduler scheduler;

			@Inject
			private CoordinationFact.Factory factFactory;

			@Inject
			private CoordinationFactBank.Factory bankFactory;

			@Override
			public CompositeActor create( final ID id )
			{
				return this.localCache.computeIfAbsent( id, k ->
				{
					return CompositeActor.of( id, this.scheduler, this,
							this.factFactory, this.bankFactory );
				} );
			}

			@Override
			public Context context()
			{
				return this.binder.context();
			}

			@Override
			public LocalId id()
			{
				return this.binder.id();
			}
		}
	}
}