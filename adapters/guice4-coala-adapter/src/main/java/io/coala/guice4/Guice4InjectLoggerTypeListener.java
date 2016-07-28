/* $Id: f190f0b00e0bcc4f38c5508f628f54fa11af51c6 $
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
package io.coala.guice4;

import java.lang.reflect.Field;

import org.slf4j.LoggerFactory;

import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import io.coala.exception.ExceptionFactory;
import io.coala.log.InjectLogger;
import io.coala.log.LogUtil;
import io.coala.name.Identified;

/**
 * {@link Guice4InjectLoggerTypeListener}
 */
public class Guice4InjectLoggerTypeListener implements TypeListener
{
	@Override
	public <T> void hear( final TypeLiteral<T> typeLiteral,
		final TypeEncounter<T> typeEncounter )
	{
		for( Field field : typeLiteral.getRawType().getDeclaredFields() )
		{
			if( !field.isAnnotationPresent( InjectLogger.class ) ) continue;

			typeEncounter.register( new MembersInjector<T>()
			{
				@Override
				public void injectMembers( final T t )
				{
					final String postfix = t instanceof Identified
							? "." + ((Identified<?>) t).id() : "";
					Object logger = null;
					try
					{
						// Log4j2
						if( field
								.getType() == org.apache.logging.log4j.Logger.class )
							logger = LogUtil.getLogger( t.getClass(), t );
						else // SLF4J
						if( field.getType() == org.slf4j.Logger.class )
						{
							logger = LoggerFactory.getLogger(
									t.getClass().getName() + postfix );
						} else // java.util.logging
						if( field.getType() == java.util.logging.Logger.class )
						{
							logger = LogUtil.getJavaLogger(
									t.getClass().getName() + postfix );
						} else
							throw ExceptionFactory.createUnchecked(
									"@{} unknown logger type for field: {}",
									InjectLogger.class.getSimpleName(), field );

						field.setAccessible( true );
						field.set( t, logger );
					} catch( final RuntimeException e )
					{
						throw e;
					} catch( final Exception e )
					{
						throw ExceptionFactory.createUnchecked( e,
								"Problem injecting Logger" );
					}
				}
			} );
		}
	}
}