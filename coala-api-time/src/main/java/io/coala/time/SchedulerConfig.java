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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.measure.Unit;

import org.aeonbits.owner.ConfigCache;

import com.fasterxml.jackson.databind.node.TextNode;

import io.coala.bind.ProviderConfig;
import io.coala.config.ConfigUtil;
import io.coala.json.JsonUtil;

/**
 * {@link SchedulerConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface SchedulerConfig extends ProviderConfig
{
	String ID_KEY = "replication.id";

	String TIME_UNIT_KEY = "replication.time-unit";

	String DURATION_KEY = "replication.duration";

	String OFFSET_KEY = "replication.offset";

	@Key( ID_KEY )
//	@DefaultValue( "repl0" )
	String rawId();

	default <T> T id( final Class<T> idType )
	{
		// assumes the id() should be parsed as a JSON text value: "..."
		return JsonUtil.valueOf( new TextNode( rawId() ), idType );
	}

	/**
	 * @return
	 */
	@Override
	@DefaultValue( "io.coala.dsol3.Dsol3Scheduler" )
	@Key( IMPLEMENTATION_KEY )
	Class<? extends Scheduler> implementation();

	@Key( TIME_UNIT_KEY )
	@DefaultValue( TimeUnits.DAYS_LABEL )
	String rawTimeUnit();

	default Unit<?> timeUnit()
	{
		final String value = Objects.requireNonNull( rawTimeUnit(),
				TIME_UNIT_KEY + " not set" );
		return value == null ? TimeUnits.STEPS
				: TimeUnits.UNIT_FORMAT.parse( value );
	}

	@Key( OFFSET_KEY )
	@DefaultValue( "2020-01-01T00:00:00" )
	String rawOffset();

	/**
	 * @return a {@link ZonedDateTime}, possibly derived via {@link Year} (at
	 *         {@linkplain Year#atDay day 1}) or {@link YearMonth} (at
	 *         {@linkplain YearMonth#atDay day 1}) &rarr; {@link LocalDate} (at
	 *         {@linkplain LocalDate#atStartOfDay start of day}) &rarr;
	 *         {@link LocalDateTime} (at {@linkplain ZoneId#systemDefault system
	 *         default zone})
	 */
	default ZonedDateTime offset()
	{
		final String value = Objects.requireNonNull( rawOffset(),
				OFFSET_KEY + " not set" );
		try
		{
			return ZonedDateTime.parse( value );
		} catch( final Exception e1 )
		{
			try
			{
				return LocalDateTime.parse( value )
						.atZone( ZoneId.systemDefault() );
			} catch( final Exception e2 )
			{
				try
				{
					return LocalDate.parse( value )
							.atStartOfDay( ZoneId.systemDefault() );
				} catch( final Exception e3 )
				{
					try
					{
						return YearMonth.parse( value ).atDay( 1 )
								.atStartOfDay( ZoneId.systemDefault() );
					} catch( final Exception e4 )
					{
						return Year.parse( value ).atDay( 1 )
								.atStartOfDay( ZoneId.systemDefault() );
					}
				}
			}
		}
	}

	@Key( DURATION_KEY )
	@DefaultValue( "300" )
	BigDecimal rawDuration();

	default Duration duration()
	{
		return Duration.of( Objects.requireNonNull( rawDuration(),
				DURATION_KEY + " not set" ), timeUnit() );
	}

	static SchedulerConfig getOrCreate( final Map<?, ?>... imports )
	{
		return ConfigCache.getOrCreate( SchedulerConfig.class, imports );
	}

	static SchedulerConfig getOrCreate( final String rawId,
		final Map<?, ?>... imports )
	{
		return ConfigCache
				.getOrCreate( rawId, SchedulerConfig.class,
						ConfigUtil.join(
								Collections.singletonMap(
										SchedulerConfig.ID_KEY, rawId ),
								imports ) );
	}
}