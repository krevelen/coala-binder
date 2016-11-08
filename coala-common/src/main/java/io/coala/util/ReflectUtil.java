package io.coala.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import io.coala.exception.Thrower;

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
			return Thrower.throwNew( IllegalAccessException.class,
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

			return Thrower.throwNew( NoSuchMethodException.class,
					"No matching public constructor found for {}({})",
					valueType, argTypes == null || argTypes.length == 0 ? ""
							: Arrays.asList( argTypes ) );
		}
	}

	/**
	 * match bean read method to bean property
	 * <p>
	 * TODO enable/recognize builder-type (i.e. chaining 'with*') setters?
	 * 
	 * @param beanType
	 * @param properties
	 * @param method
	 * @param args
	 * @return
	 * @throws IntrospectionException
	 */
	public static Object invokeAsBean( final Map<String, Object> properties,
		final Class<?> beanType, final Method method, final Object... args )
		throws IntrospectionException
	{
		final BeanInfo beanInfo = Introspector.getBeanInfo( beanType );
		for( PropertyDescriptor pd : beanInfo.getPropertyDescriptors() )
			if( method.equals( pd.getReadMethod() ) )
				return properties.get( pd.getName() );
			else if( method.equals( pd.getWriteMethod() ) )
				return properties.put( pd.getName(), args[0] );
		return Thrower.throwNew( IllegalArgumentException.class,
				"Can't invoke {} as bean method", method );
	}

	/**
	 * InvocationHandler for default interface methods (e.g. on proxy instances)
	 * <p>
	 * see http://stackoverflow.com/a/23990827,
	 * http://stackoverflow.com/a/37816247/1418999, <a href=
	 * "https://rmannibucau.wordpress.com/2014/03/27/java-8-default-interface-methods-and-jdk-dynamic-proxies/#comment-1333">
	 * </a>
	 * 
	 * @param proxy
	 * @param method
	 * @param args
	 * @return
	 * @throws Throwable
	 * @see InvocationHandler#invoke(Object, Method, Object[])
	 * @see org.aeonbits.owner.util.Java8SupportImpl#invokeDefaultMethod(Object,
	 *      Method, Object[])
	 */
	public static Object invokeDefaultMethod( final Object proxy,
		final Method method, final Object[] args ) throws Throwable
	{
		return lookup( method.getDeclaringClass() )
				.unreflectSpecial( method, method.getDeclaringClass() )
				.bindTo( proxy ).invokeWithArguments( args );
	}

	private static final Map<Class<?>, Lookup> LOOKUP_CACHE = new ConcurrentHashMap<>();

	private static Constructor<Lookup> LOOKUP_CONSTRUCTOR = null;

	private static Lookup lookup( final Class<?> type ) throws Throwable
	{
		return LOOKUP_CACHE.computeIfAbsent( type, key ->
		{
			try
			{
				return lookupConstructor().newInstance( key, Lookup.PRIVATE );
			} catch( final Exception e )
			{
				return Thrower.rethrowUnchecked( e );
			}
		} );
	}

	private static Constructor<Lookup> lookupConstructor()
		throws NoSuchMethodException, SecurityException
	{
		LOOKUP_CONSTRUCTOR = Lookup.class.getDeclaredConstructor( Class.class,
				int.class );
		LOOKUP_CONSTRUCTOR.setAccessible( true );
		return LOOKUP_CONSTRUCTOR;
	}
}
