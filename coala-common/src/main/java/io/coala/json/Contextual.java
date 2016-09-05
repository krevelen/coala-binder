/* $Id: 7b03f4605b9de1906d7db36397987e919178a9c7 $
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
import java.util.Map;

import org.aeonbits.owner.Converter;

/**
 * {@link Contextual} provides a {@link Context} {@link DynaBean} for adding
 * interesting meta data attributes (possibly from config/defaults), e.g. error
 * codes, description of the originator Object (beyond the class, file and line
 * number information, e.g. UUIDs), time-stamps, number of retries, time-outs,
 * etc.
 * 
 * @version $Id: 7b03f4605b9de1906d7db36397987e919178a9c7 $
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
	 * @version $Id: 7b03f4605b9de1906d7db36397987e919178a9c7 $
	 * @author Rick van Krevelen
	 */
	public class Context extends DynaBean
	{

		@Override
		public Map<String, Object> any()
		{
			return super.any();
		}

		@Override
		public Object set( final String name, final Object value )
		{
			return super.set( name, value );
		}

		/**
		 * @return this {@link Context} prohibiting {@link #set(String, Object)}
		 * @see DynaBean#lock()
		 */
		public Context locked()
		{
			super.lock();
			return this;
		}

		/**
		 * {@link ConfigConverter}
		 * 
		 * @version $Id: 7b03f4605b9de1906d7db36397987e919178a9c7 $
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