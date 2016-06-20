/* $Id: c183533a506e76c867a9266d7739e42017a7673b $
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
 */
package io.coala.config;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

/**
 * {@link PropertyGetter}
 */
public interface PropertyGetter
{

	/**
	 * @return the configured value
	 * @throws Exception if value was not configured nor any default was set
	 */
	String get() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	String get( String defaultValue ) throws Exception;

	/**
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 * @throws NumberFormatException
	 */
	Number getNumber() throws NumberFormatException, Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Number getNumber( Number defaultValue ) throws NumberFormatException;

	/**
	 * @param defaultValue
	 * @return
	 */
	BigDecimal getBigDecimal( BigDecimal defaultValue );

	/**
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	Boolean getBoolean() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Boolean getBoolean( Boolean defaultValue );

	/**
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 * @throws NumberFormatException
	 */
	Byte getByte() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Byte getByte( Byte defaultValue );

	/**
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	Character getChar() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Character getChar( Character defaultValue );

	/**
	 * @return
	 * @throws Exception
	 */
	Short getShort() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Short getShort( Short defaultValue );

	/**
	 * @return
	 * @throws Exception
	 */
	Integer getInt() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Integer getInt( Integer defaultValue );

	/**
	 * @return
	 * @throws Exception
	 */
	Long getLong() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Long getLong( Long defaultValue );

	/**
	 * @return
	 * @throws Exception
	 */
	Float getFloat() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Float getFloat( Float defaultValue );

	/**
	 * @return
	 * @throws Exception
	 */
	Double getDouble() throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	Double getDouble( Double defaultValue );

	/**
	 * @param enumType
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	<E extends Enum<E>> E getEnum( Class<E> enumType ) throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	<E extends Enum<E>> E getEnum( E defaultValue );

	/**
	 * @param valueType
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	<T extends Serializable> T getObject( Class<T> valueType ) throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 */
	<T extends Serializable> T getObject( T defaultValue );

	/**
	 * @param valueTypeRef
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	@SuppressWarnings( "rawtypes" )
	<T> T getJSON( TypeReference valueTypeRef ) throws Exception;

	/**
	 * @param valueType
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	<T> T getJSON( JavaType valueType ) throws Exception;

	/**
	 * @param valueType
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	<T> T getJSON( Class<T> valueType ) throws Exception;

	/**
	 * @param defaultValue
	 * @return
	 * @throws Exception if umarshalling failed
	 */
	<T> T getJSON( T defaultValue ) throws Exception;

	/**
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	Class<?> getType() throws Exception;

	/**
	 * @param superType
	 * @return
	 * @throws Exception if value was not configured nor any default was set
	 */
	<T> Class<? extends T> getType( Class<T> superType ) throws Exception;

	/**
	 * @param keySuperType
	 * @param valueSuperType
	 * @return a {@link Map} linking {@code keySuperType} subtypes to
	 *         {@code valueSuperType} subtypes
	 * @throws Exception
	 */
	<K, V> Map<Class<? extends K>, Class<? extends V>> getBindings(
		final Class<K> keySuperType, final Class<V> valueSuperType )
		throws Exception;

}
