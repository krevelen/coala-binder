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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.aeonbits.owner.ConfigCache;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.json.JsonUtil;
import io.coala.name.Identified;
import io.coala.persist.Persistable;
import io.coala.persist.UUIDToByteConverter;
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
public interface Transaction<F extends CoordinationFact> extends Proactive,
	Identified.Ordinal<Transaction.ID>, Persistable<Transaction.Dao>
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
	 * @param terminal
	 * @param expiration
	 * @param params
	 * @return
	 */
	F generate( CoordinationFactKind factKind, CoordinationFact.ID cause,
		boolean terminal, Instant expiration, Map<?, ?>... params );

	/** @param incoming */
	void on( F incoming );

	/** @return */
	Observable<F> generated();

	/** @return */
	Observable<F> expired();

	CoordinationFactBank<F> facts();

	default java.time.Instant offset()
	{
		// FIXME which cache?
		return ConfigCache.getOrCreate( ReplicateConfig.class ).offset();
	}

	default Stream<Transaction<?>> find( final EntityManager em,
		final LocalBinder binder, final String query )
	{
		return findSync( em, query ).map( dao -> dao.restore( binder ) );
	}

	default Stream<Transaction<?>> findAll( final EntityManager em,
		final LocalBinder binder )
	{
		return find( em, binder,
				"SELECT dao FROM " + Dao.ENTITY_NAME + " dao" );
	}

	@Override
	default Dao persist( final EntityManager em )
	{
		final Dao result = new Dao();
		result.id = Objects.requireNonNull( id().unwrap() );
		result.kind = Objects.requireNonNull( kind() );
		result.initiatorRef = Objects.requireNonNull( initiatorRef() )
				.persist( em );
		result.executorRef = Objects.requireNonNull( executorRef() )
				.persist( em );
		em.persist( result );
		return result;
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
	 * @param tranKind
	 * @param scheduler
	 * @param id
	 * @param initiator
	 * @param executor
	 * @return
	 */
	static <F extends CoordinationFact> Transaction<F> of(
		final LocalBinder binder, final Transaction.ID id,
		final Class<F> tranKind, final CompositeActor.ID initiator,
		final CompositeActor.ID executor )
	{
		return of( tranKind, binder.inject( Scheduler.class ), id, initiator,
				executor, binder.inject( CoordinationFact.Factory.class ),
				binder.inject( CoordinationFactBank.Factory.class ) );
	}

	/**
	 * @param kind
	 * @param scheduler
	 * @param id
	 * @param initiatorRef
	 * @param executorRef
	 * @param factFactory a {@link CoordinationFact.Factory}
	 * @param bankFactory a {@link CoordinationFactBank.Factory} or {@code null}
	 * @return a {@link Simple} instance
	 */
	static <F extends CoordinationFact> Transaction<F> of( final Class<F> kind,
		final Scheduler scheduler, final Transaction.ID id,
		final CompositeActor.ID initiatorRef,
		final CompositeActor.ID executorRef,
		final CoordinationFact.Factory factFactory,
		final CoordinationFactBank.Factory bankFactory )
	{
		return new Simple<F>( kind, scheduler, id, initiatorRef, executorRef,
				factFactory, bankFactory );
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
		private CoordinationFactBank<F> factBank;
		private Class<F> kind;
		private Scheduler scheduler;
		private Transaction.ID id;
		private CompositeActor.ID initiatorRef;
		private CompositeActor.ID executorRef;
		private CoordinationFact.Factory factFactory;

		public Simple()
		{
		}

		@Inject
		public Simple( final LocalBinder binder, final Class<F> kind,
			final Transaction.ID id, final CompositeActor.ID initiatorRef,
			final CompositeActor.ID executorRef )
		{
			this( kind, binder.inject( Scheduler.class ), id, initiatorRef,
					executorRef,
					binder.inject( CoordinationFact.Factory.class ),
					binder.inject( CoordinationFactBank.Factory.class ) );
		}

		public Simple( final Class<F> kind, final Scheduler scheduler,
			final Transaction.ID id, final CompositeActor.ID initiatorRef,
			final CompositeActor.ID executorRef,
			final CoordinationFact.Factory factFactory,
			final CoordinationFactBank.Factory bankFactory )
		{
			this.id = id;
			this.kind = kind;
			this.initiatorRef = initiatorRef;
			this.executorRef = executorRef;
			this.scheduler = scheduler;
			this.factFactory = factFactory;
			this.factBank = bankFactory == null ? null
					: bankFactory.create( kind );
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
		public CoordinationFactBank<F> facts()
		{
			return this.factBank;
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
			final CoordinationFact.ID causeRef, final boolean terminal,
			final Instant expiration, final Map<?, ?>... params )
		{
			try
			{
				final CompositeActor.ID creator = factKind.isFromInitiator()
						? initiatorRef() : executorRef();
				final F result = this.factFactory.create( kind(),
						CoordinationFact.ID.create( creator ), this, factKind,
						expiration, causeRef, params );
				if( causeRef != null ) this.pending.remove( causeRef );
				if( expiration != null )
					this.pending.put( result.id(), at( expiration ).call( () ->
					{
						Objects.requireNonNull( result.id() );
						this.pending.remove( result.id() );
						this.expired.onNext( result );
					} ) );
				this.generated.onNext( result );
				if( facts() != null ) facts().save( result );
				if( terminal )
				{
					this.pending.values().forEach( Expectation::remove );
					this.pending.clear();
					this.expired.onCompleted();
					this.generated.onCompleted();
				}
				return result;
			} catch( final Throwable e )
			{
				this.generated.onError( e );
				throw e;
			}
		}

		@Override
		public Observable<F> generated()
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
			if( fact.expiration() != null ) this.pending.put( fact.id(),
					scheduler.at( fact.expiration() ).call( () ->
					{
						this.pending.remove( fact.id() );
						this.expired.onNext( fact );
					} ) );
		}
	}

	/**
	 * {@link Dao}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Entity( name = Dao.ENTITY_NAME )
	public class Dao implements BindableDao<Transaction<?>, Dao>
	{
		public static final String ENTITY_NAME = "TRANSACTIONS";

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		@Column( name = "PK", nullable = false, updatable = false )
		protected Integer pk;

//		@Id // FIXME can't use this BINARY(16) as foreign key ?
		@Column( name = "ID", nullable = false, updatable = false, length = 16,
			columnDefinition = "BINARY(16)" )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID id;

		/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
		@Temporal( TemporalType.TIMESTAMP )
		@Column( name = "CREATED_TS", insertable = false, updatable = false,
			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
		@JsonIgnore
		protected Date created;

//		/** time stamp of last update; should never change */
//		@Version
//		@Temporal( TemporalType.TIMESTAMP )
//		@Column( name = "UPDATED_TS", insertable = false, updatable = false,
//			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
//		@JsonIgnore
//		protected Date updated;

		@Column( name = "KIND", nullable = false, updatable = false )
		protected Class<? extends CoordinationFact> kind;

		@ManyToOne( optional = false ) //( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		@JoinColumn( name = "INITIATOR_ID", updatable = false )
//		@Embedded
//		@AttributeOverrides( @AttributeOverride( name = "id",
//			column = @Column( name = "INITIATOR_ID" ) ) )
//		@AssociationOverride( name = "id",
//			joinColumns = @JoinColumn(name = "INITIATOR_ID",insertable = false, updatable = false ) )
		protected CompositeActor.ID.Dao initiatorRef;

		@ManyToOne( optional = false ) //( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		@JoinColumn( name = "EXECUTOR_ID", updatable = false )
//		@Embedded
//		@AttributeOverrides( @AttributeOverride( name = "id",
//			column = @Column( name = "EXECUTOR_ID" ) ) )
//		@AssociationOverride( name = "id",
//			joinColumns = @JoinColumn( name = "EXECUTOR_ID",insertable = false, updatable = false ) )
		protected CompositeActor.ID.Dao executorRef;

		@Override
		public Transaction<?> restore( final LocalBinder binder )
		{
			return binder.inject( Transaction.Factory.class ).create(
					Transaction.ID.of( this.id, binder.id() ), this.kind,
					CompositeActor.ID.of( this.initiatorRef.restore( binder ) ),
					CompositeActor.ID
							.of( this.executorRef.restore( binder ) ) );
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
						k ->
						{
							return Transaction.of( this.binder, id, kind,
									initiator, executor );
						} );
			}

		}
	}
}