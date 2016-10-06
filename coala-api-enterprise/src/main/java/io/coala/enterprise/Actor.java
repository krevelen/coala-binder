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
import javax.measure.unit.Unit;
import javax.xml.registry.infomodel.Organization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalContextual;
import io.coala.bind.LocalId;
import io.coala.log.LogUtil;
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
 * {@link Actor} can handle multiple {@link Transaction} types or kinds
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Actor
	extends Proactive, Identified.Ordinal<Actor.ID>, Observer<Fact>
{

	/**
	 * @param actorID
	 * @return the referenced child {@link Actor} instance
	 */
	Actor actor( ID actorID );

	/**
	 * @param actorID
	 * @return the referenced child {@link Actor} instance
	 */
	default Actor actor( final String actorID )
	{
		return actor( ID.of( actorID, id() ) );
	}

	/**
	 * @return an {@link Observable} merging {@link Transaction#expired()}
	 *         streams for all transactions involving this actor {@link #id()}
	 */
	Observable<Fact> expired();

	/**
	 * @return an {@link Observable} stream of all generated or received
	 *         {@link Fact}s
	 */
	Observable<Fact> occurred();

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <F extends Fact> Observable<F> occurred( final Class<F> tranKind )
	{
		return occurred().ofType( tranKind );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param factKind the {@link FactKind} to filter for
	 * @param creatorID the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <F extends Fact> Observable<F> occurred( final Class<F> tranKind,
		final FactKind factKind )
	{
		return occurred( tranKind ).filter( f -> f.kind().equals( factKind ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param creatorRef the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <F extends Fact> Observable<F> occurred( final Class<F> tranKind,
		final Actor.ID creatorRef )
	{
		return occurred( tranKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param factKind the {@link FactKind} to filter for
	 * @param creatorRef the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <F extends Fact> Observable<F> occurred( final Class<F> tranKind,
		final FactKind factKind, final Actor.ID creatorRef )
	{
		return occurred( tranKind, factKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/**
	 * @return an {@link Observable} of {@link Fact}s where
	 *         {@link Fact#isOutgoing() isOutgoing()==true}
	 */
	default Observable<Fact> outgoing()
	{
		return occurred().filter( Fact::isOutgoing );
	}

	default <F extends Fact> Observable<F> outgoing( final Class<F> tranKind )
	{
		return outgoing().ofType( tranKind );
	}

	default <F extends Fact> Observable<F> outgoing( final Class<F> tranKind,
		final FactKind kind )
	{
		return outgoing( tranKind ).filter( f ->
		{
			return f.kind() == kind;
		} );
	}

	/**
	 * @param tranKind the type of {@link Fact} to transact
	 * @param transaction the context {@link Transaction}, or {@code null}
	 * @param initiator the initiator {@link Actor.ID}
	 * @param executor the executor {@link Actor.ID}
	 * @param source the source {@link Actor}
	 * @return the {@link Transaction} context
	 */
	<F extends Fact> Transaction<F> transact( Class<F> tranKind,
		Transaction<F> transaction, Actor.ID initiator, Actor.ID executor,
		Actor source );

	/**
	 * @param tranKind the type of {@link Fact} to transact
	 * @param executorID the executor {@link Organization.ID}
	 * @return the {@link Transaction} context
	 */
	default <F extends Fact> Transaction<F>
		asInitiator( final Class<F> tranKind, final Actor.ID executor )
	{
		return transact( tranKind, null, id(), executor, this );
	}

	/**
	 * @param fact the {@link Fact} to respond to
	 * @return the {@link Transaction} context
	 */
	@SuppressWarnings( "unchecked" )
	default <F extends Fact> Transaction<F> asResponder( final F fact )
	{
		return transact( (Class<F>) fact.getClass(),
				(Transaction<F>) fact.transaction(), fact.creatorRef(), id(),
				this );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <F extends Fact> F initiate( final Class<F> tranKind,
		final Actor.ID executorRef, final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, null, null, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param cause the {@link Fact} triggering the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <F extends Fact> F initiate( final Class<F> tranKind,
		final Actor.ID executorRef, final Fact.ID cause,
		final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, cause, null, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param expiration the expiration {@link Instant} of the request, or
	 *            {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <F extends Fact> F initiate( final Class<F> tranKind,
		final Actor.ID executorRef, final Instant expiration,
		final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, null, expiration, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param cause the {@link Fact} triggering the request, or {@code null}
	 * @param expiration the expiration {@link Instant} of the request, or
	 *            {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <F extends Fact> F initiate( final Class<F> tranKind,
		final Actor.ID executorRef, final Fact.ID cause,
		final Instant expiration, final Map<?, ?>... params )
	{
		return (F) asInitiator( tranKind, executorRef )
				.generate( FactKind.REQUESTED, cause, expiration, params );
	}

	/**
	 * @param cause the {@link Fact} triggering the response
	 * @param factKind the {@link FactKind} of response
	 * @param params additional property (or bean attribute) values, if any
	 * @return a response {@link Fact}
	 */
	default <F extends Fact> F respond( final F cause, final FactKind factKind,
		final Map<?, ?>... params )
	{
		return respond( cause, factKind, null, params );
	}

	/**
	 * @param cause the {@link Fact} triggering the response
	 * @param factKind the {@link FactKind} of the response
	 * @param expiration the expiration {@link Instant} of the response, or
	 *            {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return a response {@link Fact}
	 */
	default <F extends Fact> F respond( final F cause, final FactKind factKind,
		final Instant expiration, final Map<?, ?>... params )
	{
		return (F) asResponder( cause ).generate( factKind, cause.id(),
				expiration, params );
	}

	static Actor of( final ID id, final Scheduler scheduler,
		final Factory actorFactory, final Transaction.Factory txFactory )
	{
		return new Simple( id, scheduler, actorFactory, txFactory );
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

		/** @return the derived root parent's {@link ID} */
		public ID organization()
		{
			for( LocalId id = this;; id = id.parentRef() )
				if( id.parentRef() instanceof ID == false ) return (ID) id;
		}
	}

	class Simple implements Actor
	{
//			final Logger LOG = LogUtil.getLogger( Simple.class );

		public static Simple of( final LocalBinder binder, final ID id )
		{
			final Simple result = binder.inject( Simple.class );
			result.id = id;
			return result;
		}

		private transient final Subject<Fact, Fact> occurred = PublishSubject
				.create();

		// TODO check if caching is already handled within Observable.ofType(..)
		private transient final Map<Class<?>, Observable<?>> typeObservableCache = new ConcurrentHashMap<>();

		/**
		 * the {@link Subject} that merges all {@link Transaction#expired()}
		 * streams for transactions involving this {@link Actor}
		 */
		private transient final Subject<Fact, Fact> expiring = PublishSubject
				.create();

		private transient final Map<Transaction.ID, Transaction<?>> txs = new ConcurrentHashMap<>();

		private transient final Map<ID, Actor> actorMap = new ConcurrentHashMap<>();

		@Inject
		private transient Scheduler scheduler;

		@Inject
		private transient Factory actorFactory;

		@Inject
		private transient Transaction.Factory txFactory;

		private ID id;

		@Inject
		public Simple()
		{
			// empty bean constructor
		}

		public Simple( final ID id, final Scheduler scheduler,
			final Factory actorFactory, final Transaction.Factory txFactory )
		{
			this.id = id;
			this.scheduler = scheduler;
			this.actorFactory = actorFactory;
			this.txFactory = txFactory;
		}

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}

		@Override
		public ID id()
		{
			return this.id;
		}

		@Override
		public Observable<Fact> expired()
		{
			return this.expiring.asObservable();
		}

		@Override
		@SuppressWarnings( "unchecked" )
		public <F extends Fact> Transaction<F> transact(
			final Class<F> tranKind, final Transaction<F> transaction,
			final Actor.ID initiatorRef, final Actor.ID executorRef,
			final Actor source )
		{
			return (Transaction<F>) this.txs.computeIfAbsent(
					transaction == null ? Transaction.ID.create( id() )
							: transaction.id(),
					tid ->
					{
						final Transaction<F> tx = transaction != null
								? transaction
								: this.txFactory.create( tid, tranKind,
										initiatorRef, executorRef );
						// tx -> actor (committed facts)
						tx.committed().subscribe( this::onNext, this::onError,
								() -> this.txs.remove( tx.id() ) );
						// tx -> actor (expired facts)
						tx.expired().subscribe( this.expiring::onNext,
								this.expiring::onError );
						return tx;
					} );
		}

		@Override
		public Actor actor( final ID actorID )
		{
			return this.actorMap.computeIfAbsent( actorID, id ->
			{
				final Actor child = this.actorFactory.create( id );
				// child -> parent
				child.occurred().subscribe( this::onNext, this::onError );
				// FIXME how to notify sibling/cousin actors without looping ?
//					occurred().filter( f -> !f.creatorRef().equals( id() ) )
//							.subscribe( result );
				return child;
			} );
		}

		@Override
		public Observable<Fact> occurred()
		{
			return this.occurred.asObservable();
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <F extends Fact> Observable<F>
			occurred( final Class<F> tranKind )
		{
			return (Observable<F>) this.typeObservableCache
					.computeIfAbsent( tranKind, occurred()::ofType );
		}

		@Override
		public void onCompleted()
		{
			this.occurred.onCompleted();
		}

		@Override
		public void onError( final Throwable e )
		{
			this.occurred.onError( e );
		}

		@Override
		public void onNext( final Fact fact )
		{
			this.occurred.onNext( fact );
		}
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Factory extends LocalContextual
	{

		/** @return the {@link java.time.Instant} UTC offset of virtual time */
		java.time.Instant offset();

		/** @return the {@link Unit} of virtual time */
		Unit<?> timeUnit();

		/**
		 * @param id the {@link ID} of the new {@link Actor}
		 * @return a (cached) {@link Actor}
		 */
		Actor create( ID id );

		default Actor create( final String name )
		{
			return create( ID.of( name, id() ) );
		}

		@Singleton
		class LocalCaching implements Factory
		{
			private final transient Map<ID, Actor> localCache = new ConcurrentHashMap<>();

			@Inject
			private transient LocalBinder binder;

			@Inject
			private transient Scheduler scheduler;

			@Inject
			private transient Factory actorFactory;

			@Inject
			private transient Transaction.Factory txFactory;

			@Override
			public Actor create( final ID id )
			{
				return this.localCache.computeIfAbsent( id, k ->
				{
					return Actor.of( id, this.scheduler, this.actorFactory,
							this.txFactory );
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

			@Override
			public java.time.Instant offset()
			{
				return this.txFactory.offset();
			}

			@Override
			public Unit<?> timeUnit()
			{
				return this.txFactory.timeUnit();
			}
		}
	}
}