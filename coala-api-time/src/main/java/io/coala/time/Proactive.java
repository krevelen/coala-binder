/* $Id: 4eb1db7e94a23f97caceb033cb485c0814b05ea6 $
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

import java.util.concurrent.Callable;

import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.unit.Unit;

//import javax.measure.quantity.Dimensionless;
//import javax.measure.quantity.Duration;

import org.jscience.physics.amount.Amount;

import io.coala.exception.Thrower;
import io.coala.function.Caller;
import io.coala.function.ThrowingBiConsumer;
import io.coala.function.ThrowingConsumer;
import io.coala.function.ThrowingRunnable;
import io.coala.util.Comparison;
import rx.Observable;
import rx.Observer;

/**
 * {@link Proactive}
 * 
 * @version $Id: 4eb1db7e94a23f97caceb033cb485c0814b05ea6 $
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
	 * @param delay the delay amount {@link Number}
	 * @param delay the delay amount {@link Unit} (
	 *            {@link javax.measure.quantity.Duration} or
	 *            {@link Dimensionless})
	 * @return the {@link FutureSelf}
	 */
	default FutureSelf after( final Number delay, final Unit<?> unit )
	{
		return after( Duration.of( delay, unit ) );
	}

	/**
	 * @param delay the {@link Amount} or {@link Measure} of delay (units of
	 *            {@link javax.measure.quantity.Duration} or
	 *            {@link Dimensionless})
	 * @return the {@link FutureSelf} wrapper to allow chaining
	 */
	default FutureSelf after( final Measurable<?> delay )
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
	 * @param when
	 * @return an {@link Observable} stream of {@link FutureSelf} wrappers
	 *         pushed after each {@link Instant} has been scheduled to occur
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	default Observable<FutureSelf> atEach( final Observable<Instant> when )
	{
		final Proactive self = this;
		return scheduler().schedule( when, (Observer) null ).map( t ->
		{
			return FutureSelf.of( self, t );
		} );
	}

	/**
	 * @param when the {@link Observable} stream of {@link Instant}s to schedule
	 * @param what the {@link ThrowingConsumer} function to call each time
	 */
	default void atEach( final Observable<Instant> when,
		final ThrowingConsumer<Instant, ?> what )
	{
		atEach( when ).subscribe( self ->
		{
			try
			{
				what.accept( self.now() );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			}
		} );
	}

	/**
	 * {@link FutureSelf} is a decorator of a {@link Proactive} object that is
	 * itself {@link Proactive} but with its {@link #now()} at a fixed (future)
	 * {@link Instant} and additional scheduling helper methods
	 * 
	 * @version $Id: 4eb1db7e94a23f97caceb033cb485c0814b05ea6 $
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
		default <R> Observable<R> call( final Callable<R> call )
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
			return call( Caller.of( call, now() )::run );
		}

		/**
		 * @param call the {@link Callable} (method) to call when time comes
		 * @param t constant arg0
		 * @return the {@link Expectation} for potential cancellation
		 */
		default <T> Expectation call( final ThrowingConsumer<T, ?> call,
			final T t )
		{
			return call( Caller.of( call, t )::run );
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
			return call( Caller.of( call, t, u )::run );
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
				Thrower.throwNew( IllegalArgumentException.class,
						"Can't schedule in past: {} < now({})", when,
						self.now() );
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
