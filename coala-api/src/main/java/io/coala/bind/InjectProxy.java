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
package io.coala.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

import javax.inject.Qualifier;

import io.coala.exception.Thrower;
import io.coala.inter.Invoker;

/**
 * {@link InjectProxy} this field should be a proxy with specified
 * {@link #value() address}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Qualifier
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface InjectProxy
{

	String value();

	/**
	 * @return the timeout duration
	 * @see Duration#parse(CharSequence)
	 */
	String timeout() default Invoker.SYNC_TIMEOUT_DEFAULT;

	class Util
	{

		/**
		 * @param t
		 * @param field
		 * @param binder
		 */
		public static void injectProxy( final Object encloser,
			final Field field, final Supplier<Invoker> invoker )
		{
			try
			{
				field.setAccessible( true );
				final InjectProxy annot = field
						.getAnnotation( InjectProxy.class );
				field.set( encloser,
						Invoker.createProxy( field.getType(),
								URI.create( annot.value() ),
								Duration.parse( annot.timeout() ), invoker ) );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
			}
		}
	}
}
