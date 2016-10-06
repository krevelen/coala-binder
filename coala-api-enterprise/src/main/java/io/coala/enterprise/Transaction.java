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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.unit.Unit;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.LocalId;
import io.coala.config.ConfigUtil;
import io.coala.config.InjectConfig;
import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.name.Identified;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.ReplicateConfig;
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
public interface Transaction<F extends Fact>
	extends Proactive, Identified.Ordinal<Transaction.ID>
{
	/** @return */
	@JsonProperty( "kind" )
	Class<F> kind();

	/** @return */
	@JsonProperty( "initiatorRef" )
	Actor.ID initiatorRef();

	/** @return */
	@JsonProperty( "executorRef" )
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

	/** @return */
	Observable<F> committed();

	/** @return */
	Observable<F> expired();

	FactBank<F> factBank();

	/** @return the {@link java.time.Instant} UTC offset of virtual time */
	java.time.Instant offset();

	/** @return the {@link Unit} of virtual time */
	Unit<?> timeUnit();

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
		final Fact.Factory factFactory, final Unit<?> timeUnit,
		final java.time.Instant offset )
	{
		return new Simple<F>( id, kind, initiatorRef, executorRef, scheduler,
				factFactory, timeUnit, offset );
	}

	/**
	 * {@link Simple}
	 * 
	 * @param <F>
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "serial" )
	@JsonInclude( Include.NON_NULL )
	class Simple<F extends Fact> implements Transaction<F>
	{
		private transient final Subject<F, F> commits = PublishSubject.create();
		private transient final Subject<F, F> expired = PublishSubject.create();
		private transient final Map<Fact.ID, Expectation> pending = new ConcurrentHashMap<>();
		private transient final Set<UUID> committedIds = Collections
				.synchronizedSet( new HashSet<>() );
		private transient Scheduler scheduler;
		private transient Fact.Factory factFactory;
		private transient FactBank<F> factBank = null;
		private transient java.time.Instant offset = null;
		private transient Unit<?> timeUnit = null;

		private Transaction.ID id;
		private Class<F> kind;
		private Actor.ID initiatorRef;
		private Actor.ID executorRef;
		private boolean terminated = false;

		@Inject
		public Simple()
		{
			// zero-arg bean constructor
		}

		public Simple( final Transaction.ID id, final Class<F> kind,
			final Actor.ID initiatorRef, final Actor.ID executorRef,
			final Scheduler scheduler, final Fact.Factory factFactory,
			final Unit<?> timeUnit, final java.time.Instant offset )
		{
			this.id = id;
			this.kind = kind;
			this.initiatorRef = initiatorRef;
			this.executorRef = executorRef;
			this.scheduler = scheduler;
			this.factFactory = factFactory;
			this.factBank = factFactory.factBank() == null ? null
					: factFactory.factBank().matchTransactionKind( kind );
			this.timeUnit = timeUnit;
			this.offset = offset;
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

		protected void checkNotTerminated()
		{
			if( this.terminated ) Thrower.throwNew( IllegalStateException.class,
					"Already terminated: {}", id() );
		}

		@Override
		public F generate( final FactKind factKind, final Fact.ID causeRef,
			final Instant expiration, final Map<?, ?>... params )
		{
			checkNotTerminated();
			return this.factFactory.create( kind(),
					Fact.ID.create( factKind.isFromInitiator() ? initiatorRef()
							: executorRef() ),
					this, factKind, expiration, causeRef, params );
		}

		@Override
		public F commit( final F fact, final boolean cleanUp )
		{
			checkNotTerminated();
			// prevent re-committing
			if( this.committedIds.contains( fact.id().unwrap() ) )
				return Thrower.throwNew( IllegalStateException.class,
						"Already committed: {}", fact );
			this.committedIds.add( fact.id().unwrap() );

			try
			{
				// un/schedule expiration
				if( fact.causeRef() != null )
					this.pending.remove( fact.causeRef() );
				if( fact.expire() != null )
					this.pending.put( fact.id(), at( fact.expire() ).call( () ->
					{
						Objects.requireNonNull( fact.id() );
						this.pending.remove( fact.id() );
						this.expired.onNext( fact );
					} ) );
				LogUtil.getLogger( Actor.class ).trace( "transact {} gen: {}",
						id().prettyHash(), fact.id().prettyHash() );

				// publish / fire / send
				this.commits.onNext( fact );

				// persist
				if( factBank() != null ) factBank().save( fact );

				// unsubscribe listeners, cancel pending expirations
				if( cleanUp )
				{
					this.terminated = true;
					this.commits.onCompleted();
					this.expired.onCompleted();
					this.pending.values().forEach( Expectation::remove );
					this.pending.clear();
					this.committedIds.clear();
				}

				// allow chaining
				return fact;
			} catch( final Throwable e )
			{
				this.commits.onError( e );
				this.expired.onError( e );
				throw e;
			}
		}

		@Override
		public Observable<F> committed()
		{
			return this.commits.asObservable();
		}

		@Override
		public Observable<F> expired()
		{
			return this.expired.asObservable();
		}

		@Override
		public java.time.Instant offset()
		{
			return this.offset;
		}

		@Override
		public Unit<?> timeUnit()
		{
			return this.timeUnit;
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

		/** @return the {@link java.time.Instant} UTC offset of virtual time */
		java.time.Instant offset();

		/** @return the {@link Unit} of virtual time */
		Unit<?> timeUnit();

		@Singleton
		class LocalCaching implements Factory
		{
			private transient final Map<ID, Transaction<?>> localCache = new ConcurrentHashMap<>();

			@Inject
			private transient Scheduler scheduler;

			@Inject
			private transient Fact.Factory factFactory;

			@InjectConfig
			private transient ReplicateConfig config;
			private transient Unit<?> timeUnitCache;
			private transient java.time.Instant offsetCache;

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
									this.factFactory, timeUnit(), offset() );
							tx.committed().subscribe( f ->
							{
							}, e -> this.localCache.remove( id ),
									() -> this.localCache.remove( id ) );
							return tx;
						} );
			}

			@Override
			public java.time.Instant offset()
			{
				return this.offsetCache != null ? this.offsetCache
						: (this.offsetCache = ConfigUtil.cachedValue(
								this.config, this.config::offset ));
			}

			@Override
			public Unit<?> timeUnit()
			{
				return this.timeUnitCache != null ? this.timeUnitCache
						: (this.timeUnitCache = ConfigUtil.cachedValue(
								this.config, this.config::timeUnit ));
			}
		}
	}
}