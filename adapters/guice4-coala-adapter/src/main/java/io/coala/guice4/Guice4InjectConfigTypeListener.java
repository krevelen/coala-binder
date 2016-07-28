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

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigCache;

import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import io.coala.config.InjectConfig;
import io.coala.exception.ExceptionFactory;
import io.coala.name.Identified;

/**
 * {@link Guice4InjectConfigTypeListener}
 */
public class Guice4InjectConfigTypeListener implements TypeListener
{
	@Override
	public <T> void hear( final TypeLiteral<T> typeLiteral,
		final TypeEncounter<T> typeEncounter )
	{
		for( Field field : typeLiteral.getRawType().getDeclaredFields() )
		{
			final InjectConfig annot = field
					.getAnnotation( InjectConfig.class );
			if( annot == null ) continue;
			if( !Config.class.isAssignableFrom( field.getType() ) )
				throw ExceptionFactory.createUnchecked(
						"@{} not annotating subtype of {} at field: {}",
						InjectConfig.class.getSimpleName(),
						Config.class.getName(), field );
			typeEncounter.register( new MembersInjector<T>()
			{
				@Override
				public void injectMembers( final T t )
				{
					try
					{
						final Class<? extends Config> type = field.getType()
								.asSubclass( Config.class );
						final Object key;
						switch( annot.scope() )
						{
						case BINDER:
							key = Guice4InjectConfigTypeListener.this;
							break;
						case FIELD:
							key = field;
							break;
						case ID:
							key = t instanceof Identified<?>
									? ((Identified<?>) t).id() : t;
							break;
						default:
						case CLASSLOADER:
							key = type;
							break;
						}
						field.setAccessible( true );
						field.set( t, ConfigCache.getOrCreate( key, type ) );
					} catch( final RuntimeException e )
					{
						throw e;
					} catch( final Exception e )
					{
						throw ExceptionFactory.createUnchecked( e,
								"Problem injecting Config" );
					}
				}
			} );
		}
	}
}