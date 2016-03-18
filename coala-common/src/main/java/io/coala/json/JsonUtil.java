/* $Id: 9a68e7e3b4196e0ec0aead8bcad855d27c75dedf $
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
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.coala.exception.ExceptionBuilder;
import io.coala.json.DynaBean.BeanProxy;
import io.coala.log.LogUtil;
import io.coala.util.TypeArguments;

/**
 * {@link JsonUtil}
 * 
 * @version $Id: 9a68e7e3b4196e0ec0aead8bcad855d27c75dedf $
 * @author Rick van Krevelen
 */
public class JsonUtil
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( JsonUtil.class );

	/** */
	private static final ObjectMapper JOM = new ObjectMapper();

	/** singleton design pattern constructor */
	private JsonUtil()
	{
		// singleton design pattern
		LOG.trace( "Using jackson v: " + JOM.version() );
		JOM.registerModule( new JodaModule() );
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
			throw ExceptionBuilder
					.unchecked( e, "Problem JSONifying rawtype: %s",
							object == null ? null : object.getClass() )
					.build();
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
			throw ExceptionBuilder.unchecked( "Problem JSONifying", e ).build();
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
			// return om.valueToTree(object);
			return om.readTree( stringify( object ) );
		} catch( final Exception e )
		{
			throw ExceptionBuilder
					.unchecked( e,
							"Problem serializing "
									+ object.getClass().getSimpleName() )
					.build();
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
			throw ExceptionBuilder.unchecked( "Problem unmarshalling", e )
					.build();
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
			throw ExceptionBuilder
					.unchecked( "Problem unmarshalling JSON: " + json, e )
					.build();
		}
	}

	@Deprecated
	public static String toPrettyJSON( final Object object )
	{
		return toJSON( object );
	}

	@Deprecated
	public static String toString( final Object value )
	{
		return toJSON( value );
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
			throw ExceptionBuilder.unchecked( "Problem unmarshalling "
					+ resultType.getName() + " from JSON stream", e ).build();
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
			return json == null || json.isEmpty() ? null
					: (T) om.readValue( json,
							checkRegistered( om, resultType, imports ) );
		} catch( final Exception e )
		{
			throw ExceptionBuilder.unchecked( "Problem unmarshalling "
					+ resultType.getName() + " from JSON: " + json, e ).build();
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
		try
		{
			return tree == null ? null
					: (T) om.treeToValue( tree,
							checkRegistered( om, resultType, imports ) );
		} catch( final Exception e )
		{
			throw ExceptionBuilder.unchecked( "Problem unmarshalling "
					+ resultType.getName() + " from JSON: " + tree, e ).build();
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
			throw ExceptionBuilder.unchecked( "Problem unmarshalling "
					+ typeReference + " from JSON: " + json, e ).build();
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

			if( type.isAnnotationPresent( BeanProxy.class ) )
			{
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
