/* $Id: ee51d7b3461fc7668d04f2bc014c4b52c598462d $
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

import static org.aeonbits.owner.util.Collections.entry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeoutException;

import javax.measure.quantity.DataAmount;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.logging.log4j.Logger;
import org.jscience.physics.amount.Amount;
import org.junit.Test;

import io.coala.dsol3.Dsol3Config;
import io.coala.log.LogUtil;
import io.coala.time.Accumulator.Integrator;
import rx.Observer;

/**
 * {@link AccumulatorTest} tests the {@link Accumulator}
 * 
 * FIXME performance leak: multiple events remain scheduled when target reached
 * 
 * @version $Id: ee51d7b3461fc7668d04f2bc014c4b52c598462d $
 * @author Rick van Krevelen
 */
public class AccumulatorTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( AccumulatorTest.class );

	private void logPoint( final Accumulator<?> acc, final TimeSpan interval )
	{
		LOG.trace( "{},{}", acc.now(),
				BigDecimal.valueOf( acc.getAmount().getEstimatedValue() )
						.setScale( 4, RoundingMode.HALF_UP ) );
		acc.after( interval ).call( this::logPoint, acc, interval );
	}

	/**
	 * Test method for {@link Accumulator#setIntegrator(Accumulator.Integrator)}
	 * , {@link Accumulator#at(Amount, Observer)},
	 * {@link Accumulator#emitAmounts()} and {@link Accumulator#getAmount()}.
	 * 
	 * @throws TimeoutException
	 */
	@Test
	public void tesAccumulator() throws TimeoutException
	{
		final Unit<?> bps = SI.BIT.divide( SI.SECOND );
		final Dsol3Config config = Dsol3Config.of(
				entry( Dsol3Config.ID_KEY, "accumTest" ),
				entry( Dsol3Config.START_TIME_KEY, "5 s" ),
				entry( Dsol3Config.RUN_LENGTH_KEY, "100" ) );
		LOG.info( "Starting DSOL test, config: {}", config.toYAML() );
		final Scheduler scheduler = config.create( s ->
		{
			final Accumulator<DataAmount> acc = Accumulator.of( s,
					Amount.valueOf( 120.4, SI.BIT ), Amount.valueOf( 2, bps ) );

			final TimeSpan delay = TimeSpan.valueOf( "1 s" );
			s.at( s.now() ).call( this::logPoint, acc, delay );

			// schedule event at target level
			final Amount<DataAmount> target = Amount.valueOf( 500, SI.BIT );
			acc.at( target, t ->
			{
				LOG.info( "reached a={} at t={}", target, t );
			} );

			// double the rate
			acc.setIntegrator( Integrator.ofRate( Amount.valueOf( 4, bps ) ) );
//			assertThat( "Can't be null", acc, not( nullValue() ) );
			LOG.info( "initialized, t={}", s.now() );
		} );

		scheduler.run();
		LOG.info( "Accumulator test complete, t={}", scheduler.now() );
	}

}
