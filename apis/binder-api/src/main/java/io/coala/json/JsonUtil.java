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
 * 
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.json;

import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.coala.exception.CoalaExceptionFactory;
import io.coala.util.Util;

/**
 * {@link JsonUtil}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class JsonUtil implements Util
{

	/** */
	// private static final Logger LOG = Logger.getLogger(JsonUtil.class);

	/** */
	private static final ObjectMapper JOM = new ObjectMapper();

	static
	{

		// .setVisibility(PropertyAccessor.FIELD,
		// Visibility.PROTECTED_AND_PUBLIC)

		// JOM.registerModule(new JsonSimTimeModule());

		// LOG.trace("Created JSON object mapper version: " +
		// JOM.version());

		/*JOM.registerModule(new SimpleModule()
		{
			{
				addSerializer(UUID.class, new JsonSerializer<UUID>()
				{
					@Override
					public void serialize(final UUID value,
							final JsonGenerator gen,
							final SerializerProvider serializers)
									throws IOException, JsonProcessingException
					{
						// TODO optimize as in Jackson's UUIDSerializer
						gen.writeString(value.toString());
					}

					@Override
					public Class<UUID> handledType()
					{
						return UUID.class;
					}
				});
				addDeserializer(UUID.class, new JsonDeserializer<UUID>()
				{
					@Override
					public UUID deserialize(final JsonParser p,
							final DeserializationContext ctxt)
									throws IOException, JsonProcessingException
					{
						return new UUID(p.getValueAsString());
					}
				});
			}
		});*/
	}

	/** singleton design pattern constructor */
	private JsonUtil()
	{
		// singleton design pattern
	}

	/** */
	public synchronized static ObjectMapper getJOM()
	{
/*		if (JOM == null)
		{
			// JOM = new ObjectMapper()

			// for marshalling private and protected fields
			;
		}
*/
		return JOM;
	}

	/**
	 * @param value
	 * @return
	 */
	public static String toString(final Object value)
	{
		try
		{
			return JsonUtil.getJOM().writeValueAsString(value);
		} catch (final JsonProcessingException e)
		{
			throw CoalaExceptionFactory.MARSHAL_FAILED.createRuntime(e,
					value.getClass(), value.getClass());
		}
	}

	/**
	 * @param object
	 * @return
	 */
	public static String toPrettyJSON(final Object object)
	{
		try
		{
			return JsonUtil.getJOM()
					.setSerializationInclusion(JsonInclude.Include.NON_NULL)
					.writer().withDefaultPrettyPrinter()
					.writeValueAsString(object);
		} catch (final JsonProcessingException e)
		{
			throw CoalaExceptionFactory.MARSHAL_FAILED.createRuntime(e, object,
					object.getClass());
		}
	}

	/**
	 * @param json the {@link InputStream}
	 * @param resultType the result type {@link T}
	 * @return the unmarshalled {@link T} instance
	 */
	public static <T> T valueOf(final InputStream json,
			final Class<T> resultType)
	{
		try
		{
			return (T) JsonUtil.getJOM().readValue(json, resultType);
		} catch (final Exception e)
		{
			throw CoalaExceptionFactory.UNMARSHAL_FAILED.createRuntime(e, json,
					resultType);
		}
	}

	/**
	 * @param json
	 * @param resultType the result type {@link T}
	 * @return the unmarshalled {@link T} instance
	 */
	public static <T> T valueOf(final String json, final Class<T> resultType)
	{
		try
		{
			return (T) getJOM().readValue(json, resultType);
		} catch (final Exception e)
		{
			throw CoalaExceptionFactory.UNMARSHAL_FAILED.createRuntime(e, json,
					resultType);
		}
	}

	/**
	 * @param json
	 * @return
	 */
	public static JsonNode toTree(final String json)
	{
		try
		{
			return getJOM().readTree(json);
		} catch (final Exception e)
		{
			throw CoalaExceptionFactory.UNMARSHAL_FAILED.createRuntime(e, json,
					JsonNode.class);
		}
	}

	/**
	 * @param object
	 * @return
	 */
	public static JsonNode toTree(final Object object)
	{
		return getJOM().valueToTree(object);
	}

	/**
	 * @param stream
	 * @return
	 */
	public static JsonNode toTree(final InputStream stream)
	{
		try
		{
			return getJOM().readTree(stream);
		} catch (final Exception e)
		{
			throw CoalaExceptionFactory.UNMARSHAL_FAILED.createRuntime(e,
					stream == null ? null : stream.getClass(), JsonNode.class);
		}
	}

	/**
	 * @param value
	 * @return
	 * @deprecated use {@link #toTree(Object)}
	 */
	@Deprecated
	public static JsonNode toJSON(final Object value)
	{
		return toTree(value);
	}

	/**
	 * @param value
	 * @return
	 * @deprecated use {@link #toString(Object)}
	 */
	@Deprecated
	public static String toJSONString(final Object value)
	{
		return toString(value);
	}

	/**
	 * @param json
	 * @return
	 * @deprecated use {@link #toTree(InputStream)}
	 */
	@Deprecated
	public static JsonNode fromJSON(final InputStream json)
	{
		return toTree(json);
	}

	/**
	 * @param string
	 * @return
	 * @deprecated use {@link #toTree(String)}
	 */
	@Deprecated
	public static JsonNode fromJSON(final String json)
	{
		return toTree(json);
	}

	/**
	 * @param json the {@link InputStream}
	 * @param resultType the result type {@link T}
	 * @return the unmarshalled {@link T} instance
	 * @deprecated use {@link #valueOf(InputStream,Class)}
	 */
	@Deprecated
	public static <T> T fromJSON(final InputStream json,
			final Class<T> resultType)
	{
		return valueOf(json, resultType);
	}

	/**
	 * @param json
	 * @param resultType the result type {@link T}
	 * @return the unmarshalled {@link T} instance
	 * @deprecated use {@link #valueOf(String,Class)}
	 */
	@Deprecated
	public static <T> T fromJSONString(final String json,
			final Class<T> resultType)
	{
		return valueOf(json, resultType);
	}

}
