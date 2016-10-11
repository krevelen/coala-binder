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
import io.coala.util.TypeArguments;
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
	 * @param actorKind
	 * @return an actor view that for performing the initiator-side of its
	 *         transaction type (and all its subtypes)
	 */
	<A extends Actor<T>, T extends F> A asInitiator( Class<A> actorKind );

	/**
	 * @param actorKind
	 * @return an actor view that for performing the executor-side of its
	 *         transaction type (and all its subtypes)
	 */
	<A extends Actor<T>, T extends F> A asExecutor( Class<A> actorKind );

	/**
	 * @return an {@link Observable} stream of all generated or received
	 *         {@link Fact}s
	 */
	Observable<F> emit();

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> emit( final Class<T> tranKind )
	{
		return emit().ofType( tranKind );
	}

	/**
	 * @param factKind the {@link FactKind} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default Observable<F> emit( final FactKind factKind )
	{
		return emit().filter( f -> f.kind().equals( factKind ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param factKind the {@link FactKind} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> emit( final Class<T> tranKind,
		final FactKind factKind )
	{
		return emit( tranKind ).filter( f -> f.kind().equals( factKind ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param creatorRef the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> emit( final Class<T> tranKind,
		final Actor.ID creatorRef )
	{
		return emit( tranKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param factKind the {@link FactKind} to filter for
	 * @param creatorRef the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default <T extends F> Observable<T> emit( final Class<T> tranKind,
		final FactKind factKind, final Actor.ID creatorRef )
	{
		return emit( tranKind, factKind )
				.filter( fact -> fact.creatorRef().equals( creatorRef ) );
	}

	/**
	 * @return all {@link #emit()} where {@link Fact#isOutgoing() isOutgoing()}
	 *         {@code == true}
	 */
	default Observable<F> outgoing()
	{
		return emit().filter( Fact::isOutgoing );
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

	@SuppressWarnings( "unchecked" )
	default Class<F> tranKind()
	{
		return (Class<F>) TypeArguments.of( Actor.class, getClass() ).get( 0 );
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
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	@SuppressWarnings( "unchecked" )
	default F initiate( final Actor.ID executorRef, final Map<?, ?>... params )
	{
		return initiate( tranKind(), executorRef, null, null, params );
	}

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
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param cause the {@link Fact} triggering the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default F initiate( final Actor.ID executorRef, final Fact.ID cause,
		final Map<?, ?>... params )
	{
		return initiate( tranKind(), executorRef, cause, null, params );
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
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param expire the expire {@link Instant} of the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default F initiate( final Actor.ID executorRef, final Instant expire,
		final Map<?, ?>... params )
	{
		return initiate( tranKind(), executorRef, null, expire, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param expire the expire {@link Instant} of the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Instant expire,
		final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, null, expire, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param cause the {@link Fact} triggering the request, or {@code null}
	 * @param expire the expire {@link Instant} of the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Fact.ID cause, final Instant expire,
		final Map<?, ?>... params )
	{
		return transact( tranKind, id(), executorRef )
				.generate( FactKind.REQUESTED, cause, expire, params );
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
	 * @param expire the expire {@link Instant} of the response, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return a response {@link Fact}
	 */
	@SuppressWarnings( "unchecked" )
	default F respond( final F cause, final FactKind factKind,
		final Instant expire, final Map<?, ?>... params )
	{
		return ((Transaction<F>) cause.transaction()).generate( factKind,
				cause.id(), expire, params );
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
			public Observable<S> emit()
			{
				return parent.emit( tranKind );
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
				asInitiator( final Class<A> actorKind )
			{
				return parent.asInitiator( actorKind );
			}

			@Override
			public <A extends Actor<T>, T extends S> A
				asExecutor( final Class<A> actorKind )
			{
				return parent.asExecutor( actorKind );
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
		public static Simple of( final LocalBinder binder, final ID id )
		{
			final Simple result = binder.inject( Simple.class );
			result.id = id;
			return result;
		}

		private transient final Map<Transaction.ID, Transaction<?>> txs = new ConcurrentHashMap<>();

		private transient final Subject<Fact, Fact> emit = PublishSubject
				.create();

		private transient final Map<Class<?>, Observable<?>> typeemit = new ConcurrentHashMap<>();

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
			asInitiator( final Class<A> actorKind )
		{
			return ((Actor<? super F>) this.specialists.computeIfAbsent(
					TypeArguments.of( Actor.class, actorKind ).get( 0 ),
					key -> this.specializeIn( (Class<F>) key ) ))
							.proxyAs( actorKind );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <A extends Actor<F>, F extends Fact> A
			asExecutor( final Class<A> actorKind )
		{
			return ((Actor<? super F>) this.specialists.computeIfAbsent(
					TypeArguments.of( Actor.class, actorKind ).get( 0 ),
					key -> this.specializeIn( (Class<F>) key ) ))
							.proxyAs( actorKind );
		}

		@Override
		public Observable<Fact> emit()
		{
			return this.emit.asObservable();
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <F extends Fact> Observable<F> emit( final Class<F> tranKind )
		{
			return (Observable<F>) this.typeemit.computeIfAbsent( tranKind,
					emit()::ofType );
		}

		@Override
		public void onCompleted()
		{
			this.emit.onCompleted();
		}

		@Override
		public void onError( final Throwable e )
		{
			this.emit.onError( e );
		}

		@Override
		public void onNext( final Fact fact )
		{
			this.emit.onNext( fact );
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