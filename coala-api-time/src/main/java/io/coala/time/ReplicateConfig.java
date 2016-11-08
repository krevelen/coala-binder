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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import javax.measure.Unit;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Converter;

import com.fasterxml.jackson.databind.node.TextNode;

import io.coala.config.ConfigUtil;
import io.coala.config.GlobalConfig;
import io.coala.json.JsonUtil;

/**
 * {@link ReplicateConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface ReplicateConfig extends GlobalConfig
{
	String ID_KEY = "replication.id";

	String ID_DEFAULT = "repl0";

	String SCHEDULER_TYPE_KEY = "replication.scheduler-type";

	String SCHEDULER_TYPE_DEFAULT = "io.coala.dsol3.Dsol3Scheduler";

	String TIME_UNIT_KEY = "replication.time-unit";

	String TIME_UNIT_DEFAULT = TimeUnits.DAYS_LABEL;

	String DURATION_KEY = "replication.duration";

	String DURATION_DEFAULT = "300";

	String OFFSET_KEY = "replication.offset";

	String OFFSET_DEFAULT = "2020-01-01T00:00:00Z";

	@Key( ID_KEY )
	@DefaultValue( ID_DEFAULT )
	String rawId();

	default <T> T id( final Class<T> idType )
	{
		// assumes the id() should be parsed as a JSON text value: "..."
		return JsonUtil.valueOf( new TextNode( rawId() ), idType );
	}

	/**
	 * @return
	 */
	@Key( SCHEDULER_TYPE_KEY )
	@DefaultValue( SCHEDULER_TYPE_DEFAULT )
	Class<? extends Scheduler> schedulerType();

	@Key( TIME_UNIT_KEY )
	@DefaultValue( TIME_UNIT_DEFAULT )
	String rawTimeUnit();

	@DefaultValue( "${" + TIME_UNIT_KEY + "}" )
	@ConverterClass( TimeUnitsConverter.class )
	Unit<?> timeUnit();

	@Key( OFFSET_KEY )
	@DefaultValue( OFFSET_DEFAULT )
	String rawOffset();

	@DefaultValue( "${" + OFFSET_KEY + "}" )
	@ConverterClass( InstantConverter.class )
	Instant offset();

	@Key( DURATION_KEY )
	@DefaultValue( DURATION_DEFAULT )
	BigDecimal rawDuration();

	@DefaultValue( "${" + DURATION_KEY + "} ${" + TIME_UNIT_KEY + "}" )
	Duration duration();

	class InstantConverter implements Converter<Instant>
	{
		@Override
		public Instant convert( final Method method, final String input )
		{
			return Instant.parse( input );
		}
	}

	static ReplicateConfig getOrCreate( final Map<?, ?>... imports )
	{
		return ConfigCache.getOrCreate( ReplicateConfig.class, imports );
	}

	static ReplicateConfig getOrCreate( final String rawId,
		final Map<?, ?>... imports )
	{
		return ConfigCache
				.getOrCreate( rawId, ReplicateConfig.class,
						ConfigUtil.join(
								Collections.singletonMap(
										ReplicateConfig.ID_KEY, rawId ),
								imports ) );
	}
}