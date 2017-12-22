/* $Id: c42c818be7ac37e941ca94c96ac25f1a01b74baa $
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.measure.Quantity;

import org.apache.logging.log4j.Logger;

import io.coala.log.LogUtil;
import io.coala.math.DecimalUtil;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * {@link Accumulator}
 * 
 * FIXME extend Indicator ?
 * 
 * @param <Q> the type of accumulated {@link Quantity}
 * @version $Id: c42c818be7ac37e941ca94c96ac25f1a01b74baa $
 * @author Rick van Krevelen
 */
public class Accumulator<Q extends Quantity<Q>> implements Proactive
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( Accumulator.class );

	private final transient Subject<Quantity<Q>> amounts = PublishSubject
			.create();

	private final transient Map<TargetAmount<Q>, Expectation> intercepts = new HashMap<>();

	private Integrator<Q> integrator;

	private Quantity<Q> amount;

	private Scheduler scheduler;

	private Instant t;

	public Accumulator( final Scheduler scheduler )
	{
		this.scheduler = scheduler;
		this.t = now();
	}

	@Override
	public String toString()
	{
		return getAmount().toString();
	}

	@Override
	public Scheduler scheduler()
	{
		return this.scheduler;
	}

	public synchronized void setIntegrator( final Integrator<Q> integrator )
	{
		this.integrator = integrator;
		recalculate();
	}

//	@SuppressWarnings( "unchecked" )
	protected synchronized void recalculate()
	{
		final Instant t0 = this.t, t1 = now();
		if( t0.equals( t1 ) ) return;
		this.t = t1;
		final Quantity<Q> delta = this.integrator.delta( t0, t1 );
		final Quantity<Q> amount = this.amount == null ? delta
				: this.amount.add( delta );
		setAmount( amount );
	}

	public synchronized void setAmount( final Quantity<Q> amount )
	{
		this.amount = amount;
		this.amounts.onNext( amount );

		this.intercepts.keySet().forEach( this::reschedule );
	}

	protected void reschedule( final TargetAmount<Q> target )
	{
		final Expectation e = this.intercepts.put( target, null );
		if( e == null ) return;
		e.remove(); // unschedule target
		LOG.trace( "unscheduled a={} at t={}, total={}", target.amount, e,
				this.intercepts.size() );
		scheduleReached( target ); // reschedule target, if any
	}

	protected void onReached( final TargetAmount<Q> target )
	{
		setAmount( target.amount );
		target.consumer.accept( now() );
		scheduleReached( target );
	}

	@SuppressWarnings( "unchecked" )
	protected void scheduleReached( final TargetAmount<Q> target )
	{
		final Instant t1 = now();
		final Instant t2 = this.integrator.when( t1,
				(Q) target.amount.subtract( this.amount ) );
		if( t2 == null || t2.compareTo( t1 ) <= 0 ) return; // no repeats, push onCompleted()?

		// schedule repeat
		/*
		 * if( t2.compareTo( t1 ) <= 0 ) throw ExceptionBuilder .unchecked(
		 * "Got time in past: %s =< %s", t2, t1 ).build();
		 */

		this.intercepts.put( target,
				at( t2 ).call( t -> onReached( target ) ) );
		LOG.trace( "scheduled a={} at t={}, total={}", target.amount, t2,
				this.intercepts.size() );
	}

	public void at( final Quantity<Q> amount, final Consumer<Instant> observer )
	{
		scheduleReached( TargetAmount.of( amount, observer ) );
	}

	public Quantity<Q> getAmount()
	{
		recalculate();
		return this.amount;
	}

	public Observable<Quantity<Q>> emitAmounts()
	{
		return this.amounts;
	}

	public static <Q extends Quantity<Q>> Accumulator<Q>
		of( final Scheduler scheduler, final Integrator<Q> integrator )
	{
		return of( scheduler, null, integrator );
	}

	public static <Q extends Quantity<Q>> Accumulator<Q> of(
		final Scheduler scheduler, final Quantity<Q> initialAmount,
		final Quantity<?> initialRate )
	{
		return of( scheduler, initialAmount,
				Integrator.<Q>ofRate( initialRate ) );
	}

	public static <Q extends Quantity<Q>> Accumulator<Q> of(
		final Scheduler scheduler, final Quantity<Q> initialAmount,
		final Integrator<Q> integrator )
	{
		final Accumulator<Q> result = new Accumulator<>( scheduler );
		result.setAmount( initialAmount );
		result.setIntegrator( integrator );
		return result;
	}

	static class TargetAmount<Q extends Quantity<Q>>
	{
		Quantity<Q> amount;
		Consumer<Instant> consumer;

		public static <Q extends Quantity<Q>> TargetAmount<Q>
			of( final Quantity<Q> amount, final Consumer<Instant> consumer )
		{
			final TargetAmount<Q> result = new TargetAmount<Q>();
			result.amount = amount;
			result.consumer = consumer;
			return result;
		}
	}

	public interface Integrator<Q extends Quantity<Q>>
	{
		/**
		 * @param start the interval start {@link Instant}
		 * @param end the interval end {@link Instant}
		 * @return the integral {@link Amount} of change
		 */
		Q delta( Instant start, Instant end );

		/**
		 * @param now the current {@link Instant}
		 * @param delta the target {@link Amount} in/decrease
		 * @return the next {@link Instant} (after {@code now}) when given
		 *         {@link Amount} occurs again, or {@code null} if never
		 */
		Instant when( Instant now, Q delta );

		static <Q extends Quantity<Q>> Integrator<Q>
			ofRate( final Quantity<?> rate )
		{
			return new Integrator<Q>()
			{
				@SuppressWarnings( "unchecked" )
				@Override
				public Q delta( final Instant start, final Instant end )
				{
					return (Q) rate.multiply( end.subtract( start ).unwrap() );
				}

				@Override
				public Instant when( final Instant now, final Q delta )
				{
					final Quantity<?> duration = delta.divide( rate );
					return DecimalUtil.signum( duration.getValue() ) < 0 ? null
							: now.add( duration );
				}
			};
		}
	}
}