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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;

import org.apache.logging.log4j.Logger;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.coala.enterprise.dao.CoordinationFactDao;
import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.persist.JPAUtil;
import io.coala.time.Instant;
import io.coala.time.Scheduler;
import rx.Observer;

/**
 * {@link CoordinationFact}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface CoordinationFact
	extends Identified.Ordinal<CoordinationFact.ID>
{
	/** @return */
	Transaction.ID tranID();

	/** @return */
	CompositeActor.ID creatorID();

	/** @return */
	CoordinationFactType kind();

	/** @return */
	Instant occurrence();

	/** @return */
	Instant expiration();

	/** @return */
	ID causeID();

	/** @return */
	@JsonAnyGetter
	Map<String, Object> properties();

	@JsonAnySetter
	default void set( final String name, final Object value )
	{
		properties().put( name, value );
	}

	/**
	 * @param subtype
	 * @param callObserver
	 * @return
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
			} catch( final Exception e )
			{
				if( callObserver != null ) callObserver.onError( e );
				return Thrower.rethrowUnchecked( e );
			}
		} );
	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends Id.Ordinal<UUID>
	{
		/** @return an {@link ID} with specified {@link UUID} */
		public static CoordinationFact.ID of( UUID value )
		{
			return Util.of( value, new ID() );
		}

		/** @return a new {@link ID} */
		public static CoordinationFact.ID create()
		{
			return of( new UUID() );
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

		private Transaction.ID tranID;

		private CompositeActor.ID creatorID;

		private CoordinationFactType kind;

		private Instant expiration;

		private CoordinationFact.ID causeID;

		private Map<String, Object> properties = new HashMap<>();

		/**
		 * {@link Simple} zero-arg bean constructor
		 */
		protected Simple()
		{

		}

		protected Simple( final Class<?> type, final CoordinationFact.ID id,
			final Instant occurrence, final Transaction.ID tranID,
			final CompositeActor.ID creatorID, final CoordinationFactType kind,
			final Instant expiration, final CoordinationFact.ID causeID,
			final Map<?, ?>... properties )
		{
			this.type = type;
			this.id = id;
			this.occurrence = occurrence;
			this.tranID = tranID;
			this.creatorID = creatorID;
			this.kind = kind;
			this.expiration = expiration;
			this.causeID = causeID;
			if( properties != null ) for( Map<?, ?> map : properties )
				map.forEach( ( key, value ) ->
				{
					this.properties.put( key.toString(), value );
				} );
		}

		@Override
		public String toString()
		{
			return this.type.getSimpleName() + '[' + kind() + '|' + creatorID()
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

		@Override
		public Transaction.ID tranID()
		{
			return this.tranID;
		}

		@Override
		public CompositeActor.ID creatorID()
		{
			return this.creatorID;
		}

		@Override
		public CoordinationFactType kind()
		{
			return this.kind;
		}

		@Override
		public Instant expiration()
		{
			return this.expiration;
		}

		@Override
		public ID causeID()
		{
			return this.causeID;
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

		/**
		 * @param type the type of {@link CoordinationFact} (transaction kind)
		 * @param id the {@link CoordinationFact.ID}
		 * @param tranID the {@link Transaction.ID}
		 * @param creatorID the {@link Organization.ID}
		 * @param kind the {@link CoordinationFactType} (process step kind)
		 * @param expiration the {@link Instant} of expiration
		 * @param causeID the {@link CoordinationFact.ID} of a cause, or
		 *            {@code null} for external initiation
		 * @param params the {@link CoordinationFact.ID}
		 * @return a {@link CoordinationFact}
		 */
		<F extends CoordinationFact> F create( //Scheduler scheduler,
			Class<F> type, CoordinationFact.ID id, Transaction.ID tranID,
			CompositeActor.ID creatorID, CoordinationFactType kind,
			Instant expiration, CoordinationFact.ID causeID,
			Map<?, ?>... params );

		/**
		 * {@link Factory.Simple} generates the desired extension of
		 * {@link CoordinationFact} as proxy decorating a new
		 * {@link CoordinationFact.Simple} instance
		 */
		@Singleton
		class Simple implements Factory
		{
			@Inject
			private Scheduler scheduler;

			@Override
			public <F extends CoordinationFact> F create(
				final Class<F> factKind, final CoordinationFact.ID id,
				final Transaction.ID tranID, final CompositeActor.ID creatorID,
				final CoordinationFactType kind, final Instant expiration,
				final CoordinationFact.ID causeID, final Map<?, ?>... params )
			{
				return new CoordinationFact.Simple( factKind, id,
						this.scheduler.now(), tranID, creatorID, kind,
						expiration, causeID, params ).proxyAs( factKind, null );
			}
		}
	}

	interface Persister extends AutoCloseable
	{

		/**
		 * @return an {@link Iterable} of all persisted facts
		 */
		Iterable<CoordinationFact> findAll();

		/**
		 * @param fact the type of {@link CoordinationFact} to match
		 * @return an {@link Iterable} of the matching persisted facts
		 */
		<F extends CoordinationFact> Iterable<F> find( Class<F> fact );

		/**
		 * @param fact the type of {@link CoordinationFact} to match
		 * @param factType the {@link CoordinationFactType} to match
		 * @return an {@link Iterable} of the matching persisted facts
		 */
		<F extends CoordinationFact> Iterable<F> find( Class<F> fact,
			CoordinationFactType factType );

		/**
		 * @param fact the type of {@link CoordinationFact} to return
		 * @param id the {@link ID} to match
		 * @return the persisted fact or {@code null} if not found
		 */
		<F extends CoordinationFact> F find( Class<F> fact, ID id );

		/**
		 * {@link SimpleJPA}
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		@Singleton
		public class SimpleJPA implements Persister
		{

			/** */
			private static final Logger LOG = LogUtil
					.getLogger( CoordinationFact.Persister.SimpleJPA.class );

			@Inject
			private EntityManagerFactory emf;

			@Override
			public Iterable<CoordinationFact> findAll()
			{
				JPAUtil.transact( this.emf, em ->
				{
					LOG.trace( "Read table, result: {}",
							em.createQuery( "SELECT f FROM "
									+ CoordinationFactDao.TABLE_NAME + " f",
									CoordinationFact.class ).getResultList() );
				} );
				return null;
			}

			@Override
			public <F extends CoordinationFact> Iterable<F>
				find( final Class<F> fact )
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <F extends CoordinationFact> Iterable<F>
				find( final Class<F> fact, final CoordinationFactType factType )
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <F extends CoordinationFact> F find( final Class<F> fact,
				ID id )
			{
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void close() throws Exception
			{
				this.emf.close();
			}

		}

	}
}