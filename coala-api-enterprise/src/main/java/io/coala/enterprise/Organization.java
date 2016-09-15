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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.coala.bind.LocalBinder;
import io.coala.enterprise.CompositeActor.ID;
import io.coala.name.Identified;
import io.coala.time.Proactive;
import io.coala.time.Scheduler;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Organization}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Organization extends Proactive, Identified.Ordinal<ID>
{

	/** @return */
	Observable<CoordinationFact> incoming();

	default <F extends CoordinationFact> Observable<F>
		incoming( final Class<F> factKind )
	{
		return incoming().ofType( factKind );
	}

	default <F extends CoordinationFact> Observable<F>
		incoming( final Class<F> tranKind, final CoordinationFactKind factKind )
	{
		return incoming( tranKind ).filter( f ->
		{
			return f.kind() == factKind;
		} );
	}

	/** @return */
	Observable<CoordinationFact> outgoing();

	default <F extends CoordinationFact> Observable<F>
		outgoing( final Class<F> tranKind )
	{
		return outgoing().ofType( tranKind );
	}

	default <F extends CoordinationFact> Observable<F>
		outgoing( final Class<F> tranKind, final CoordinationFactKind kind )
	{
		return outgoing( tranKind ).filter( f ->
		{
			return f.kind() == kind;
		} );
	}

	/**
	 * @param actorID
	 * @return
	 */
	default CompositeActor actor( final String actorID )
	{
		return actor( ID.of( actorID, id() ) );
	}

	/**
	 * @param actorID
	 * @return
	 */
	CompositeActor actor( ID actorID );

	/** @param incoming */
	void consume( CoordinationFact incoming );

//	/**
//	 * {@link ID}
//	 * 
//	 * @version $Id$
//	 * @author Rick van Krevelen
//	 */
//	class ID extends LocalId
//	{
//		@Override
//		public String toString()
//		{
//			return unwrap().toString(); // hide local context
//		}
//
//		public static ID of( final String name, final LocalId ctx )
//		{
//			return Id.of( new ID(), name, ctx );
//		}
//
//		protected static ID of( final LocalId ctx )
//		{
//			return of( (String) ctx.unwrap(), ctx.parent() );
//		}
//
//		@Entity( name = Dao.ENTITY_NAME )
//		public static class Dao extends LocalId.Dao
//		{
//			public static final String ENTITY_NAME = "ORGANIZATION";
//
//			@Override
//			public ID restore( final LocalBinder binder )
//			{
//				return ID.of( this.id, this.parent.restore( binder ) );
//			}
//		}
//	}

	static Organization of( final LocalBinder binder, final String name )
	{
		return of( binder, ID.of( name, binder.id() ) );
	}

	static Organization of( final LocalBinder binder, final ID id )
	{
		return of( id, binder.inject( Scheduler.class ),
				binder.inject( CoordinationFact.Factory.class ),
				binder.inject( CoordinationFactBank.Factory.class ) );
	}

	static Organization of( final ID id, final Scheduler scheduler,
		final CoordinationFact.Factory factFactory,
		final CoordinationFactBank.Factory bankFactory )
	{
		final Subject<CoordinationFact, CoordinationFact> incoming = PublishSubject
				.create();
		final Subject<CoordinationFact, CoordinationFact> outgoing = PublishSubject
				.create();
		final Map<ID, CompositeActor> actorMap = new ConcurrentHashMap<>();
		return new Organization()
		{
			@Override
			public Scheduler scheduler()
			{
				return scheduler;
			}

			@Override
			public ID id()
			{
				return id;
			}

			@Override
			public Observable<CoordinationFact> incoming()
			{
				return incoming;
			}

			@Override
			public Observable<CoordinationFact> outgoing()
			{
				return outgoing.asObservable();
			}

			@Override
			public CompositeActor actor( final CompositeActor.ID actorID )
			{
				return actorMap.computeIfAbsent( actorID, id ->
				{
					// TODO use binder/factory
					final CompositeActor result = CompositeActor.of( id, this,
							factFactory, bankFactory );
					result.outgoing().subscribe( outgoing );
					return result;
				} );
			}

			@Override
			public void consume( final CoordinationFact fact )
			{
				incoming.onNext( fact );
			}
		};
	}

	interface Factory
	{
		Organization create( final String name );

		Organization create( ID name );

		@Singleton
		class LocalCaching implements Factory
		{
			private final Map<ID, Organization> localCache = new ConcurrentHashMap<>();

			@Inject
			private LocalBinder binder;

			@Inject
			private Scheduler scheduler;

			@Inject
			private CoordinationFact.Factory factFactory;

			@Inject
			private CoordinationFactBank.Factory bankFactory;

			@Override
			public Organization create( final String name )
			{
				return create( ID.of( name, this.binder.id() ) );
			}

			@Override
			public Organization create( final ID id )
			{
				return this.localCache.computeIfAbsent( id, k ->
				{
					return Organization.of( id, this.scheduler,
							this.factFactory, this.bankFactory );
				} );
			}
		}
	}
}