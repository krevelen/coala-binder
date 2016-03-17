package io.coala.json.x;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.coala.exception.x.ExceptionBuilder;
import io.coala.util.TypeArguments;

/**
 * {@link DynaBeanProxyProvider}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class DynaBeanProxyProvider<T> implements Provider<T>
{

	/** cache of type arguments for known {@link Proxy} sub-types */
	private static final Map<Class<?>, List<Class<?>>> PROXY_TYPE_ARGUMENT_CACHE = new HashMap<>();

	/**
	 * @param proxyType should be a non-abstract concrete {@link Class} that has
	 *            a public zero-arg constructor
	 * @return the new {@link DynaBeanProxyProvider} instance
	 */
	public static <T> DynaBeanProxyProvider<T> of( final Class<T> proxyType,
		final Properties... imports )
	{
		return of( JsonUtil.getJOM(), proxyType, imports );
	}

	/**
	 * @param om the {@link ObjectMapper} for get and set de/serialization
	 * @param proxyType should be a non-abstract concrete {@link Class} that has
	 *            a public zero-arg constructor
	 * @return the new {@link DynaBeanProxyProvider} instance
	 */
	public static <T> DynaBeanProxyProvider<T> of( final ObjectMapper om,
		final Class<T> proxyType, final Properties... imports )
	{
		return new DynaBeanProxyProvider<T>( om, proxyType, new DynaBean(),
				imports );
	}

	/**
	 * @param om the {@link ObjectMapper} for get and set de/serialization
	 * @param beanType should be a non-abstract concrete {@link Class} that has
	 *            a public zero-arg constructor
	 * @param cache the {@link Map} of previously created instances
	 * @return the cached (new) {@link DynaBeanProxyProvider} instance
	 */
	public static <T> DynaBeanProxyProvider<T> of( final ObjectMapper om,
		final Class<T> beanType,
		final Map<Class<?>, DynaBeanProxyProvider<?>> cache,
		final Properties... imports )
	{
		if( cache == null ) return of( om, beanType, imports );

		synchronized( cache )
		{
			@SuppressWarnings( "unchecked" )
			DynaBeanProxyProvider<T> result = (DynaBeanProxyProvider<T>) cache
					.get( beanType );
			if( result == null )
			{
				result = of( om, beanType, imports );
				cache.put( beanType, result );
			}
			return result;
		}
	}

	/** */
	private final ObjectMapper om;

	/** */
	private final Class<T> proxyType;

	/** */
	private final DynaBean bean;

	/** */
	private final Properties[] imports;

	/**
	 * {@link DynaBeanProxyProvider} constructor
	 * 
	 * @param om
	 * @param proxyType
	 * @param bean the (possibly prepared) {@link DynaBean}
	 * @param imports
	 */
	public DynaBeanProxyProvider( final ObjectMapper om,
		final Class<T> proxyType, final DynaBean bean,
		final Properties... imports )
	{
		this.om = om;
		this.proxyType = proxyType;
		this.bean = bean;
		this.imports = imports;
	}

	@Override
	public T get()
	{
		try
		{
			@SuppressWarnings( "unchecked" )
			final Class<T> proxyType = this.proxyType == null
					? (Class<T>) TypeArguments.of( DynaBeanProxyProvider.class,
							getClass(), PROXY_TYPE_ARGUMENT_CACHE ).get( 0 )
					: this.proxyType;
			return DynaBean.proxyOf( this.om, proxyType, this.bean,
					this.imports );
		} catch( final Throwable t )
		{
			throw ExceptionBuilder.unchecked( t,
					"Problem providing proxy instance for %s", this.proxyType )
					.build();
		}
	}
}