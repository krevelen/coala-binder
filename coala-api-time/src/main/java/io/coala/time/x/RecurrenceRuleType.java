/* $Id: 31d0ce9187af30ae8ffb082f4def62f55ad5f386 $
 * $URL$
 * 
 * Part of the EU project Inertia, see http://www.inertia-project.eu/
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
 * Copyright (c) 2014 Almende B.V. 
 */
package io.coala.time.x;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;

/**
 * {@link RecurrenceRuleType} of a {@linkplain Timing} with JSON
 * {@linkplain #value()} tokens. Note that {@link JsonParser} accepts
 * {@linkplain #ordinal()} as well.
 * 
 * @date $Date$
 * @version $Id: 31d0ce9187af30ae8ffb082f4def62f55ad5f386 $
 * @author <a href="mailto:rick@almende.org">Rick</a>
 */
public enum RecurrenceRuleType
{
	/** an absolute virtual time of occurrence */
	ONCE("timeout"),

	/**
	 * an recurring interval (starting from an absolute virtual time offset)
	 */
	PERIODIC("interval"),

	/** a CRON rule. TODO confirm CRON (nanosecond) precision */
	CRON_RULE("cron"),

	/** an iCal RRULE. TODO confirm iCal (nanosecond) precision */
	ICAL_RULE("ical"),

	;

	/** */
	private final String jsonValue;

	/**
	 * {@link RecurrenceRuleType} enum constant constructor
	 * 
	 * @param jsonValue
	 */
	private RecurrenceRuleType(final String jsonValue)
	{
		this.jsonValue = jsonValue;
	}

	@JsonValue
	final String value()
	{
		return this.jsonValue;
	}
}