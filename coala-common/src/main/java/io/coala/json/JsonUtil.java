/* $Id$
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
package io.coala.json;

import java.beans.PropertyEditorSupport;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.eaio.UUIDModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.coala.exception.Thrower;
import io.coala.json.DynaBean.BeanProxy;
import io.coala.log.LogUtil;
import io.coala.util.TypeArguments;

/**
 * {@link JsonUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class JsonUtil
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( JsonUtil.class );

	/** */
	private static final ObjectMapper JOM = new ObjectMapper();

	/** singleton design pattern constructor */
	static//private JsonUtil()
	{
		// singleton design pattern
		LOG.trace( "Using jackson v: " + JOM.version() );
		JOM.registerModule( new JodaModule() );
		JOM.registerModule( new UUIDModule() );
	}

	/** */
	public synchronized static ObjectMapper getJOM()
	{
		return JOM;
	}

	/**
	 * @param object the object to serialize/marshal
	 * @return the (minimal) JSON representation
	 */
	public static String stringify( final Object object )
	{
		final ObjectMapper om = getJOM();
		try
		{
			checkRegistered( om, object.getClass() );
			return om.writer().writeValueAsString( object );
		} catch( final JsonProcessingException e )
		{
			Thrower.rethrowUnchecked( e );
			return null;
		}
	}

	/**
	 * @param object the object to serialize/marshal
	 * @return the (pretty) JSON representation
	 */
	public static String toJSON( final Object object )
	{
		return toJSON( getJOM(), object );
	}

	/**
	 * @param object the object to serialize/marshal as (pretty) JSON
	 * @return the (pretty) JSON representation
	 */
	public static String toJSON( final ObjectMapper om, final Object object )
	{
		try
		{
			return om
					// .setSerializationInclusion(JsonInclude.Include.NON_NULL)
					.writer().withDefaultPrettyPrinter()
					.writeValueAsString( object );
		} catch( final JsonProcessingException e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param profile
	 * @return
	 */
	public static JsonNode toTree( final Object object )
	{
		final ObjectMapper om = getJOM();
		try
		{
			// checkRegistered(om, object.getClass());
			return om.valueToTree( object );
//			return om.readTree( stringify( object ) );
		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
			return null;
		}
	}

	/**
	 * @param json the {@link InputStream} of JSON formatted value
	 * @return the parsed/deserialized/unmarshalled {@link JsonNode} tree
	 * @see ObjectMapper#readTree(InputStream)
	 */
	public static JsonNode toTree( final InputStream json )
	{
		try
		{
			return json == null ? null : getJOM().readTree( json );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param json the JSON formatted value
	 * @return the parsed/deserialized/unmarshalled {@link JsonNode} tree
	 * @see ObjectMapper#readTree(String)
	 */
	public static JsonNode toTree( final String json )
	{
		try
		{
			return json == null || json.isEmpty() ? null
					: getJOM().readTree( json );
		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
			return null;
		}
	}

	/**
	 * @param value the {@link Object} to serialize
	 * @return the {@link String} representation in pretty JSON format
	 * @deprecated use {@link #toJSON(Object)}
	 */
	@Deprecated
	public static String toPrettyJSON( final Object object )
	{
		return toJSON( object );
	}

	/**
	 * @param value the {@link Object} to serialize
	 * @return the {@link String} representation in JSON format
	 * @deprecated use {@link #stringify(Object)}
	 */
	@Deprecated
	public static String toString( final Object value )
	{
		return stringify( value );
	}

	/**
	 * @param json the {@link InputStream}
	 * @param resultType the type of result {@link Object}
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	public static <T> T valueOf( final InputStream json,
		final Class<T> resultType, final Properties... imports )
	{
		try
		{
			final ObjectMapper om = getJOM();
			return json == null ? null
					: (T) om.readValue( json,
							checkRegistered( om, resultType, imports ) );
		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
			return null;
		}
	}

	/**
	 * @param json the JSON formatted {@link String} value
	 * @param resultType the type of result {@link Object}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	public static <T> T valueOf( final String json, final Class<T> resultType,
		final Properties... imports )
	{
		return valueOf( getJOM(), json, resultType, imports );
	}

	/**
	 * @param om the {@link ObjectMapper} used to parse/deserialize/unmarshal
	 * @param json the JSON formatted {@link String} value
	 * @param resultType the type of result {@link Object}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	public static <T> T valueOf( final ObjectMapper om, final String json,
		final Class<T> resultType, final Properties... imports )
	{
		try
		{
			return json == null || json.equalsIgnoreCase( "null" ) ? null
					: (T) om.readValue(
							!json.startsWith( "\"" )
									&& resultType == String.class
											? "\"" + json + "\"" : json,
							checkRegistered( om, resultType, imports ) );
		} catch( final Throwable e )
		{
			Thrower.rethrowUnchecked( e );
			return null;
		}
	}

	/**
	 * @param tree the partially parsed JSON {@link TreeNode}
	 * @param resultType the type of result {@link Object}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	public static <T> T valueOf( final TreeNode tree, final Class<T> resultType,
		final Properties... imports )
	{
		return valueOf( getJOM(), tree, resultType, imports );
	}

	/**
	 * @param om the {@link ObjectMapper} used to parse/deserialize/unmarshal
	 * @param tree the partially parsed JSON {@link TreeNode}
	 * @param resultType the type of result {@link Object}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	public static <T> T valueOf( final ObjectMapper om, final TreeNode tree,
		final Class<T> resultType, final Properties... imports )
	{
		// TODO add work-around for Wrapper sub-types?
		if( resultType.isMemberClass()
				&& !Modifier.isStatic( resultType.getModifiers() ) )
			Thrower.throwNew( IllegalArgumentException.class,
					"Unable to deserialize non-static member: {}", resultType );

		try
		{
			return tree == null ? null
					: (T) om.treeToValue( tree,
							checkRegistered( om, resultType, imports ) );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param json the JSON formatted {@link String} value
	 * @param typeReference the result type {@link TypeReference}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	public static <T> T valueOf( final String json,
		final TypeReference<T> typeReference, final Properties... imports )
	{
		return valueOf( getJOM(), json, typeReference, imports );
	}

	/**
	 * @param om the {@link ObjectMapper} used to parse/deserialize/unmarshal
	 * @param json the JSON formatted {@link String} value
	 * @param typeReference the result type {@link TypeReference}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T valueOf( final ObjectMapper om, final String json,
		final TypeReference<T> typeReference, final Properties... imports )
	{
		try
		{
			final Class<?> rawType = om.getTypeFactory()
					.constructType( typeReference ).getRawClass();
			checkRegistered( om, rawType, imports );
			return json == null ? null
					: (T) om.readValue( json, typeReference );

		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
			return null;
		}
	}

	public static class JsonPropertyEditor<E> extends PropertyEditorSupport
	{
		/** */
		private Class<E> type;

		@SuppressWarnings( { "unchecked" } )
		private JsonPropertyEditor()
		{
			this.type = (Class<E>) TypeArguments
					.of( JsonPropertyEditor.class, getClass() ).get( 0 );
		}

		@Override
		public void setAsText( final String json )
			throws IllegalArgumentException
		{
			try
			{
				setValue( JsonUtil.valueOf( json, this.type ) );
			} catch( final Throwable e )
			{
				throw new IllegalArgumentException(
						"Problem editing property of type: "
								+ this.type.getName() + " from JSON value: "
								+ json,
						e );
			}
		}
	}

	/**
	 * cache of registered {@link Wrapper} or {@link DynaBean} types per
	 * {@link ObjectMapper}'s {@link #hashCode()}
	 */
	public static final Map<ObjectMapper, Set<Class<?>>> JSON_REGISTRATION_CACHE = new WeakHashMap<>();

	/**
	 * @param om the {@link ObjectMapper} used to parse/deserialize/unmarshal
	 * @param type the {@link Class} to register
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static <T> Class<T> checkRegistered( final ObjectMapper om,
		final Class<T> type, final Properties... imports )
	{
		synchronized( JSON_REGISTRATION_CACHE )
		{
			Set<Class<?>> cache = JSON_REGISTRATION_CACHE.get( om );
			if( cache == null )
			{
				cache = new HashSet<>();
				JSON_REGISTRATION_CACHE.put( om, cache );
			}
			if( cache.contains( type ) ) return type;

			// use Class.forName(String) ?
			// see http://stackoverflow.com/a/9130560

//			LOG.trace( "Checking JSON conversion for type: {}", type );
			if( type.isAnnotationPresent( BeanProxy.class ) )
			{
				if( !type.isInterface() )
					Thrower.throwNew( IllegalArgumentException.class,
							"@{} must target an interface, but annotates: {}",
							BeanProxy.class.getSimpleName(), type );

				DynaBean.registerType( om, type, imports );

				for( Method method : type.getDeclaredMethods() )
					if( method.getReturnType() != Void.TYPE
							&& method.getReturnType() != type
							&& !cache.contains( type ) )
					{
						checkRegistered( om, method.getReturnType(), imports );
						cache.add( method.getReturnType() );
					}

				// LOG.trace("Registered Dynabean de/serializer for: " + type);
			} else if( Wrapper.class.isAssignableFrom( type ) )
				// {
				Wrapper.Util.registerType( om,
						(Class<? extends Wrapper>) type );

			// LOG.trace("Registered Wrapper de/serializer for: " + type);
			// } else
			// LOG.trace("Assume default de/serializer for: " + type);

			cache.add( type );

			return type;
		}
	}
}
