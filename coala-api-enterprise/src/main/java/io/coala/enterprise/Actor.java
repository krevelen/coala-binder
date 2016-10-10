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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalContextual;
import io.coala.bind.LocalId;
import io.coala.log.LogUtil;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.time.Instant;
import io.coala.util.ReflectUtil;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Actor} can handle multiple {@link Transaction} types or kinds
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Actor<F extends Fact>
	extends Identified.Ordinal<Actor.ID>, Observer<F>
{

	/**
	 * @return the properties {@link Map} as used for extended getters/setters
	 */
	Map<String, Object> properties();

	/**
	 * @param tranKind
	 * @return an actor that handles the initiator-side of specified transaction
	 *         type (and all its subtypes)
	 */
	<A extends Actor<T>, T extends F> A initiator( Class<T> tranKind,
		Class<A> actorKind );

	<A extends Actor<T>, T extends F> A executor( Class<T> tranKind,
		Class<A> actorKind );

	/**
	 * @return an {@link Observable} stream of all generated or received
	 *         {@link Fact}s
	 */
	Observable<F> commits();

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> commits( final Class<T> tranKind )
	{
		return commits().ofType( tranKind );
	}

	/**
	 * @param factKind the {@link FactKind} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default Observable<F> commits( final FactKind factKind )
	{
		return commits().filter( f -> f.kind().equals( factKind ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param factKind the {@link FactKind} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> commits( final Class<T> tranKind,
		final FactKind factKind )
	{
		return commits( tranKind ).filter( f -> f.kind().equals( factKind ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param creatorRef the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> commits( final Class<T> tranKind,
		final Actor.ID creatorRef )
	{
		return commits( tranKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param factKind the {@link FactKind} to filter for
	 * @param creatorRef the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> commits( final Class<T> tranKind,
		final FactKind factKind, final Actor.ID creatorRef )
	{
		return commits( tranKind, factKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/**
	 * @return all {@link #commits()} where {@link Fact#isOutgoing()
	 *         isOutgoing()} {@code == true}
	 */
	default Observable<F> outgoing()
	{
		return commits().filter( Fact::isOutgoing );
	}

	default <T extends F> Observable<T> outgoing( final Class<T> tranKind )
	{
		return outgoing().ofType( tranKind );
	}

	default <T extends F> Observable<T> outgoing( final Class<T> tranKind,
		final FactKind kind )
	{
		return outgoing( tranKind ).filter( f -> f.kind() == kind );
	}

	/**
	 * @param tranKind the type of {@link Fact} to initiate or continue
	 * @param initiatorRef the initiator {@link Actor.ID}
	 * @param executorRef the executor {@link Actor.ID}
	 * @return the {@link Transaction} context
	 */
	<T extends Fact> Transaction<T> transact( Class<T> tranKind,
		Actor.ID initiatorRef, Actor.ID executorRef );

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, null, null, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param cause the {@link Fact} triggering the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Fact.ID cause,
		final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, cause, null, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param expiration the expiration {@link Instant} of the request, or
	 *            {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Instant expiration,
		final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, null, expiration, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param cause the {@link Fact} triggering the request, or {@code null}
	 * @param expiration the expiration {@link Instant} of the request, or
	 *            {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Fact.ID cause,
		final Instant expiration, final Map<?, ?>... params )
	{
		return transact( tranKind, id(), executorRef )
				.generate( FactKind.REQUESTED, cause, expiration, params );
	}

	/**
	 * @param cause the {@link Fact} triggering the response
	 * @param factKind the {@link FactKind} of response
	 * @param params additional property (or bean attribute) values, if any
	 * @return a response {@link Fact}
	 */
	default F respond( final F cause, final FactKind factKind,
		final Map<?, ?>... params )
	{
		return respond( cause, factKind, null, params );
	}

	/**
	 * @param cause the {@link Fact} triggering the response
	 * @param factKind the {@link FactKind} of the response
	 * @param expiration the expiration {@link Instant} of the response, or
	 *            {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return a response {@link Fact}
	 */
	@SuppressWarnings( "unchecked" )
	default F respond( final F cause, final FactKind factKind,
		final Instant expiration, final Map<?, ?>... params )
	{
		return ((Transaction<F>) cause.transaction()).generate( factKind,
				cause.id(), expiration, params );
	}

	default <S extends F> Actor<S> specializeIn( final Class<S> tranKind )
	{
		final Actor<F> parent = this;
		return new Actor<S>()
		{
			private final ID id = ID.of( tranKind, parent.id() );

			@Override
			public ID id()
			{
				return this.id;
			}

			@Override
			public Observable<S> commits()
			{
				return parent.commits( tranKind );
			}

			@Override
			public void onCompleted()
			{
				parent.onCompleted();
			}

			@Override
			public void onError( final Throwable e )
			{
				parent.onError( e );
			}

			@Override
			public void onNext( final S fact )
			{
				parent.onNext( fact );
			}

			@Override
			public <A extends Actor<T>, T extends S> A
				initiator( final Class<T> tranKind, final Class<A> actorKind )
			{
				return parent.initiator( tranKind, actorKind );
			}

			@Override
			public <A extends Actor<T>, T extends S> A
				executor( final Class<T> tranKind, final Class<A> actorKind )
			{
				return parent.executor( tranKind, actorKind );
			}

			@Override
			public <T extends Fact> Transaction<T> transact(
				final Class<T> tranKind, final ID initiatorRef,
				final ID executorRef )
			{
				return parent.transact( tranKind, initiatorRef, executorRef );
			}

			@Override
			public Map<String, Object> properties()
			{
				return parent.properties();
			}
		};
	}

	/**
	 * @param actorKind the type of {@link Actor} to mimic
	 * @return the {@link Proxy} instance
	 */
	@SuppressWarnings( "unchecked" )
	default <A extends Actor<T>, T extends F> A
		proxyAs( final Class<A> actorKind )
	{
		return proxyAs( actorKind, null );
	}

	/**
	 * @param actorKind the type of {@link Actor} to mimic
	 * @param callObserver an {@link Observer} of method call, or {@code null}
	 * @return the {@link Proxy} instance
	 */
	@SuppressWarnings( "unchecked" )
	default <A extends Actor<T>, T extends F> A
		proxyAs( final Class<A> actorKind, final Observer<Method> callObserver )
	{
		return proxyAs( this, actorKind, callObserver );
	}

	/**
	 * @param actorType the type of {@link Actor} to mimic
	 * @param callObserver an {@link Observer} of method call, or {@code null}
	 * @return the {@link Proxy} instance
	 */
	@SuppressWarnings( "unchecked" )
	static <A extends Actor<T>, F extends Fact, T extends F> A proxyAs(
		final Actor<F> impl, final Class<A> actorType,
		final Observer<Method> callObserver )
	{
		final A proxy = (A) Proxy.newProxyInstance( actorType.getClassLoader(),
				new Class<?>[]
		{ actorType }, ( self, method, args ) ->
		{
			try
			{
				final Object result = method.isDefault()
						&& Proxy.isProxyClass( self.getClass() )
								? ReflectUtil.invokeDefaultMethod( self, method,
										args )
								: method.invoke( impl, args );
				if( callObserver != null ) callObserver.onNext( method );
				return result;
			} catch( Throwable e )
			{
				if( e instanceof IllegalArgumentException ) try
				{
					return ReflectUtil.invokeAsBean( impl.properties(),
							actorType, method, args );
				} catch( final Exception ignore )
				{
					LogUtil.getLogger( Fact.class ).warn(
							"{}method call failed: {}",
							method.isDefault() ? "default " : "", method,
							ignore );
				}
				if( e instanceof InvocationTargetException ) e = e.getCause();
				if( callObserver != null ) callObserver.onError( e );
				throw e;
			}
		} );
		return proxy;
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
				return of( valueOf( value ) );
			}
		}

		/**
		 * @param name
		 * @param parent
		 * @return
		 */
		public static ID of( final Class<? extends Fact> tranKind,
			final ID parent )
		{
			return Id.of( new ID(), tranKind.getSimpleName(), parent );
		}

		/**
		 * @param name
		 * @param parent
		 * @return
		 */
		public static ID of( final String name, final LocalId parent )
		{
			return Id.of( new ID(), name, parent );
		}

		/**
		 * @param raw
		 * @return
		 */
		public static ID of( final LocalId raw )
		{
			return raw == null || raw.parentRef() == null ? null
					: of( raw.unwrap().toString(), raw.parentRef() );
		}

		/** @return the derived root parent's {@link ID} */
		public ID organization()
		{
			for( LocalId id = this;; id = id.parentRef() )
				if( id.parentRef() instanceof ID == false ) return (ID) id;
		}
	}

	class Simple implements Actor<Fact>
	{
//			final Logger LOG = LogUtil.getLogger( Simple.class );

		public static Simple of( final LocalBinder binder, final ID id )
		{
			final Simple result = binder.inject( Simple.class );
			result.id = id;
			return result;
		}

		private transient final Map<Transaction.ID, Transaction<?>> txs = new ConcurrentHashMap<>();

		private transient final Subject<Fact, Fact> commits = PublishSubject
				.create();

		private transient final Map<Class<?>, Observable<?>> typeCommits = new ConcurrentHashMap<>();

		private transient final Map<Class<?>, Actor<?>> specialists = new ConcurrentHashMap<>();

		private transient final Map<String, Object> properties = new ConcurrentHashMap<>();

		@Inject
		private transient Transaction.Factory txFactory;

		private ID id;

		@Inject
		public Simple()
		{
			// empty bean constructor
		}

		public Simple( final ID id, final Transaction.Factory txFactory )
		{
			this.id = id;
			this.txFactory = txFactory;
		}

		@Override
		public ID id()
		{
			return this.id;
		}

		@Override
		public Map<String, Object> properties()
		{
			return this.properties;
		}

		@Override
		@SuppressWarnings( "unchecked" )
		public <T extends Fact> Transaction<T> transact(
			final Class<T> tranKind, final Actor.ID initiatorRef,
			final Actor.ID executorRef )
		{
			return (Transaction<T>) this.txs
					.computeIfAbsent( Transaction.ID.create( id() ), tid ->
					{
						final Transaction<T> tx = this.txFactory.create( tid,
								tranKind, initiatorRef, executorRef );
						// tx -> actor (committed facts)
						tx.commits().subscribe( this::onNext, this::onError,
								() -> this.txs.remove( tid ) );
						return tx;
					} );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <A extends Actor<F>, F extends Fact> A
			initiator( final Class<F> tranKind, final Class<A> actorKind )
		{
			return ((Actor<? super F>) this.specialists.computeIfAbsent(
					tranKind, key -> this.specializeIn( tranKind ) ))
							.proxyAs( actorKind );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <A extends Actor<F>, F extends Fact> A
			executor( final Class<F> tranKind, final Class<A> actorKind )
		{
			return ((Actor<? super F>) this.specialists.computeIfAbsent(
					tranKind, key -> this.specializeIn( tranKind ) ))
							.proxyAs( actorKind );
		}

		@Override
		public Observable<Fact> commits()
		{
			return this.commits.asObservable();
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <F extends Fact> Observable<F> commits( final Class<F> tranKind )
		{
			return (Observable<F>) this.typeCommits.computeIfAbsent( tranKind,
					commits()::ofType );
		}

		@Override
		public void onCompleted()
		{
			this.commits.onCompleted();
		}

		@Override
		public void onError( final Throwable e )
		{
			this.commits.onError( e );
		}

		@Override
		public void onNext( final Fact fact )
		{
			this.commits.onNext( fact );
		}
	}

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface Factory extends LocalContextual
	{

		/** @return the {@link java.time.Instant} UTC offset of virtual time */
		java.time.Instant offset();

		/** @return the {@link Unit} of virtual time */
		Unit<?> timeUnit();

		/**
		 * @param id the {@link ID} of the new {@link Actor}
		 * @return a (cached) {@link Actor}
		 */
		<F extends Fact> Actor<F> create( ID id );

		default <F extends Fact> Actor<F> create( final String name )
		{
			return create( ID.of( name, id() ) );
		}

		@Singleton
		class LocalCaching implements Factory
		{
			private final transient Map<ID, Simple> localCache = new ConcurrentHashMap<>();

			@Inject
			private transient LocalBinder binder;

			@Inject
			private transient Transaction.Factory txFactory;

			@Override
			public Actor<Fact> create( final ID id )
			{
				return this.localCache.computeIfAbsent( id, k ->
				{
					return new Simple( id, this.txFactory );
				} );
			}

			@Override
			public Context context()
			{
				return this.binder.context();
			}

			@Override
			public LocalId id()
			{
				return this.binder.id();
			}

			@Override
			public java.time.Instant offset()
			{
				return this.txFactory.offset();
			}

			@Override
			public Unit<?> timeUnit()
			{
				return this.txFactory.timeUnit();
			}
		}
	}
}