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
import java.lang.reflect.Field;
import java.util.Map;

import javax.inject.Qualifier;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;

import io.coala.bind.LocalBinder;
import io.coala.name.Identified;

/**
 * {@link InjectConfig} marks an {@link Inject}able type's member field(s) which
 * extend(s) {@link org.aeonbits.config.Config}, and controls the caching
 * behavior of injection using e.g. the {@link ConfigFactory} or
 * {@link ConfigCache}, depending on the {@link Scope} value specified by
 * {@link #value()}.
 * <p>
 * See also the <a href=http://owner.aeonbits.org/>OWNER API</a>.
 * <p>
 * Inspired by
 * <a href="http://java-taste.blogspot.nl/2011/10/guiced-configuration.html" >
 * here</a>
 */
@Qualifier
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD } )
public @interface InjectConfig
{

	/**
	 * @return the cache {@link Scope} of the injected {@link Config} instance
	 */
	Scope value() default Scope.DEFAULT;

	String[] yamlURI() default {};

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
		 * use the {@link Config} sub-type as caching key: get the
		 * {@link Config} instance shared across current ClassLoader (also the
		 * default key in {@link ConfigCache#getOrCreate(Class, Map...)})
		 */
		DEFAULT,

		/**
		 * use the injectable field as caching key: get the {@link Config}
		 * instance shared for this {@link Field} across current ClassLoader
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

		/**
		 * inject a new {@link Config} instance, don't cache/share
		 */
		NONE,
	}
}
