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

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.coala.exception.Thrower;
import io.coala.name.Id;
import io.coala.name.Identified;
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
	Organization.ID creatorID();

	/** @return */
	CoordinationFactType type();

	/** @return */
	Instant time();

	/** @return */
	Instant expiration();

	/** @return */
	ID causeID();

	/** @return */
	@JsonAnyGetter
	Map<String, Object> params();

	@JsonAnySetter
	default void set( final String name, final Object value )
	{
		params().put( name, value );
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
		return (F) Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class<?>[]
				{ subtype }, ( proxy, method, args ) ->
				{
					try
					{
						final Object result = method.invoke( self, args );
						if( callObserver != null )
							callObserver.onNext( method );
						return result;
					} catch( final Exception e )
					{
						if( callObserver != null ) callObserver.onError( e );
						return Thrower.rethrowUnchecked( e );
					}
				} );
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
		 * @param scheduler
		 * @param factKind
		 * @param id
		 * @param tranID
		 * @param creatorID
		 * @param type
		 * @param expiration
		 * @param causeID
		 * @param params
		 * @return
		 */
		<F extends CoordinationFact> F create( Scheduler scheduler,
			Class<F> factKind, CoordinationFact.ID id, Transaction.ID tranID,
			Organization.ID creatorID, CoordinationFactType type,
			Instant expiration, CoordinationFact.ID causeID,
			Map<?, ?>... params );
	}

	interface Persister
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

	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends Id.Ordinal<UUID>
	{
		/** @return */
		public static CoordinationFact.ID create()
		{
			return Util.of( new UUID(), new ID() );
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

		private Class<?> kind;

		private CoordinationFact.ID id;

		private Instant time;

		private Transaction.ID tranID;

		private Organization.ID creatorID;

		private CoordinationFactType type;

		private Instant expiration;

		private CoordinationFact.ID causeID;

		private Map<String, Object> params = new HashMap<>();

		/**
		 * {@link Simple} zero-arg bean constructor
		 */
		protected Simple()
		{

		}

		protected Simple( final Class<?> kind, final CoordinationFact.ID id,
			final Instant time, final Transaction.ID tranID,
			final Organization.ID creatorID, final CoordinationFactType type,
			final Instant expiration, final CoordinationFact.ID causeID,
			final Map<?, ?>... params )
		{
			this.kind = kind;
			this.id = id;
			this.time = time;
			this.tranID = tranID;
			this.creatorID = creatorID;
			this.type = type;
			this.expiration = expiration;
			this.causeID = causeID;
			if( params != null ) for( Map<?, ?> param : params )
				param.forEach( ( key, value ) ->
				{
					this.params.put( key.toString(), value );
				} );
		}

		@Override
		public String toString()
		{
			return this.kind.getSimpleName() + '[' + type() + '|' + creatorID()
					+ '|' + time() + ']' + params();
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
		public Instant time()
		{
			return this.time;
		}

		@Override
		public Transaction.ID tranID()
		{
			return this.tranID;
		}

		@Override
		public Organization.ID creatorID()
		{
			return this.creatorID;
		}

		@Override
		public CoordinationFactType type()
		{
			return this.type;
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
		public Map<String, Object> params()
		{
			return this.params;
		}
	}

	/**
	 * {@link SimpleFactory} generates the desired {@link CoordinationFact}
	 * sub-type as proxy decorating a new {@link Simple} instance
	 */
	class SimpleFactory implements Factory
	{
		@Override
		public <F extends CoordinationFact> F create( final Scheduler scheduler,
			final Class<F> factKind, final CoordinationFact.ID id,
			final Transaction.ID tranID, final Organization.ID creatorID,
			final CoordinationFactType type, final Instant expiration,
			final CoordinationFact.ID causeID, final Map<?, ?>... params )
		{
			return new Simple( factKind, id, scheduler.now(), tranID, creatorID,
					type, expiration, causeID, params ).proxyAs( factKind,
							null );
		}
	}
}