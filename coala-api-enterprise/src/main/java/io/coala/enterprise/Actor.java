/* $Id: 84a0e0593199134740368157dc1dd614a0a46739 $
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

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalBinding;
import io.coala.bind.LocalContextual;
import io.coala.bind.LocalId;
import io.coala.enterprise.FactExchange.Direction;
import io.coala.exception.Thrower;
import io.coala.function.ThrowingBiConsumer;
import io.coala.function.ThrowingConsumer;
import io.coala.json.Attributed;
import io.coala.log.LogUtil;
import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.time.Expectation;
import io.coala.time.Instant;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import io.coala.time.Timing;
import io.coala.util.TypeArguments;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Actor} can handle multiple {@link Transaction} types or kinds
 * 
 * @version $Id: 84a0e0593199134740368157dc1dd614a0a46739 $
 * @author Rick van Krevelen
 */
public interface Actor<F extends Fact> extends Identified.Ordinal<Actor.ID>,
	Observer<Fact>, Proactive, LocalBinding, Attributed.Publisher
{

	/** setup hook triggered after instantiation/injection is complete */
	default void onInit()
	{
	}

	@SuppressWarnings( "unchecked" )
	default <A extends Actor<F>> A onRequest( final BiConsumer<A, F> rq )
	{
		emit( FactKind.REQUESTED ).subscribe( f -> rq.accept( (A) this, f ),
				this::onError );
		return (A) this;
	}

	/**
	 * Builder-style setter
	 * 
	 * @param property the property (or bean attribute) to change
	 * @param value the new value
	 * @return this {@link Actor} cast to run-time role() type to allow chaining
	 * @see #with(String, Object, Class)
	 */
	@SuppressWarnings( "unchecked" )
	default <A extends Actor<F>> A with( final String property,
		final Object value )
	{
		return (A) with( property, value, (Class<A>) role() );
	}

	/**
	 * TODO {@link Flowable} or {@link Observable} / {@link Single} /
	 * {@link Maybe} / {@link Completable}? see
	 * http://stackoverflow.com/a/40326875 and
	 * http://stackoverflow.com/a/42526830
	 * 
	 * @return an {@link Observable} of incoming {@link Fact}s, incl other
	 *         transaction kinds (e.g. pm/dc/st) than own specialty {@link F}
	 */
	Observable<Fact> emitFacts();

	/**
	 * @param filter
	 * @param handler
	 * @return self
	 */
	default <A extends Actor<F>> A emit( final FactFilter filter,
		final ThrowingBiConsumer<A, Fact, ?> handler )
	{
		@SuppressWarnings( "unchecked" )
		final A self = (A) this;
		emitFacts().filter( filter::match ).subscribe( rq ->
		{
			try
			{
				handler.accept( self, rq );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			}
		}, e -> LogUtil.getLogger( getClass() ).error( "Problem", e ) );
		return self;
	}

	/**
	 * @param tranKind the {@link Fact} sub-type to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s, incl other
	 *         transaction kinds (e.g. pm/dc/st) than own specialty {@link F}
	 */
	default <T extends Fact> Observable<T> emit( final Class<T> tranKind )
	{
		return emitFacts().ofType( tranKind );
	}

	/**
	 * @param factKind the {@link FactKind} to filter for
	 * @return an {@link Observable} of incoming (specialism) {@link Fact}s
	 */
	default Observable<F> emit( final FactKind factKind )
	{
		return emit( specialism() ).filter( f -> f.kind().equals( factKind ) );
	}

	/**
	 * @param factKind the {@link FactKind} to filter for
	 * @param creatorRef the origin {@link Actor.ID} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s
	 */
	default Observable<F> emit( final FactKind factKind,
		final Actor.ID creatorRef )
	{
		return emit( specialism() ).filter( f -> f.kind().equals( factKind )
				&& f.creatorRef().organizationRef()
						.equals( creatorRef.organizationRef() ) );
	}

	/**
	 * @param tranKind the type of {@link Fact} to filter for
	 * @param factKind the {@link FactKind} to filter for
	 * @return an {@link Observable} of incoming {@link Fact}s, incl other
	 *         transaction kinds (e.g. pm/dc/st) than own specialty {@link F}
	 */
	default <T extends Fact> Observable<T> emit( final Class<T> tranKind,
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
		return emit( tranKind ).filter( f -> f.creatorRef().organizationRef()
				.equals( creatorRef.organizationRef() ) );
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
		return emit( tranKind, factKind ).filter( fact -> fact.creatorRef()
				.organizationRef().equals( creatorRef.organizationRef() ) );
	}

	@FunctionalInterface
	interface FactFilter //extends Func1<Fact, Boolean> 
	{
		Boolean match( Fact fact );

		static FactFilter of( final FactKind factKind )
		{
			return f -> f.kind().equals( factKind );
		}
	}

	/** @deprecated may change */
	FactFilter RQ_FILTER = FactFilter.of( FactKind.REQUESTED );

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
//	@SuppressWarnings( "unchecked" )
	default F initiate( final Actor.ID executorRef, final Map<?, ?>... params )
	{
		return initiate( specialism(), executorRef, null, null, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact} FIXME replace by chaining API
	 *         {@link #with(String, Object)}
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
	 * @param causeRef the {@link Fact} triggering the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact} FIXME replace by chaining APIs
	 *         #causeRef(Fact.ID) and {@link #with(String, Object)}
	 */
	default F initiate( final Actor.ID executorRef, final Fact.ID causeRef,
		final Map<?, ?>... params )
	{
		return initiate( specialism(), executorRef, causeRef, null, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param causeRef the {@link Fact} triggering the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact} FIXME replace by chaining APIs
	 *         #causeRef(Fact.ID) and {@link #with(String, Object)}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Fact.ID causeRef,
		final Map<?, ?>... params )
	{
		return initiate( tranKind, executorRef, causeRef, null, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param expire the expire {@link Instant} of the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact} FIXME replace by chaining APIs
	 *         #expire(Instant) and {@link #with(String, Object)}
	 */
	default F initiate( final Actor.ID executorRef, final Instant expire,
		final Map<?, ?>... params )
	{
		return initiate( specialism(), executorRef, null, expire, params );
	}

	/**
	 * create a request initiating a new {@link Transaction}
	 * 
	 * @param tranKind the type of {@link Fact} being coordinated
	 * @param executorRef a {@link Actor.ID} of the intended executor
	 * @param expire the expire {@link Instant} of the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact} FIXME replace by chaining APIs
	 *         #expire(Instant) and {@link #with(String, Object)}
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
	 * @param causeRef the {@link Fact} triggering the request, or {@code null}
	 * @param expire the expire {@link Instant} of the request, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return the initial request {@link Fact} FIXME replace by chaining APIs
	 *         #expire(Instant), #causeRef(Fact.ID) and
	 *         {@link #with(String, Object)}
	 */
	default <T extends Fact> T initiate( final Class<T> tranKind,
		final Actor.ID executorRef, final Fact.ID causeRef,
		final Instant expire, final Map<?, ?>... params )
	{
		return transact( tranKind, id(), executorRef )
				.generate( FactKind.REQUESTED, causeRef, expire, params );
	}

	/**
	 * @param cause the {@link Fact} triggering the response
	 * @param factKind the {@link FactKind} of response
	 * @param params additional property (or bean attribute) values, if any
	 * @return a response {@link Fact} FIXME replace by chaining API
	 *         {@link #with(String, Object)}
	 */
	default F respond( final F cause, final FactKind factKind,
		final Map<?, ?>... params )
	{
		return respond( cause, factKind, null, params );
	}

	/**
	 * @param cause the {@link Fact} triggering the response, for reference
	 * @param factKind the {@link FactKind} of the response
	 * @param expire the expire {@link Instant} of the response, or {@code null}
	 * @param params additional property (or bean attribute) values, if any
	 * @return a response {@link Fact} FIXME replace by chaining APIs
	 *         #expire(Instant) and {@link #with(String, Object)}
	 */
	@SuppressWarnings( "unchecked" )
	default F respond( final F cause, final FactKind factKind,
		final Instant expire, final Map<?, ?>... params )
	{
		return ((Transaction<F>) cause.transaction()).generate( factKind,
				cause.id(), expire, params );
	}

	default Factory factory()
	{
		return binder().inject( Factory.class );
	}

	default Observable<Instant> atEach( final Timing when )
	{
		try
		{
			return atEach( when.offset( scheduler().offset() ).iterate() );
		} catch( final ParseException e )
		{
			return Observable.error( e );
		}
	}

	default Observable<Expectation> atEach( final Timing when,
		final ThrowingConsumer<Instant, ?> what )
	{
		try
		{
			return atEach( when.offset( scheduler().offset() ).iterate(), what );
		} catch( final ParseException e )
		{
			return Observable.error( e );
		}
	}

	Actor<Fact> root();

	/**
	 * @param name the peer name
	 * @return a new {@link Actor.ID} with the same {@link #parentRef()}
	 */
	default Actor.ID peerRef( final String name )
	{
		return root().id().peerRef( name );
	}

	/** @return the role or "concrete" type of {@link Actor} */
	<A extends Actor<? extends F>> Class<A> role();

	/**
	 * @return the transaction kind (Fact type arument) or agendum handled by
	 *         this {@link #role()}
	 */
	Class<F> specialism();

	/**
	 * @param actorKind
	 * @return an actor view for performing either or both of the initiator and
	 *         executor sides of its transaction sub-type(s)
	 */
	default <A extends Actor<T>, T extends F> Actor.ID specialist(
		final Class<A> actorKind, final ThrowingConsumer<A, ?> consumer )
	{
		try
		{
			final A role = subRole( actorKind );
			consumer.accept( role );
			return role.id();
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param actorKind
	 * @return an actor view for performing either or both of the initiator and
	 *         executor sides of its transaction sub-type(s)
	 */
	default <A extends Actor<T>, T extends F> A
		subRole( final Class<A> actorKind )
	{
		return specialist( actorKind, this );
	}

	/**
	 * @param actorKind
	 * @param actorImpl
	 * @return an actor view for performing either or both of the initiator and
	 *         executor sides of its transaction kind (fact type argument)
	 */
	<A extends Actor<T>, T extends F> A specialist( Class<A> actorKind,
		Actor<? super T> actorImpl );

	default <S extends F> Actor<S> specializeIn( final Class<S> tranKind )
	{
		final Actor<F> parent = this;
		return new Actor<S>()
		{
			private final ID specialistId = ID.of( tranKind, root().id() );

			@Override
			public ID id()
			{
				return this.specialistId;
			}

			@Override
			public Actor<Fact> root()
			{
				return parent.root();
			}

			@Override
			public Class<S> specialism()
			{
				return tranKind;
			}

			@Override
			public Observable<Fact> emitFacts()
			{
				return parent.emitFacts();
			}

			@Override
			public void onSubscribe( final Disposable d )
			{
				parent.onSubscribe( d );
			}

			@Override
			public void onComplete()
			{
				parent.onComplete();
			}

			@Override
			public void onError( final Throwable e )
			{
				parent.onError( e );
			}

			@Override
			public void onNext( final Fact fact )
			{
				parent.onNext( fact );
			}

			@Override
			public <A extends Actor<T>, T extends S> A specialist(
				final Class<A> actorKind, final Actor<? super T> actorImpl )
			{
				return parent.specialist( actorKind, actorImpl );
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
				return parent.properties(); // TODO localize properties?
			}

			@Override
			public Scheduler scheduler()
			{
				return parent.scheduler();
			}

			@Override
			public LocalBinder binder()
			{
				return parent.binder();
			}

			@Override
			public <A extends Actor<? extends S>> Class<A> role()
			{
				return parent.role();
			}

			@Override
			public Observable<PropertyChangeEvent> emitChanges()
			{
				return parent.emitChanges();
			}
		};
	}

	/**
	 * @param actorKind the type of {@link Actor} to mimic
	 * @return the {@link Proxy} instance
	 */
//	@SuppressWarnings( "unchecked" )
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
//	@SuppressWarnings( "unchecked" )
	default <A extends Actor<T>, T extends F> A
		proxyAs( final Class<A> actorKind, final Observer<Method> callObserver )
	{
		return Attributed.createProxyInstance( this, actorKind, callObserver );
	}

	/**
	 * @param actorKind the type of {@link Actor} to mimic
	 * @param impl an implementation to route calls to
	 * @return the {@link Proxy} instance
	 */
//	@SuppressWarnings( "unchecked" )
	default <A extends Actor<T>, T extends F> A
		proxyAs( final Actor<? super T> impl, final Class<A> actorKind )
	{
		return Attributed.createProxyInstance( impl, actorKind, null );
	}

	/**
	 * {@link Actor.ID}
	 */
	@JsonDeserialize( converter = ID.FromStringConverter.class )
	class ID extends LocalId
	{

		public static String SPECIALIST_NAME_POSTFIX = "Exec";

		public static class FromStringConverter extends StdConverter<String, ID>
		{
			@Override
			public ID convert( final String value )
			{
				return of( LocalId.valueOf( value ) );
			}
		}

		/**
		 * @param name
		 * @param parent
		 * @return an {@link Actor.ID}
		 */
		public static ID of( final Class<? extends Fact> tranKind,
			final ID parent )
		{
			return Id.of( new ID(),
					tranKind.getSimpleName() + SPECIALIST_NAME_POSTFIX,
					parent );
		}

		/**
		 * @param name
		 * @param parent
		 * @return an {@link Actor.ID}
		 */
		public static ID of( final Comparable<?> name, final LocalId parent )
		{
			return Id.of( new ID(), name, parent );
		}

		/**
		 * @param raw
		 * @return an {@link Actor.ID} for {@link Actor} references, or
		 *         {@link LocalId} for {@link LocalContextual} references
		 */
		public static LocalId ofParent( final LocalId raw )
		{
			return raw == null ? null
					: raw.parentRef() == null ? raw
							: ID.of( raw.unwrap(), raw.parentRef() );
		}

		/**
		 * @param raw
		 * @return an {@link Actor.ID}
		 */
		public static ID of( final LocalId raw )
		{
			Objects.requireNonNull( raw );
			Objects.requireNonNull( raw.parentRef(),
					"LocalContextual identifier? " + raw.unwrap() );
			return ID.of( raw.unwrap(), ofParent( raw.parentRef() ) );
		}

		/** @return the recursed root ancestor {@link Actor.ID} */
		public ID organizationRef()
		{
			for( LocalId id = this;; id = id.parentRef() )
				if( id.parentRef() instanceof ID == false ) return (ID) id;
		}

		/**
		 * @param name the peer name
		 * @return a new {@link Actor.ID} with the same {@link #parentRef()}
		 */
		public ID peerRef( final Comparable<?> name )
		{
			return ID.of( name, parentRef() );
		}
	}

	class Simple implements Actor<Fact>
	{
		public static Simple of( final LocalBinder binder, final ID id,
			final Class<? extends Actor<?>> role )
		{
			final Simple result = binder.inject( Simple.class );
			result.id = id;
			result.role = role;
			return result;
		}

		private transient final Map<Transaction.ID, Transaction<?>> txs = new ConcurrentHashMap<>();

		private transient final Subject<Fact> facts = PublishSubject.create();

		private transient final Map<Class<?>, Observable<?>> typeemit = new ConcurrentHashMap<>();

		private transient final Map<Class<?>, Actor<?>> specialists = new ConcurrentHashMap<>();

		private transient final Map<String, Object> properties = new ConcurrentHashMap<>();

		private transient final Subject<PropertyChangeEvent> changes = PublishSubject
				.create();

		@Inject
		private transient Transaction.Factory txFactory;

		@Inject
		private transient Scheduler scheduler;

		@Inject
		private transient LocalBinder binder;

		@SuppressWarnings( "rawtypes" )
		private transient Class<? extends Actor> role = Actor.class;

		private ID id;

		@Inject
		public Simple()
		{
			// empty bean constructor
		}

		@Override
		public ID id()
		{
			return this.id;
		}

		@Override
		public Actor<Fact> root()
		{
			return this;
		}

		@Override
		public Map<String, Object> properties()
		{
			return this.properties;
		}

		@JsonAnySetter
		@Override
		public Object set( final String propertyName, final Object newValue )
		{
			final Object oldValue = properties().put( propertyName, newValue );
			final Actor<?> self = this;
			this.changes.onNext( new PropertyChangeEvent( self, propertyName,
					oldValue, newValue ) );
			return oldValue;
		}

		@Override
		public Observable<PropertyChangeEvent> emitChanges()
		{
			return this.changes;
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
						tx.commits().subscribe( root()::onNext, root()::onError,
								() -> this.txs.remove( tid ) );
						return tx;
					} );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <A extends Actor<? extends Fact>> Class<A> role()
		{
			return (Class<A>) this.role;
		}

		@Override
		public Class<Fact> specialism()
		{
			return Fact.class;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <A extends Actor<F>, F extends Fact> A specialist(
			final Class<A> actorKind, final Actor<? super F> actorImpl )
		{
			final Class<F> specialism = (Class<F>) TypeArguments
					.of( Actor.class, actorKind ).get( 0 );
			final Actor<? super F> specialist = (Actor<? super F>) this.specialists
					.computeIfAbsent( specialism,
							key -> actorImpl.specializeIn( (Class<F>) key ) );
			final A result = specialist.proxyAs( specialist, actorKind );
			result.onInit();
			return result;
		}

		@Override
		public Observable<Fact> emitFacts()
		{
			return this.facts;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public <F extends Fact> Observable<F> emit( final Class<F> tranKind )
		{
			return (Observable<F>) this.typeemit.computeIfAbsent( tranKind,
					key -> emitFacts()
							.filter( f -> key.isAssignableFrom( f.type() ) )
							.map( tranKind::cast ) );
		}

		@Override
		public void onSubscribe( final Disposable d )
		{
			this.facts.onSubscribe( d );
		}

		@Override
		public void onComplete()
		{
			this.facts.onComplete();
		}

		@Override
		public void onError( final Throwable e )
		{
			this.facts.onError( e );
		}

		@Override
		public void onNext( final Fact fact )
		{
			try
			{
				this.facts.onNext( fact );
			} catch( final Exception e )
			{
				e.printStackTrace();
			}
		}

		@Override
		public Scheduler scheduler()
		{
			return this.scheduler;
		}

		@Override
		public LocalBinder binder()
		{
			return this.binder;
		}

		private Simple withId( final ID id )
		{
			this.id = id;
			return this;
		}

		/**
		 * @param factExchange
		 * @return
		 */
		private Simple withExchangeDirection( final FactExchange factExchange,
			final Direction direction )
		{
			factExchange.register( this, direction );
			return this;
		}
	}

	/**
	 * {@link Factory} should ensure that created Actor.IDs are globally unique
	 * 
	 * @version $Id: 84a0e0593199134740368157dc1dd614a0a46739 $
	 * @author Rick van Krevelen
	 */
	interface Factory extends LocalContextual
	{

		/**
		 * @param id the {@link ID} of the new {@link Actor}
		 * @return a (cached) {@link Actor}
		 */
		Actor<Fact> create( ID id );

		default Actor<Fact> create( final Comparable<?> name )
		{
			return create( ID.of( name, id() ) );
		}

		@Singleton
		class LocalCaching implements Factory
		{
			private final transient Map<ID, Actor.Simple> localCache = new ConcurrentHashMap<>();

			@Inject
			private transient LocalBinder binder;

			@Inject
			private transient FactExchange factExchange;

			@Override
			public Actor.Simple create( final ID id )
			{
				return this.localCache.computeIfAbsent( id.organizationRef(),
						orgRef ->
						{
							final Actor.Simple result = this.binder
									.inject( Actor.Simple.class ).withId( id )
									.withExchangeDirection( this.factExchange,
											Direction.BIDI );
							result.onInit();
							return result;
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
		}
	}
}