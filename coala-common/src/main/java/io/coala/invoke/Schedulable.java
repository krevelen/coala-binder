/* $Id: b5d07783b03c0c681e94f8c1ba83d98fdd8ed441 $
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
package io.coala.invoke;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.util.ClassUtil;

import io.coala.exception.ExceptionFactory;

/**
 * {@link Schedulable}
 * 
 * @version $Id: b5d07783b03c0c681e94f8c1ba83d98fdd8ed441 $
 * @author Rick van Krevelen
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
public @interface Schedulable
{

	/**
	 * @return a class-wide unique reference to identify the {@link Schedulable}
	 *         method within its declaring {@link Class}
	 */
	String value();

	/**
	 * {@link Util}
	 */
	@SuppressWarnings( "rawtypes" )
	class Util
	{
		/** */
		// private static final Logger LOG = LogUtil
		// .getLogger(Schedulable.Util.class);

		/**
		 * @param reference the method's annotated {@link Schedulable} value
		 * @param target the object implementing the {@link Schedulable} method
		 * @param arguments the arguments to call the method with
		 * @return the {@link Callable} object
		 */
		public static Callable<Object> toCallable( final String reference,
			final Object target, final Object... arguments )
		{
			return new Callable<Object>()
			{
				@Override
				public Object call() throws Exception
				{
					return Util.call( reference, target, arguments );
				}
			};
		}

		/**
		 * @param reference the method's annotated {@link Schedulable} value
		 * @param target the object implementing the {@link Schedulable} method
		 * @param arguments the arguments to call the method with
		 */
		public static Callable<Object> toCallable( final String reference,
			final Object target, final List arguments )
		{
			return new Callable<Object>()
			{
				@Override
				public Object call() throws Exception
				{
					return Util.call( reference, target, arguments );
				}
			};
		}

		/**
		 * @param reference the method's annotated {@link Schedulable} value
		 * @param target the object implementing the {@link Schedulable} method
		 * @param arguments the arguments to call the method with (or none or
		 *            {@code null})
		 * @throws Exception
		 */
		public static Object call( final String reference, final Object target,
			final Object... arguments ) throws Exception
		{
			// convert args to list
			final List args;
			if( arguments == null || arguments.length == 0 )
				args = null;// Collections.emptyList();
			else
				args = Arrays.asList( arguments );

			return call( reference, target, args );
		}

		/**
		 * @param reference the method's annotated {@link Schedulable} value
		 * @param target the object implementing the {@link Schedulable} method
		 * @param arguments the arguments to call the method with (or
		 *            {@code null})
		 * @throws Exception
		 */
		@SuppressWarnings( "unchecked" )
		public static Object call( final String reference, final Object target,
			final List arguments ) throws Exception
		{
			Method method = findSchedulableMethod( target.getClass(),
					reference );

			// search super types/interfaces
			if( method == null )
				for( Class<?> superType : ClassUtil.findRawSuperTypes(
						target.getClass(), Object.class, false ) )
				if( (method = findSchedulableMethod( superType, reference )) != null ) break;

			if( method == null ) throw ExceptionFactory.createUnchecked(
					"No annotation {} in {} with {}", Schedulable.class, target,
					reference );

			method.setAccessible( true );
			return method.invoke( target, arguments == null ? null
					: arguments.toArray( new Object[arguments.size()] ) );
		}

		private static Method findSchedulableMethod( final Class<?> superType,
			final String reference )
		{
			for( Method method : superType.getDeclaredMethods() )
			{
				// TODO ignore static methods?

				final Schedulable annot = method
						.getAnnotation( Schedulable.class );

				// TODO add duck-typing?

				if( annot != null && annot.value().equals( reference ) )
					return method;
			}
			return null;
		}
	}
}
