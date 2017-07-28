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
package io.coala.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import javax.inject.Qualifier;

import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;

import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.name.Identified;

/**
 * {@link InjectLogger} follows this <a
 * href=https://github.com/google/guice/wiki/CustomInjections>guice example</a>
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Qualifier
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface InjectLogger
{

	class Util
	{

		public static void injectLogger( final Object encloser,
			final Field field )
		{
			final String postfix = encloser instanceof Identified
					? "." + ((Identified<?>) encloser).id() : "";
			Object logger = null;
			try
			{
				// Log4j2
				if( field.getType() == Logger.class )
					logger = LogUtil.getLogger( encloser.getClass(), encloser );
				else // SLF4J
				if( field.getType() == org.slf4j.Logger.class )
				{
					logger = LoggerFactory.getLogger(
							encloser.getClass().getName() + postfix );
				} else // java.util.logging
				if( field.getType() == java.util.logging.Logger.class )
				{
					logger = LogUtil.getJavaLogger(
							encloser.getClass().getName() + postfix );
				} else
					Thrower.throwNew( UnsupportedOperationException::new,
							() -> "@" + InjectLogger.class.getSimpleName()
									+ " only injects " + Logger.class.getName()
									+ ", " + org.slf4j.Logger.class.getName()
									+ " or " + java.util.logging.Logger.class
											.getName() );

				field.setAccessible( true );
				field.set( encloser, logger );
			} catch( final Exception e )
			{
				Thrower.rethrowUnchecked( e );
			}
		}
	}
}
