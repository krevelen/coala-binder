package io.coala.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.inject.Provider;

import io.coala.exception.ExceptionFactory;
import io.coala.json.DynaBean;
import io.coala.json.DynaBean.ProxyProvider;
import io.coala.json.JsonUtil;

/**
 * {@link Instantiator}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Instantiator<T>
{

	/** the {@link Instantiator} cache by type */
	private static final Map<Class<?>, Instantiator<?>> INSTANTIATOR_CACHE = new WeakHashMap<>();

	/**
	 * @param valueType the type to instantiate
	 * @param args the constructor arguments
	 * @return the instance
	 */
	public static <T> T instantiate( final Class<T> valueType,
		final Object... args )
	{
		return providerOf( valueType, args ).get();
	}

	/**
	 * @param valueType the {@link Class} to instantiate/proxy
	 * @param args the constructor arguments (constant for each instantiation)
	 * @return a new {@link Provider}
	 */
	public static <T> Provider<T> providerOf( final Class<T> valueType,
		final Object... args )
	{
		final Class<?>[] argTypes = new Class<?>[args == null ? 0
				: args.length];
		for( int i = 0; args != null && i < args.length; i++ )
			argTypes[i] = args[i] == null ? null : args[i].getClass();
		final Instantiator<T> instantiator = of( valueType, argTypes );

		return new Provider<T>()
		{
			@Override
			public T get()
			{
				return instantiator.instantiate( args );
			}

			@Override
			public String toString()
			{
				return valueType.getName() + (args == null
						? Collections.emptyList() : Arrays.asList( args ));
			}
		};
	}

	/**
	 * @param valueType the {@link Class} to instantiate/proxy
	 * @return a new or cached {@link Instantiator}
	 */
	public static <T> Instantiator<T> of( final Class<T> valueType,
		final Class<?>... argTypes )
	{
		return of( INSTANTIATOR_CACHE, valueType, argTypes );
	}

	/**
	 * @param cache a {@link Map} of reusable {@link Instantiator}s, or null
	 * @param type the {@link Class} to provide instances of
	 * @param argTypes the constructor argument types
	 * @return a new or cached {@link Instantiator} instance
	 */
	public static <T> Instantiator<T> of(
		final Map<Class<?>, Instantiator<?>> cache, final Class<T> type,
		final Class<?>... argTypes )
	{
		if( cache == null ) return new Instantiator<T>( type, argTypes );

		synchronized( cache )
		{
			@SuppressWarnings( "unchecked" )
			Instantiator<T> result = (Instantiator<T>) cache.get( type );
			if( result == null )
			{
				result = new Instantiator<T>( type, argTypes );
				cache.put( type, result );
			}
			return result;
		}
	}

	/** the {@link Class} to instantiate/proxy */
	private final Class<T> type;

	private final Constructor<T> constructor;

	/**
	 * {@link Instantiator} constructor
	 * 
	 * @param type the {@link Class} to instantiate/proxy
	 */
	public Instantiator( final Class<T> type, final Class<?>... argTypes )
	{
		this.type = type;
		// test bean property of having an accessible public
		// zero-arg constructor
		this.constructor = ClassUtil.isAbstract( type ) ? null
				: ReflectUtil.getAccessibleConstructor( type, argTypes );
	}

	/**
	 * @param args the {@link Constructor} arguments, or {@link DynaBean}
	 *            imports (i.e. {@link Properties Properties[]})
	 * @return the newly constructed instance
	 */
	public T instantiate( final Object... args )
	{
		try
		{
			if( ClassUtil.isAbstract( this.type ) )
			{
				final Properties[] imports = args == null ? null
						: new Properties[args.length];
				for( int i = 0; i < args.length; i++ )
					imports[i] = (Properties) args[i];
				return ProxyProvider.of( JsonUtil.getJOM(), this.type, imports )
						.get();
			}
			return this.constructor != null
					? this.constructor.newInstance( args )
					: this.type.newInstance();
		} catch( final Throwable t )
		{
			throw ExceptionFactory.createUnchecked( t,
					"Problem instantiating {}", this.type );
		}
	}

}