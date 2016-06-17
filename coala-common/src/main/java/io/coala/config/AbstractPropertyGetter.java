/* $Id: 477f095b595d0f93e71a36218aa6e217cb8e6daa $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/config/AbstractPropertyGetter.java $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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
 * 
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.config;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import io.coala.exception.ExceptionFactory;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.util.SerializableUtil;

/**
 * {@link AbstractPropertyGetter}
 * 
 * @version $Id: 477f095b595d0f93e71a36218aa6e217cb8e6daa $
 * @author Rick van Krevelen
 */
@Deprecated
public abstract class AbstractPropertyGetter implements PropertyGetter
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( AbstractPropertyGetter.class );

	/** */
	private final String key;

	/** */
	private final String defaultValue;

	/**
	 * {@link AbstractPropertyGetter} constructor
	 * 
	 * @param key
	 */
	public AbstractPropertyGetter( final String key )
	{
		this( key, null );
	}

	/**
	 * {@link AbstractPropertyGetter} constructor
	 * 
	 * @param key
	 * @param defaultValue
	 */
	public AbstractPropertyGetter( final String key, final String defaultValue )
	{
		this.key = key;
		this.defaultValue = defaultValue;
	}

	protected abstract Properties getProperties();

	@Override
	public String get() throws Exception
	{
		final String result = getProperties().getProperty( this.key );

		if( result != null && !result.equals( "" + null ) ) return result;

		if( this.defaultValue != null ) return this.defaultValue;

		LOG.trace(
				"Property '" + this.key + "' not found nor default value set: "
						+ getProperties().getProperty( this.key ) );

		throw ExceptionFactory.createChecked( "No value for key {} in {}",
				this.key, ConfigUtil.CONFIG_FILE_BOOTTIME );
	}

	@Override
	public String get( final String defaultValue )
	{
		final String result = CoalaPropertyMap.getInstance()
				.getProperty( this.key );
		if( result != null ) return result;

		LOG.trace( "No value for key {} in {}, using default: {}", this.key,
				ConfigUtil.CONFIG_FILE_BOOTTIME, this.defaultValue );
		return defaultValue;
	}

	@Override
	public Number getNumber() throws Exception
	{
		return Double.valueOf( get() );
	}

	@Override
	public Number getNumber( final Number defaultValue )
		throws NumberFormatException
	{
		final String value = get(
				defaultValue == null ? null : defaultValue.toString() );
		return value == null ? null : Double.valueOf( value );
	}

	@Override
	public BigDecimal getBigDecimal( final BigDecimal defaultValue )
	{
		return new BigDecimal( get( defaultValue.toString() ) );
	}

	@Override
	public Boolean getBoolean() throws Exception
	{
		return Boolean.valueOf( get() );
	}

	@Override
	public Boolean getBoolean( final Boolean defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : Boolean.valueOf( value );
	}

	@Override
	public Byte getByte() throws Exception
	{
		return Byte.valueOf( get() );
	}

	@Override
	public Byte getByte( final Byte defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : Byte.valueOf( value );
	}

	@Override
	public Character getChar() throws Exception
	{
		return Character.valueOf( get().charAt( 0 ) );
	}

	@Override
	public Character getChar( final Character defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : value.charAt( 0 );
	}

	@Override
	public Short getShort() throws Exception
	{
		return Short.valueOf( get() );
	}

	@Override
	public Short getShort( final Short defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : Short.valueOf( value );
	}

	@Override
	public Integer getInt() throws Exception
	{
		return Integer.valueOf( get() );
	}

	@Override
	public Integer getInt( final Integer defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : Integer.valueOf( value );
	}

	@Override
	public Long getLong() throws Exception
	{
		return Long.valueOf( get() );
	}

	@Override
	public Long getLong( final Long defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : Long.valueOf( value );
	}

	@Override
	public Float getFloat() throws Exception
	{
		return Float.valueOf( get() );
	}

	@Override
	public Float getFloat( final Float defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : Float.valueOf( value );
	}

	@Override
	public Double getDouble() throws Exception
	{
		return Double.valueOf( get() );
	}

	@Override
	public Double getDouble( final Double defaultValue )
	{
		final String value = get( "" );
		return value.isEmpty() ? defaultValue : Double.valueOf( value );
	}

	@Override
	public <E extends Enum<E>> E getEnum( final Class<E> enumType )
		throws Exception
	{
		return Enum.valueOf( enumType, get() );
	}

	@Override
	public <E extends Enum<E>> E getEnum( final E defaultValue )
	{
		final String value = defaultValue == null ? get( "" )
				: get( defaultValue.name() );
		if( value != null && !value.isEmpty() ) try
		{
			return Enum.valueOf( defaultValue.getDeclaringClass(), value );
		} catch( final Throwable e )
		{
			LOG.warn( "Using default", ExceptionFactory.createChecked( e,
					"Problem reading {} as {}", value, defaultValue != null
							? defaultValue.getClass() : Enum.class ) );
		}
		return defaultValue;
	}

	@Override
	public <T extends Serializable> T getObject( final Class<T> valueType )
		throws Exception
	{
		return valueType
				.cast( SerializableUtil.deserialize( get(), valueType ) );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T extends Serializable> T getObject( final T defaultValue )
	{
		final String value = get( "" );
		if( !value.isEmpty() ) try
		{
			return (T) SerializableUtil.deserialize( value,
					defaultValue.getClass() );
		} catch( final Throwable e )
		{
			LOG.warn( "Using default", ExceptionFactory.createChecked( e,
					"Problem reading {} as {}", value, defaultValue != null
							? defaultValue.getClass() : Object.class ) );
		}
		return defaultValue;
	}

	@Override
	@SuppressWarnings( "rawtypes" )
	public <T> T getJSON( final TypeReference valueTypeRef ) throws Exception
	{
		return JsonUtil.getJOM().readValue( get(), valueTypeRef );
	}

	@Override
	public <T> T getJSON( final JavaType valueType ) throws Exception
	{
		return JsonUtil.getJOM().readValue( get(), valueType );
	}

	@Override
	public <T> T getJSON( final Class<T> valueType ) throws Exception
	{
		return JsonUtil.getJOM().readValue( get(), valueType );
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> T getJSON( final T defaultValue ) throws Exception
	{
		final String value = get( "" );
		if( value.isEmpty() ) return defaultValue;

		return JsonUtil.getJOM().readValue( get(),
				(Class<T>) defaultValue.getClass() );
	}

	@Override
	public Class<?> getType() throws Exception
	{
		return Class.forName( get() );
	}

	@Override
	public <T> Class<? extends T> getType( final Class<T> superType )
		throws Exception
	{
		return getType().asSubclass( superType );
	}

	@Override
	public <K, V> Map<Class<? extends K>, Class<? extends V>> getBindings(
		final Class<K> keySuperType, final Class<V> valueSuperType )
		throws Exception
	{
		final Map<Class<? extends K>, Class<? extends V>> result = new HashMap<>();
		for( Entry<?, ?> entry : ((Map<?, ?>) getJSON( Map.class )).entrySet() )
		{
			try
			{
				final Class<? extends K> keyType = Class
						.forName( (String) entry.getKey() )
						.asSubclass( keySuperType );
				final Class<? extends V> valueType = Class
						.forName( (String) entry.getValue() )
						.asSubclass( valueSuperType );
				result.put( keyType, valueType );
			} catch( final Throwable t )
			{
				new IllegalArgumentException(
						"Could not set type from config: " + entry, t )
								.printStackTrace();
			}
		}
		return result;
	}

}