/* $Id: 8d658a57db9308ad045378be1e35163d0cc4bccd $
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
 */
package io.coala.config;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.coala.agent.Agent;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.model.ModelID;
import io.coala.time.TimeUnit;

/**
 * {@link CoalaProperty}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public enum CoalaProperty
{

	/** This property specifies the model name as {@link ModelID} */
	modelName("_model_" + DateTime.now().getMillisOfDay()),

	/** */
	identifierFactoryType,

	/** */
	binderFactoryType,

	/** */
	clockName("_clock_"),

	/** */
	clockOffset(DateTime.now().withTimeAtStartOfDay().toDate()),

	/** see ISO Period format specification in {@link ISOPeriodFormat#standard()} */
	clockDuration(Period.days(25).toString()),

	/** */
	baseTimeUnit(TimeUnit.HOURS.name()),

	/** */
	bootAgentNames(new String[] { "_launcher_" }),

	/** */
	defaultAgentType(Agent.class),

	/** */
	customAgentNames(new String[] {}),

	/** */
	agentType(Agent.class),

	/** */
	singletonServiceTypes(new HashMap<String, String>()),

	/** */
	instantServiceTypes(new HashMap<String, String>()),

	/** */
	customFactoryTypes(new HashMap<String, String>()),

	/** */
	randomSeed(System.currentTimeMillis()),

	/** */
	addOriginatorStackTrace(false),

	;

	/** */
	private static final Logger LOG = LogUtil.getLogger(CoalaProperty.class);

	/** */
	private final Object defaultValue;

	/**
	 * {@link CoalaProperty} constructor
	 */
	private CoalaProperty()
	{
		this(null);
	}

	/**
	 * {@link CoalaProperty} constructor
	 * 
	 * @param defaultValue
	 */
	private CoalaProperty(final String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	/**
	 * {@link CoalaProperty} constructor
	 * 
	 * @param defaultValue
	 */
	private CoalaProperty(final Object defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	/** @return the default value */
	public Object defaultValue()
	{
		return this.defaultValue;
	}

	/** @return the default value */
	public boolean isDefault(final Object value)
	{
		return defaultValue().equals(value);
	}

	/**
	 * @param prefixes
	 * @return the {@link CoalaPropertyGetter} utility object
	 */
	public CoalaPropertyGetter value(final String... prefixes)
	{
		String defaultValue = null;
		if (this.defaultValue instanceof String)
			defaultValue = (String) this.defaultValue;
		else if (this.defaultValue != null)
			try
			{
				defaultValue = JsonUtil.getJOM().writeValueAsString(
						this.defaultValue);
				// LOG.trace("Marshalled default value for '" + name() +
				// "' to: " + defaultValue);
			} catch (final JsonProcessingException e)
			{
				LOG.warn("Unable to marshal default value for " + name(), e);
			}

		final CoalaPropertyGetter result = new CoalaPropertyGetter(
				CoalaPropertyGetter.addKeyPrefixes(name(), prefixes),
				defaultValue);
		// LOG.trace("Created '" + key
		// + "' property's getter with default value type: "
		// + (this.defaultValue == null ? "<?>" : this.defaultValue
		// .getClass().getName()));
		return result;
	}
}
