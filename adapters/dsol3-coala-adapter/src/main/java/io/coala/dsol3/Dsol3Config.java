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
package io.coala.dsol3;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;

import javax.measure.Quantity;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Converter;

import io.coala.config.GlobalConfig;
import io.coala.config.YamlConfig;
import io.coala.function.ThrowingConsumer;
import io.coala.math.QuantityUtil;
import io.coala.time.Duration;
import io.coala.time.ReplicateConfig;
import io.coala.time.Scheduler;
import io.coala.util.MapBuilder;
import nl.tudelft.simulation.dsol.experiment.ReplicationMode;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;

/**
 * {@link Dsol3Config}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Dsol3Config extends GlobalConfig, YamlConfig
{
	String ID_KEY = "dsol3.replication.id";

	String START_TIME_KEY = "dsol3.replication.start-time";

	String WARMUP_LENGTH_KEY = "dsol3.replication.warm-up-length";

	String RUN_LENGTH_KEY = "dsol3.replication.run-length";

	String PAUSE_ON_ERROR_KEY = "dsol3.replication.simulator.pause-on-error";

	String REPLICATION_MODE_KEY = "dsol3.replication.simulator.mode";

	String SIMULATOR_TYPE_KEY = "dsol3.replication.simulator.class";

	@Key( ID_KEY )
	@DefaultValue( "repl0" )
	String id();

	@Key( START_TIME_KEY )
	@DefaultValue( "0 day" )
	@ConverterClass( DsolTimeConverter.class )
	DsolTime<?> startTime();

	@Key( WARMUP_LENGTH_KEY )
	@DefaultValue( "0" )
	BigDecimal warmUpLength();

	@Key( RUN_LENGTH_KEY )
	@DefaultValue( "100" )
	BigDecimal runLength();

	@Key( SIMULATOR_TYPE_KEY )
	@DefaultValue( "nl.tudelft.simulation.dsol.simulators.DEVSSimulator" )
	@SuppressWarnings( "rawtypes" )
	Class<? extends DEVSSimulator> simulatorType();

	@Key( REPLICATION_MODE_KEY )
	@DefaultValue( "TERMINATING" )
	ReplicationMode replicationMode();

	@Key( PAUSE_ON_ERROR_KEY )
	@DefaultValue( "true" )
	boolean pauseOnError();

	class DsolTimeConverter implements Converter<DsolTime<?>>
	{
		@Override
		public DsolTime<?> convert( final Method method, final String input )
		{
			return DsolTime.valueOf( input );
		}
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	static Dsol3Config get()
	{
		return of( (Map) null );
	}

	@SafeVarargs
	static Dsol3Config of( final Map<String, Object>... imports )
	{
		return ConfigCache.getOrCreate( Dsol3Config.class, imports );
	}

	/**
	 * @param replConfig the {@link ReplicateConfig}
	 * @return a cached {@link Dsol3Config}
	 */
	static Dsol3Config of( final ReplicateConfig replConfig )
	{
		final Duration duration = replConfig.duration();

		return of(
				MapBuilder.<String, Object>unordered()
						.put( ID_KEY, replConfig.rawId() )
						.put( START_TIME_KEY,
								DsolTime.valueOf( 0, duration.unit() )
										.toString() )
						.put( RUN_LENGTH_KEY,
								QuantityUtil.toBigDecimal( duration.unwrap() )
										.toString() )
						.build(),
				replConfig.export() );
	}

	default <Q extends Quantity<Q>> Dsol3Scheduler<Q> create()
	{
		return Dsol3Scheduler.of( this );
	}

	default <Q extends Quantity<Q>> Dsol3Scheduler<Q>
		create( final ThrowingConsumer<Scheduler, ?> modelInitializer )
	{
		return Dsol3Scheduler.of( this, modelInitializer );
	}
}