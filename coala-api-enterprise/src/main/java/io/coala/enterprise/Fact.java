/* $Id: f06fe9dae625cd8ea1c78fb6e3ddecf36d3f3750 $
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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.text.DateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.Unit;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalId;
import io.coala.enterprise.persist.FactDao;
import io.coala.exception.Thrower;
import io.coala.json.Attributed;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.log.LogUtil.Pretty;
import io.coala.math.DecimalUtil;
import io.coala.math.QuantityJsonModule;
import io.coala.name.Identified;
import io.coala.persist.JPAUtil;
import io.coala.persist.Persistable;
import io.coala.time.Instant;
import io.coala.util.ReflectUtil;
import io.reactivex.Observer;

/**
 * {@link Fact}
 * 
 * @version $Id: f06fe9dae625cd8ea1c78fb6e3ddecf36d3f3750 $
 * @author Rick van Krevelen
 */
public interface Fact extends Identified.Ordinal<Fact.ID>, Persistable<FactDao>,
	Attributed.Reactive
{
	String TRANSACTION_JSON_PROPERTY = "transaction";

	String KIND_JSON_PROPERTY = "kind";

	String OCCUR_JSON_PROPERTY = "occur";

	String OCCUR_POSIX_JSON_PROPERTY = "occurPosixMS";

	String EXPIRE_JSON_PROPERTY = "expire";

	String EXPIRE_POSIX_JSON_PROPERTY = "expirePosixMS";

	String CAUSE_REF_JSON_PROPERTY = "causeRef";

	/** @return */
	@JsonProperty( TRANSACTION_JSON_PROPERTY )
	<F extends Fact> Transaction<F> transaction();

	<F extends Fact> F self(); // "this" points to concrete impl, not proxy

	/**
	 * Builder-style bean property setter
	 * 
	 * @param property the property (or bean attribute) to change
	 * @param value the new value
	 * @return this {@link Fact} to allow chaining
	 * @see #with(String, Object, Class)
	 */
	@JsonIgnore
	default Fact with( final String property, final Object value )
	{
		set( property, value );
		return this;
	}

	/**
	 * Commit (i.e. save &amp; send) this {@link Fact}
	 * 
	 * @return the {@link Fact} again to allow chaining
	 */
	@SuppressWarnings( "unchecked" )
	default <F extends Fact> F commit()
	{
		return (F) commit( kind().isTerminal() );
	}

	/**
	 * Commit (i.e. save &amp; send) this {@link Fact}
	 * 
	 * @param cleanUp {@code true} iff the {@link Transaction} may clean up,
	 *            e.g. it has terminated or no further facts are expected
	 * @return the {@link Fact} again to allow chaining
	 */
	@SuppressWarnings( "unchecked" )
	default <F extends Fact> F commit( final boolean cleanUp )
	{
		return ((Transaction<F>) transaction()).commit( self(), cleanUp );
	}

	default Class<? extends Fact> type()
	{
		return Objects.requireNonNull( transaction() ).kind();
	}

	@SuppressWarnings( "unchecked" )
	default <F extends Fact> F typed()
	{
		return (F) type().cast( this );
	}

	default Actor.ID creatorRef()
	{
		return kind().originatorRoleType() == RoleKind.EXECUTOR
				? transaction().executorRef() : transaction().initiatorRef();
	}

	default Actor.ID responderRef()
	{
		return kind().responderRoleKind() == RoleKind.EXECUTOR
				? transaction().executorRef() : transaction().initiatorRef();
	}

	@JsonIgnore
	default boolean isIncoming( final Actor.ID id )
	{
		return (Boolean) properties().computeIfAbsent(
				"_incoming" + id.unwrap(), key -> id.organizationRef()
						.equals( responderRef().organizationRef() ) );
	}

	@JsonIgnore
	default boolean isOutgoing( final Actor.ID id )
	{
		return !isIncoming( id );
	}

	/**
	 * @return {@code true} iff {@link #creator()} and {@link #responder()} are
	 *         in the same {@link Actor.ID#organizationRef()}
	 */
	@JsonIgnore
	default boolean isInternal()
	{
		return !creatorRef().organizationRef()
				.equals( responderRef().organizationRef() );
	}

	/** @return */
	@JsonProperty( KIND_JSON_PROPERTY )
	FactKind kind();

	/** @return */
	@JsonProperty( OCCUR_JSON_PROPERTY )
	Instant occur();

	/** @return */
	default ZonedDateTime occurPosix()
	{
		return occur().toJava8( offset() );
	}

	/** @return */
	@JsonProperty( OCCUR_POSIX_JSON_PROPERTY )
	default BigInteger occurPosixMillis()
	{
		return DecimalUtil.toMillis( occurPosix() );
	}

	/** @return */
	@JsonProperty( EXPIRE_JSON_PROPERTY )
	Instant expire();

	/** @return */
	default ZonedDateTime expirePosix()
	{
		return expire() == null ? null : expire().toJava8( offset() );
	}

	/** @return */
	@JsonProperty( EXPIRE_POSIX_JSON_PROPERTY )
	default BigInteger expirePosixMillis()
	{
		return DecimalUtil.toMillis( expirePosix() );
	}

	/** @return */
	@JsonProperty( CAUSE_REF_JSON_PROPERTY )
	Fact.ID causeRef();

	default ZonedDateTime offset()
	{
		Transaction<Fact> tx = Objects.requireNonNull( transaction(), "no tx" );
		return Objects.requireNonNull( tx.scheduler(), "no sched" ).offset();
	}

	/**
	 * Semi-{@link Transactional} persistence short-hand utility method
	 * 
	 * @param em the {@link EntityManager} to use for persistence calls
	 * @param binder the {@link LocalBinder} to use for restoring results
	 * @param query the JPQL query, e.g. {@code "SELECT f FROM "}
	 * @return a {@link Stream} of all matching {@link Fact}s
	 */
	default Stream<Fact> find( final EntityManager em, final LocalBinder binder,
		final String query )
	{
		return em.createQuery( query, FactDao.class ).getResultList().stream()
				.map( dao -> dao.restore( binder ) );
	}

	/**
	 * Semi-{@link Transactional} persistence short-hand utility method
	 * 
	 * @param em the {@link EntityManager} to use for persistence calls
	 * @param binder the {@link LocalBinder} to use for restoring results
	 * @return a {@link Stream} of all known {@link Fact}s
	 */
	default Stream<Fact> findAll( final EntityManager em,
		final LocalBinder binder )
	{
		// TODO use criteria builder
		return find( em, binder,
				"SELECT dao FROM " + FactDao.class.getSimpleName() + " dao" );
	}

	/**
	 * @param subtype the type of {@link Fact} to mimic
	 * @return the {@link Proxy} instance
	 */
//	@SuppressWarnings( "unchecked" )
	default <F extends Fact> F proxyAs( final Class<F> subtype )
	{
		return proxyAs( subtype, null );
	}

	/**
	 * @param subtype the type of {@link Fact} to mimic
	 * @param callObserver an (optional) {@link Observer} of methods called
	 * @return the {@link Proxy} instance
	 */
	default <F extends Fact> F proxyAs( final Class<F> subtype,
		final Observer<Method> callObserver )
	{
		return ReflectUtil.createProxyInstance( this, subtype, this::properties,
				callObserver );
	}

	// @JsonValue
	default String toJSON()
	{
		return JsonUtil.stringify( this );
	}

	ObjectMapper DEFAULT_OM = JsonUtil.getJOM();

	static <F extends Fact> F fromJSON( final String json,
		final Class<F> factType )
	{
		return fromJSON( DEFAULT_OM, json, factType );
	}

	static <F extends Fact> F fromJSON( final TreeNode json,
		final Class<F> factType )
	{
		return fromJSON( DEFAULT_OM, json, factType );
	}

	static <F extends Fact> F fromJSON( final ObjectMapper om,
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

	static <F extends Fact> F fromJSON( final ObjectMapper om,
		final TreeNode json, final Class<F> factType )
	{
		if( json == null ) return null;
		try
		{
			Simple.checkRegistered( om );
			QuantityJsonModule.checkRegistered( om );
			final Simple result = JsonUtil.valueOf( om, json, Simple.class );
			treeToMap( om, json, factType, result.properties() );
			return result.proxyAs( factType, null );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * override and deserialize bean properties as declared in factType
	 * <p>
	 * TODO detect properties from builder methods: {@code withKey(T value)}
	 * 
	 * @param om
	 * @param json
	 * @param factType
	 * @param properties
	 * @return the properties again, to allow chaining
	 * @throws IntrospectionException
	 */
	static <T extends Fact> Map<String, Object> treeToMap(
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

//	@SuppressWarnings( "unchecked" )
	@Override
	default FactDao persist( final EntityManager em )
	{
		JPAUtil.existsOrCreate( em, () -> FactDao.exists( em, id() ),
				() -> FactDao.create( em, self() ) );
		return null;
	}

	default String toString( final Object occur )
	{
		return type().getSimpleName() + '['
				+ Integer.toHexString( id().unwrap().hashCode() ) + '|' + kind()
				+ '|' + creatorRef().organizationRef().unwrap() + "->"
				+ responderRef().organizationRef().unwrap() + '|' + occur + ']'
				+ (properties().isEmpty() ? "" : properties());
	}

	default Pretty prettify( final int scale )
	{
		return prettify( occur().unit(), scale );
	}

	default Pretty prettify( final Unit<?> unit )
	{
		return Pretty.of( () -> toString(
				occur().to( unit ).decimal().toPlainString() + unit ) );
	}

	default Pretty prettify( final Unit<?> unit, final int scale )
	{
		return Pretty.of( () -> toString(
				DecimalUtil.toScale( occur().to( unit ).decimal(), scale )
						.toPlainString() + unit ) );
	}

	default Pretty prettify( final DateFormat formatter )
	{
		return Pretty.of( () -> toString( formatter.format(
				occur().toDate( Date.from( offset().toInstant() ) ) ) ) );
	}

	default Pretty prettify( final DateTimeFormatter java8Formatter )
	{
		return Pretty.of( () -> toString(
				java8Formatter.format( occur().toJava8( offset() ) ) ) );
	}

	default Pretty
		prettify( final org.joda.time.format.DateTimeFormatter jodaFormatter )
	{
		final ZonedDateTime zdt = offset();
		return Pretty.of( () -> toString( jodaFormatter.print(
				occur().toJoda( new DateTime( zdt.toInstant().toEpochMilli(),
						DateTimeZone.forTimeZone( TimeZone
								.getTimeZone( zdt.getZone() ) ) ) ) ) ) );
	}

	/**
	 * {@link ID}
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
		public Transaction.ID parentRef()
		{
			return (Transaction.ID) super.parentRef();
		}

		public Pretty prettyHash()
		{
			return LogUtil.Pretty
					.of( () -> Integer.toHexString( unwrap().hashCode() ) );
		}

		/** @return an {@link ID} with specified {@link UUID} */
		public static ID of( final UUID value, final Transaction.ID ctx )
		{
			return LocalId.of( new ID(), value, ctx );
		}

		/** @return a new {@link ID} */
		public static ID create( final Transaction.ID ctx )
		{
			return of( new UUID(), ctx );
		}
	}

	/**
	 * {@link Fact.Simple}
	 */
	@JsonInclude( Include.NON_NULL )
	class Simple extends Attributed.Reactive.SimpleOrdinal<Identified<Fact.ID>>
		implements Fact
	{
		private static Map<ObjectMapper, Module> TX_MODULE_CACHE = new HashMap<>();

		/**
		 * @param om
		 */
		public static void checkRegistered( final ObjectMapper om )
		{
			JsonUtil.checkRegisteredMembers( om, Fact.Simple.class );
			TX_MODULE_CACHE.computeIfAbsent( om, key ->
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

		private transient Fact proxy = null;
		private Fact.ID id;
		private Instant occurrence;
		private Transaction<?> transaction;
		private FactKind kind;
		private Instant expiration;
		private Fact.ID causeRef;

		/**
		 * {@link Simple} zero-arg bean constructor
		 */
		protected Simple()
		{

		}

		public Simple( final Fact.ID id, final Instant occurrence,
			final Transaction<?> transaction, final FactKind kind,
			final Instant expiration, final Fact.ID causeRef,
			final Map<?, ?>... properties )
		{
			this.id = Objects.requireNonNull( id );
			this.occurrence = Objects.requireNonNull( occurrence );
			this.transaction = Objects.requireNonNull( transaction );
			this.kind = Objects.requireNonNull( kind );
			this.expiration = expiration;
			this.causeRef = causeRef;
			if( properties != null ) for( Map<?, ?> map : properties )
				map.forEach( ( key, value ) -> set( key.toString(), value ) );
		}

		@Override
		public String toString()
		{
			final ZonedDateTime offset = offset();
			return toString( offset == null ? occur()
					: DateTimeFormatter.ISO_LOCAL_DATE_TIME
							.format( occur().toJava8( offset ) ) );
		}

		@Override
		public int hashCode()
		{
			return Identified.hashCode( this );
		}

		@Override
		public Fact.ID id()
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
		public FactKind kind()
		{
			return this.kind;
		}

		@Override
		public Instant expire()
		{
			return this.expiration;
		}

		@Override
		public Fact.ID causeRef()
		{
			return this.causeRef;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <F extends Fact> F self()
		{
			return (F) this.proxy;
		}

		/**
		 * @param subtype the type of {@link Fact} to mimic
		 * @param callObserver an {@link Observer} of method call, or
		 *            {@code null}
		 * @return the {@link Proxy} instance
		 */
		@Override
		@SuppressWarnings( "unchecked" )
		public <F extends Fact> F proxyAs( final Class<F> subtype,
			final Observer<Method> callObserver )
		{
			this.proxy = ReflectUtil.createProxyInstance( this, subtype,
					this::properties, callObserver );
			return (F) this.proxy;
		}
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id: f06fe9dae625cd8ea1c78fb6e3ddecf36d3f3750 $
	 * @author Rick van Krevelen
	 */
	interface Factory
	{

		LocalId ownerRef();

		FactBank<?> factBank();

		/**
		 * @param tranKind the type of {@link Fact} (transaction kind)
		 * @param id the {@link Fact.ID}
		 * @param transaction the {@link Transaction}
		 * @param factKind the {@link FactKind} (process step kind)
		 * @param expiration the {@link Instant} of expiration
		 * @param causeRef the cause {@link Fact.ID}, or {@code null} for
		 *            external initiation
		 * @param properties property {@link Map mappings}, if any
		 * @return a {@link Fact}
		 */
		default <F extends Fact> F create( Class<F> tranKind, Fact.ID id,
			Transaction<? super F> transaction, FactKind factKind,
			Instant expiration, Fact.ID causeRef, Map<?, ?>... properties )
		{
			return create( tranKind, id, transaction, factKind,
					Objects.requireNonNull( transaction.now() ), expiration,
					causeRef, properties );
		}

		/**
		 * @param tranKind the type of {@link Fact} (transaction kind)
		 * @param id the {@link Fact.ID}
		 * @param transaction the {@link Transaction}
		 * @param factKind the {@link FactKind} (process step kind)
		 * @param occurrence the {@link Instant} of occurrence
		 * @param expiration the {@link Instant} of expiration
		 * @param causeRef the cause {@link Fact.ID}, or {@code null} for
		 *            external initiation
		 * @param properties property {@link Map mappings}, if any
		 * @return a {@link Fact}
		 */
		<F extends Fact> F create( Class<F> tranKind, Fact.ID id,
			Transaction<? super F> transaction, FactKind factKind,
			Instant occurrence, Instant expiration, Fact.ID causeRef,
			Map<?, ?>... properties );

		/**
		 * {@link SimpleProxies} generates the desired extension of {@link Fact}
		 * as proxy decorating a new {@link Fact.Simple} instance
		 */
		@Singleton
		class SimpleProxies implements Factory
		{
			@Inject
			private LocalBinder binder;

			@SuppressWarnings( "rawtypes" )
			@Inject
			private FactBank bank;

			@Override
			public <F extends Fact> F create( final Class<F> tranKind,
				final Fact.ID id, final Transaction<? super F> transaction,
				final FactKind factKind, final Instant occurrence,
				final Instant expiration, final Fact.ID causeRef,
				final Map<?, ?>... params )
			{
				return new Simple( id, occurrence, transaction, factKind,
						expiration, causeRef, params ).proxyAs( tranKind,
								null );
			}

			@Override
			public LocalId ownerRef()
			{
				return this.binder.id();
			}

//			@SuppressWarnings( "unchecked" )
			@Override
			public FactBank<?> factBank()
			{
				return this.bank;
			}
		}
	}
}