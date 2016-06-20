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
	/** */
	String name();

	/** */
	String defaultValue() default "";

	/** */
	byte defaultByteValue() default 0;

	/** */
	short defaultShortValue() default 0;

	/** */
	int defaultIntValue() default 0;

	/** */
	long defaultLongValue() default 0;

	/** */
	float defaultFloatValue() default 0;

	/** */
	double defaultDoubleValue() default 0.0;

	/** */
	boolean defaultBooleanValue() default false;

	/** */
	@SuppressWarnings( "rawtypes" )
	Class<? extends ConfigConverter> converter() default ConfigDefaultConverter.class;
}
