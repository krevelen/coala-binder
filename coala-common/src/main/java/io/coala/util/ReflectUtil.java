package io.coala.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import io.coala.exception.ExceptionFactory;

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
		final String name, final Class<?>... argTypes )
		throws IllegalAccessException, SecurityException, NoSuchMethodException
	{
		final Method result = valueType.getMethod( name, argTypes );
		if( !Modifier.isStatic( result.getModifiers() ) )
			return ExceptionFactory.throwNew( IllegalAccessException.class,
					"{}({}) not static", name, Arrays.asList( argTypes ) );
		if( !result.isAccessible() ) result.setAccessible( true );
		return result;
	}

	@SuppressWarnings( "unchecked" )
	public static <T> Constructor<T> getAccessibleConstructor(
		final Class<T> valueType, final Class<?>... argTypes )
		throws NoSuchMethodException
	{
		if( valueType.getConstructors().length == 0
				&& (argTypes == null || argTypes.length == 0) )
			return null; // go with zero-arg default constructor

		try
		{
			final Constructor<T> result = valueType.getConstructor( argTypes );
			if( !result.isAccessible() ) result.setAccessible( true );
			return result;
		} catch( final Exception e )
		{
			constrLoop: for( Constructor<?> constructor : valueType
					.getConstructors() )
			{
				final Class<?>[] paramTypes = constructor.getParameterTypes();
				if( paramTypes.length != argTypes.length )
				{
					continue;
				}

				boolean match = true;
				for( int i = 0; match && i < paramTypes.length; i++ )
				{
					if( !ClassUtil.isAssignableFrom( paramTypes[i],
							argTypes[i] ) )
						continue constrLoop;
				}
				if( !constructor.isAccessible() )
					constructor.setAccessible( true );
				return (Constructor<T>) constructor;
			}

			// assume first @Inject constructor can be matched by some Injector
			for( Constructor<?> constructor : valueType.getConstructors() )
				if( constructor.isAnnotationPresent( Inject.class ) )
					return null;

			return ExceptionFactory.throwNew( NoSuchMethodException.class,
					"No matching public constructor found for {}{}", valueType,
					argTypes == null ? Collections.emptyList()
							: Arrays.asList( argTypes ) );
		}
	}

}
