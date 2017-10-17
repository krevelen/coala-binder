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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Time;

import io.coala.exception.Thrower;
import io.coala.function.Caller;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.math.QuantityUtil;
import io.coala.util.Comparison;
import io.reactivex.Observable;

/**
 * {@link Proactive} tags entities with self-initiating behaviors, by using
 * their {@link #scheduler()} and several shorthand utility methods
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@FunctionalInterface
public interface Proactive extends Timed
{

	/** @return the {@link Scheduler} of this {@link Proactive} object */
	Scheduler scheduler();

	/** @return the current {@link Instant} */
	@Override
	default Instant now()
	{
		return scheduler().now();
	}

	default FutureSelf after( final java.time.Duration delay )
	{
		return after( delay.toNanos(), TimeUnits.NANOS );
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
		Objects.requireNonNull( delay, "no delay?" );
		return after( QuantityUtil.valueOf( delay, unit ) );
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
		Objects.requireNonNull( delay, "no delay?" );
		return FutureSelf.of( this,
				QuantityUtil.signum( delay ) < 1 ? now() : now().add( delay ) );
	}

	/**
	 * @param delay the {@link Supplier} of {@link Quantity} delays to schedule
	 * @return the {@link Instant}s, {@link Observable} after each delay passes
	 */
	default Observable<Instant>
		infiniterate( final Callable<? extends Quantity<?>> delay )
	{
		return atEach( () -> (Infiniterator<Instant>) () ->
		{
			try
			{
				return now().add( delay.call() );
			} catch( final Exception e )
			{
				scheduler().fail( e );
				return Thrower.rethrowUnchecked( e );
			}
		} );
	}

	/**
	 * @param delay the {@link Supplier} of {@link Quantity} delays to schedule
	 * @return the {@link Instant}s, {@link Observable} after each delay passes
	 */
	default Observable<Expectation> infiniterate(
		final Callable<? extends Quantity<?>> delay,
		final ThrowingConsumer<Instant, ?> call )
	{
		return atEach( () -> (Infiniterator<Instant>) () ->
		{
			try
			{
				return now().add( delay.call() );
			} catch( final Exception e )
			{
				scheduler().fail( e );
				return Thrower.rethrowUnchecked( e );
			}
		}, call );
	}

//	/**
//	 * @param when the {@link Iterable} stream of {@link Instant}s to schedule
//	 * @return an {@link Observable} stream of {@link FutureSelf} wrappers
//	 *         pushed to any {@link Observable#subscribe} caller upon each
//	 *         {@link Instant}'s scheduled occurrence
//	 */
//	default Observable<Instant> atEach( final Iterable<Instant> when )
//	{
//		return scheduler().schedule( when );
//	}
//
//	/**
//	 * @param when the {@link Iterable} stream of {@link Instant}s to schedule
//	 * @param what the {@link ThrowingConsumer} function to call each time
//	 * @return {@link Observable} stream of {@link Expectation}s pushed to any
//	 *         {@link Observable#subscribe} caller
//	 */
//	default Observable<Expectation> atEach( final Iterable<Instant> when,
//		final ThrowingConsumer<Instant, ?> what )
//	{
//		return scheduler().schedule( when, what );
//	}
//
//	/**
//	 * NOTE irreproducible: iterable is buffered asynchronously/unordered
//	 * 
//	 * @param when the {@link Observable} stream of {@link Instant}s to schedule
//	 * @return an {@link Observable} stream of {@link Instant}s pushed to any
//	 *         {@link Observable#subscribe} caller upon each {@link Instant}'s
//	 *         scheduled occurrence
//	 */
//	default Observable<Instant> atEach( final Observable<Instant> when )
//	{
//		return scheduler().schedule( when );
//	}
//
//	/**
//	 * NOTE irreproducible: iterable is buffered asynchronously/unordered
//	 * 
//	 * @param when the {@link Observable} stream of {@link Instant}s to schedule
//	 * @param what the {@link ThrowingConsumer} function to call each time
//	 * @return
//	 */
//	default Observable<Expectation> atEach( final Observable<Instant> when,
//		final ThrowingConsumer<Instant, ?> what )
//	{
//		return scheduler().schedule( when, what );
//	}

	/**
	 * @param runner the {@link Runnable} (method) to call when time comes
	 * @return the {@link Expectation} for potential cancellation
	 */
	default Expectation atOnce( final ThrowingRunnable<?> runner )
	{
		return scheduler().schedule( now(), runner );
	}

	/**
	 * @param call the {@link Callable} (method) to call when time comes
	 * @param t arg0
	 * @return the {@link Expectation} for potential cancellation
	 */
	default Expectation atOnce( final ThrowingConsumer<Instant, ?> call )
	{
		return atOnce( Caller.ofThrowingConsumer( call, now() )::run );
	}

	/**
	 * @param when the future {@link Instant}
	 * @return a {@link FutureSelf} wrapper to allow chaining
	 */
	default FutureSelf at( final Instant when )
	{
		return FutureSelf.of( this, when );
	}

	/**
	 * @param when the future {@link Instant}
	 * @return a {@link FutureSelf} wrapper to allow chaining
	 */
	default FutureSelf at( final java.time.Instant when )
	{
		return after( java.time.Duration
				.between( scheduler().nowDT().toInstant(), when ) );
	}

	/**
	 * @param when the future {@link Instant}
	 * @return a {@link FutureSelf} wrapper to allow chaining
	 */
	default FutureSelf at( final ZonedDateTime when )
	{
		return at( when.toInstant() );
	}

	/**
	 * @param when the future {@link Instant}
	 * @return a {@link FutureSelf} wrapper to allow chaining
	 */
	default FutureSelf at( final LocalDateTime when )
	{
		return at( when.toInstant( scheduler().offset().getOffset() ) );
	}

	/**
	 * @param when the series of {@link Instant}s to schedule
	 * @return an {@link Observable} stream of {@link FutureSelf} wrappers
	 *         pushed to any {@link Observable#subscribe} caller upon each
	 *         {@link Instant}'s scheduled occurrence
	 */
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
	default Observable<FutureSelf> atEach( final Observable<Instant> when )
	{
		return scheduler().schedule( when )
				.map( t -> FutureSelf.of( this, t ) );
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
		 * @param t arg0
		 * @return the {@link Expectation} for potential cancellation
		 */
		default Expectation call( final ThrowingConsumer<Instant, ?> call )
		{
			return call( () -> call.accept( now() ) );
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
