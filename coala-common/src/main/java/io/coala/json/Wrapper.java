/* $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.coala.exception.Thrower;
import io.coala.name.Id;
import io.coala.util.Instantiator;
import io.coala.util.TypeArguments;

/**
 * {@link Wrapper} is a tag for decorator types that are (or should be)
 * automatically un/wrapped upon JSON de/serialization
 * 
 * @version $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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
	String WRAP_PROPERTY = "wrap";

	String UNWRAP_PROPERTY = "unwrap";

	/**
	 * @return the wrapped value
	 */
//	@JsonProperty( "value" )
//	@JsonValue
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
	 * @version $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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
		Class<?> numberAs()

		default Empty.class;

		/**
		 * @return the value sub-type to parse in case of a
		 *         {@link JsonNodeType#STRING} or {@link JsonToken#VALUE_STRING}
		 */
		Class<?> stringAs()

		default Empty.class;

		/**
		 * @return the value sub-type to parse in case of a
		 *         {@link JsonNodeType#OBJECT} or {@link JsonToken#START_OBJECT}
		 */
		Class<?> objectAs()

		default Empty.class;

		/**
		 * @return the value sub-type to parse in case of a
		 *         {@link JsonNodeType#BOOLEAN} or {@link JsonToken#VALUE_TRUE}
		 *         or {@link JsonToken#VALUE_FALSE}
		 */
		Class<?> booleanAs() default Empty.class;

		/**
		 * {@link Empty}
		 * 
		 * @version $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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
	 * @version $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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
	 * @version $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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
	 * @version $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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

	@SuppressWarnings( "serial" )
	class JsonSerializer//<S, T extends Wrapper<S>>
		extends StdSerializer<Wrapper<?>>
	{
		private final JavaType valueType;

		public JsonSerializer( final ObjectMapper om,
			final Class<? extends Wrapper<?>> wrapperType )
		{
			this( om.getTypeFactory().constructType( wrapperType ) );
		}

		public JsonSerializer( final JavaType wrapperType )
		{
			super( wrapperType );
			this.valueType = Util.resolveValueType( wrapperType );
		}

//		@SuppressWarnings( "unchecked" )
		@Override
		public void serialize( final Wrapper<?> wrapper,
			final JsonGenerator jgen, final SerializerProvider provider )
			throws IOException, JsonProcessingException
		{
			final Object value = wrapper == null ? null : wrapper.unwrap();
			if( value == null )
				jgen.writeNull();
//			else if( value.getClass() == String.class )
//				jgen.writeString( (String) value ); // avoid extra quotes
//			else if( value instanceof Number )
//				jgen.writeNumber(
//						value instanceof BigDecimal ? (BigDecimal) value
//								: BigDecimal.valueOf(
//										((Number) value).doubleValue() ) );
			else
			{
//				final com.fasterxml.jackson.databind.JsonSerializer<?> ser = provider
//						.findValueSerializer( this.valueType );
//				if( ser instanceof MapSerializer )
//				{
//					final com.fasterxml.jackson.databind.JsonSerializer<?> keySer = ((MapSerializer) ser)
//							.getKeySerializer();
//					if( keySer == null )
//					{
//						final MapType type = (MapType) this.valueType;
//						// use om.writerFor() to handle map types, see http://stackoverflow.com/a/13944325/1418999
//						final String json = ((ObjectMapper) jgen.getCodec())
//								.writerFor( type ).writeValueAsString( value );
//						Util.LOG.trace( "Serialized {} to {}", type, json );
//						return;
//					}
//				}
				final ObjectMapper om = (ObjectMapper) jgen.getCodec();
//				final String json = om.writerFor( this.valueType )
//						.writeValueAsString( value );
//				Util.LOG.trace( "Serializing wrapped {} value: {} -> {}",
//						this.valueType, value, json );
				om.writerFor( this.valueType ).writeValue( jgen, value );
//				((com.fasterxml.jackson.databind.JsonSerializer<? super Object>) ser)
//						.serialize( value, jgen, provider );
			}
		}
	}

	@SuppressWarnings( "serial" )
	class JsonKeySerializer//<S, T extends Wrapper<S>>
		extends StdSerializer<Wrapper<?>>
	{
		private final JavaType valueType;

		public JsonKeySerializer( final ObjectMapper om,
			final Class<? extends Wrapper<?>> wrapperType )
		{
			this( om.getTypeFactory().constructType( wrapperType ) );
		}

		public JsonKeySerializer( final JavaType wrapperType )
		{
			super( wrapperType );
			this.valueType = Util.resolveValueType( wrapperType );
		}

		@Override
		public void serialize( final Wrapper<?> wrapper,
			final JsonGenerator jgen, final SerializerProvider serializers )
			throws IOException, JsonProcessingException
		{
			final Object value = wrapper.unwrap() == null ? null
					: wrapper.unwrap();
			final ObjectMapper om = (ObjectMapper) jgen.getCodec();
			// allow any java type as key, see http://heli0s.darktech.org/jackson-serialize-map-with-non-string-key-in-fact-with-any-serializable-key-and-abstract-classes/
			final String json = om.writerFor( this.valueType )
					.writeValueAsString( value );
//			Util.LOG.trace( "Serialized {} value {} as key field name: {}",
//					wrapper.getClass(), value, json );
			jgen.writeFieldName( json );
		}
	}

	@SuppressWarnings( { "serial", /*"rawtypes",*/ "unchecked" } )
	class JsonDeserializer<S, T extends Wrapper<S>> extends StdDeserializer<T>
	{
		private final Provider<T> wrapperProvider;
		private final JavaType valueType;
		private final JavaPolymorph annot;

//		@SuppressWarnings( "rawtypes" )
		public JsonDeserializer( final ObjectMapper om, final Class<T> type )
		{
			this( Instantiator.providerOf( type ),
					om.getTypeFactory().constructType( type ),
					type.getAnnotation( JavaPolymorph.class ) );
		}

		public JsonDeserializer( final Provider<T> wrapperProvider,
			final JavaType wrapperType, final JavaPolymorph annot )
		{
			super( wrapperType );
			this.wrapperProvider = wrapperProvider;
			this.valueType = Util.resolveValueType( wrapperType );
			this.annot = annot;
		}

		@Override
		public T deserialize( final JsonParser jp,
			final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			final String json = jp.getText();
			if( json == null || json.length() == 0
					|| json.equalsIgnoreCase( "null" ) )
				return null;

			final ObjectMapper om = (ObjectMapper) jp.getCodec();
			final JavaType valueType = this.annot == null ? this.valueType
					: om.getTypeFactory()
							.constructType( Util.resolveAnnotType( this.annot,
									this.valueType.getRawClass(),
									jp.getCurrentToken() ) );
			final Object value = om.readerFor( valueType ).readValue( jp );

			final T result = this.wrapperProvider.get();
			result.wrap( (S) value );
			return result;
		}
	}

//	@SuppressWarnings( "serial" )
	class JsonKeyDeserializer extends KeyDeserializer
	{
		private final ObjectMapper om;
		private final JavaType wrapperType;

		public JsonKeyDeserializer( final ObjectMapper om,
			final Class<? extends Wrapper<?>> type )
		{
			this.om = om;
			this.wrapperType = om.getTypeFactory().constructType( type );
		}

		@Override
		public Wrapper<?> deserializeKey( final String key,
			final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			Util.LOG.trace( "deser field name: {}", key );
			// allow any java type as key, see http://heli0s.darktech.org/jackson-serialize-map-with-non-string-key-in-fact-with-any-serializable-key-and-abstract-classes/
			return this.om.readValue( key, this.wrapperType );
		}
	}

	/**
	 * {@link Util} provides global utility functions
	 * 
	 * @version $Id: c63f5df0731459f556ac07b63b8b818d65a2d35e $
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
			om.registerModule( new SimpleModule()
					.addSerializer( type, new JsonSerializer( om, type ) )
					.addDeserializer( type,
							new JsonDeserializer<S, T>( om, type ) )
					.addKeySerializer( type, new JsonKeySerializer( om, type ) )
					.addKeyDeserializer( type,
							new JsonKeyDeserializer( om, type ) ) );

			// LOG.trace("Resolving value type arg for {}", type);
//			@SuppressWarnings( "unchecked" )
//			final List<Class<?>> typeArgs = TypeArguments.of( Wrapper.class,
//					type );
//			if( typeArgs.size() != 1 )
//				Thrower.throwNew( IllegalArgumentException.class,
//						"Expecting 1 type argument of Wrapper extension {}",
//						type );

//			final Class<S> valueType = (Class<S>) typeArgs.get( 0 );
//			LOG.trace( "Resolved {}'s Wrapper type arg: {}", type.getTypeName(),
//					valueType );
////			if( Map.class.isAssignableFrom( valueType ) )
////				LOG.trace( "Resolved {}'s Map type args: {}", valueType,
////						TypeArguments.of( Map.class,
////								valueType.asSubclass( Map.class ) ) );
//			if( valueType == null )
//				Thrower.throwNew( IllegalArgumentException.class,
//						"Could not determine value type for {}, got: {}", type,
//						valueType );
//			registerValueType( om, type, valueType );
		}

		/**
		 * @param om the {@link ObjectMapper} to register with
		 * @param type the {@link Wrapper} sub-type to register
		 * @param valueType the wrapped type to deserialize
		 */
//		public static <S, T extends Wrapper<S>> void registerValueType(
//			final ObjectMapper om, final Class<T> type,
//			final Class<S> valueType )
//		{
//			Objects.requireNonNull( valueType ); // for deserialization
//			if( valueType.getGenericSuperclass() instanceof ParameterizedType )
//				LOG.trace(
//						"creating serializers for generic parameterized type: "
//								+ valueType.getGenericSuperclass() );
//			else
//				LOG.trace( "creating serializers for type: "
//						+ valueType.getTypeName() );
//			om.registerModule( new SimpleModule()
//					.addSerializer( type, createJsonSerializer( om, type ) )
//					.addDeserializer( type,
//							createJsonDeserializer( type, valueType ) )
//					.addKeySerializer( type, createJsonKeySerializer( type ) )
//					.addKeyDeserializer( type,
//							createJsonKeyDeserializer( type, valueType ) ) );
//		}

//		private static final Map<Class<?>, Provider<?>> DYNABEAN_PROVIDER_CACHE = new HashMap<>();

		@SuppressWarnings( "unchecked" )
		public <S, T extends Wrapper<S>> Class<S> resolveValueType(
			final ObjectMapper om, final Class<T> wrapperType )
		{
			return (Class<S>) resolveValueType(
					om.getTypeFactory().constructType( wrapperType ) )
							.getRawClass();
		}

		public static JavaType resolveValueType( final JavaType wrapperType )
		{
			JavaType valueType = null;
			JavaType superType = wrapperType;
			while( valueType == null
					&& superType.getRawClass() != Object.class )
			{
				for( JavaType intf : superType.getInterfaces() )
					if( intf.getRawClass() == Wrapper.class )
						return intf.containedType( 0 );
//				Util.LOG.trace(
//						"Looked in {} interfaces: {}, on to supertype: {}",
//						superType, superType.getInterfaces(),
//						superType.getSuperClass() );
				superType = superType.getSuperClass();
			}
			return Thrower.throwNew( IllegalStateException::new,
					() -> "Could not resolve Wrapper type argument for "
							+ wrapperType );
		}

		/**
		 * @param json the JSON representation {@link String}
		 * @param type the type of {@link Wrapper} to generate
		 * @return the deserialized {@link Wrapper} sub-type
		 */
		public static <S, T extends Wrapper<S>> T valueOf( final String json,
			final Class<T> type )
		{
			if( type.isInterface() )
				return valueOf( json, DynaBean.ProxyProvider.of( type ).get() );
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
			if( !(self instanceof Wrapper) ) return other instanceof Wrapper
					? compare( self, (Comparable) ((Wrapper) other).unwrap() )
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
		 * @param valueType the wrapped value (super) type
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