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
package io.coala.time;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Time;

import io.coala.exception.Thrower;
import io.coala.function.Caller;
import io.coala.function.ThrowingBiConsumer;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.util.Comparison;
import io.reactivex.Observable;

/**
 * {@link Proactive} tags entities with self-initiating behaviors, by using
 * their {@link #scheduler()} and several shorthand utility methods
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Proactive extends Timed
{

	/** @return the {@link Scheduler} of this {@link Proactive} object */
	Scheduler scheduler();

	/** @return the current {@link Instant} */
	default Instant now()
	{
		return scheduler().now();
	}

	/**
	 * @param delay the {@link Duration} of delay
	 * @return the {@link FutureSelf}
	 */
	default FutureSelf after( final Duration delay )
	{
		return after( delay.unwrap() );
	}

	/**
	 * @param delay the {@link Number} of delay, in default units
	 * @return the {@link FutureSelf}
	 */
	default FutureSelf after( final Number delay )
	{
		return after( delay, now().unit() );
	}

	/**
	 * shorthand
	 * 
	 * @param delay the delay amount {@link Number}
	 * @param unit the delay amount {@link Unit} ( {@link Time} or
	 *            {@link Dimensionless} steps)
	 * @return the {@link FutureSelf}
	 */
	default FutureSelf after( final Number delay, final Unit<?> unit )
	{
		return after( Duration.of( delay, unit ) );
	}

	/**
	 * shorthand
	 * 
	 * @param delay the {@link Quantity} of delay (units of {@link Time} or
	 *            {@link Dimensionless} steps)
	 * @return the {@link FutureSelf} wrapper to allow chaining
	 */
	default FutureSelf after( final Quantity<?> delay )
	{
		return FutureSelf.of( this, now().add( delay ) );
	}

	/**
	 * @param delay the future {@link Instant}
	 * @return a {@link FutureSelf} wrapper to allow chaining
	 */
	default FutureSelf at( final Instant when )
	{
		return FutureSelf.of( this, when );
	}

	/**
	 * @param when the series of {@link Instant}s to schedule
	 * @return an {@link Observable} stream of {@link FutureSelf} wrappers
	 *         pushed to any {@link Observable#subscribe} caller upon each
	 *         {@link Instant}'s scheduled occurrence
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default Observable<Instant> atEach( final Instant... when )
	{
		if( when == null || when.length == 0 ) return Observable.empty();
		return atEach( Arrays.asList( when ) );
	}

	/**
	 * @param when the {@link Iterable} stream of {@link Instant}s to schedule
	 * @return an {@link Observable} stream of {@link FutureSelf} wrappers
	 *         pushed to any {@link Observable#subscribe} caller upon each
	 *         {@link Instant}'s scheduled occurrence
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default Observable<Instant> atEach( final Iterable<Instant> when )
	{
		return scheduler().schedule( when );
	}

	/**
	 * @param when the {@link Iterable} stream of {@link Instant}s to schedule
	 * @param what the {@link ThrowingConsumer} function to call each time
	 * @return {@link Observable} stream of {@link Expectation}s pushed to any
	 *         {@link Observable#subscribe} caller
	 */
	default Observable<Expectation> atEach( final Iterable<Instant> when,
		final ThrowingConsumer<Instant, ?> what )
	{
		return scheduler().schedule( when, what );
	}

	/**
	 * NOTE irreproducible: iterable is buffered asynchronously/unordered
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s to schedule
	 * @return an {@link Observable} stream of {@link Instant}s pushed to any
	 *         {@link Observable#subscribe} caller upon each {@link Instant}'s
	 *         scheduled occurrence
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default Observable<Instant> atEach( final Observable<Instant> when )
	{
		return scheduler().schedule( when );
	}

	/**
	 * NOTE irreproducible: iterable is buffered asynchronously/unordered
	 * 
	 * @param when the {@link Observable} stream of {@link Instant}s to schedule
	 * @param what the {@link ThrowingConsumer} function to call each time
	 * @return
	 */
	default Observable<Expectation> atEach( final Observable<Instant> when,
		final ThrowingConsumer<Instant, ?> what )
	{
		return scheduler().schedule( when, what );
	}

	/**
	 * {@link Infiniterator} is a utility interface for lazy scheduling of some
	 * infinite behavior with variable delays using a single functional lambda
	 * expression, e.g.
	 * 
	 * <pre>
	 * myProactiveObj.{@link Proactive#atEach(Iterable) atEach}( 
	 *   () -> ({@link Infiniterator}) 
	 *     () -> {@link #now()}.add( this::myVariableDelay ) 
	 * ).subscribe( this::myInfiniteBehavior );
	 * </pre>
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@FunctionalInterface
	public interface Infiniterator extends Iterator<Instant>
	{
		@Override
		default boolean hasNext()
		{
			return true; // on to infinity (i.e. when scheduler completes)
		}
	}

	/**
	 * {@link FutureSelf} is a decorator of a {@link Proactive} object that is
	 * itself {@link Proactive} but with its {@link #now()} at a fixed (future)
	 * {@link Instant} and additional scheduling helper methods
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface FutureSelf extends Proactive
	{
		Proactive self();

		@Override
		default Scheduler scheduler()
		{
			return self().scheduler();
		}

		/**
		 * @param runner the {@link Runnable} (method) to call when time comes
		 * @return the {@link Expectation} for potential cancellation
		 */
		default Expectation call( final ThrowingRunnable<?> runner )
		{
			return scheduler().schedule( now(), runner );
		}

		/**
		 * @param call the {@link Callable} (method) to call when time comes
		 * @return the {@link Expectation} for potential cancellation
		 */
		default <R> Observable<R> emit( final Callable<R> call )
		{
			return scheduler().schedule( Observable.just( now() ), call );
		}

		/**
		 * @param call the {@link Callable} (method) to call when time comes
		 * @param t arg0
		 * @return the {@link Expectation} for potential cancellation
		 */
		default Expectation call( final ThrowingConsumer<Instant, ?> call )
		{
			return call( Caller.ofThrowingConsumer( call, now() )::run );
		}

		/**
		 * @param call the {@link Callable} (method) to call when time comes
		 * @param t constant arg0
		 * @return the {@link Expectation} for potential cancellation
		 */
		default <T> Expectation call( final ThrowingConsumer<T, ?> call,
			final T t )
		{
			return call( Caller.ofThrowingConsumer( call, t )::run );
		}

		/**
		 * @param call the {@link Callable} (method) to call when time comes
		 * @param t constant arg0
		 * @param u constant arg1
		 * @return the {@link Expectation} for potential cancellation
		 */
		default <T, U, E extends Exception> Expectation
			call( final ThrowingBiConsumer<T, U, E> call, final T t, final U u )
		{
			return call( Caller.ofThrowingBiConsumer( call, t, u )::run );
		}

		/**
		 * {@link FutureSelf} factory method
		 * 
		 * @param self the {@link Proactive} to project forward
		 * @param when the {@link Instant} to project onto
		 * @return a {@link FutureSelf} wrapper of the {@link Proactive} self at
		 *         specified {@link Instant}
		 */
		static FutureSelf of( final Proactive self, final Instant when )
		{
			if( Comparison.is( when ).lt( self.now() ) )
				Thrower.throwNew( IllegalArgumentException::new,
						() -> "Can't schedule in past: " + when + " < (now) "
								+ self.now() );
			return new FutureSelf()
			{
				@Override
				public Proactive self()
				{
					return self;
				}

				@Override
				public Instant now()
				{
					return when;
				}
			};
		}
	}
}
