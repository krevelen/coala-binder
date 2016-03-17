package io.coala.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

import io.coala.exception.x.ExceptionBuilder;

/**
 * {@link ReflectUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class ReflectUtil implements Util
{

	/**
	 * {@link ReflectUtil} inaccessible singleton constructor
	 */
	private ReflectUtil()
	{
	}

	public static Method getAccessibleMethod( final Class<?> valueType,
		final String name, final Class<?>... argTypes ) throws Exception
	{
		final Method result = valueType.getMethod( name, argTypes );
		if( !Modifier.isStatic( result.getModifiers() ) )
			throw new IllegalAccessException(
					name + "(" + Arrays.asList( argTypes ) + ") not static" );
		if( !result.isAccessible() ) result.setAccessible( true );
		return result;
	}

	public static <T> Constructor<T> getAccessibleConstructor(
		final Class<T> valueType, final Class<?>... argTypes )
	{
		try
		{
			final Constructor<T> result = valueType.getConstructor( argTypes );
			if( !result.isAccessible() ) result.setAccessible( true );
			return result;
		} catch( final Exception e )
		{
			throw ExceptionBuilder.unchecked(
					"No public zero-arg bean constructor found for %s%s",
					valueType, argTypes == null ? Collections.emptyList()
							: Arrays.asList( argTypes ) )
					.build();
		}
	}

	public static boolean isAbstract( final Class<?> type )
	{
		return Modifier.isAbstract( type.getModifiers() ) || type.isInterface()
				|| Proxy.isProxyClass( type );
	}

}
