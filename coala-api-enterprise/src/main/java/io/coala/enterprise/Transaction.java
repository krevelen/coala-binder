/* $Id: 27c637b196be24dd0bf8bd6cb967448d1aec5fe7 $
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.InjectConfig;
import io.coala.bind.LocalId;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.name.Identified;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.SchedulerConfig;
import io.coala.time.Scheduler;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Transaction} is an instance of some {@link F transaction kind}
 * occurring between some {@link #initiatorRef()} and {@link #executorRef()},
 * each {@link Identified} by a unique {@link Transaction.ID id}, with utility
 * methods to {@link #generate(FactKind, Fact.ID, Instant, Map...) generate} and
 * {@link #commit(Fact, boolean) commit} new {@link F facts}, published via an
 * {@link Observable} stream of {@link #commits()}, and {@link Proactive timing}
 * their expiration handling
 * 
 * @param <F> the type of {@link Fact} representing the transaction kind
 * @version $Id: 27c637b196be24dd0bf8bd6cb967448d1aec5fe7 $
 * @author Rick van Krevelen
 */
public interface Transaction<F extends Fact>
	extends Proactive, Identified.Ordinal<Transaction.ID>
{

	String KIND_JSON_PROPERTY = "kind";

	String INITIATOR_JSON_PROPERTY = "initiatorRef";

	String EXECUTOR_JSON_PROPERTY = "executorRef";

	/** @return */
	@JsonProperty( KIND_JSON_PROPERTY )
	Class<F> kind();

	/** @return */
	@JsonProperty( INITIATOR_JSON_PROPERTY )
	Actor.ID initiatorRef();

	/** @return */
	@JsonProperty( EXECUTOR_JSON_PROPERTY )
	Actor.ID executorRef();

	/**
	 * @param factKind
	 * @param cause
	 * @param expiration
	 * @param params
	 * @return
	 */
	F generate( FactKind factKind, Fact.ID cause, Instant expiration,
		Map<?, ?>... params );

	/**
	 * saves; sends; schedules expiration; cancels cause expiration (if pending)
	 * 
	 * @param fact the {@link Fact} to commit, i.e. save &amp; send
	 * @return the {@link Fact} again to allow chaining
	 */
	default F commit( F fact )
	{
		return commit( fact, false );
	}

	/**
	 * @param fact the {@link Fact} to commit, i.e. save &amp; send
	 * @param cleanUp {@code true} iff the {@link Transaction} may clean up,
	 *            e.g. it has terminated or no further facts are expected
	 * @return the {@link Fact} again to allow chaining
	 */
	F commit( F fact, boolean cleanUp );

	/**
	 * @param fact the {@link Fact} to commit, i.e. save &amp; send
	 * @param onExpiration the {@link ThrowingConsumer} to call upon expiration
	 * @return the {@link Fact} again to allow chaining
	 */
	default F commit( final F fact, final ThrowingConsumer<F, ?> onExpiration )
	{
		return commit( fact, () -> onExpiration.accept( fact ) );
	}

	/**
	 * @param fact the {@link Fact} to commit, i.e. save &amp; send
	 * @param onExpiration the {@link ThrowingRunnable} to call upon expiration
	 * @return the {@link Fact} again to allow chaining
	 */
	F commit( F fact, ThrowingRunnable<?> onExpiration );

	/** @return */
	Observable<F> commits();

	FactBank<F> factBank();

	/**
	 * {@link ID}
	 * 
	 * @version $Id: 27c637b196be24dd0bf8bd6cb967448d1aec5fe7 $
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
				return of( new UUID( value ), null );
			}
		}

		@Override
		@JsonValue
		public String toJSON()
		{
			// omit parentRef
			return unwrap().toString();
		}

		/** @return the wrapped value */
		@Override
		public UUID unwrap()
		{
			return (UUID) super.unwrap();
		}

		public String prettyHash()
		{
			return Integer.toHexString( unwrap().hashCode() );
		}

		/** @return */
		public static ID of( final UUID value, final LocalId ctx )
		{
			return LocalId.of( new ID(), value, ctx );
		}

		/** @return */
		public static ID create( final LocalId ctx )
		{
			return of( new UUID(), ctx );
		}
	}

	/**
	 * @param id
	 * @param kind
	 * @param initiatorRef
	 * @param executorRef
	 * @param config
	 * @param scheduler
	 * @param factFactory a {@link Fact.Factory}
	 * @param bankFactory a {@link FactBank.Factory} or {@code null}
	 * @return a {@link Simple} instance
	 */
	static <F extends Fact> Transaction<F> of( final Transaction.ID id,
		final Class<F> kind, final Actor.ID initiatorRef,
		final Actor.ID executorRef, final Scheduler scheduler,
		final Fact.Factory factFactory )
	{
		return new Simple<F>( id, kind, initiatorRef, executorRef, scheduler,
				factFactory );
	}

	/**
	 * {@link Simple}
	 * 
	 * @param <F>
	 */
//	@SuppressWarnings( "serial" )
	@JsonInclude( Include.NON_NULL )
	class Simple<F extends Fact> implements Transaction<F>
	{
		private transient final Subject<F> commits = PublishSubject.create();
		private transient final Map<Fact.ID, Expectation> pending = new ConcurrentHashMap<>();
		private transient final Set<UUID> committedIds = Collections
				.synchronizedSet( new HashSet<>() );
		private transient Scheduler scheduler;
		private transient Fact.Factory factFactory;
		private transient FactBank<F> factBank = null;

		private Transaction.ID id;
		private Class<F> kind;
		private Actor.ID initiatorRef;
		private Actor.ID executorRef;
		private boolean initiated = false;
		private boolean terminated = false;

		@Inject
		public Simple()
		{
			// zero-arg bean constructor
		}

		public Simple( final Transaction.ID id, final Class<F> kind,
			final Actor.ID initiatorRef, final Actor.ID executorRef,
			final Scheduler scheduler, final Fact.Factory factFactory )
		{
			this.id = Objects.requireNonNull( id );
			this.kind = Objects.requireNonNull( kind );
			this.initiatorRef = Objects.requireNonNull( initiatorRef );
			this.executorRef = Objects.requireNonNull( executorRef );
			this.scheduler = Objects.requireNonNull( scheduler );
			this.factFactory = Objects.requireNonNull( factFactory );
			this.factBank = factFactory.factBank() == null ? null
					: factFactory.factBank().matchTransactionKind( kind );
		}

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}

		@Override
		public Class<F> kind()
		{
			return this.kind;
		}

		@Override
		public Transaction.ID id()
		{
			return this.id;
		}

		@Override
		public Actor.ID initiatorRef()
		{
			return this.initiatorRef;
		}

		@Override
		public Actor.ID executorRef()
		{
			return this.executorRef;
		}

		protected void checkNotInitiated()
		{
			if( this.initiated ) Thrower.throwNew( IllegalStateException::new,
					() -> "Already initiated: " + id() );
		}

		protected void checkNotTerminated()
		{
			if( this.terminated ) Thrower.throwNew( IllegalStateException::new,
					() -> "Already terminated: " + id() );
		}

		@Override
		public F generate( final FactKind factKind, final Fact.ID causeRef,
			final Instant expiration, final Map<?, ?>... params )
		{
			if( causeRef == null || !causeRef.parentRef().equals( id() ) )
				checkNotInitiated();
			this.initiated = true;
			checkNotTerminated();
			return this.factFactory.create( kind(), Fact.ID.create( id() ),
					this, factKind, expiration, causeRef, params );
		}

		@Override
		public F commit( final F fact, final ThrowingRunnable<?> onExpiration )
		{
			return commit( fact, false, onExpiration );
		}

		@Override
		public F commit( final F fact, final boolean cleanUp )
		{
			return commit( fact, cleanUp, null );
		}

		public F commit( final F fact, final boolean cleanUp,
			final ThrowingRunnable<?> onExpiration )
		{
			checkNotTerminated();
			// prevent re-committing
			if( this.committedIds.contains( fact.id().unwrap() ) )
				return Thrower.throwNew( IllegalStateException::new,
						() -> "Already committed: " + fact );
			this.committedIds.add( fact.id().unwrap() );

			try
			{
				// un/schedule expiration
				if( fact.causeRef() != null )
					this.pending.remove( fact.causeRef() );
				if( fact.expire() != null )
					this.pending.put( Objects.requireNonNull( fact.id() ),
							at( fact.expire() ).call( () ->
							{
								if( onExpiration != null ) onExpiration.run();
								this.pending.remove( fact.id() );
							} ) );
				else if( onExpiration != null )
					return Thrower.throwNew( IllegalStateException::new,
							() -> "Expiration function never gets called: "
									+ fact );

				// publish / fire / send
				this.commits.onNext( fact );

				// persist
				if( factBank() != null ) factBank().save( fact );

				// unsubscribe listeners, cancel pending expirations
				if( cleanUp )
				{
					this.terminated = true;
					this.commits.onComplete();
					this.pending.values().forEach( Expectation::remove );
					this.pending.clear();
					this.committedIds.clear();
				}

				// allow chaining
				return fact;
			} catch( final Throwable e )
			{
				this.commits.onError( e ); // finish other observers
				return null;
			}
		}

		@Override
		public Observable<F> commits()
		{
			return this.commits;
		}

		@Override
		public FactBank<F> factBank()
		{
			return this.factBank;
		}
	}

	interface Factory
	{
		<F extends Fact> Transaction<F> create( Transaction.ID id,
			Class<F> factType, Actor.ID initiator, Actor.ID executor );

		@Singleton
		class LocalCaching implements Factory
		{
			private transient final Map<ID, Transaction<?>> localCache = new ConcurrentHashMap<>();

			@Inject
			private transient Scheduler scheduler;

			@Inject
			private transient Fact.Factory factFactory;

			@InjectConfig
			private transient SchedulerConfig config;

			@SuppressWarnings( "unchecked" )
			@Override
			public <F extends Fact> Transaction<F> create( final ID id,
				final Class<F> kind, final Actor.ID initiatorRef,
				final Actor.ID executorRef )
			{
				return (Transaction<F>) this.localCache.computeIfAbsent( id,
						key ->
						{
							final Transaction<?> tx = Transaction.of( id, kind,
									initiatorRef, executorRef, this.scheduler,
									this.factFactory );
							tx.commits().subscribe( f ->
							{
							}, e -> this.localCache.remove( id ),
									() -> this.localCache.remove( id ) );
							return tx;
						} );
			}
		}
	}
}