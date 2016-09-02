package io.coala.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;

/**
 * {@link InstanceParser}
 * 
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public abstract class InstanceParser<T>
{

	private static final Map<Class<?>, InstanceParser<?>> INVOKER_CACHE = new HashMap<>();

	protected static <T> InstanceParser<T> of( final Class<T> valueType,
		final Method method )
	{
		return new InstanceParser<T>( valueType )
		{
			@Override
			public T parse( final String value ) throws Throwable
			{
				try
				{
					return valueType.cast( method.invoke( valueType, value ) );
				} catch( final InvocationTargetException e )
				{
					throw e.getCause();
				}
			}
		};
	}

	protected static <T> InstanceParser<T> of( final Class<T> valueType,
		final Constructor<T> constructor )
	{
		return new InstanceParser<T>( valueType )
		{
			@Override
			public T parse( final String value ) throws Exception
			{
				return valueType.cast( constructor.newInstance( value ) );
			}
		};
	}

	protected static <T> InstanceParser<T> of( final Class<T> valueType,
		final ObjectMapper om )
	{
		return new InstanceParser<T>( valueType )
		{
			@Override
			public T parse( final String value ) throws Exception
			{
				return om.readValue( "\"" + value + "\"",
						JsonUtil.checkRegistered( om, valueType ) );
			}
		};
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	protected static <E extends Enum> InstanceParser<E>
		ofEnum( final Class<E> valueType )
	{
		return new InstanceParser<E>( valueType )
		{
			@Override
			public E parse( final String value ) throws Exception
			{
				try
				{
					return (E) Enum.valueOf( valueType, value );
				} catch( final Exception e )
				{
					for( E constant : valueType.getEnumConstants() )
						if( constant.toString().equalsIgnoreCase( value ) )
							return constant;
					throw e;
				}
			}
		};
	}

	/**
	 * @param valueType the {@link Class} to parse
	 * @return the cached (new) {@link InstanceParser} instance
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> InstanceParser<T> of( final Class<T> valueType )
	{
		@SuppressWarnings( "unchecked" )
		InstanceParser<T> result = (InstanceParser<T>) INVOKER_CACHE
				.get( valueType );
		if( result != null ) return result;

		try
		{
			if( valueType.isInterface() )
				result = of( valueType, JsonUtil.getJOM() );
			else if( valueType.isEnum() )
				result = (InstanceParser<T>) ofEnum(
						valueType.asSubclass( Enum.class ) );
			else
				result = of( valueType, ReflectUtil.getAccessibleMethod(
						valueType, "valueOf", String.class ) );
		} catch( final Exception e )
		{
			try
			{
				result = of( valueType, ReflectUtil.getAccessibleMethod(
						valueType, "valueOf", CharSequence.class ) );
			} catch( final Exception e1 )
			{
				try
				{
					result = of( valueType,
							ReflectUtil.getAccessibleConstructor( valueType,
									String.class ) );
				} catch( final Exception e2 )
				{
					try
					{
						result = of( valueType,
								ReflectUtil.getAccessibleConstructor( valueType,
										CharSequence.class ) );
					} catch( final Exception e3 )
					{
						Thrower.rethrowUnchecked( e );
						return null;
					}
				}
			}
		}
		INVOKER_CACHE.put( valueType, result );
		return result;
	}

	private final Class<T> valueType;

	public InstanceParser( final Class<T> valueType )
	{
		this.valueType = valueType;
	}

	/**
	 * @param value the {@link String} representation to parse
	 * @return the parsed instance
	 * @throws Exception when no parsing method is available
	 */
	public T parseOrTrimmed( final String value ) throws Exception
	{
		try
		{
			return parse( value );
		} catch( final Throwable e )
		{
			try
			{
				return parse( value.trim() );
			} catch( final Throwable e1 )
			{
				return Thrower.throwNew( IllegalArgumentException.class, e,
						"Problem parsing {} from un/trimmed value: '{}'",
						this.valueType, value );
			}
		}
	}

	/**
	 * @param value the {@link String} representation to parse
	 * @return the parsed instance
	 * @throws Exception when no parsing method is available
	 */
	public abstract T parse( String value ) throws Throwable;

}