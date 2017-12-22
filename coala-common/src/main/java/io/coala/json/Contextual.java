/* $Id: e85337757927d515113f3281de855db5335a890f $
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
package io.coala.json;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.Converter;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * {@link Contextual} provides a {@link Context} {@link DynaBean} for adding
 * interesting meta data attributes (possibly from config/defaults), e.g. error
 * codes, description of the originator Object (beyond the class, file and line
 * number information, e.g. UUIDs), time-stamps, number of retries, time-outs,
 * etc.
 * 
 * @version $Id: e85337757927d515113f3281de855db5335a890f $
 * @author Rick van Krevelen
 */
//@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
//	property = "@class" )
public interface Contextual
{

	/** @return the {@link Context} */
	Context context();

	// context (application id, error code, ...)

	// origin (originator id, stack, time, thread)

	// trace (handlers, retries, timeout start/duration, ...)

	/**
	 * {@link Context} exposes some protected methods inherited from
	 * {@link DynaBean}
	 * 
	 * @version $Id: e85337757927d515113f3281de855db5335a890f $
	 * @author Rick van Krevelen
	 */
	public class Context //extends DynaBean
	{

		private Map<String, Object> values = null;

		@JsonAnyGetter
		public Map<String, Object> any()
		{
			return this.values != null ? this.values
					: (this.values = new HashMap<>());
		}

		@JsonAnySetter
		public Object set( final String name, final Object value )
		{
			return any().put( name, value );
		}

		/**
		 * @return this {@link Context} prohibiting {@link #set(String, Object)}
		 * @see DynaBean#lock()
		 */
		public Context locked()
		{
			if( this.values != null )
				this.values = Collections.unmodifiableMap( this.values );
			return this;
		}

		/**
		 * {@link ConfigConverter}
		 * 
		 * @version $Id: e85337757927d515113f3281de855db5335a890f $
		 * @author Rick van Krevelen
		 */
		public static class ConfigConverter implements Converter<Context>
		{
			@Override
			public Context convert( final Method method, final String input )
			{
				return JsonUtil.valueOf( input, Context.class );
			}
		}

	}
}