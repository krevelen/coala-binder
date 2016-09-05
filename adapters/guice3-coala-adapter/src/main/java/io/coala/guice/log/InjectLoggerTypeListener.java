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
package io.coala.guice.log;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import io.coala.log.LogUtil;

/**
 * {@link InjectLoggerTypeListener}
 */
@Deprecated
public class InjectLoggerTypeListener implements TypeListener
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( InjectLoggerTypeListener.class );

	@Override
	public <T> void hear( final TypeLiteral<T> typeLiteral,
		final TypeEncounter<T> typeEncounter )
	{
		for( Field field : typeLiteral.getRawType().getDeclaredFields() )
		{
			if( !field.isAnnotationPresent( Inject.class ) ) continue;

			if( field.getType() == org.apache.logging.log4j.Logger.class )
				typeEncounter.register( new Log4JMembersInjector<T>( field ) );
			else if( field.getType() == org.slf4j.Logger.class )
				typeEncounter.register( new SLF4JMembersInjector<T>( field ) );
			else if( field.getType() == java.util.logging.Logger.class )
				typeEncounter.register( new JULMembersInjector<T>( field ) );
			else
				LOG.warn( "@" + Inject.class.getSimpleName()
						+ " annotated unknown logger type " + field.getType() );

			// TODO add/inject other logger type implementations
		}
	}
}