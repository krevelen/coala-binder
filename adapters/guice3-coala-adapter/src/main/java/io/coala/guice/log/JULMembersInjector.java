/* $Id: cfed8dfcd21ca27cd12fc1fc9751bd7bba1d889c $
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

import org.apache.logging.log4j.Logger;

import com.google.inject.MembersInjector;

import io.coala.log.LogUtil;
import io.coala.model.ModelComponent;

/**
 * {@link JULMembersInjector}
 * 
 * @param <T>
 */
@Deprecated
class JULMembersInjector<T> implements MembersInjector<T>
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( JULMembersInjector.class );

	/** */
	private final Field field;

	/**
	 * {@link JULMembersInjector} constructor
	 * 
	 * @param field
	 */
	public JULMembersInjector( final Field field )
	{
		this.field = field;
		field.setAccessible( true );
	}

	@Override
	public void injectMembers( final T t )
	{
		final java.util.logging.Logger logger = t instanceof ModelComponent
				? LogUtil.getJavaLogger( ((ModelComponent<?>) t).getID() + " "
						+ this.field.getDeclaringClass().getName() )
				: LogUtil.getJavaLogger(
						this.field.getDeclaringClass().getName() );
		try
		{
			this.field.set( t, logger );
			LOG.trace(
					"Injected " + java.util.logging.Logger.class.getSimpleName()
							+ " into a " + t.getClass().getSimpleName() );
		} catch( final IllegalAccessException e )
		{
			throw new RuntimeException( e );
		}
	}
}