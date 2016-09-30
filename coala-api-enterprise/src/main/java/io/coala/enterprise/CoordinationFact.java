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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.DecimalMeasure;
import javax.measure.unit.Unit;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.math.MeasureUtil;
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

	String OCCUR_PROPERTY = "occur";

	String OCCUR_UTC_PROPERTY = "occurUtcSec";

	String EXPIRE_PROPERTY = "expire";

	String EXPIRE_UTC_PROPERTY = "expireUtcSec";

	String CAUSE_REF_PROPERTY = "causeRef";

	/** @return */
	@JsonProperty( TRANSACTION_PROPERTY )
	<F extends CoordinationFact> Transaction<F> transaction();

	<F extends CoordinationFact> F self(); // "this" points to impl, not proxy

	@SuppressWarnings( "unchecked" )
	default <F extends CoordinationFact> F commit( final boolean cleanUp )
	{
		return ((Transaction<F>) transaction()).commit( self(), cleanUp );
	}

	default Class<? extends CoordinationFact> type()
	{
		return Objects.requireNonNull( transaction() ).kind();
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
	@JsonProperty( OCCUR_PROPERTY )
	Instant occur();

	/** @return */
	@JsonProperty( OCCUR_UTC_PROPERTY )
	default java.time.Instant occurUtc()
	{
		return occur().toDate( transaction().offset() );
	}

	/** @return */
	@JsonProperty( EXPIRE_PROPERTY )
	Instant expire();

	/** @return */
	@JsonProperty( EXPIRE_UTC_PROPERTY )
	default java.time.Instant expireUtc()
	{
		return expire() == null ? null
				: expire().toDate( transaction().offset() );
	}

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

	@SuppressWarnings( "unchecked" )
	@Override
	default Dao persist( final EntityManager em )
	{
		final Date offset = Date.from( transaction().offset() );
		final Unit<?> unit = transaction().timeUnit();
		final Instant occurrence = Objects.requireNonNull( occur() );
		final Instant expiration = expire();
		final Transaction<?> tx = Objects.requireNonNull( transaction() );
		final ID causeRef = causeRef();

		final Dao result = new Dao();
		result.id = Objects.requireNonNull( id().unwrap() );
		result.tid = Objects.requireNonNull( tx.id().unwrap() );
		result.type = Objects.requireNonNull( tx.kind() );
		result.kind = Objects.requireNonNull( kind() );
		result.initiatorRef = Objects.requireNonNull( tx.initiatorRef() )
				.persist( em );
		result.executorRef = Objects.requireNonNull( tx.executorRef() )
				.persist( em );
		result.creatorRef = Objects.requireNonNull( creatorRef() )
				.persist( em );
		result.causeRef = causeRef == null ? null
				: Objects.requireNonNull( causeRef.unwrap() );
		result.causeCreatorRef = causeRef == null ? null
				: Objects.requireNonNull( causeRef.parentRef() ).persist( em );
		result.occurrence = Dao.InstantDao.of( occurrence, offset, unit );
		result.expiration = Dao.InstantDao.of( expiration, offset, unit );
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
		final CoordinationFact impl = this;
		final F proxy = (F) Proxy.newProxyInstance( subtype.getClassLoader(),
				new Class<?>[]
		{ subtype }, ( self, method, args ) ->
		{
			try
			{
				final Object result = method.invoke( impl, args );
				if( callObserver != null ) callObserver.onNext( method );
				return result;
			} catch( Throwable e )
			{
				if( e instanceof IllegalArgumentException )
					return invokeAsBean( impl.properties(), subtype, method,
							args );
				if( e instanceof InvocationTargetException ) e = e.getCause();
				if( callObserver != null ) callObserver.onError( e );
				throw e;
			}
		} );
		if( impl instanceof Simple ) ((Simple) impl).proxy = proxy;
		return proxy;
	}

	/**
	 * match bean read method to bean property
	 * <p>
	 * TODO allow builder-type (i.e. chaining 'with*') setters?
	 * 
	 * @param beanType
	 * @param properties
	 * @param method
	 * @param args
	 * @return
	 * @throws IntrospectionException
	 */
	static Object invokeAsBean( final Map<String, Object> properties,
		final Class<?> beanType, final Method method, final Object... args )
		throws IntrospectionException
	{
		final BeanInfo beanInfo = Introspector.getBeanInfo( beanType );
		for( PropertyDescriptor pd : beanInfo.getPropertyDescriptors() )
			if( method.equals( pd.getReadMethod() ) )
				return properties.get( pd.getName() );
			else if( method.equals( pd.getWriteMethod() ) )
				return properties.put( pd.getName(), args[0] );
		return Thrower.throwNew( IllegalArgumentException.class,
				"Can't invoke {} as bean method", method );
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
		if( json == null ) return null;
		try
		{
			Transaction.Simple.checkRegistered( om );
			JsonUtil.checkRegisteredMembers( om,
					CoordinationFact.Simple.class );
			final Simple result = JsonUtil.valueOf( om, json, Simple.class );
			fromJSON( om, json, factType, result.properties() );
			return result.proxyAs( factType, null );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * override and deserialize bean properties as declared in factType
	 * <p>
	 * TODO allow builder-type (i.e. with* chaining setter) properties?
	 * 
	 * @param om
	 * @param json
	 * @param factType
	 * @param properties
	 * @return the properties again, to allow chaining
	 * @throws IntrospectionException
	 */
	static <T extends CoordinationFact> Map<String, Object> fromJSON(
		final ObjectMapper om, final TreeNode json, final Class<T> factType,
		final Map<String, Object> properties )
	{
		try
		{
			final ObjectNode tree = (ObjectNode) json;
			final BeanInfo beanInfo = Introspector.getBeanInfo( factType );
			for( PropertyDescriptor pd : beanInfo.getPropertyDescriptors() )
				if( tree.has( pd.getName() ) )
					properties.computeIfPresent( pd.getName(),
							( property, current ) -> JsonUtil.valueOf( om,
									tree.get( property ),
									pd.getPropertyType() ) );
			return properties;
		} catch( final Throwable e )
		{
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
	public class Dao implements BindableDao<CoordinationFact, Dao>
	{
		public static final String ENTITY_NAME = "FACTS";

		public static final String TYPE_COL_NAME = "TYPE";

		public static final String KIND_COL_NAME = "KIND";

		@Embeddable
		public static class InstantDao
			implements BindableDao<Instant, InstantDao>
		{
			public static final String UTC_ATTR_NAME = "utc"; // Java attribute
			public static final String NUM_ATTR_NAME = "num"; // Java attribute
			public static final String STR_ATTR_NAME = "str"; // Java attribute

			@Temporal( TemporalType.TIMESTAMP )
			@Column
			protected Date utc; // derived, based on offset

			@Column
			protected BigDecimal num; // derived, based on common time unit

			@Column
			protected String str; // exact precision, scale, and unit

			/**
			 * @param expiration
			 * @param offset
			 * @param unit
			 * @return
			 */
			@SuppressWarnings( { "unchecked", "rawtypes" } )
			public static InstantDao of( final Instant instant,
				final Date offset, final Unit unit )
			{
				final InstantDao result = new InstantDao();
				if( instant == null )
				{
					result.utc = null;
					result.num = null;
					result.str = null;
				} else
				{
					result.utc = instant.toDate( offset );
					result.num = MeasureUtil.toBigDecimal( instant.unwrap() );
					result.str = instant.toString();
				}
				return result;
			}

			@Override
			public Instant restore( final LocalBinder binder )
			{
				return this.str == null ? null
						: Instant.of( DecimalMeasure.valueOf( this.str ) );
			}
		}

		/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
		@Temporal( TemporalType.TIMESTAMP )
		@Column( name = "CREATED_TS", insertable = false, updatable = false,
			columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" )
		@JsonIgnore
		protected Date created;

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		@Column( name = "PK", nullable = false, updatable = false )
		protected Integer pk;

		@Column( name = "ID", nullable = false, updatable = false, length = 16,
			columnDefinition = "BINARY(16)", unique = true )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID id;

		@ManyToOne( optional = false, fetch = FetchType.LAZY,
			cascade = CascadeType.PERSIST )
		@JoinColumn( name = "CREATOR_ID", updatable = false )
		protected CompositeActor.ID.Dao creatorRef;

		@Column( name = "TID", nullable = false, updatable = false, length = 16,
			columnDefinition = "BINARY(16)" )
		@Convert( converter = UUIDToByteConverter.class )
		protected UUID tid;

		@Column( name = "TYPE", nullable = false, updatable = false )
		protected Class<? extends CoordinationFact> type;

		@ManyToOne( optional = false ) //( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		@JoinColumn( name = "INITIATOR_ID", updatable = false )
		protected LocalId.Dao initiatorRef;

		@ManyToOne( optional = false ) //( fetch = FetchType.LAZY, cascade = CascadeType.PERSIST )
		@JoinColumn( name = "EXECUTOR_ID", updatable = false )
		protected LocalId.Dao executorRef;

		@Column( name = KIND_COL_NAME, nullable = false, updatable = false )
		protected CoordinationFactKind kind;

		@AttributeOverrides( {
				@AttributeOverride( name = InstantDao.UTC_ATTR_NAME,
					column = @Column( name = "OCCUR_ISO", nullable = false,
						updatable = false ) ),
				@AttributeOverride( name = InstantDao.NUM_ATTR_NAME,
					column = @Column( name = "OCCUR", nullable = false,
						updatable = false ) ),
				@AttributeOverride( name = InstantDao.STR_ATTR_NAME,
					column = @Column( name = "OCCUR_STR", nullable = false,
						updatable = false ) ), } )
		@Embedded
		protected InstantDao occurrence;

		@AttributeOverrides( {
				@AttributeOverride( name = InstantDao.UTC_ATTR_NAME,
					column = @Column( name = "EXPIRE_ISO",
						updatable = false ) ),
				@AttributeOverride( name = InstantDao.NUM_ATTR_NAME,
					column = @Column( name = "EXPIRE", updatable = false ) ),
				@AttributeOverride( name = InstantDao.STR_ATTR_NAME,
					column = @Column( name = "EXPIRE_STR",
						updatable = false ) ), } )
		@Embedded
		protected InstantDao expiration;

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
//			Objects.requireNonNull( this.transaction );
			Objects.requireNonNull( this.creatorRef );
			if( this.causeRef != null )
				Objects.requireNonNull( this.causeCreatorRef );

			final CompositeActor.ID creator = CompositeActor.ID
					.of( this.creatorRef.restore( binder ) );
			final ID id = ID.of( Objects.requireNonNull( this.id ), creator );
			final Transaction tx = binder.inject( Transaction.Factory.class )
					.create( Transaction.ID.of( this.tid, binder.id() ),
							this.type,
							CompositeActor.ID
									.of( this.initiatorRef.restore( binder ) ),
							CompositeActor.ID
									.of( this.executorRef.restore( binder ) ) );
			final ID cause = this.causeRef == null ? null
					: ID.of( this.causeRef, CompositeActor.ID
							.of( this.causeCreatorRef.restore( binder ) ) );
			final Instant occurrence = Objects.requireNonNull( this.occurrence )
					.restore( binder );
			final Instant expiration = this.expiration == null ? null
					: this.expiration.restore( binder );

			final Map<String, Object> properties = JsonUtil
					.valueOf( this.properties, Map.class );
			return binder.inject( CoordinationFact.Factory.class ).create(
					tx.kind(), id, tx, this.kind, occurrence, expiration, cause,
					fromJSON( JsonUtil.getJOM(), this.properties, tx.kind(),
							properties ) );
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
		private transient CoordinationFact proxy = null;
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
					+ kind() + '|' + creatorRef() + '|' + occur() + ']'
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
		public Instant occur()
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
		public Instant expire()
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

		@SuppressWarnings( "unchecked" )
		@Override
		public <F extends CoordinationFact> F self()
		{
			return (F) this.proxy;
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