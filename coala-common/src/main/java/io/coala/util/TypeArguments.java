/* $Id: 99b983878b62652cfd2e2839ea9f800b6532499b $
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
package io.coala.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import io.coala.exception.x.ExceptionBuilder;
import io.coala.log.LogUtil;

/**
 * {@link TypeArguments}
 * 
 * @date $Date$
 * @version $Id: 99b983878b62652cfd2e2839ea9f800b6532499b $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class TypeArguments
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( TypeArguments.class );

	/**
	 * {@link TypeArguments} singleton constructor
	 */
	private TypeArguments()
	{
		// empty
	}

	/**
	 * Get the underlying class for a type, or null if the type is a variable
	 * type. See
	 * <a href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860" >
	 * description</a>
	 * 
	 * @param type the type
	 * @return the underlying class
	 */
	private static Class<?> toClass( final Type type )
	{
		if( type instanceof Class )
		{
			// LOG.trace("Type is a class/interface: "+type);
			return (Class<?>) type;
		}

		if( type instanceof ParameterizedType )
		{
			// LOG.trace("Type is a ParameterizedType: "+type);
			return toClass( ((ParameterizedType) type).getRawType() );
		}

		if( type instanceof GenericArrayType )
		{
			// LOG.trace("Type is a GenericArrayType: "+type);
			final Type componentType = ((GenericArrayType) type)
					.getGenericComponentType();
			final Class<?> componentClass = toClass( componentType );
			if( componentClass != null )
				return Array.newInstance( componentClass, 0 ).getClass();
		}
		LOG.trace( "Type is a variable type: " + type.toString() );
		return null;
	}

	/**
	 * Get the actual type arguments a child class has used to extend a generic
	 * base class. See
	 * <a href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860" >
	 * description</a>
	 * 
	 * @param genericAncestorType the base class
	 * @param concreteDescendantType the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	@SuppressWarnings( "unchecked" )
	public static <T, S extends T> List<Class<?>> of(
		final Class<T> genericAncestorType,
		final Class<S> concreteDescendantType )
	{
		// sanity check
		if( genericAncestorType == null ) throw ExceptionBuilder
				.unchecked( "NOT SET: genericAncestorType" ).build();
		if( concreteDescendantType == null ) throw ExceptionBuilder
				.unchecked( "NOT SET: concreteDescendantType" ).build();

		final Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
		Type type = concreteDescendantType;
		Class<S> typeClass = (Class<S>) toClass( type );

		// start walking up the inheritance hierarchy until we hit parentClass
		while( !genericAncestorType.equals( typeClass ) )
		{
			if( type instanceof Class )
			{
				// there is no useful information for us in raw types, so just
				// keep going.

				if( genericAncestorType.isInterface() )
				{
					Type intfType = null;
					Class<? super S> superClass = typeClass;
					while( intfType == null && superClass != Object.class )
					{
//						LOG.trace( "Finding "
//								+ genericAncestorType
//								+ " among interfaces of "
//								+ superClass.getSimpleName() + ": "
//								+ Arrays.asList(
//										superClass.getGenericInterfaces() ) );
						for( Type intf : superClass.getGenericInterfaces() )
						{
							if( intf instanceof ParameterizedType == false )
							{
//								LOG.trace(
//										"super {} not parameterized, extensions {}",
//										intf, Arrays.asList( ((Class) intf)
//												.getInterfaces() ) );
								for( Type extIntf : ((Class<?>) intf)
										.getGenericInterfaces() )
									if( extIntf instanceof ParameterizedType
											&& ((ParameterizedType) extIntf)
													.getRawType().equals(
															genericAncestorType ) )
									{
//										LOG.trace(
//												"Found target {} as extension with args {}",
//												genericAncestorType,
//												((ParameterizedType) extIntf)
//														.getActualTypeArguments() );
										type = (ParameterizedType) extIntf;
										break;
									}
								continue;
							}

							final ParameterizedType parameterizedIntf = (ParameterizedType) intf;
							final Type rawIntf = parameterizedIntf.getRawType();
							if( !genericAncestorType.equals( rawIntf ) )
								continue;

							LOG.trace( "supertype params: "
									+ Arrays.asList(
											superClass.getTypeParameters() )
									+ ", intf params: "
									+ Arrays.asList( toClass( rawIntf )
											.getTypeParameters() )
									+ ", actual args: "
									+ Arrays.asList( parameterizedIntf
											.getActualTypeArguments() ) );

							if( superClass.getTypeParameters().length > 0 )
								return of( superClass, concreteDescendantType );
							// intfType=getClass(rawIntf);
							type = parameterizedIntf;
						}
						superClass = (Class<? super S>) superClass
								.getSuperclass();
						if( superClass == null ) break;
					}
					// if (intfType == null)
					// type = typeClass.getGenericSuperclass();
				} else
					type = typeClass.getGenericSuperclass();

				if( type == null )
				{
					if( !typeClass.isInterface() )
					{
						LOG.warn(
								"UNEXPECTED: No generic super classes found for child class: "
										+ typeClass + " of parent: "
										+ genericAncestorType );
						return Collections.emptyList();
					}
					for( Type intf : typeClass.getGenericInterfaces() )
					{
						if( intf instanceof ParameterizedType )
						{
							type = intf;
							break;
						}
					}
					if( type == null )
					{
						LOG.warn(
								"UNEXPECTED: No generic ancestors found for child interface: "
										+ typeClass + " of parent: "
										+ genericAncestorType );
						return Collections.emptyList();
					}
				}
				// LOG.trace(String.format("Trying generic super of %s: %s",
				// typeClass.getSimpleName(), type));
			} else
			{
				final ParameterizedType parameterizedType = (ParameterizedType) type;
				final Class<?> rawType = (Class<?>) parameterizedType
						.getRawType();

				final Type[] actualTypeArguments = parameterizedType
						.getActualTypeArguments();
				final TypeVariable<?>[] typeParameters = rawType
						.getTypeParameters();
				for( int i = 0; i < actualTypeArguments.length; i++ )
				{
					resolvedTypes.put( typeParameters[i],
							actualTypeArguments[i] );
				}

				if( !genericAncestorType.equals( rawType ) )
				{
					type = rawType.getGenericSuperclass();
					// LOG.trace(String.format(
					// "Trying generic super of child %s: %s", rawType,
					// type));
				}
				// else // done climbing the hierarchy
				// LOG.trace("Matched generic " + type + " to ancestor: "
				// + genericAncestorType);
			}
			typeClass = (Class<S>) toClass( type );
			// LOG.trace("Trying generic " + typeClass + " from: "
			// + Arrays.asList(typeClass.getGenericInterfaces()));
		}

		// finally, for each actual type argument provided to baseClass,
		// determine (if possible)
		// the raw class for that type argument.
		final Type[] actualTypeArguments;
		if( type instanceof Class )
		{
			actualTypeArguments = typeClass.getTypeParameters();
		} else
		{
			actualTypeArguments = ((ParameterizedType) type)
					.getActualTypeArguments();
		}

		// resolve types by chasing down type variables.
		final List<Class<?>> result = new ArrayList<Class<?>>();
		for( Type baseType : actualTypeArguments )
		{
			while( resolvedTypes.containsKey( baseType ) )
				baseType = resolvedTypes.get( baseType );

			result.add( toClass( baseType ) );
		}
		// LOG.trace(String.format(
		// "Got child %s's type arguments for %s: %s",
		// childClass.getName(), parentClass.getSimpleName(),
		// parentTypeArguments));
		return result;
	}

	/**
	 * @param genericType the ancestor/base class
	 * @param type the concrete descendant/child class
	 * @param typeArgCache
	 * @return a (cached) list of the raw classes for the actual type arguments.
	 */
	public static <T> List<Class<?>> of( final Class<T> genericType,
		final Class<? extends T> type,
		final Map<Class<?>, List<Class<?>>> typeArgCache )
	{
		synchronized( typeArgCache )
		{
			List<Class<?>> result = typeArgCache.get( type );
			if( result == null )
			{
				result = TypeArguments.of( genericType, type );
				typeArgCache.put( type, result );
			}
			return result;
		}
	}

}
