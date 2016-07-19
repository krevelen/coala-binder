/* $Id$
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.aeonbits.owner.ConfigCache;

import io.coala.bind.LocalBinder;
import io.coala.name.Identified;

/**
 * {@link InjectConfig} inspired by
 * <a href="http://java-taste.blogspot.nl/2011/10/guiced-configuration.html" >
 * here</a>
 * 
 * See also OWNER API at http://owner.aeonbits.org/
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface InjectConfig
{
	/**
	 * @return the {@link Scope} for sharing injected {@link Config} instances
	 */
	Scope scope() default Scope.CONFIG;

	/**
	 * {@link Scope} determines which key to use for
	 * {@link ConfigCache#getOrCreate(Object, Class, java.util.Map...)}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	enum Scope
	{
		/**
		 * use the {@link Config} sub-type as caching key (i.e. {@link Config}
		 * instance shared across current ClassLoader)
		 */
		CONFIG,

		/**
		 * use the injectable field as caching key (i.e. share {@link Config}
		 * instance for this {@link Field} across current ClassLoader)
		 */
		FIELD,

		/**
		 * use the {@link LocalBinder} instance as caching key (i.e. share
		 * {@link Config} instance unique for this {@link LocalBinder})
		 */
		BINDER,

		/**
		 * use the {@link Identified#id()} as caching key (i.e. share
		 * {@link Config} instance for the {@link Identified#id()} value)
		 */
		ID,
	}
}
