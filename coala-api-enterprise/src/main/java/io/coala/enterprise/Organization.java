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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.coala.name.Id;
import io.coala.name.Identified;
import io.coala.time.Scheduler;
import io.coala.time.Proactive;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link Organization}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Organization
	extends Proactive, Identified.Ordinal<Organization.ID>
{

	/** @return */
	Observable<CoordinationFact> incoming();

	default <F extends CoordinationFact> Observable<F>
		incoming( final Class<F> factKind )
	{
		return incoming().ofType( factKind );
	}

	default <F extends CoordinationFact> Observable<F>
		incoming( final Class<F> factKind, final CoordinationFactType type )
	{
		return incoming( factKind ).filter( f ->
		{
			return f.type() == type;
		} );
	}

	/** @return */
	Observable<CoordinationFact> outgoing();

	default <F extends CoordinationFact> Observable<F>
		outgoing( final Class<F> factKind )
	{
		return outgoing().ofType( factKind );
	}

	default <F extends CoordinationFact> Observable<F>
		outgoing( final Class<F> factKind, final CoordinationFactType type )
	{
		return outgoing( factKind ).filter( f ->
		{
			return f.type() == type;
		} );
	}

	/**
	 * @param actorID
	 * @return
	 */
	CompositeActor actor( CompositeActor.ID actorID );

	/** @param incoming */
	void consume( CoordinationFact incoming );

	/**
	 * @param actorID
	 * @return
	 */
	default CompositeActor actor( final String actorID )
	{
		return actor( CompositeActor.ID.of( actorID, id() ) );
	}

	/**
	 * {@link ID}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class ID extends Id.Ordinal<String>
	{
		public static Organization.ID of( final String name )
		{
			return Util.of( name, new ID() );
		}
	}

	static Organization of( final Scheduler scheduler, final String name,
		final CoordinationFact.Factory factFactory )
	{
		final Organization.ID id = ID.of( name );
		final Subject<CoordinationFact, CoordinationFact> incoming = PublishSubject
				.create();
		final Subject<CoordinationFact, CoordinationFact> outgoing = PublishSubject
				.create();
		final Map<CompositeActor.ID, CompositeActor> actorMap = new HashMap<>();
		return new Organization()
		{
			@Override
			public Scheduler scheduler()
			{
				return scheduler;
			}

			@Override
			public Organization.ID id()
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
					final CompositeActor result = CompositeActor.of( id, this,
							factFactory );
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
		Organization create( String name );

		@Singleton
		class Simple implements Factory
		{
			@Inject
			private Scheduler scheduler;

			@Inject
			private CoordinationFact.Factory factFactory;

			@Override
			public Organization create( final String name )
			{
				return Organization.of( this.scheduler, name,
						this.factFactory );
			}
		}

	}
}