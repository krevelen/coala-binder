package io.coala.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.inject.Provider;

import io.coala.exception.Thrower;
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
	 * FIXME caching ignores argument types!
	 * 
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
		// test bean has an accessible public zero-arg constructor
		Constructor<T> c = null;
		try
		{
			c = ClassUtil.isAbstract( type ) ? null
					: ReflectUtil.getAccessibleConstructor( type, argTypes );
		} catch( final NoSuchMethodException cause )
		{
			Thrower.throwNew( IllegalArgumentException.class, cause,
					"Missing 'static' 'public' constructor {}({})",
					type.getName(), argTypes == null ? "" : argTypes );
		}
		this.constructor = c;
	}

	/**
	 * @param args the {@link Constructor} arguments, or {@link DynaBean}
	 *            imports (i.e. {@link Properties Properties[]})
	 * @return the newly constructed instance
	 */
	@SuppressWarnings( "unchecked" )
	public T instantiate( final Object... args )
	{
		if( ClassUtil.isAbstract( this.type ) )
			return ProxyProvider.of( JsonUtil.getJOM(), this.type
			// FIXME apply args for JSON-based instantiations 
			).get();
		if( this.constructor != null ) try
		{
			return this.constructor.newInstance( args );
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}

		if( this.type.isMemberClass()
				&& !Modifier.isStatic( this.type.getModifiers() )
				&& (args == null || args.length == 0 || args[0] == null
						|| args[0].getClass() != this.type
								.getEnclosingClass()) )
			return Thrower.throwNew( IllegalArgumentException.class,
					"First argument should be an instance of enclosing {} "
							+ "for instantiating a non-static member: {}",
					this.type.getEnclosingClass(), this.type );
		try
		{
			return this.type.getConstructor( this.type.isMemberClass()
					? new Class<?>[]
			{ args[0].getClass() } : new Class<?>[0] ).newInstance( args );
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

}