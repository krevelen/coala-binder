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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.json.JsonUtil;
import io.coala.name.Identified;
import io.coala.persist.JsonNodeToStringConverter;
import io.coala.persist.Persistable;
import io.coala.persist.UUIDToByteConverter;
import io.coala.time.Instant;
import rx.Observer;

/**
 * {@link CoordinationFact}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface CoordinationFact extends
	Identified.Ordinal<CoordinationFact.ID>, Persistable<CoordinationFact.Dao>
{
	/** @return */
	<F extends CoordinationFact> Transaction<F> transaction();

	@SuppressWarnings( "unchecked" )
	default <F extends CoordinationFact> void save()
	{
		((Transaction<F>) transaction()).facts().save( (F) this );
	}

	/** @return */
	default CompositeActor.ID creator()
	{
		return id().parent();
	}

	/** @return */
	CoordinationFactKind kind();

	/** @return */
	Instant occurrence();

	/** @return */
	Instant expiration();

	/** @return */
	@JsonIgnore
	CoordinationFact.ID cause();

	/** @return */
	@JsonAnyGetter
	Map<String, Object> properties();

	default Stream<CoordinationFact> find( final EntityManager em,
		final LocalBinder binder, final String query )
	{
		return em.createQuery( query, Dao.class ).getResultList().stream()
				.map( dao ->
				{
					return dao.restore( binder );
				} );
	}

	default Stream<CoordinationFact> findAll( final EntityManager em,
		final LocalBinder binder )
	{
		return find( em, binder,
				"SELECT dao FROM " + Dao.ENTITY_NAME + " dao" );
	}

	default Dao persist( final EntityManager em )
	{
		final Dao result = new Dao();

		final Date offset = Date.from( transaction().offset() );

		result.id = Objects.requireNonNull( id().unwrap() );
		result.kind = Objects.requireNonNull( kind() );
		result.transaction = Objects.requireNonNull( transaction() )
				.persist( em );
		result.creator = Objects.requireNonNull( creator() ).persist( em );
		result.cause = cause() == null ? null
				: Objects.requireNonNull( cause().unwrap() );
		result.causeCreator = cause() == null ? null
				: Objects.requireNonNull( cause().parent() ).persist( em );
		result.occurrence = Objects.requireNonNull( occurrence() )
				.toDate( offset );
		result.expiration = expiration() == null ? null
				: expiration().toDate( offset );
		result.properties = JsonUtil.toTree( properties() );
		em.persist( result );
		return result;
	}

	@JsonAnySetter
	default void set( final String name, final Object value )
	{
		properties().put( name, value );
	}

	/**
	 * @param subtype the type of {@link CoordinationFact} to mimic
	 * @param callObserver an (optional) {@link Observer} of method call
	 * @return the {@link Proxy} instance
	 */
	@SuppressWarnings( "unchecked" )
	default <F extends CoordinationFact> F proxyAs( final Class<F> subtype,
		final Observer<Method> callObserver )
	{
		final CoordinationFact self = this;
		return (F) Proxy.newProxyInstance( subtype.getClassLoader(),
				new Class<?>[]
		{ subtype }, ( proxy, method, args ) ->
		{
			try
			{
				final Object result = method.invoke( self, args );
				if( callObserver != null ) callObserver.onNext( method );
				// FIXME allow getter calls as lookup in properties() map
				return result;
			} catch( Throwable e )
			{
				if( e instanceof InvocationTargetException ) e = e.getCause();
				if( callObserver != null ) callObserver.onError( e );
				throw e;//return null;//Thrower.rethrowUnchecked( e );
			}
		} );
	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends LocalId
	{

		@Override
		public UUID unwrap()
		{
			return (UUID) super.unwrap();
		}

		@Override
		public CompositeActor.ID parent()
		{
			return (CompositeActor.ID) super.parent();
		}

		/** @return an {@link ID} with specified {@link UUID} */
		public static ID of( final UUID value, final CompositeActor.ID ctx )
		{
			return LocalId.of( new ID(), value, ctx );
		}

		/** @return a new {@link ID} */
		public static ID create( final CompositeActor.ID ctx )
		{
			return of( new UUID(), ctx );
		}

//		@Entity( name = Dao.ENTITY_NAME )
//		public static class Dao extends LocalId.Dao
//		{
//			public static final String ENTITY_NAME = "FACT_IDS";
//
//			@Override
//			public ID restore( final LocalBinder binder )
//			{
//				return ID.of( new UUID( this.myId ),
//						this.parentId.restore( binder ) );
//			}
//
//			@Override
//			public Dao prePersist( final LocalId source )
//			{
//				super.prePersist( source );
//				return this;
//			}
//		}
	}

	/**
	 * {@link Dao}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Entity( name = Dao.ENTITY_NAME )
	@AttributeOverride( name = "transaction.kind",
		column = @Column( name = Dao.TYPE_ATTR_NAME ) )
	public class Dao implements BindableDao<CoordinationFact, Dao>
	{
		public static final String ENTITY_NAME = "FACTS";

		public static final String TYPE_ATTR_NAME = "TYPE";

		public static final String KIND_ATTR_NAME = "KIND";

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

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		@Column( name = "PK", nullable = false, updatable = false )
		protected Integer pk;

//		@Id
		@Column( name = "ID", nullable = false, updatable = false, length = 16,
			columnDefinition = "BINARY(16)" )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID id;

		@ManyToOne( optional = false, fetch = FetchType.LAZY,
			cascade = CascadeType.PERSIST )
		@JoinColumn( name = "CREATOR_ID", updatable = false )
//		@Embedded
//		@AssociationOverride( name = "id",
//			joinColumns = @JoinColumn( name = "CREATOR_ID", insertable = false,
//				updatable = false ) )
		protected CompositeActor.ID.Dao creator;

		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		@JoinColumn( name = "TRANSACTION_ID", nullable = false,
			updatable = false )
		protected Transaction.Dao transaction;

		@Column( name = KIND_ATTR_NAME, nullable = false, updatable = false )
		protected CoordinationFactKind kind;

		@Temporal( TemporalType.TIMESTAMP )
		@Column( name = "OCCURRENCE", nullable = false, updatable = false )
		protected Date occurrence;

		@Temporal( TemporalType.TIMESTAMP )
		@Column( name = "EXPIRATION", nullable = true, updatable = false )
		protected Date expiration;

		@Column( name = "CAUSE_ID", nullable = true, updatable = false,
			length = 16, columnDefinition = "BINARY(16)" )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID cause;

		@ManyToOne( optional = true, fetch = FetchType.LAZY,
			cascade = CascadeType.PERSIST )
		@JoinColumn( name = "CAUSER_ID", updatable = false )
//		@Embedded
//		@AttributeOverrides( { @AttributeOverride( name = "id",
//			column = @Column( name = "CAUSER_ID" ) ), } )
//		@AssociationOverride( name = "id",
//			joinColumns = @JoinColumn( name = "CAUSER_ID", insertable = false,
//				updatable = false ) )
		protected CompositeActor.ID.Dao causeCreator;

		@Column( name = "PROPERTIES", nullable = true, updatable = false )
		@Convert( converter = JsonNodeToStringConverter.class )
		protected JsonNode properties;

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public CoordinationFact restore( final LocalBinder binder )
		{
			Objects.requireNonNull( this.transaction );
			Objects.requireNonNull( this.creator );
			if( this.cause != null )
				Objects.requireNonNull( this.causeCreator );

			final CompositeActor.ID creator = CompositeActor.ID
					.of( this.creator.restore( binder ) );
			final ID id = ID.of( Objects.requireNonNull( this.id ), creator );
			final Transaction tran = this.transaction.restore( binder );
			final ID cause = this.cause == null ? null
					: ID.of( this.cause, CompositeActor.ID
							.of( this.causeCreator.restore( binder ) ) );
			final Date offset = Date.from( tran.offset() );
			final Instant occurrence = Instant
					.of( Objects.requireNonNull( this.occurrence ), offset );
			final Instant expiration = this.expiration == null ? null
					: Instant.of( this.expiration, offset );

			return binder.inject( CoordinationFact.Factory.class ).create(
					tran.kind(), id, tran, this.kind, occurrence, expiration,
					cause, (Map<?, ?>) JsonUtil.valueOf( this.properties,
							Map.class ) );
		}
	}

	/**
	 * {@link Simple}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Simple implements CoordinationFact
	{

		private Class<?> type;

		private CoordinationFact.ID id;

		private Instant occurrence;

		private Transaction<?> transaction;

		private CoordinationFactKind kind;

		private Instant expiration;

		private CoordinationFact.ID cause;

		private Map<String, Object> properties = new HashMap<>();

		/**
		 * {@link Simple} zero-arg bean constructor
		 */
		protected Simple()
		{

		}

		protected Simple( final Class<?> type, final CoordinationFact.ID id,
			final Instant occurrence, final Transaction<?> transaction,
			final CoordinationFactKind kind, final Instant expiration,
			final CoordinationFact.ID cause, final Map<?, ?>... properties )
		{
			this.type = type;
			this.id = id;
			this.occurrence = occurrence;
			this.transaction = transaction;
			this.kind = kind;
			this.expiration = expiration;
			this.cause = cause;
			if( properties != null ) for( Map<?, ?> map : properties )
				map.forEach( ( key, value ) ->
				{
					this.properties.put( key.toString(), value );
				} );
		}

		@Override
		public String toString()
		{
			return this.type.getSimpleName() + '[' + kind() + '|' + creator()
					+ '|' + occurrence() + ']' + properties();
		}

		@Override
		public int hashCode()
		{
			return Identified.hashCode( this );
		}

		@Override
		public CoordinationFact.ID id()
		{
			return this.id;
		}

		@Override
		public Instant occurrence()
		{
			return this.occurrence;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public Transaction<?> transaction()
		{
			return this.transaction;
		}

		@Override
		public CoordinationFactKind kind()
		{
			return this.kind;
		}

		@Override
		public Instant expiration()
		{
			return this.expiration;
		}

		@Override
		public CoordinationFact.ID cause()
		{
			return this.cause;
		}

		@Override
		public Map<String, Object> properties()
		{
			return this.properties;
		}
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Factory
	{

		LocalId owner();

		/**
		 * @param tranKind the type of {@link CoordinationFact} (transaction
		 *            kind)
		 * @param id the {@link CoordinationFact.ID}
		 * @param transaction the {@link Transaction}
		 * @param factKind the {@link CoordinationFactKind} (process step kind)
		 * @param expiration the {@link Instant} of expiration
		 * @param cause the cause {@link CoordinationFact.ID}, or {@code null}
		 *            for external initiation
		 * @param properties the properties
		 * @return a {@link CoordinationFact}
		 */
		default <F extends CoordinationFact> F create( Class<F> tranKind,
			CoordinationFact.ID id, Transaction<? super F> transaction,
			CoordinationFactKind factKind, Instant expiration,
			CoordinationFact.ID cause, Map<?, ?>... properties )
		{
			return create( tranKind, id, transaction, factKind,
					transaction.now(), expiration, cause, properties );
		}

		<F extends CoordinationFact> F create( Class<F> tranKind,
			CoordinationFact.ID id, Transaction<? super F> transaction,
			CoordinationFactKind factKind, Instant occurrence,
			Instant expiration, CoordinationFact.ID cause,
			Map<?, ?>... properties );

		/**
		 * {@link CoordinationFact.Factory.Simple} generates the desired
		 * extension of {@link CoordinationFact} as proxy decorating a new
		 * {@link CoordinationFact.Simple} instance
		 */
		@Singleton
		class Simple implements Factory
		{
			@Inject
			private LocalBinder binder;

			@Override
			public <F extends CoordinationFact> F create(
				final Class<F> tranKind, final CoordinationFact.ID id,
				final Transaction<? super F> transaction,
				final CoordinationFactKind factKind, final Instant occurrence,
				final Instant expiration, final CoordinationFact.ID cause,
				final Map<?, ?>... params )
			{
				return new CoordinationFact.Simple( tranKind, id, occurrence,
						transaction, factKind, expiration, cause, params )
								.proxyAs( tranKind, null );
			}

			@Override
			public LocalId owner()
			{
				return this.binder.id();
			}
		}
	}
}