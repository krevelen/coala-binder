package io.coala.util;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.inject.Provider;

import io.coala.exception.ExceptionBuilder;
import io.coala.json.DynaBean;
import io.coala.json.DynaBeanProxyProvider;
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

	/** the {@link Instantiator}s cached per type */
	private static final Map<Class<?>, Instantiator<?>> INSTANTIATOR_CACHE = new WeakHashMap<>();

	/**
	 * @param valueType the type of the stored property value
	 * @param args the arguments for construction
	 * @return the property value's class instantiated
	 */
	public static <T> T instantiate( final Class<T> valueType,
		final Object... args )
	{
		final Class<?>[] argTypes = new Class<?>[args.length];
		for( int i = 0; i < args.length; i++ )
			argTypes[i] = args[i] == null ? null : args[i].getClass();

		return of( valueType, argTypes ).instantiate( args );
	}

	/**
	 * @param type the {@link Class} to instantiate/proxy
	 * @return the new {@link Provider}
	 */
	public static <T> Instantiator<T> of( final Class<T> type,
		final Class<?>... argTypes )
	{
		return of( INSTANTIATOR_CACHE, type, argTypes );
	}

	/**
	 * @param type the {@link Class} to instantiate/proxy
	 * @return the new {@link Provider} instance
	 */
	public static <T> Instantiator<T> of(
		final Map<Class<?>, Instantiator<?>> cache, final Class<T> type,
		final Class<?>... argTypes )
	{
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
				return DynaBeanProxyProvider
						.of( JsonUtil.getJOM(), this.type, imports ).get();
			}
			return this.constructor.newInstance( args );
		} catch( final Throwable t )
		{
			throw ExceptionBuilder
					.unchecked( t, "Problem instantiating %s", this.type )
					.build();
		}
	}

}