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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.aeonbits.owner.util.Collections;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.eaio.UUIDModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.coala.exception.Thrower;
import io.coala.json.DynaBean.BeanProxy;
import io.coala.log.LogUtil;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;

/**
 * {@link JsonUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class JsonUtil
{

	/** */
	private static final ObjectMapper JOM = new ObjectMapper();

	/** singleton design pattern constructor */
	static//private JsonUtil()
	{
		// singleton design pattern
		initialize( JOM );
	}

	/**
	 * @param instance
	 */
	public synchronized static void initialize( final ObjectMapper om )
	{
		om.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );

		final Module[] modules = { new JodaModule(), new UUIDModule(),
				new JavaTimeModule() };
		om.registerModules( modules );

		// Log4j2 may cause recursive call, when initialization is during logging event
//		System.err.println( "Using jackson v: " + om.version() + " with: "
//				+ Arrays.asList( modules ).stream()
//						.map( m -> m.getModuleName() )
//						.collect( Collectors.toList() ) );
	}

	/** */
	public synchronized static ObjectMapper getJOM()
	{
		return JOM;
	}

	/**
	 * @param object the object to serialize/marshal
	 * @return the (minimal) JSON representation
	 * @see #toJSON(Object)
	 */
	public static String stringify( final Object object )
	{
		final ObjectMapper om = getJOM();
		try
		{
			if( REGISTER_ON_SERIALIZE && object != null )
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
	 * @see #stringify(Object)
	 */
	public static String toJSON( final Object object )
	{
		return toJSON( getJOM(), object );
	}

	private static final Boolean REGISTER_ON_SERIALIZE = true;

	/**
	 * @param object the object to serialize/marshal as (pretty) JSON
	 * @return the (pretty) JSON representation
	 */
	public static String toJSON( final ObjectMapper om, final Object object )
	{
		try
		{
			if( REGISTER_ON_SERIALIZE && object != null )
				checkRegistered( om, object.getClass() );
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
			if( REGISTER_ON_SERIALIZE && object != null )
				checkRegistered( om, object.getClass() );
			return om.valueToTree( object );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
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
		if( json == null || json.isEmpty() ) return null;
		try
		{
			return getJOM().readTree( json );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
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
	 * TODO split into common parsing Observable with JsonParser provider
	 * 
	 * @param json the JSON array {@link InputStream}
	 * @param resultType the type of {@link Object} in the JSON array
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	@SafeVarargs
	public static <T> Observable<T> readArrayAsync(
		final Callable<InputStream> json, final Class<T> resultType,
		final Map<String, ?>... imports )
	{
		final ObjectMapper om = getJOM();
		if( REGISTER_ON_SERIALIZE ) checkRegistered( om, resultType, imports );
		return readArrayAsync(
				() -> om.getFactory().createParser( json.call() ),
				() -> om.readerFor(
						om.getTypeFactory().constructType( resultType ) ) );
	}

	/**
	 * @param json the JSON array {@link InputStream}
	 * @param resultType the type of {@link Object} to parse in the JSON array
	 * @return the deserialized {@link Object}s form first array encountered
	 */
	public static <T> Observable<T> readArrayAsync(
		final Callable<JsonParser> jpFactory,
		final Callable<ObjectReader> orFactory )
	{
		// see http://www.cowtowncoder.com/blog/archives/2009/01/entry_132.html
		return Observable.using( jpFactory, jp ->
		{
			final StringBuffer preamble = new StringBuffer();
			// parse whichever array comes first, skip until start-array '['
			while( jp.nextToken() != JsonToken.START_ARRAY )
			{
				if( jp.currentToken() == null ) return Observable
						.error( new IOException( "Missing input" ) );
				preamble.append( jp.getText() );
			}
			if( preamble.length() > 0 ) LogUtil.getLogger( JsonUtil.class )
					.warn( "Ignoring unexpected preamble: {}", preamble );

			return Observable.<T>create( emitter ->
			{
				try
				{

					final ObjectReader or = orFactory.call();

					int i = 0;
					T t = null;
					while( !emitter.isDisposed()
							&& jp.nextToken() != JsonToken.END_ARRAY )
					{
						emitter.onNext( t = or.readValue( jp ) );
						i++;
					}
					emitter.onComplete();
					LogUtil.getLogger( JsonUtil.class ).trace( "Parsed {} x {}",
							i, t == null ? "?" : t.getClass().getSimpleName() );
				} catch( final IOException e )
				{
					throw Exceptions.propagate( e );
				}
			} );
		}, JsonParser::close );
	}

	/**
	 * @param json the {@link InputStream}
	 * @param resultType the type of result {@link Object}
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	@SafeVarargs
	public static <T> T valueOf( final InputStream json,
		final Class<T> resultType, final Map<String, ?>... imports )
	{
		if( json == null ) return null;
		try
		{
			final ObjectMapper om = getJOM();
			return (T) om.readValue( json,
					checkRegistered( om, resultType, imports ) );
		} catch( final Exception e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param json the JSON formatted {@link String} value
	 * @param resultType the type of result {@link Object}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	@SafeVarargs
	public static <T> T valueOf( final String json, final Class<T> resultType,
		final Map<String, ?>... imports )
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
	@SafeVarargs
	public static <T> T valueOf( final ObjectMapper om, final String json,
		final Class<T> resultType, final Map<String, ?>... imports )
	{
		if( json == null || json.equalsIgnoreCase( "null" ) ) return null;
		try
		{
			return (T) om.readValue(
					!json.startsWith( "\"" ) && resultType == String.class
							? "\"" + json + "\"" : json,
					checkRegistered( om, resultType, imports ) );
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * @param tree the partially parsed JSON {@link TreeNode}
	 * @param resultType the type of result {@link Object}
	 * @param imports the {@link Properties} instances for default values, etc.
	 * @return the parsed/deserialized/unmarshalled {@link Object}
	 */
	@SafeVarargs
	public static <T> T valueOf( final TreeNode tree, final Class<T> resultType,
		final Map<String, ?>... imports )
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
	@SafeVarargs
	public static <T> T valueOf( final ObjectMapper om, final TreeNode tree,
		final Class<T> resultType, final Map<String, ?>... imports )
	{
		if( tree == null ) return null;
		// TODO add work-around for Wrapper sub-types?
		if( resultType.isMemberClass()
				&& !Modifier.isStatic( resultType.getModifiers() ) )
			return Thrower.throwNew( IllegalArgumentException::new,
					() -> "Unable to deserialize non-static member: "
							+ resultType );

		try
		{
			return (T) om.treeToValue( tree,
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
	@SafeVarargs
	public static <T> T valueOf( final String json,
		final TypeReference<T> typeReference, final Map<String, ?>... imports )
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
		final TypeReference<T> typeReference, final Map<String, ?>... imports )
	{
		if( json == null ) return null;
		try
		{
			final Class<?> rawType = om.getTypeFactory()
					.constructType( typeReference ).getRawClass();
			checkRegistered( om, rawType, imports );
			return (T) om.readValue( json, typeReference );
		} catch( final Exception e )
		{
			Thrower.rethrowUnchecked( e );
			return null;
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
	@SafeVarargs
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static <T> Class<T> checkRegistered( final ObjectMapper om,
		final Class<T> type, final Map<String, ?>... imports )
	{
		synchronized( JSON_REGISTRATION_CACHE )
		{
			if( type.isPrimitive() ) return type;
			Set<Class<?>> cache = JSON_REGISTRATION_CACHE.computeIfAbsent( om,
					key -> new HashSet<>() );
			if( type.getPackage() == Object.class.getPackage()
					|| type.getPackage() == Collection.class.getPackage()
					// assume java.lang.* and java.util.* are already mapped
					|| TreeNode.class.isAssignableFrom( type )
					|| cache.contains( type ) )
				return type;

			// use Class.forName(String) ?
			// see http://stackoverflow.com/a/9130560

//			LOG.trace( "Register JSON conversion of type: {}", type	 );
			if( type.isAnnotationPresent( BeanProxy.class ) )
			{
//				if( !type.isInterface() )
//					return Thrower.throwNew( IllegalArgumentException.class,
//							"@{} must target an interface, but annotates: {}",
//							BeanProxy.class.getSimpleName(), type );

				DynaBean.registerType( om, type, imports );
				checkRegisteredMembers( om, type, imports );
				// LOG.trace("Registered Dynabean de/serializer for: " + type);
			} else if( Wrapper.class.isAssignableFrom( type ) )
			{
				Wrapper.Util.registerType( om,
						(Class<? extends Wrapper>) type );
				checkRegisteredMembers( om, type, imports );
				// LOG.trace("Registered Wrapper de/serializer for: " + type);
			}
			// else
			// LOG.trace("Assume default de/serializer for: " + type);

			cache.add( type );

			return type;
		}
	}

	@SafeVarargs
	public static <T> Class<T> checkRegisteredMembers( final ObjectMapper om,
		final Class<T> type, final Map<String, ?>... imports )
	{
		for( Method method : type.getDeclaredMethods() )
			if( method.getParameterCount() == 0
					&& method.getReturnType() != Void.TYPE
					&& method.getReturnType() != type )
			{
//				LOG.trace(
//						"Checking {}#{}(..) return type: {} @JsonProperty={}",
//						type.getSimpleName(), method.getName(),
//						method.getReturnType().getSimpleName(),
//						method.getAnnotation( JsonProperty.class ) );
				checkRegistered( om, method.getReturnType(), imports );
			}
		return type;
	}

	/**
	 * @param json {key1: val1, key2: val2, ...}
	 * @param onNext
	 */
	public static void forEach( final ObjectNode json,
		final BiConsumer<String, JsonNode> onNext )
	{
		json.fields().forEachRemaining(
				e -> onNext.accept( e.getKey(), e.getValue() ) );
	}

	/**
	 * @param json [val1, val2, ...]
	 * @param onNext
	 */
	public static void forEach( final ArrayNode json,
		final BiConsumer<Integer, JsonNode> onNext )
	{
		IntStream.range( 0, json.size() )
				.forEach( i -> onNext.accept( i, json.get( i ) ) );
	}

	/**
	 * @param json [val1, val2, ...]
	 * @param onNext
	 */
	public static void forEachParallel( final ArrayNode json,
		final BiConsumer<Integer, JsonNode> onNext )
	{
		IntStream.range( 0, json.size() ).parallel()
				.forEach( i -> onNext.accept( i, json.get( i ) ) );
	}

	/**
	 * @param json {key1: val1, key2: val2, ...}
	 * @param parallel
	 * @return the (synchronous) stream
	 */
	public static Stream<Entry<String, JsonNode>> stream( final ObjectNode json,
		final boolean parallel )
	{
		return StreamSupport.stream(
				((Iterable<Map.Entry<String, JsonNode>>) () -> json.fields())
						.spliterator(),
				parallel );
	}

	/**
	 * @param json {key1: val1, key2: val2, ...}
	 * @return {@link Observable} stream of {@link JsonNode} mappings
	 */
	public static Observable<Entry<String, JsonNode>>
		streamAsync( final ObjectNode json )
	{
		return Observable.fromIterable(
				(Iterable<Entry<String, JsonNode>>) () -> json.fields() );
	}

	/**
	 * @param json [val1, val2, ...]
	 * @return {@link Observable} stream of {@link JsonNode} mappings
	 */
	public static Observable<Entry<Integer, JsonNode>>
		streamAsync( final ArrayNode json )
	{
		return Observable.range( 0, json.size() )
				.map( i -> Collections.entry( i, json.get( i ) ) );
	}

	/**
	 * @param json {key1: val1, key2: val2, ...}
	 * @param mapper (key,node)->v | e
	 * @return {@link TreeMap}
	 */
	public static <V> TreeMap<String, V> toMap( final ObjectNode json,
		final boolean parallel, final BiFunction<String, JsonNode, V> mapper )
	{
		return stream( json, parallel )
				.collect( Collectors.toMap( Entry::getKey,
						prop -> mapper.apply( prop.getKey(), prop.getValue() ),
						( k1, k2 ) -> k1, TreeMap::new ) );
	}

	/**
	 * @param json {key1: val1, key2: val2, ...}
	 * @param mapper (key,node)->v | e
	 * @return a {@link TreeMap}
	 */
	public static <V> TreeMap<String, V> toMap( final ObjectNode json,
		final BiFunction<String, JsonNode, V> mapper )
	{
		return (TreeMap<String, V>) streamAsync( json ).toMap( Entry::getKey,
				prop -> mapper.apply( prop.getKey(), prop.getValue() ),
				TreeMap::new ).blockingGet();
	}

	/**
	 * @param json [val1, val2, ...]
	 * @param mapper (key,node)->v | e
	 * @return {@link TreeMap}
	 */
//	public static <V> TreeMap<String, V> toMap( final ArrayNode json,
//		final Function<Integer, String> keyMapper,
//		final Function<JsonNode, V> valueMapper )
//	{
//		return IntStream.range( 0, json.size() ).mapToObj( i -> i )
//				.collect( Collectors.toMap( i -> keyMapper.apply( i ),
//						i -> valueMapper.apply( json.get( i ) ),
//						( k1, k2 ) -> k1, TreeMap::new ) );
//	}

	/**
	 * @param json {key1: val1, key2: val2, ...}
	 * @param mapper (key,node)->v | e
	 * @return a {@link TreeMap}
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static <V> TreeMap<String, V> toMap( final ArrayNode json,
		final IntFunction<String> keyMapper,
		final BiFunction<String, JsonNode, V> mapper )
	{
		return IntStream.range( 0, json.size() )
				// FIXME use a Map collector compatible with ObjIntConsumer
				.mapToObj( i -> i )
				.collect( Collectors.<Integer, String, V, TreeMap>toMap(
						i -> keyMapper.apply( i ),
						i -> mapper.apply( keyMapper.apply( i ),
								json.get( i ) ),
						( v1, v2 ) -> v1, TreeMap::new ) );
	}
}
