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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import javax.measure.Unit;

import org.aeonbits.owner.ConfigCache;

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

	String SCHEDULER_TYPE_KEY = "replication.scheduler-type";

	String TIME_UNIT_KEY = "replication.time-unit";

	String DURATION_KEY = "replication.duration";

	String OFFSET_KEY = "replication.offset";

	@Key( ID_KEY )
	@DefaultValue( "repl0" )
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
	@DefaultValue( "io.coala.dsol3.Dsol3Scheduler" )
	Class<? extends Scheduler> schedulerType();

	@Key( TIME_UNIT_KEY )
	@DefaultValue( TimeUnits.DAYS_LABEL )
	String rawTimeUnit();

	default Unit<?> timeUnit()
	{
		return TimeUnits.UNIT_FORMAT.parse( rawTimeUnit() );
	}

	@Key( OFFSET_KEY )
	@DefaultValue( "2020-01-01T00:00:00" )
	String rawOffset();

	/**
	 * @return a {@link ZonedDateTime}, possibly converted from
	 *         {@link LocalDateTime#parse(CharSequence)} at zone
	 *         {@link ZoneId#systemDefault()}
	 */
	default ZonedDateTime offset()
	{
		try
		{
			return ZonedDateTime.parse( rawOffset() );
		} catch( final Exception e )
		{
			return LocalDateTime.parse( rawOffset() )
					.atZone( ZoneId.systemDefault() );
		}
	}

	@Key( DURATION_KEY )
	@DefaultValue( "300" )
	BigDecimal rawDuration();

	default Duration duration()
	{
		return Duration.of( rawDuration(), timeUnit() );
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