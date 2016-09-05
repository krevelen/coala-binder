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

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import io.coala.json.DynaBean.ProxyProvider;
import io.coala.name.Id;
import io.coala.util.Instantiator;
import io.coala.util.TypeArguments;

/**
 * {@link Wrapper} is a tag for decorator types that are (or should be)
 * automatically un/wrapped upon JSON de/serialization
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@JsonInclude( Include.NON_NULL )
//Don't add type info, this forces simple strings to become objects {@class:""}
//@JsonTypeInfo(
//// include = JsonTypeInfo.As.WRAPPER_OBJECT, 
//// defaultImpl = Wrapper.Simple.class
//	use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
//	property = "@class" )
public interface Wrapper<T>
{

	/**
	 * @return the wrapped value
	 */
//	@JsonGetter( "value" )
	T unwrap();

	/**
	 * @param value the value to wrap
	 */
//	@JsonSetter( "value" )
	Wrapper<T> wrap( T value );

	/**
	 * {@linkplain JavaPolymorph} indicates that a certain {@linkplain Wrapper}
	 * sub-type can be deserialized (using alternate sub-types of the default
	 * wrapped value type, applying respective {@linkplain JsonSerializer}s and
	 * {@linkplain JsonDeserializer}s) from various JSON value types (number,
	 * string, object, or boolean).
	 * <p>
	 * For instance, a {@code MyJsonWrapper} wraps a {@link Number} for its
	 * values, and is annotated as {@linkplain #objectAs()
	 * JsonPolymorphic(objectType=MyNumber.class)} to indicate JSON object type
	 * values must be deserialized as custom defined {@code MyNumber} instances
	 * (which also extend the default {@link Number} value type)
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.TYPE )
	@interface JavaPolymorph
	{
		/**
		 * @return the value sub-type to parse in case of a
		 *         {@link JsonNodeType#NUMBER} or
		 *         {@link JsonToken#VALUE_NUMBER_INT} or
		 *         {@link JsonToken#VALUE_NUMBER_FLOAT}
		 */
		Class<?> numberAs() default Empty.class;

		/**
		 * @return the value sub-type to parse in case of a
		 *         {@link JsonNodeType#STRING} or {@link JsonToken#VALUE_STRING}
		 */
		Class<?> stringAs() default Empty.class;

		/**
		 * @return the value sub-type to parse in case of a
		 *         {@link JsonNodeType#OBJECT} or {@link JsonToken#START_OBJECT}
		 */
		Class<?> objectAs() default Empty.class;

		/**
		 * @return the value sub-type to parse in case of a
		 *         {@link JsonNodeType#BOOLEAN} or {@link JsonToken#VALUE_TRUE}
		 *         or {@link JsonToken#VALUE_FALSE}
		 */
		Class<?> booleanAs() default Empty.class;

		/**
		 * {@link Empty}
		 * 
		 * @version $Id$
		 * @author Rick van Krevelen
		 */
		class Empty
		{

		}
	}

	/**
	 * {@link Simple} implements a {@link Wrapper} with some basic redirection
	 * to the wrapped {@link Object}'s {@link #hashCode()},
	 * {@link #equals(Object)}, and {@link #toString()} methods
	 *
	 * @param <T> the type of wrapped objects
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Simple<T> implements Wrapper<T>
	{

		/** */
		private T value = null;

		/**
		 * @param value the new value to wrap
		 */
		@Override
//		@JsonSetter( "value" )
		public Wrapper<T> wrap( final T value )
		{
			this.value = value;
			return this;
		}

		/** @return the wrapped value */
		@Override
		public T unwrap()
		{
			return this.value;
		}

		@Override
		public int hashCode()
		{
			return Util.hashCode( this );
		}

		@Override
		public boolean equals( final Object that )
		{
			return Util.equals( this, that );
		}

		@Override
		public String toString()
		{
			return Util.toString( this );
		}
	}

	/**
	 * {@link SimpleOrdinal} extends the {@link Simple} implementation with
	 * redirection for wrapped {@link Comparable} object's
	 * {@link #compareTo(Object)} method
	 *
	 * @param <T> the concrete {@link Comparable} type of wrapped objects
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( { "rawtypes" } )
	interface Ordinal<T extends Comparable>
		extends Wrapper<T>, Comparable<Comparable>
	{

	}

	/**
	 * {@link SimpleOrdinal} extends the {@link Simple} implementation with
	 * redirection for wrapped {@link Comparable} object's
	 * {@link #compareTo(Object)} method
	 *
	 * @param <T> the concrete {@link Comparable} type of wrapped objects
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( { "rawtypes" } )
	class SimpleOrdinal<T extends Comparable> extends Simple<T>
		implements Ordinal<T>
	{
		@Override
		public int compareTo( final Comparable that )
		{
			return Util.compare( this, that );
		}
	}

	/**
	 * {@link Util} provides global utility functions
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Util
	{

		/** */
		private static final Logger LOG = LogManager.getLogger( Util.class );

		/** singleton constructor */
		private Util()
		{
			// singleton
		}

		/** cache of type arguments for known {@link Id} sub-types */
		public static final Map<Class<?>, List<Class<?>>> WRAPPER_TYPE_ARGUMENT_CACHE = new WeakHashMap<>();

		/**
		 * @param om the {@link ObjectMapper} to register with
		 * @param type the {@link Wrapper} sub-type to register
		 */
		public static <S, T extends Wrapper<S>> void
			registerType( final ObjectMapper om, final Class<T> type )
		{
			// LOG.trace("Resolving value type arg for: " + type.getName());
			@SuppressWarnings( "unchecked" )
			final Class<S> valueType = (Class<S>) TypeArguments
					.of( Wrapper.class, type ).get( 0 );
			// LOG.trace("Resolved value type arg: " + valueType);
			registerValueType( om, type, valueType );
		}

		/**
		 * @param om the {@link ObjectMapper} to register with
		 * @param type the {@link Wrapper} sub-type to register
		 * @param valueType the wrapped type to de/serialize
		 */
		public static <S, T extends Wrapper<S>> void registerValueType(
			final ObjectMapper om, final Class<T> type,
			final Class<S> valueType )
		{
			om.registerModule( new SimpleModule()
					.addSerializer( type,
							createJsonSerializer( type, valueType ) )
					.addDeserializer( type,
							createJsonDeserializer( type, valueType ) )
					.addKeyDeserializer( type,
							createJsonKeyDeserializer( type, valueType ) ) );
		}

		/**
		 * @param type the wrapper type to serialize
		 * @param valueType the wrapped type to serialize
		 * @return the {@link JsonSerializer}
		 */
		public static final <S, T extends Wrapper<S>> JsonSerializer<T>
			createJsonSerializer( final Class<T> type,
				final Class<S> valueType )
		{
			return new JsonSerializer<T>()
			{
				@Override
				public void serialize( final T value, final JsonGenerator jgen,
					final SerializerProvider serializers )
					throws IOException, JsonProcessingException
				{
//					LOG.trace( "Finding serializer for {} value: {} ({})", type,
//							value.unwrap(), value.unwrap().getClass() );
					if( value == null || value.unwrap() == null )
						jgen.writeNull();
					else if( value.unwrap().getClass() == String.class )
						jgen.writeString( (String) value.unwrap() );
					else if( value.unwrap() instanceof Number )
						jgen.writeNumber( value.unwrap() instanceof BigDecimal
								? (BigDecimal) value.unwrap()
								: BigDecimal.valueOf( ((Number) value.unwrap())
										.doubleValue() ) );
					else
						serializers.findValueSerializer( valueType, null )
								.serialize( value.unwrap(), jgen, serializers );
				}

				@Override
				public void serializeWithType( final T value,
					final JsonGenerator jgen,
					final SerializerProvider serializers,
					final TypeSerializer typeSer ) throws IOException
				{
					if( value.unwrap() == null )
						jgen.writeNull();
					else
						serializers
								.findValueSerializer( value.unwrap().getClass(),
										null )
								.serialize( value.unwrap(), jgen, serializers );
				}
			};
		}

		/**
		 * @param type the wrapper type to deserialize
		 * @param valueType the wrapped type to deserialize
		 * @return the {@link JsonDeserializer}
		 */
		public static final <S, T extends Wrapper<S>> JsonDeserializer<T>
			createJsonDeserializer( final Class<T> type,
				final Class<S> valueType )
		{
			return new JsonDeserializer<T>()
			{
				private final Instantiator<T> provider = Instantiator
						.of( type );

				@Override
				public T deserializeWithType( final JsonParser jp,
					final DeserializationContext ctxt,
					final TypeDeserializer typeDeserializer )
					throws IOException, JsonProcessingException
				{
					return deserialize( jp, ctxt );
				}

				@Override
				public T deserialize( final JsonParser jp,
					final DeserializationContext ctxt )
					throws IOException, JsonProcessingException
				{
//					LOG.trace( "parsing {} as {}", jp.getText(),
//							type.getName() );

					if( jp.getText() == null || jp.getText().length() == 0
							|| jp.getText().equalsIgnoreCase( "null" ) )
						return null;

					final JavaPolymorph annot = type
							.getAnnotation( JavaPolymorph.class );

					final S value; // = jp.readValueAs(valueType)

					if( annot == null )
					{
//						LOG.trace( "parsing {} as {}", jp.getText(),
//								type.getName() );
						value = jp.readValueAs( valueType );
					} else
					{
						final Class<? extends S> valueSubtype = resolveAnnotType(
								annot, valueType, jp.getCurrentToken() );
						// LOG.trace("parsing " + jp.getCurrentToken() + " ("
						// + jp.getText() + ") as "
						// + valueSubtype.getName());
						value = jp.readValueAs( valueSubtype );

						// final JsonNode tree = jp.readValueAsTree();
						// final Class<? extends S> valueSubtype =
						// resolveSubtype(
						// annot, valueType, tree.getNodeType());
						// LOG.trace("parsing " + tree.getNodeType() + " as "
						// + valueSubtype.getName());
						// value = JsonUtil.getJOM().treeToValue(tree,
						// valueSubtype);
					}

					final T result = this.provider.instantiate();
					result.wrap( value );
					return result;
				}
			};
		}

		/**
		 * @param type the wrapper type to deserialize
		 * @param valueType the wrapped type to deserialize
		 * @return the {@link JsonDeserializer}
		 */
		public static final <S, T extends Wrapper<S>> KeyDeserializer
			createJsonKeyDeserializer( final Class<T> type,
				final Class<S> valueType )
		{
			return new KeyDeserializer()
			{
				private final Instantiator<T> provider = Instantiator
						.of( type );

				@Override
				public Object deserializeKey( final String key,
					final DeserializationContext ctxt )
					throws IOException, JsonProcessingException
				{
					if( key == null || key.length() == 0
							|| key.equalsIgnoreCase( "null" ) )
						return null;

					// FIXME assumes a Wrapper<String> for now
					@SuppressWarnings( "unchecked" )
					final S value = (S) key;
					final T result = this.provider.instantiate();
					result.wrap( value );
					return result;
				}
			};
		}

		/**
		 * @param json the JSON representation {@link String}
		 * @return the deserialized {@link Wrapper} sub-type
		 */
		@SuppressWarnings( "unchecked" )
		@Deprecated
		public static <S, T extends Wrapper<S>> T valueOf( final String json )
		{
			/*
			 * try { final Method method =
			 * Util.class.getDeclaredMethod("valueOf", String.class);
			 * // @SuppressWarnings("unchecked") final ParameterizedType type =
			 * (ParameterizedType) ((TypeVariable<?>) method
			 * .getGenericReturnType()).getBounds()[0];
			 * 
			 * @SuppressWarnings("unchecked") final Class<T> beanType =
			 * (Class<T>) type.getRawType(); LOG.trace(
			 * "Resolved run-time return type to: " + type); return
			 * valueOf(json, beanType); } catch (final Exception e) {
			 * e.printStackTrace(); throw ExceptionBuilder.unchecked(
			 * "Problem determining return type for this method", e) .build(); }
			 */

			// FIXME assumes Wrapper<String> for now, determine actual @class
			return (T) JsonUtil.valueOf( json,
					new TypeReference<Wrapper.SimpleOrdinal<String>>()
					{
					} );
		}

//		private static final Map<Class<?>, Provider<?>> DYNABEAN_PROVIDER_CACHE = new HashMap<>();

		/**
		 * @param json the JSON representation {@link String}
		 * @param type the type of {@link Wrapper} to generate
		 * @return the deserialized {@link Wrapper} sub-type
		 */
		public static <S, T extends Wrapper<S>> T valueOf( final String json,
			final Class<T> type )
		{
			if( type.isInterface() )
				return valueOf( json, ProxyProvider.of( type ).get() );
			return valueOf( json, Instantiator.of( type ) );
		}

		/**
		 * @param json the JSON representation {@link String}
		 * @param provider a {@link Provider} of (empty) wrapper instances
		 * @return the deserialized {@link Wrapper} sub-type
		 */
		public static <S, T extends Wrapper<S>> T valueOf( final String json,
			final Instantiator<T> provider )
		{
			return valueOf( json, provider.instantiate() );
		}

		/**
		 * @param json the JSON representation {@link String}
		 * @param result a {@link Wrapper} to (re)use
		 * @return the deserialized {@link Wrapper} sub-type
		 */
		@SuppressWarnings( "unchecked" )
		public static <S, T extends Wrapper<S>> T valueOf( final String json,
			final T result )
		{
			final Class<S> valueType = (Class<S>) TypeArguments
					.of( Wrapper.class, result.getClass(),
							Wrapper.Util.WRAPPER_TYPE_ARGUMENT_CACHE )
					.get( 0 );

			final JavaPolymorph annot = result.getClass()
					.getAnnotation( JavaPolymorph.class );

			final S value;
			if( annot == null )
			{
				final Class<?> type = valueType != Object.class ? valueType
						: json.startsWith( "\"" ) ? String.class
								: json.equalsIgnoreCase(
										Boolean.TRUE.toString() )
										|| json.equalsIgnoreCase(
												Boolean.FALSE.toString() )
														? Boolean.class
														: BigDecimal.class;
//				LOG.trace( "{}->{}->? ({})", json, valueType, type );
				value = valueType == String.class && !json.startsWith( "\"" )
						? (S) json : (S) JsonUtil.valueOf( json, type );
//				LOG.trace( "{}->{}->{} ({})", json, valueType, value, type );
			} else
			{
				final JsonNode tree = JsonUtil.toTree( json );
				final Class<? extends S> annotType = resolveAnnotType( annot,
						valueType, tree.getNodeType() );
				value = JsonUtil.valueOf( json, annotType );
			}
			return of( value, result );
		}

		/**
		 * @param value the wrapped value
		 * @param result the {@link Wrapper} object to (re)use
		 * @return the updated {@link Wrapper} object
		 */
		public static <S, T extends Wrapper<S>> T of( final S value,
			final Class<T> type )
		{
			return of( value, Instantiator.instantiate( type ) );
		}

		/**
		 * @param value the wrapped value
		 * @param wrapper the {@link Wrapper} object to (re)use
		 * @return the updated {@link Wrapper} object
		 */
		@SuppressWarnings( "unchecked" )
		public static <S, T extends Wrapper<S>> T of( final S value,
			final T wrapper )
		{
			return (T) wrapper.wrap( value );
		}

		/**
		 * @param self the {@link Wrapper} object to provide the {@link String}
		 *            representation of
		 * @return the {@code #toString()} value of wrapped object or
		 *         {@code null} if unavailable
		 */
		@SuppressWarnings( "rawtypes" )
		public static String toString( final Wrapper self )
		{
			return self == null || self.unwrap() == null ? null
					: self.unwrap().toString();
		}

		/**
		 * @param self the {@link Wrapper} object to determine a hash code for
		 * @return the hash code of wrapped object and {@link Wrapper} type
		 */
		@SuppressWarnings( "rawtypes" )
		public static int hashCode( final Wrapper self )
		{
			return self == null ? 0
					: self.getClass().hashCode() + (self.unwrap() == null ? 0
							: self.unwrap().hashCode() * 31);
		}

		/**
		 * In this implementation, {@code null} comes before non-{@code null},
		 * otherwise same as {@link Comparator#compare(Object, Object)}
		 * 
		 * @param self the self
		 * @param other the other
		 * @return the order of the other compared to self (<0, 0, or >0)
		 * @see Comparator#compare(Object, Object)
		 */
		@SuppressWarnings( { "rawtypes", "unchecked" } )
		public static int compare( final Comparable self,
			final Comparable other )
		{
			if( self == null ) return other == null ? 0 : -1;
			if( other == null ) return 1;
			if( !(self instanceof Wrapper) )
				return other instanceof Wrapper
						? compare( self,
								(Comparable) ((Wrapper) other).unwrap() )
						: self.compareTo( other );
			if( !(other instanceof Wrapper) )
				return compare( (Comparable) ((Wrapper) self).unwrap(), other );
			return compare( (Comparable) ((Wrapper) self).unwrap(),
					(Comparable) ((Wrapper) other).unwrap() );
		}

		/**
		 * @param self the visited {@link Wrapper} object
		 * @param obj the other {@link Object} to test equality
		 * @return {@code true} iff equal in both runtime type (
		 *         {@link #getClass()}) and wrapped value ({@link #equals()})
		 */
		@SuppressWarnings( "rawtypes" )
		public static <T> boolean equals( final Wrapper self, final Object obj )
		{
			if( obj == null || self.getClass() != obj.getClass() ) return false;
			if( self == obj ) return true;
			final Wrapper other = self.getClass().cast( obj );
			return self.unwrap() == null ? other.unwrap() == null
					: self.unwrap().equals( other.unwrap() );
		}

		/**
		 * @param annot the {@link JavaPolymorph} annotated values
		 * @param valueType the wrapped type
		 * @param jsonToken the {@link JsonToken} being parsed
		 * @return the corresponding {@link Wrapper} sub-type to generate
		 */
		public static <S, T extends Wrapper<S>> Class<? extends S>
			resolveAnnotType( final JavaPolymorph annot,
				final Class<S> valueType, final JsonToken jsonToken )
		{
			final Class<?> result;
			switch( jsonToken )
			{
			case VALUE_TRUE:
			case VALUE_FALSE:
				result = annot.booleanAs();
				break;
			case VALUE_NUMBER_INT:
			case VALUE_NUMBER_FLOAT:
				result = annot.numberAs();
				break;
			case START_OBJECT:
			case VALUE_EMBEDDED_OBJECT:
				result = annot.objectAs();
				break;
			case VALUE_STRING:
				result = annot.stringAs();
				break;
			default:
				return valueType;
			}

			if( result == null || result == JavaPolymorph.Empty.class )
				return valueType;

			if( !valueType.isAssignableFrom( result ) )
			{
				LOG.warn( JavaPolymorph.class.getSimpleName()
						+ " annotation contains illegal value: "
						+ result.getName() + " does not extend/implement "
						+ valueType.getName() );
				return valueType;
			}

			return result.asSubclass( valueType );
		}

		/**
		 * @param annot the {@link JavaPolymorph} annotated values
		 * @param valueType the wrapped type
		 * @param jsonNodeType the {@link JsonNodeType} being parsed
		 * @return the corresponding {@link Wrapper} sub-type to generate
		 */
		public static <S, T extends Wrapper<S>> Class<? extends S>
			resolveAnnotType( final JavaPolymorph annot,
				final Class<S> valueType, final JsonNodeType jsonNodeType )
		{
			final Class<?> result;
			switch( jsonNodeType )
			{
			case BOOLEAN:
				result = annot.booleanAs();
				break;
			case NUMBER:
				result = annot.numberAs();
				break;
			case OBJECT:
				result = annot.objectAs();
				break;
			case POJO:
				result = annot.objectAs();
				break;
			case STRING:
				result = annot.stringAs();
				break;
			default:
				return valueType;
			}

			if( result == null || result == JavaPolymorph.Empty.class )
				return valueType;

			if( !valueType.isAssignableFrom( result ) )
			{
				LOG.warn( JavaPolymorph.class.getSimpleName()
						+ " annotation contains illegal value: "
						+ result.getName() + " does not extend/implement "
						+ valueType.getName() );
				return valueType;
			}

			return result.asSubclass( valueType );
		}
	}
}
