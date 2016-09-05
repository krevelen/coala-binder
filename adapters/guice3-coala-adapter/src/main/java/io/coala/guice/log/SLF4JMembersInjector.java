/* $Id: 803b062ca3de7202e9a7f465b9f0007c2fafe6f1 $
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
import org.slf4j.LoggerFactory;

import com.google.inject.MembersInjector;

import io.coala.log.LogUtil;
import io.coala.model.ModelComponent;

@Deprecated
public class SLF4JMembersInjector<T> implements MembersInjector<T>
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( SLF4JMembersInjector.class );

	/** */
	private final Field field;

	/**
	 * {@link SLF4JMembersInjector} constructor
	 * 
	 * @param field
	 */
	public SLF4JMembersInjector( Field field )
	{
		this.field = field;
		field.setAccessible( true );
	}

	@Override
	public void injectMembers( T t )
	{
		final org.slf4j.Logger logger = t instanceof ModelComponent
				? LoggerFactory.getLogger( ((ModelComponent<?>) t).getID() + " "
						+ this.field.getDeclaringClass().getName() )
				: LoggerFactory.getLogger( this.field.getDeclaringClass() );
		try
		{
			this.field.set( t, logger );
			LOG.trace( "Injected " + org.slf4j.Logger.class.getSimpleName()
					+ " into a " + t.getClass().getSimpleName() );
		} catch( final IllegalAccessException e )
		{
			throw new RuntimeException( e );
		}
	}
}