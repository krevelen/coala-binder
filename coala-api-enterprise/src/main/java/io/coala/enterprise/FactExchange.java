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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link FactExchange}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface FactExchange
{
	public enum Direction
	{
		NONE, IN, OUT, BIDI;

		public boolean isIncoming()
		{
			return this == BIDI || this == IN;
		}

		public boolean isOutgoing()
		{
			return this == BIDI || this == OUT;
		}
	}

	default List<Disposable> register( final Actor<?> actor )
	{
		return register( actor, Direction.BIDI );
	}

	default List<Disposable> register( final Actor<?> actor,
		final Direction direction )
	{
		return register( actor, direction.isIncoming(),
				direction.isOutgoing() );
	}

	List<Disposable> register( Actor<?> actor, boolean incoming,
		boolean outgoing );

	Observable<Fact> snif();

	@Singleton
	class SimpleBus implements FactExchange
	{
		private final Map<Actor.ID, List<Disposable>> registry = new ConcurrentHashMap<>();

		private final Subject<Fact> bus = PublishSubject.create();

		private Disposable subscribeIncoming( final Actor<?> actor )
		{
			return this.bus.filter( fact -> fact.isIncoming( actor.id() ) )
					.subscribe( actor::onNext );
		}
		
		/**
		 * @return all {@link #emitFacts() emitted Facts} where
		 *         {@link Fact#isInternal() outgoing} {@code == true}
		 */
		private Disposable subscribeOutgoing( final Actor<?> actor )
		{
			return actor.root().emitFacts()
					.filter( fact -> fact.isOutgoing( actor.id() ) )
					.subscribe( this.bus::onNext );
		}

		private Disposable switchSub( final Disposable current,
			final boolean subscribe, final Supplier<Disposable> supplier )
		{
			if( current == null ) return subscribe ? supplier.get() : null;
			if( subscribe )
				return current.isDisposed() ? supplier.get() : current;
			if( !current.isDisposed() ) current.dispose();
			return null;
		}

		@Override
		public List<Disposable> register( final Actor<?> actor,
			final boolean incoming, final boolean outgoing )
		{
			return this.registry.compute( actor.id().organizationRef(), (
				orgRef, subs ) -> Arrays.asList(
						switchSub( subs == null ? null : subs.get( 0 ),
								incoming, () -> subscribeIncoming( actor ) ),
						switchSub( subs == null ? null : subs.get( 1 ),
								outgoing,
								() -> subscribeOutgoing( actor ) ) ) );
		}

		@Override
		public Observable<Fact> snif()
		{
			return this.bus;
		}
	}
}
