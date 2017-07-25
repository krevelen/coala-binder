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

import java.util.concurrent.TimeoutException;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.dsol3.Dsol3Config;
import io.coala.log.LogUtil;
import io.coala.math.QuantityUtil;
import io.coala.time.Accumulator.Integrator;
import io.coala.util.MapBuilder;
import io.reactivex.Observer;
import tec.uom.se.unit.Units;

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

	private void logPoint( final Accumulator<?> acc, final Duration interval )
	{
		LOG.trace( "{}", acc.now().prettify( 4 ) );
		acc.after( interval ).call( t -> logPoint( acc, interval ) );
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
		final Unit<?> mps = Units.METRE.divide( Units.SECOND );
		final Dsol3Config config = Dsol3Config
				.of( MapBuilder.<String, Object>unordered()
						.put( Dsol3Config.ID_KEY, "accumTest" )
						.put( Dsol3Config.START_TIME_KEY, "5 s" )
						.put( Dsol3Config.RUN_LENGTH_KEY, "100" ).build() );
		LOG.info( "Starting DSOL test, config: {}", config.toYAML() );
		final Scheduler scheduler = config.create( s ->
		{
			final Accumulator<Length> acc = Accumulator.of( s,
					QuantityUtil.valueOf( 120.4, Units.METRE ),
					QuantityUtil.valueOf( 2, mps ) );

			final Duration delay = Duration.valueOf( "1 s" );
			s.at( s.now() ).call( t -> logPoint( acc, delay ) );

			// schedule event at target level
			final Quantity<Length> target = QuantityUtil.valueOf( 500,
					Units.METRE );
			acc.at( target,
					t -> LOG.info( "reached a={} at t={}", target, t ) );

			// double the rate
			acc.setIntegrator(
					Integrator.ofRate( QuantityUtil.valueOf( 4, mps ) ) );
//			assertThat( "Can't be null", acc, not( nullValue() ) );
			LOG.trace( "initialized, t={}", s.now() );
		} );

		scheduler.run();
		LOG.info( "Accumulator test complete, t={}", scheduler.now() );
	}

}
