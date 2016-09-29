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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.exception.Thrower;
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
//@BeanProxy
public interface CoordinationFact extends
	Identified.Ordinal<CoordinationFact.ID>, Persistable<CoordinationFact.Dao>
{
	String TRANSACTION_PROPERTY = "transaction";

	String KIND_PROPERTY = "kind";

	String OCCURRENCE_PROPERTY = "occurrence";

	String EXPIRATION_PROPERTY = "expiration";

	String CAUSE_REF_PROPERTY = "causeRef";

	/** @return */
	@JsonProperty( TRANSACTION_PROPERTY )
	<F extends CoordinationFact> Transaction<F> transaction();

	default Class<? extends CoordinationFact> type()
	{
		return transaction() == null ? getClass() : transaction().kind();
	}

	@SuppressWarnings( "unchecked" )
	default <F extends CoordinationFact> void save()
	{
		((Transaction<F>) transaction()).facts().save( (F) this );
	}

	/** @return */
	// derived @JsonIgnore 
	default CompositeActor.ID creatorRef()
	{
		return id().parentRef() != null ? id().parentRef()
				: kind().originatorRoleType() == CoordinationRoleKind.EXECUTOR
						? transaction().executorRef()
						: transaction().initiatorRef();
	}

	/** @return */
	// derived @JsonIgnore 
	default CompositeActor.ID responderRef()
	{
		return kind().responderRoleKind() == CoordinationRoleKind.EXECUTOR
				? transaction().executorRef() : transaction().initiatorRef();
	}

	/**
	 * @return {@code true} iff {@link #creator()} and {@link #responder()} are
	 *         of different {@linkplain CompositeActor.ID#organization() roots}
	 */
	@JsonIgnore // derived  
	default boolean isOutgoing()
	{
		return !creatorRef().organization()
				.equals( responderRef().organization() );
	}

	/** @return */
	@JsonProperty( KIND_PROPERTY )
	CoordinationFactKind kind();

	/** @return */
	@JsonProperty( OCCURRENCE_PROPERTY )
	Instant occurrence();

	/** @return */
	@JsonProperty( EXPIRATION_PROPERTY )
	Instant expiration();

	/** @return */
	@JsonProperty( CAUSE_REF_PROPERTY )
	CoordinationFact.ID causeRef();

	/** @return */
	// implement as @JsonAnyGetter
	Map<String, Object> properties();

	// implement as @JsonAnySetter
	default Object set( final String name, final Object value )
	{
		return properties().put( name, value );
	}

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

	@Override
	default Dao persist( final EntityManager em )
	{
		final Dao result = new Dao();

		final Date offset = Date.from( transaction().offset() );

		result.id = Objects.requireNonNull( id().unwrap() );
		result.kind = Objects.requireNonNull( kind() );
		result.transaction = Objects.requireNonNull( transaction() )
				.persist( em );
		result.creatorRef = Objects.requireNonNull( creatorRef() )
				.persist( em );
		result.causeRef = causeRef() == null ? null
				: Objects.requireNonNull( causeRef().unwrap() );
		result.causeCreatorRef = causeRef() == null ? null
				: Objects.requireNonNull( causeRef().parentRef() )
						.persist( em );
		result.occurrence = Objects.requireNonNull( occurrence() )
				.toDate( offset );
		result.expiration = expiration() == null ? null
				: expiration().toDate( offset );
		result.properties = JsonUtil.toTree( properties() );
		em.persist( result );
		return result;
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

	// @JsonValue
	default String toJSON()
	{
		return JsonUtil.stringify( this );
	}

	static <F extends CoordinationFact> F fromJSON( final String json,
		final Class<F> factType )
	{
		return fromJSON( JsonUtil.getJOM(), json, factType );
	}

	static <F extends CoordinationFact> F fromJSON( final TreeNode json,
		final Class<F> factType )
	{
		return fromJSON( JsonUtil.getJOM(), json, factType );
	}

	static <F extends CoordinationFact> F fromJSON( final ObjectMapper om,
		final String json, final Class<F> factType )
	{
		try
		{
			return json == null ? null
					: fromJSON( om, om.readTree( json ), factType );
		} catch( final Exception e )
		{
			e.printStackTrace();
			return Thrower.rethrowUnchecked( e );
		}
	}

	static <F extends CoordinationFact> F fromJSON( final ObjectMapper om,
		final TreeNode json, final Class<F> factType )
	{
		try
		{
			Transaction.Simple.checkRegistered( om );
//			JsonUtil.checkRegisteredMembers( om, Transaction.Simple.class );
			JsonUtil.checkRegisteredMembers( om,
					CoordinationFact.Simple.class );
			return json == null ? null
					: JsonUtil.valueOf( json, Simple.class ).proxyAs( factType,
							null );
		} catch( final Exception e )
		{
			e.printStackTrace();
			return Thrower.rethrowUnchecked( e );
		}
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

		@Override
		public UUID unwrap()
		{
			return (UUID) super.unwrap();
		}

		@Override
		public CompositeActor.ID parentRef()
		{
			return (CompositeActor.ID) super.parentRef();
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
		protected CompositeActor.ID.Dao creatorRef;

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
		protected UUID causeRef;

		@ManyToOne( optional = true, fetch = FetchType.LAZY,
			cascade = CascadeType.PERSIST )
		@JoinColumn( name = "CAUSER_ID", updatable = false )
//		@Embedded
//		@AttributeOverrides( { @AttributeOverride( name = "id",
//			column = @Column( name = "CAUSER_ID" ) ), } )
//		@AssociationOverride( name = "id",
//			joinColumns = @JoinColumn( name = "CAUSER_ID", insertable = false,
//				updatable = false ) )
		protected CompositeActor.ID.Dao causeCreatorRef;

		@Column( name = "PROPERTIES", nullable = true, updatable = false )
		@Convert( converter = JsonNodeToStringConverter.class )
		protected JsonNode properties;

		@SuppressWarnings( { "unchecked", "rawtypes" } )
		@Override
		public CoordinationFact restore( final LocalBinder binder )
		{
			Objects.requireNonNull( this.transaction );
			Objects.requireNonNull( this.creatorRef );
			if( this.causeRef != null )
				Objects.requireNonNull( this.causeCreatorRef );

			final CompositeActor.ID creator = CompositeActor.ID
					.of( this.creatorRef.restore( binder ) );
			final ID id = ID.of( Objects.requireNonNull( this.id ), creator );
			final Transaction tran = this.transaction.restore( binder );
			final ID cause = this.causeRef == null ? null
					: ID.of( this.causeRef, CompositeActor.ID
							.of( this.causeCreatorRef.restore( binder ) ) );
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
	@JsonInclude( Include.NON_NULL )
	class Simple implements CoordinationFact
	{
		private CoordinationFact.ID id;
		private Instant occurrence;
		private Transaction<?> transaction;
		private CoordinationFactKind kind;
		private Instant expiration;
		private CoordinationFact.ID causeRef;
		private Map<String, Object> properties = new HashMap<>();

		/**
		 * {@link Simple} zero-arg bean constructor
		 */
		protected Simple()
		{

		}

		protected Simple( //final Class<?> type, 
			final CoordinationFact.ID id, final Instant occurrence,
			final Transaction<?> transaction, final CoordinationFactKind kind,
			final Instant expiration, final CoordinationFact.ID causeRef,
			final Map<?, ?>... properties )
		{
			this.id = id;
			this.occurrence = occurrence;
			this.transaction = transaction;
			this.kind = kind;
			this.expiration = expiration;
			this.causeRef = causeRef;
			if( properties != null ) for( Map<?, ?> map : properties )
				map.forEach( ( key, value ) ->
				{
					this.properties.put( key.toString(), value );
				} );
		}

		@Override
		public String toString()
		{
			return type().getSimpleName() + '['
					+ Integer.toHexString( id().unwrap().hashCode() ) + '|'
					+ kind() + '|' + creatorRef() + '|' + occurrence() + ']'
					+ properties();
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
		public CoordinationFact.ID causeRef()
		{
			return this.causeRef;
		}

		@Override
		@JsonAnyGetter
		public Map<String, Object> properties()
		{
			return this.properties;
		}

		@Override
		@JsonAnySetter
		public Object set( final String key, final Object value )
		{
			return properties.put( key, value );
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

		LocalId ownerRef();

		/**
		 * @param tranKind the type of {@link CoordinationFact} (transaction
		 *            kind)
		 * @param id the {@link CoordinationFact.ID}
		 * @param transaction the {@link Transaction}
		 * @param factKind the {@link CoordinationFactKind} (process step kind)
		 * @param expiration the {@link Instant} of expiration
		 * @param causeRef the cause {@link CoordinationFact.ID}, or
		 *            {@code null} for external initiation
		 * @param properties property {@link Map mappings}, if any
		 * @return a {@link CoordinationFact}
		 */
		default <F extends CoordinationFact> F create( Class<F> tranKind,
			CoordinationFact.ID id, Transaction<? super F> transaction,
			CoordinationFactKind factKind, Instant expiration,
			CoordinationFact.ID causeRef, Map<?, ?>... properties )
		{
			return create( tranKind, id, transaction, factKind,
					transaction.now(), expiration, causeRef, properties );
		}

		/**
		 * @param tranKind the type of {@link CoordinationFact} (transaction
		 *            kind)
		 * @param id the {@link CoordinationFact.ID}
		 * @param transaction the {@link Transaction}
		 * @param factKind the {@link CoordinationFactKind} (process step kind)
		 * @param occurrence the {@link Instant} of occurrence
		 * @param expiration the {@link Instant} of expiration
		 * @param causeRef the cause {@link CoordinationFact.ID}, or
		 *            {@code null} for external initiation
		 * @param properties property {@link Map mappings}, if any
		 * @return a {@link CoordinationFact}
		 */
		<F extends CoordinationFact> F create( Class<F> tranKind,
			CoordinationFact.ID id, Transaction<? super F> transaction,
			CoordinationFactKind factKind, Instant occurrence,
			Instant expiration, CoordinationFact.ID causeRef,
			Map<?, ?>... properties );

		/**
		 * {@link SimpleProxies} generates the desired extension of
		 * {@link CoordinationFact} as proxy decorating a new
		 * {@link CoordinationFact.Simple} instance
		 */
		@Singleton
		class SimpleProxies implements Factory
		{
			@Inject
			private LocalBinder binder;

			@Override
			public <F extends CoordinationFact> F create(
				final Class<F> tranKind, final CoordinationFact.ID id,
				final Transaction<? super F> transaction,
				final CoordinationFactKind factKind, final Instant occurrence,
				final Instant expiration, final CoordinationFact.ID causeRef,
				final Map<?, ?>... params )
			{
				return new CoordinationFact.Simple( //tranKind, 
						id, occurrence, transaction, factKind, expiration,
						causeRef, params ).proxyAs( tranKind, null );
			}

			@Override
			public LocalId ownerRef()
			{
				return this.binder.id();
			}
		}
	}
}