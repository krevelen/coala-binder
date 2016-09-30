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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.unit.Unit;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.config.ConfigUtil;
import io.coala.config.InjectConfig;
import io.coala.json.JsonUtil;
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
//@BeanProxy
public interface Transaction<F extends CoordinationFact>
	extends Proactive, Identified.Ordinal<Transaction.ID>//, Persistable<Transaction.Dao>
{
	/** @return */
	@JsonProperty( "kind" )
	Class<F> kind();

	/** @return */
	// owner changes on transport
	LocalId ownerRef();

	/** @return */
	@JsonProperty( "initiatorRef" )
	CompositeActor.ID initiatorRef();

	/** @return */
	@JsonProperty( "executorRef" )
	CompositeActor.ID executorRef();

	/**
	 * @param factKind
	 * @param cause
	 * @param expiration
	 * @param params
	 * @return
	 */
	F generate( CoordinationFactKind factKind, CoordinationFact.ID cause,
		Instant expiration, Map<?, ?>... params );

	F commit( F fact, boolean cleanUp );

	/** @param incoming */
	void on( F incoming );

	/** @return */
	Observable<F> committed();

	/** @return */
	Observable<F> expired();

	CoordinationFactBank<F> factBank();

	/** @return the {@link java.time.Instant} real offset of virtual time */
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
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of(
		final LocalBinder binder, final Transaction.ID id, final Class<F> kind,
		final CompositeActor.ID initiatorRef,
		final CompositeActor.ID executorRef )
	{
		return binder.injectMembers(
				new Simple<F>( id, kind, initiatorRef, executorRef ) );
	}

	/**
	 * @param id
	 * @param kind
	 * @param initiatorRef
	 * @param executorRef
	 * @param config
	 * @param scheduler
	 * @param factFactory a {@link CoordinationFact.Factory}
	 * @param bankFactory a {@link CoordinationFactBank.Factory} or {@code null}
	 * @return a {@link Simple} instance
	 */
	static <F extends CoordinationFact> Transaction<F> of(
		final Transaction.ID id, final Class<F> kind,
		final CompositeActor.ID initiatorRef,
		final CompositeActor.ID executorRef, final ReplicateConfig config,
		final Scheduler scheduler, final CoordinationFact.Factory factFactory,
		final CoordinationFactBank.Factory bankFactory )
	{
		return new Simple<F>( id, kind, initiatorRef, executorRef, config,
				scheduler, factFactory, bankFactory );
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
	class Simple<F extends CoordinationFact> implements Transaction<F>
	{
		private static Map<ObjectMapper, Module> REGISTERED_CACHE = new HashMap<>();

		/**
		 * @param om
		 */
		public static void checkRegistered( ObjectMapper om )
		{
			REGISTERED_CACHE.computeIfAbsent( om, key ->
			{
				final SimpleModule result = new SimpleModule(
						Transaction.class.getSimpleName(),
						new Version( 1, 0, 0, null, null, null ) );
				result.addAbstractTypeMapping( Transaction.class,
						JsonUtil.checkRegisteredMembers( om,
								Transaction.Simple.class ) );
				om.registerModule( result );
				return result;
			} );
		}

		private final Subject<F, F> generated = PublishSubject.create();
		private final Subject<F, F> expired = PublishSubject.create();
		private final Map<CoordinationFact.ID, Expectation> pending = new ConcurrentHashMap<>();
		private Transaction.ID id;
		private Class<F> kind;
		private CompositeActor.ID initiatorRef;
		private CompositeActor.ID executorRef;

		@Inject
		private transient Scheduler scheduler;

		@Inject
		private transient CoordinationFact.Factory factFactory;

		@Inject
		private transient CoordinationFactBank.Factory bankFactory;
		private CoordinationFactBank<F> factBank = null;

		@InjectConfig
		private transient ReplicateConfig config;
		private java.time.Instant offset = null;
		private Unit<?> timeUnit = null;

		public Simple()
		{
		}

		protected Simple( final Transaction.ID id, final Class<F> kind,
			final CompositeActor.ID initiatorRef,
			final CompositeActor.ID executorRef )
		{
			this.id = id;
			this.kind = kind;
			this.initiatorRef = initiatorRef;
			this.executorRef = executorRef;
		}

		public Simple( final Transaction.ID id, final Class<F> kind,
			final CompositeActor.ID initiatorRef,
			final CompositeActor.ID executorRef, final ReplicateConfig config,
			final Scheduler scheduler,
			final CoordinationFact.Factory factFactory,
			final CoordinationFactBank.Factory bankFactory )
		{
			this( id, kind, initiatorRef, executorRef );
			this.config = config;
			this.scheduler = scheduler;
			this.factFactory = factFactory;
			this.bankFactory = bankFactory;
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
		public LocalId ownerRef()
		{
			return this.factFactory.ownerRef();
		}

		@Override
		public CompositeActor.ID initiatorRef()
		{
			return this.initiatorRef;
		}

		@Override
		public CompositeActor.ID executorRef()
		{
			return this.executorRef;
		}

		@Override
		public F generate( final CoordinationFactKind factKind,
			final CoordinationFact.ID causeRef, final Instant expiration,
			final Map<?, ?>... params )
		{
			return this.factFactory.create( kind(),
					CoordinationFact.ID.create( factKind.isFromInitiator()
							? initiatorRef() : executorRef() ),
					this, factKind, expiration, causeRef, params );
		}

		@Override
		public F commit( final F fact, final boolean cleanUp )
		{
			try
			{
				if( fact.causeRef() != null )
					this.pending.remove( fact.causeRef() );
				if( fact.expire() != null )
					this.pending.put( fact.id(), at( fact.expire() ).call( () ->
					{
						Objects.requireNonNull( fact.id() );
						this.pending.remove( fact.id() );
						this.expired.onNext( fact );
					} ) );
				LogUtil.getLogger( Transaction.class ).trace( "{} type: {}: {}",
						kind(), fact.getClass(),
						kind().isAssignableFrom( fact.getClass() ) );
				this.generated.onNext( fact );
				if( factBank() != null ) factBank().save( fact );
				if( cleanUp )
				{
					this.pending.values().forEach( Expectation::remove );
					this.pending.clear();
					this.expired.onCompleted();
					this.generated.onCompleted();
				}
				return fact;
			} catch( final Throwable e )
			{
				this.generated.onError( e );
				throw e;
			}
		}

		@Override
		public Observable<F> committed()
		{
			return this.generated.asObservable();
		}

		@Override
		public Observable<F> expired()
		{
			return this.expired.asObservable();
		}

		@Override
		public void on( final F fact )
		{
			if( fact.causeRef() != null )
				this.pending.remove( fact.causeRef() );
			if( fact.expire() != null )
				this.pending.put( fact.id(), at( fact.expire() ).call( () ->
				{
					this.pending.remove( fact.id() );
					this.expired.onNext( fact );
				} ) );
		}

		@Override
		public java.time.Instant offset()
		{
			return this.offset != null ? this.offset
					: (this.offset = ConfigUtil.cachedValue( this.config,
							this.config::offset ));
		}

		@Override
		public Unit<?> timeUnit()
		{
			return this.timeUnit != null ? this.timeUnit
					: (this.timeUnit = ConfigUtil.cachedValue( this.config,
							this.config::timeUnit ));
		}

		@Override
		public CoordinationFactBank<F> factBank()
		{
			return this.factBank != null ? this.factBank
					: this.bankFactory == null ? null
							: (this.factBank = this.bankFactory
									.create( kind() ));
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
						key -> Transaction.of( this.binder, id, kind, initiator,
								executor ) );
			}
		}
	}
}