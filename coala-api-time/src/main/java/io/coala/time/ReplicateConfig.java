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
import java.util.Map;
import java.util.regex.Pattern;

import javax.measure.DecimalMeasure;
import javax.measure.unit.Unit;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Converter;
import org.aeonbits.owner.util.Collections;

import com.fasterxml.jackson.databind.node.TextNode;

import io.coala.config.GlobalConfig;
import io.coala.config.JsonConverter;
import io.coala.config.YamlConfig;
import io.coala.json.JsonUtil;

/**
 * {@link ReplicateConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface ReplicateConfig extends GlobalConfig, YamlConfig
{
	String ID_KEY = "replication.id";

	String ID_DEFAULT = "repl0";

//	String SEED_KEY = "replication.seed";
//
//	String SEED_DEFAULT = "1234";

	String TIME_UNIT_KEY = "replication.time-unit";

	String TIME_UNIT_DEFAULT = Units.DAYS_ALIAS;

	String OFFSET_KEY = "replication.offset";

	String OFFSET_DEFAULT = "2020-01-01T00:00:00Z";

	String DURATION_KEY = "replication.duration";

	String DURATION_DEFAULT = "300"/* + Units.DAYS_ALIAS */;

	@Key( ID_KEY )
	@DefaultValue( ID_DEFAULT )
	String rawId();

	default <T> T id( final Class<T> idType )
	{
		// assumes the id() should be parsed as a JSON text value: "..."
		return JsonUtil.valueOf( new TextNode( rawId() ), idType );
	}

//	@Key( SEED_KEY )
//	@DefaultValue( SEED_DEFAULT )
//	String rawSeed();
//
//	@DefaultValue( "${" + SEED_KEY + "}" )
//	Long seed();

	@Key( TIME_UNIT_KEY )
	@DefaultValue( TIME_UNIT_DEFAULT )
	String rawTimeUnit();

	@DefaultValue( "${" + TIME_UNIT_KEY + "}" )
	@ConverterClass( UnitConverter.class )
	Unit<?> timeUnit();

	@Key( OFFSET_KEY )
	@DefaultValue( OFFSET_DEFAULT )
	String rawOffset();

	@DefaultValue( "${" + OFFSET_KEY + "}" )
	@ConverterClass( InstantConverter.class )
	Instant offset();

	@Key( DURATION_KEY )
	@DefaultValue( DURATION_DEFAULT )
	String rawDuration();

	String VALUE_SEP = "|";

	@DefaultValue( "${" + DURATION_KEY + "}" + VALUE_SEP + "${" + TIME_UNIT_KEY
			+ "}" )
	@ConverterClass( DurationConverter.class )
	Duration duration();

	/**
	 * {@link UnitConverter} as OWNER ignores {@link Unit#valueOf(CharSequence)}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class UnitConverter implements Converter<Unit<?>>
	{
		static
		{
			Units.registerAliases();
		}

		@Override
		public Unit<?> convert( final Method method, final String input )
		{
			return Unit.valueOf( input );
		}
	}

	class InstantConverter implements Converter<Instant>
	{
		@Override
		public Instant convert( final Method method, final String input )
		{
			return Instant.parse( input );
		}
	}

	class DurationConverter extends JsonConverter<Duration>
	{
		@Override
		public Duration convert( final Method method, final String input )
		{
			final String[] split = input.split( Pattern.quote( VALUE_SEP ) );
			final Duration result = super.convert( method, split[0] );
			final Unit<?> unit = Unit
					.valueOf( split.length == 2 ? split[1] : "" );
			if( result.unit().isCompatible( Unit.ONE ) )
				return Duration.of( result.unwrap().multiply(
						DecimalMeasure.valueOf( BigDecimal.ONE, unit ) ) );
			return result.to( unit );
		}
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	static ReplicateConfig get()
	{
		return of( new Map[0] );
	}

	@SafeVarargs
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	static ReplicateConfig of( final Map.Entry... entries )
	{
		return of( Collections.map( entries ) );
	}

	@SafeVarargs
	@SuppressWarnings( { "rawtypes" } )
	static ReplicateConfig of( final Map... imports )
	{
		return ConfigCache.getOrCreate( ReplicateConfig.class, imports );
	}
}