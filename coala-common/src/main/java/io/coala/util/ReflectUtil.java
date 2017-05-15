package io.coala.util;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.inject.Inject;

import io.coala.exception.Thrower;
import io.reactivex.Observer;

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
		// empty
	}

	/**
	 * <p>
	 * TODO enable/recognize builder-type (i.e. chaining 'with*') setters?
	 * 
	 * @param impl
	 * @param intfType
	 * @param properties
	 * @param callObserver
	 * @return
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T createProxyInstance( final Object impl,
		final Class<T> intfType, final Supplier<Map<String, Object>> properties,
		final Observer<Method> callObserver )
	{
		return (T) Proxy.newProxyInstance( intfType.getClassLoader(),
				new Class<?>[]
		{ intfType }, ( proxy, method, args ) ->
		{
			try
			{
				final Object result;
				if( method.isDefault() )
				{
					// method is Java8 'default' declaration
					result = invokeDefaultMethod( proxy, method, args );
				} else if( method.getDeclaringClass().isInstance( impl ) )
				{
					// method declared in intfType and/or impl
					result = method.invoke( impl, args );
				} else
				{
					// bean read(get)/write(set) method
					result = invokeAsBean( properties.get(), intfType, method,
							args == null || args.length == 0 ? null : args[0] );
				}
				if( callObserver != null ) callObserver.onNext( method );
				return result;

			} catch( final Throwable e )
			{
//				LogUtil.getLogger( ReflectUtil.class )
//						.warn( LogUtil.messageOf( "Failed call {}(..) @ {}: {}",
//								method.getName(), intfType.getSimpleName() ),
//								e );
//				if( e instanceof InvocationTargetException ) e = e.getCause();
				if( callObserver != null ) callObserver.onError( e );
				throw e;
			}
		} );
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

	private static final Map<Method, PropertyDescriptor> BEAN_INFO_CACHE = new HashMap<>();

	/**
	 * match bean read method to bean property
	 * 
	 * @param properties the value store
	 * @param beanType the concrete bean type
	 * @param method the called read(get)/write(set)-method
	 * @param value the value to set, or {@code null} to remove
	 * @return the current (get) or previous (set) value (possibly {@code null})
	 */
	public static Object invokeAsBean( final Map<String, Object> properties,
		final Class<?> beanType, final Method method, final Object value )
	{
		final PropertyDescriptor prop = BEAN_INFO_CACHE.computeIfAbsent( method,
				key ->
				{
					try
					{
						for( PropertyDescriptor pd : Introspector
								.getBeanInfo( beanType )
								.getPropertyDescriptors() )
							if( method.equals( pd.getReadMethod() )
									|| method.equals( pd.getWriteMethod() ) )
								return pd;

						// see http://stackoverflow.com/q/185004/1418999
						for( Class<?> intf : beanType.getInterfaces() )
							for( PropertyDescriptor pd : Introspector
									.getBeanInfo( intf )
									.getPropertyDescriptors() )
								if( method.equals( pd.getReadMethod() )
										|| method
												.equals( pd.getWriteMethod() ) )
									return pd;
						return null;
					} catch( final IntrospectionException e )
					{
						return Thrower.rethrowUnchecked( e );
					}
				} );
		if( prop == null )
			return Thrower.throwNew( IllegalArgumentException.class,
					"Not a bean write(set)/read(get)-method"
							+ ", or undeclared in (interfaces of) {}: {}",
					beanType, method );

		if( method.equals( prop.getReadMethod() ) ) return Objects
				.requireNonNull( properties.get( prop.getName() ), "Missing "
						+ prop.getName() + " in this " + beanType.getName() );

		// some stores can't accept null-values, so remove them
		if( value == null ) return properties.remove( prop.getName() );

		return properties.put( prop.getName(), value );
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
