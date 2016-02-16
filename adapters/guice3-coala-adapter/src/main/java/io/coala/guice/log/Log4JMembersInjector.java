/* $Id: 130f32a1439ffa2dbf3d0838536fd0fa520a23e1 $
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

/**
 * {@link Log4JMembersInjector}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
class Log4JMembersInjector<T> implements MembersInjector<T>
{
	/** */
//	private static final Logger LOG = LogUtil
//			.getLogger( Log4JMembersInjector.class );

	/** */
	private final Field field;

	/**
	 * {@link Log4JMembersInjector} constructor
	 * 
	 * @param field
	 */
	public Log4JMembersInjector( final Field field )
	{
		this.field = field;
		field.setAccessible( true );
	}

	@Override
	public void injectMembers( final T t )
	{
//		final String prefix = CoalaLog4jLogger.determineLoggerPrefixForObject(t);
		final Logger logger = LogUtil.getLogger( t.getClass(), t );
//		final String actualPrefix = ((CoalaLog4jLogger) logger).getPrefix();
//		if (!actualPrefix.equals(prefix))
//			LOG.warn("Injecting logger for " + t.getClass()
//					+ " with wrong name prefix: " + actualPrefix
//					+ " should be: " + prefix);
		try
		{
			this.field.set( t, logger );
//			LOG.trace("Injected " + Logger.class.getSimpleName() + " into a "
//					+ t.getClass().getSimpleName());
		} catch( final IllegalAccessException e )
		{
			throw new RuntimeException( e );
		}
	}
}