<<<<<<< HEAD
/* $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Provider;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Mutable;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ValueNode;

import io.coala.exception.ExceptionFactory;
import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.util.ReflectUtil;
import io.coala.util.TypeArguments;

/**
 * {@link DynaBean} implements a dynamic bean, ready for JSON de/serialization
 * 
 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
@JsonInclude( Include.NON_NULL )
public final class DynaBean implements Cloneable, Comparable
{

	/**
	 * {@link BeanProxy} is a annotation used to recognize {@link DynaBean}
	 * entities/tags during de/serialization and specify the property to use for
	 * {@link Comparable}s
	 * 
	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
	 * @author Rick van Krevelen
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.TYPE )
	public @interface BeanProxy
	{
		/**
		 * @return
		 */
		String comparableOn() default "";
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( DynaBean.class );

	/** leave null as long as possible */
	@JsonIgnore
	private Map<String, Object> dynamicProperties = null;

	/**
	 * {@link DynaBean} zero-arg bean constructor for (de)serialization
	 */
	@JsonCreator
	protected DynaBean()
	{
		// empty
	}

	protected void lock()
	{
		if( this.dynamicProperties != null )
			this.dynamicProperties = Collections
					.unmodifiableMap( this.dynamicProperties );
	}

	/**
	 * @return the map of property values
	 */
	@JsonAnyGetter
	protected Map<String, Object> any()
	{
		return this.dynamicProperties == null ? Collections.emptyMap()
				: this.dynamicProperties;
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean has( final String key )
	{
		return any().containsKey( key );
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean hasNonNull( final String key )
	{
		final Object value = get( key );
		return value != null;
	}

	/**
	 * @param key
	 * @param value
	 * @return {@code true} iff this bean contains the specified {@code value}
	 *         at specified {@code key}, i.e. both null/empty or both equal
	 */
	public boolean match( final String key, final Object value )
	{
		final Object v = get( key );
		return value == null ? v == null : value.equals( v );
	}

	/**
	 * @param key
	 * @return
	 */
	public Object get( final String key )
	{
		return any().get( key );
	}

	/**
	 * helper-method
	 * 
	 * @param key
	 * @param defaultValue
	 * @return the dynamically set value, or {@code defaultValue} if not set
	 */
	@SuppressWarnings( "unchecked" )
	protected <T> T get( final String key, final T defaultValue )
	{
		final Object result = get( key );
		return result == null ? defaultValue : (T) result;
	}

	/**
	 * helper-method
	 * 
	 * @param key
	 * @param returnType
	 * @return the currently set value, or {@code null} if not set
	 */
	@SuppressWarnings( "unchecked" )
	protected <T> T get( final String key, final Class<T> returnType )
	{
		return (T) get( key );
	}

	private Map<String, Object> getOrCreateMap()
	{
		if( this.dynamicProperties == null )
			this.dynamicProperties = new TreeMap<String, Object>();
		return this.dynamicProperties;
	}

	protected void set( final Map<String, ?> values )
	{
		Map<String, Object> map = getOrCreateMap();
		synchronized( map )
		{
			map.putAll( values );
		}
	}

	@JsonAnySetter
	protected Object set( final String key, final Object value )
	{
		Map<String, Object> map = getOrCreateMap();
		synchronized( map )
		{
			return map.put( key, value );
		}
	}

	protected Object remove( final String key )
	{
		Map<String, Object> map = getOrCreateMap();
		synchronized( map )
		{
			return map.remove( key );
		}
	}

	@SuppressWarnings( "unchecked" )
	@Override
	protected DynaBean clone()
	{
		final Map<String, Object> values = any();
		final DynaBean result = new DynaBean();
		result.set( JsonUtil.valueOf( JsonUtil.toTree( values ),
				values.getClass() ) );
		return result;
	}

	@Override
	public int hashCode()
	{
		return any().hashCode();
	}

	@Override
	public boolean equals( final Object other )
	{
		return any().equals( other );
	}

	@Override
	public int compareTo( final Object o )
	{
		return Thrower.throwNew( IllegalStateException::new,
				() -> "Invocation should be intercepted" );
	}

	@Override
	public String toString()
	{
		try
		{
			return JsonUtil.getJOM()
					.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS )
					.writeValueAsString( any() );
		} catch( final IOException e )
		{
			LOG.warn( "Problem serializing " + getClass().getName(), e );
			return super.toString();
		}
	}

	/** cache of type arguments for known {@link Identifier} sub-types */
	// static final Map<Class<?>, Provider<?>> DYNABEAN_PROVIDER_CACHE = new
	// WeakHashMap<>();

	/**
	 * {@link DynaBeanInvocationHandler}
	 * 
	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
	 * @author Rick van Krevelen
	 */
	static class DynaBeanInvocationHandler implements InvocationHandler
	{
		/** */
		private static final Logger LOG = LogUtil
				.getLogger( DynaBeanInvocationHandler.class );

		/** */
		private final Class<?> type;

		/** */
		private final Config config;

		/** */
		protected final DynaBean bean;

		/**
		 * {@link DynaBeanInvocationHandler} constructor
		 */
		@SafeVarargs
		public DynaBeanInvocationHandler( final ObjectMapper om,
			final Class<?> type, final DynaBean bean,
			final Map<String, ?>... imports )
		{
			this.type = type;
			this.bean = bean;
			// LOG.trace("Using imports: " + Arrays.asList(imports));
			Config config = null;
			if( Config.class.isAssignableFrom( type ) )
			{
				// always create fresh, never from cache
				config = ConfigFactory.create( type.asSubclass( Config.class ),
						imports );
				if( Mutable.class.isAssignableFrom( type ) )
				{
					final Mutable mutable = (Mutable) config;
					mutable.addPropertyChangeListener(
							new PropertyChangeListener()
							{
								@Override
								public void propertyChange(
									final PropertyChangeEvent change )
								{
									LOG.trace( "{} changed: {} = {} (was {})",
											type.getSimpleName(),
											change.getPropertyName(),
											change.getNewValue(),
											change.getOldValue() );

									// remove bean property in favor of changed
									// default config
									// bean.remove(change.getPropertyName());

									/*
									 * TODO parse actual value into bean try {
									 * final Method method =
									 * type.getMethod(change
									 * .getPropertyName()); final Object
									 * newValue = om.readValue(
									 * change.getNewValue().toString(),
									 * JsonUtil.checkRegistered(om,
									 * method.getReturnType(), imports));
									 * bean.set(change.getPropertyName(),
									 * newValue); } catch (final Throwable t) {
									 * LOG.warn(
									 * "Could not deserialize property: " +
									 * change.getPropertyName(), t); }
									 */
								}
							} );
				}
			} else if( imports != null ) for( Map<String, ?> imp : imports )
				this.bean.set( imp );
			this.config = config;

			// TODO use event listeners of Mutable interface to dynamically add
			// Converters at runtime
		}

//		@SuppressWarnings( "rawtypes" )
		@Override
		public Object invoke( final Object proxy, final Method method,
			final Object[] args ) throws Throwable
		{
			if( method.isDefault() )
				return ReflectUtil.invokeDefaultMethod( proxy, method, args );

			final String beanProp = method.getName();
//			LOG.trace( "Calling <{}> {}::{}({})",
//					method.getReturnType().getSimpleName(), this.type,
//					method.getName(), args == null ? "" : args );

			switch( args == null ? 0 : args.length )
			{
			case 0:
				if( beanProp.equals( Wrapper.UNWRAP_PROPERTY ) )
					return this.bean.get( Wrapper.WRAP_PROPERTY );

				if( beanProp.equals( "toString" ) )
				{
					if( Wrapper.class.isAssignableFrom( this.type ) )
						return this.bean.get( Wrapper.WRAP_PROPERTY )
								.toString();
					JsonUtil.checkRegistered( JsonUtil.getJOM(), this.type );
					return this.bean.toString();
				}

				if( beanProp.equals( "hashCode" ) ) return this.bean.hashCode();

				// ! can't intercept call to native method
				// if (method.getName().equals("getClass"))
				// return this.type;

				Object result = this.bean.any().get( beanProp );
				if( result == null ) // no value currently
				{
					if( this.config != null ) // obtain value from config
					{
						// cache (immutable) result
						result = method.invoke( this.config, args );
						if( this.config instanceof Mutable == false )
							this.bean.any().put( beanProp, result );
					} else
						try
						{
							return ReflectUtil.invokeAsBean(
									this.bean.dynamicProperties, this.type,
									method, args );
						} catch( final Exception e )
						{
							// ignoring non-bean method
						}
				}
				return result;

			case 1:
				if( beanProp.equals( "equals" ) )
					return this.bean.equals( args[0] );

				final DynaBean.BeanProxy annot = this.type
						.getAnnotation( DynaBean.BeanProxy.class );
				if( beanProp.equals( "compareTo" ) && annot != null )
					return DynaBean.getComparator( annot ).compare(
							(Comparable) this.bean, (Comparable) args[0] );

				// assume setter method, e.g. void setVal()
				if( method.getParameterTypes().length == 1
						&& method.getParameterTypes()[0]
								.isAssignableFrom( args[0].getClass() )
				//&& method.getName().startsWith( "set" ) )
				//&& method.getReturnType().equals( Void.TYPE ) 
				) try
				{
					return ReflectUtil.invokeAsBean(
							this.bean.dynamicProperties, this.type, method,
							args[0] );
				} catch( final Exception e )
				{
					// non-bean method, assume setter, e.g. val(..), withVal(..)
					return this.bean.set( beanProp, args[0] );
				}

				LOG.warn( "{} ({}) unknown: {}#{}({})",
						DynaBean.class.getSimpleName(),
						method.getReturnType().isPrimitive() ? "primitive"
								: "Object",
						this.type, beanProp, Arrays.asList( args ) );
				break;
			}

			if( this.config != null )
			{
				// LOG.trace("Passing call to Config");
				return method.invoke( this.config, args );
			}

//			if( method.getReturnType().equals( Void.TYPE ) )
//			{
//				LOG.warn( "Ignoring call to: void " + this.type.getSimpleName()
//						+ "#" + beanProp + "(" + Arrays.asList( args ) + ")" );
//				return null;
//			}

			throw ExceptionFactory.createUnchecked(
					"{} ({}) value not set: {}#{}({})",
					DynaBean.class.getSimpleName(),
					method.getReturnType().isPrimitive() ? "primitive"
							: "Object",
					this.type.getSimpleName(), beanProp,
					Arrays.asList( args ) );
		}
	}

	/**
	 * @param <T>
	 * @param wrapperType
	 * @return
	 */
	static final <T> JsonSerializer<T>
		createJsonSerializer( final Class<T> type )
	{
		return new JsonSerializer<T>()
		{
			@Override
			public void serialize( final T value, final JsonGenerator jgen,
				final SerializerProvider serializers )
				throws IOException, JsonProcessingException
			{
				// non-Proxy objects get default treatment
				if( !Proxy.isProxyClass( value.getClass() ) )
				{
					@SuppressWarnings( "unchecked" )
					final JsonSerializer<T> ser = (JsonSerializer<T>) serializers
							.findValueSerializer( value.getClass() );
					if( ser != this )
						ser.serialize( value, jgen, serializers );
					else
						LOG.warn( "Problem serializing: {}", value );
					return;
				}

				// BeanWrapper gets special treatment
				if( DynaBeanInvocationHandler.class
						.isInstance( Proxy.getInvocationHandler( value ) ) )
				{
					final DynaBeanInvocationHandler handler = (DynaBeanInvocationHandler) Proxy
							.getInvocationHandler( value );

					// Wrapper extensions get special treatment
					if( Wrapper.class.isAssignableFrom( handler.type ) )
					{
						final Object wrap = handler.bean.get( "wrap" );
						serializers.findValueSerializer( wrap.getClass(), null )
								.serialize( wrap, jgen, serializers );
						return;
					}
					// Config (Accessible) extensions get special treatment
					else if( Accessible.class.isAssignableFrom( handler.type ) )
					{
						final Map<String, Object> copy = new HashMap<>(
								handler.bean.any() );
						final Accessible config = (Accessible) handler.config;
						for( String key : config.propertyNames() )
							copy.put( key, config.getProperty( key ) );
						serializers.findValueSerializer( copy.getClass(), null )
								.serialize( copy, jgen, serializers );
						return;
					} else if( Config.class.isAssignableFrom( handler.type ) )
						throw new JsonGenerationException(
								"BeanWrapper should extend "
										+ Accessible.class.getName()
										+ " required for serialization: "
										+ Arrays.asList(
												handler.type.getInterfaces() ),
								jgen );

					// BeanWrappers that do not extend OWNER API's Config
					serializers
							.findValueSerializer( handler.bean.getClass(),
									null )
							.serialize( handler.bean, jgen, serializers );
					return;
				}

				// Config (Accessible) gets special treatment
				if( Accessible.class.isInstance( value ) )
				{
					final Accessible config = (Accessible) value;
					final Properties entries = new Properties();
					for( String key : config.propertyNames() )
						entries.put( key, config.getProperty( key ) );
					serializers.findValueSerializer( entries.getClass(), null )
							.serialize( entries, jgen, serializers );
					return;
				}

				if( Config.class.isInstance( value ) )
					throw new JsonGenerationException(
							"Config should extend " + Accessible.class.getName()
									+ " required for serialization: "
									+ Arrays.asList(
											value.getClass().getInterfaces() ),
							jgen );

				throw new JsonGenerationException(
						"No serializer found for proxy of: " + Arrays
								.asList( value.getClass().getInterfaces() ),
						jgen );
			}
		};
	}

	/**
	 * @param referenceType
	 * @param <S>
	 * @param <T>
	 * @return
	 */
	@SafeVarargs
	static final <S, T> JsonDeserializer<T> createJsonDeserializer(
		final ObjectMapper om, final Class<T> resultType,
		final Map<String, ?>... imports )
	{
		return new JsonDeserializer<T>()
		{

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
				if( jp.getCurrentToken() == JsonToken.VALUE_NULL ) return null;

//				if( Wrapper.class.isAssignableFrom( resultType ) )
//				{
//					// FIXME
//					LOG.trace( "deser wrapper intf of {}", jp.getText() );
//					return (T) Wrapper.Util.valueOf( jp.getText(),
//							resultType.asSubclass( Wrapper.class ) );
//				} 
				if( Config.class.isAssignableFrom( resultType ) )
				{
					final Map<String, Object> entries = jp.readValueAs(
							new TypeReference<Map<String, Object>>()
							{
							} );

					final Iterator<Entry<String, Object>> it = entries
							.entrySet().iterator();
					for( Entry<String, Object> next = null; it
							.hasNext(); next = it.next() )
						if( next != null && next.getValue() == null )
						{
							LOG.trace( "Ignoring null value: {}", next );
							it.remove();
						}
					return resultType.cast( ConfigFactory.create(
							resultType.asSubclass( Config.class ), entries ) );
				}
				// else if (Config.class.isAssignableFrom(resultType))
				// throw new JsonGenerationException(
				// "Config does not extend "+Mutable.class.getName()+" required for deserialization: "
				// + Arrays.asList(resultType
				// .getInterfaces()));

				// can't parse directly to interface type
				final DynaBean bean = new DynaBean();
				final TreeNode tree = jp.readValueAsTree();

				// override attributes as defined in interface getters
				final Set<String> attributes = new HashSet<>();
				for( Method method : resultType.getMethods() )
				{
					if( method.getReturnType().equals( Void.TYPE )
							|| method.getParameterTypes().length != 0 )
						continue;

					final String attribute = method.getName();
					if( attribute.equals( "toString" )
							|| attribute.equals( "hashCode" ) )
						continue;

					attributes.add( attribute );
					final TreeNode value = tree.get( attribute );// bean.any().get(attributeName);
					if( value == null ) continue;

					bean.set( method.getName(),
							om.treeToValue( value, JsonUtil.checkRegistered( om,
									method.getReturnType(), imports ) ) );
				}
				if( tree.isObject() )
				{
					// keep superfluous properties as TreeNodes, just in case
					final Iterator<String> fieldNames = tree.fieldNames();
					while( fieldNames.hasNext() )
					{
						final String fieldName = fieldNames.next();
						if( !attributes.contains( fieldName ) )
							bean.set( fieldName, tree.get( fieldName ) );
					}
				} else if( tree.isValueNode() )
				{
					for( Class<?> type : resultType.getInterfaces() )
						for( Method method : type.getDeclaredMethods() )
						{
//							LOG.trace( "Scanning {}", method );
							if( method
									.isAnnotationPresent( JsonProperty.class ) )
							{
								final String property = method
										.getAnnotation( JsonProperty.class )
										.value();
//								LOG.trace( "Setting {}: {}", property,
//										((ValueNode) tree).textValue() );
								bean.set( property,
										((ValueNode) tree).textValue() );
							}
						}
				} else
					throw ExceptionFactory.createUnchecked(
							"Expected {} but parsed: {}", resultType,
							tree.getClass() );

				return DynaBean.proxyOf( om, resultType, bean, imports );
			}
		};
	}

	/** */
	@SafeVarargs
	public static <T> void registerType( final ObjectMapper om,
		final Class<T> type, final Map<String, ?>... imports )
	{
		// TODO implement dynamic generic Converter(s) for JSON bean
		// properties ?

		// if (Config.class.isAssignableFrom(type))
		// {
		// final Class<?> editorType = new
		// JsonPropertyEditor<T>().getClass();
		// PropertyEditorManager.registerEditor(type, editorType);
		// LOG.trace("Registered " + editorType + " - "
		// + PropertyEditorManager.findEditor(type));
		// }

		om.registerModule( new SimpleModule()
				.addSerializer( type, createJsonSerializer( type ) )
				.addDeserializer( type,
						createJsonDeserializer( om, type, imports ) ) );
	}

	/** */
	private static final Map<BeanProxy, Comparator<?>> COMPARATOR_CACHE = new TreeMap<>();

	/**
	 * @param annot the {@link BeanProxy} instance for the type of wrapper of
	 *            {@link DynaBean}s containing the {@link Comparable} value type
	 *            in the annotated property key
	 * @return a (cached) comparator
	 */
	@SuppressWarnings( { "unchecked"/*, "rawtypes"*/ } )
	public static <S extends Comparable> Comparator<S>
		getComparator( final BeanProxy annot )
	{
		if( annot.comparableOn().isEmpty() ) return null;
		synchronized( COMPARATOR_CACHE )
		{
			Comparator<S> result = (Comparator<S>) COMPARATOR_CACHE
					.get( annot );
			if( result == null )
			{
				result = new Comparator<S>()
				{
					@Override
					public int compare( final S o1, final S o2 )
					{
						final S key1 = (S) ((DynaBeanInvocationHandler) Proxy
								.getInvocationHandler( o1 )).bean.any()
										.get( annot.comparableOn() );
						final S key2 = (S) ((DynaBeanInvocationHandler) Proxy
								.getInvocationHandler( o2 )).bean.any()
										.get( annot.comparableOn() );
						return key1.compareTo( key2 );
					}
				};
				LOG.trace( "Created comparator for " + annot );
				COMPARATOR_CACHE.put( annot, result );
			}
			return result;
		}
	}

	/**
	 * @param type the type of {@link Proxy} to generate
	 * @param imports default value {@link Properties} of the bean
	 * @return a {@link Proxy} instance backed by an empty {@link DynaBean}
	 */
	@SafeVarargs
	public static <T> T proxyOf( final Class<T> type,
		final Map<String, ?>... imports )
	{
		return proxyOf( type, new DynaBean(), imports );
	}

	/**
	 * @param type the type of {@link Proxy} to generate
	 * @param bean the (prepared) {@link DynaBean} for proxied getters/setters
	 * @param imports default value {@link Properties} of the bean
	 * @return a {@link Proxy} instance backed by an empty {@link DynaBean}
	 */
	@SafeVarargs
	protected static <T> T proxyOf( final Class<T> type, final DynaBean bean,
		final Map<String, ?>... imports )
	{
		return proxyOf( JsonUtil.getJOM(), type, bean, imports );
	}

	/**
	 * @param om the {@link ObjectMapper} for get and set de/serialization
	 * @param type the type of {@link Proxy} to generate
	 * @param bean the (prepared) {@link DynaBean} for proxied getters/setters
	 * @param imports default value {@link Properties} of the bean
	 * @return a {@link Proxy} instance backed by an empty {@link DynaBean}
	 */
	@SuppressWarnings( "unchecked" )
	protected static <T> T proxyOf( final ObjectMapper om, final Class<T> type,
		final DynaBean bean, final Map<String, ?>... imports )
	{
//		if( !type.isAnnotationPresent( BeanProxy.class ) )
//			throw ExceptionFactory.createUnchecked( "{} is not a @{}", type,
//					BeanProxy.class.getSimpleName() );

		return (T) Proxy.newProxyInstance( type.getClassLoader(),
				new Class[]
		{ type }, new DynaBeanInvocationHandler( om, type, bean, imports ) );
	}

	public static Class<?> typeOf( final Object proxy )
	{
		return ((DynaBeanInvocationHandler) Proxy
				.getInvocationHandler( proxy )).type;
	}

	/**
	 * {@link ProxyProvider}
	 * 
	 * @param <T>
	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
	 * @author Rick van Krevelen
	 */
	public static class ProxyProvider<T> implements Provider<T>
	{

		/** cache of type arguments for known {@link Proxy} sub-types */
		private static final Map<Class<?>, List<Class<?>>> PROXY_TYPE_ARGUMENT_CACHE = new HashMap<>();

		/**
		 * @param proxyType should be a non-abstract concrete {@link Class} that
		 *            has a public zero-arg constructor
		 * @return the new {@link ProxyProvider} instance
		 */
		@SafeVarargs
		public static <T> ProxyProvider<T> of( final Class<T> proxyType,
			final Map<String, ?>... imports )
		{
			return of( JsonUtil.getJOM(), proxyType, imports );
		}

		/**
		 * @param om the {@link ObjectMapper} for get and set de/serialization
		 * @param proxyType should be a non-abstract concrete {@link Class} that
		 *            has a public zero-arg constructor
		 * @return the new {@link ProxyProvider} instance
		 */
		@SafeVarargs
		public static <T> ProxyProvider<T> of( final ObjectMapper om,
			final Class<T> proxyType, final Map<String, ?>... imports )
		{
			return new ProxyProvider<T>( om, proxyType, new DynaBean(),
					imports );
		}

		/**
		 * @param om the {@link ObjectMapper} for get and set de/serialization
		 * @param beanType should be a non-abstract concrete {@link Class} that
		 *            has a public zero-arg constructor
		 * @param cache the {@link Map} of previously created instances
		 * @return the cached (new) {@link ProxyProvider} instance
		 */
		@SafeVarargs
		public static <T> ProxyProvider<T> of( final ObjectMapper om,
			final Class<T> beanType,
			final Map<Class<?>, ProxyProvider<?>> cache,
			final Map<String, ?>... imports )
		{
			if( cache == null ) return of( om, beanType, imports );

			synchronized( cache )
			{
				@SuppressWarnings( "unchecked" )
				ProxyProvider<T> result = (ProxyProvider<T>) cache
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
		private final Map<String, ?>[] imports;

		/**
		 * {@link ProxyProvider} constructor
		 * 
		 * @param om
		 * @param proxyType
		 * @param bean the (possibly prepared) {@link DynaBean}
		 * @param imports
		 */
		@SafeVarargs
		public ProxyProvider( final ObjectMapper om, final Class<T> proxyType,
			final DynaBean bean, final Map<String, ?>... imports )
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
						? (Class<T>) TypeArguments.of( ProxyProvider.class,
								getClass(), PROXY_TYPE_ARGUMENT_CACHE ).get( 0 )
						: this.proxyType;
				return DynaBean.proxyOf( this.om, proxyType, this.bean,
						this.imports );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
				return null;
			}
		}
	}
//	/**
//	 * {@link Builder}
//	 * 
//	 * @param <T> the result type
//	 * @param <THIS> the builder type
//	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
//	 * @author Rick van Krevelen
//	 */
//	public static class Builder<T, THIS extends Builder<T, THIS>>
//		extends ProxyProvider<T>
//	{
//
//		/** */
//		private final DynaBean bean;
//
//		/**
//		 * {@link Builder} constructor, to be extended by a public zero-arg
//		 * constructor in concrete sub-types
//		 */
//		protected Builder( final Properties... imports )
//		{
//			this( JsonUtil.getJOM(), new DynaBean(), imports );
//		}
//
//		/**
//		 * {@link Builder} constructor, to be extended by a public zero-arg
//		 * constructor in concrete sub-types
//		 */
//		protected Builder( final ObjectMapper om, final Properties... imports )
//		{
//			this( om, new DynaBean(), imports );
//		}
//
//		/**
//		 * {@link Builder} constructor, to be extended by a public zero-arg
//		 * constructor in concrete sub-types
//		 */
//		protected Builder( final ObjectMapper om, final DynaBean bean,
//			final Properties... imports )
//		{
//			super( om, null, bean, imports );
//			this.bean = bean;
//		}
//
//		/**
//		 * helper-method
//		 * 
//		 * @param key
//		 * @param returnType
//		 * @return the currently set value, or {@code null} if not set
//		 */
//		protected <S> S get( final String key, final Class<S> returnType )
//		{
//			return returnType.cast( this.bean.get( key ) );
//		}
//
//		/**
//		 * @param key
//		 * @param value
//		 * @return
//		 */
//		@SuppressWarnings( "unchecked" )
//		public THIS with( final String key, final Object value )
//		{
//			this.bean.set( key, value );
//			return (THIS) this;
//		}
//
//		public THIS with( final String key, final TreeNode value,
//			final Class<?> valueType )
//		{
//			return (THIS) with( key, JsonUtil.valueOf( value, valueType ) );
//		}
//
//		/**
//		 * @return this Builder with the immutable bean
//		 */
//		@SuppressWarnings( "unchecked" )
//		public THIS lock()
//		{
//			this.bean.lock();
//			return (THIS) this;
//		}
//
//		/**
//		 * @return the provided instance of <T>
//		 */
//		public T build()
//		{
//			return get();
//		}
//	}
=======
/* $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Provider;

import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Mutable;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ValueNode;

import io.coala.exception.ExceptionFactory;
import io.coala.exception.Thrower;
import io.coala.log.LogUtil;
import io.coala.util.ReflectUtil;
import io.coala.util.TypeArguments;

/**
 * {@link DynaBean} implements a dynamic bean, ready for JSON de/serialization
 * 
 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
@JsonInclude( Include.NON_NULL )
public final class DynaBean implements Cloneable, Comparable
{

	/**
	 * {@link BeanProxy} is a annotation used to recognize {@link DynaBean}
	 * entities/tags during de/serialization and specify the property to use for
	 * {@link Comparable}s
	 * 
	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
	 * @author Rick van Krevelen
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	@Target( ElementType.TYPE )
	public @interface BeanProxy
	{
		/**
		 * @return
		 */
		String comparableOn() default "";
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( DynaBean.class );

	/** leave null as long as possible */
	@JsonIgnore
	private Map<String, Object> dynamicProperties = null;

	/**
	 * {@link DynaBean} zero-arg bean constructor for (de)serialization
	 */
	@JsonCreator
	protected DynaBean()
	{
		// empty
	}

	protected void lock()
	{
		if( this.dynamicProperties != null )
			this.dynamicProperties = Collections
					.unmodifiableMap( this.dynamicProperties );
	}

	/**
	 * @return the map of property values
	 */
	@JsonAnyGetter
	protected Map<String, Object> any()
	{
		return this.dynamicProperties == null ? Collections.emptyMap()
				: this.dynamicProperties;
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean has( final String key )
	{
		return any().containsKey( key );
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean hasNonNull( final String key )
	{
		final Object value = get( key );
		return value != null;
	}

	/**
	 * @param key
	 * @param value
	 * @return {@code true} iff this bean contains the specified {@code value}
	 *         at specified {@code key}, i.e. both null/empty or both equal
	 */
	public boolean match( final String key, final Object value )
	{
		final Object v = get( key );
		return value == null ? v == null : value.equals( v );
	}

	/**
	 * @param key
	 * @return
	 */
	public Object get( final String key )
	{
		return any().get( key );
	}

	/**
	 * helper-method
	 * 
	 * @param key
	 * @param defaultValue
	 * @return the dynamically set value, or {@code defaultValue} if not set
	 */
	@SuppressWarnings( "unchecked" )
	protected <T> T get( final String key, final T defaultValue )
	{
		final Object result = get( key );
		return result == null ? defaultValue : (T) result;
	}

	/**
	 * helper-method
	 * 
	 * @param key
	 * @param returnType
	 * @return the currently set value, or {@code null} if not set
	 */
	@SuppressWarnings( "unchecked" )
	protected <T> T get( final String key, final Class<T> returnType )
	{
		return (T) get( key );
	}

	private Map<String, Object> getOrCreateMap()
	{
		if( this.dynamicProperties == null )
			this.dynamicProperties = new TreeMap<String, Object>();
		return this.dynamicProperties;
	}

	protected void set( final Map<String, ?> values )
	{
		Map<String, Object> map = getOrCreateMap();
		synchronized( map )
		{
			map.putAll( values );
		}
	}

	@JsonAnySetter
	protected Object set( final String key, final Object value )
	{
		Map<String, Object> map = getOrCreateMap();
		synchronized( map )
		{
			return map.put( key, value );
		}
	}

	protected Object remove( final String key )
	{
		Map<String, Object> map = getOrCreateMap();
		synchronized( map )
		{
			return map.remove( key );
		}
	}

	@SuppressWarnings( "unchecked" )
	@Override
	protected DynaBean clone()
	{
		final Map<String, Object> values = any();
		final DynaBean result = new DynaBean();
		result.set( JsonUtil.valueOf( JsonUtil.toTree( values ),
				values.getClass() ) );
		return result;
	}

	@Override
	public int hashCode()
	{
		return any().hashCode();
	}

	@Override
	public boolean equals( final Object other )
	{
		return any().equals( other );
	}

	@Override
	public int compareTo( final Object o )
	{
		return Thrower.throwNew( IllegalStateException::new,
				() -> "Invocation should be intercepted" );
	}

	@Override
	public String toString()
	{
		try
		{
			return JsonUtil.getJOM()
					.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS )
					.writeValueAsString( any() );
		} catch( final IOException e )
		{
			LOG.warn( "Problem serializing " + getClass().getName(), e );
			return super.toString();
		}
	}

	/** cache of type arguments for known {@link Identifier} sub-types */
	// static final Map<Class<?>, Provider<?>> DYNABEAN_PROVIDER_CACHE = new
	// WeakHashMap<>();

	/**
	 * {@link DynaBeanInvocationHandler}
	 * 
	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
	 * @author Rick van Krevelen
	 */
	static class DynaBeanInvocationHandler implements InvocationHandler
	{
		/** */
		private static final Logger LOG = LogUtil
				.getLogger( DynaBeanInvocationHandler.class );

		/** */
		private final Class<?> type;

		/** */
		private final Config config;

		/** */
		protected final DynaBean bean;

		/**
		 * {@link DynaBeanInvocationHandler} constructor
		 */
		@SafeVarargs
		public DynaBeanInvocationHandler( final ObjectMapper om,
			final Class<?> type, final DynaBean bean,
			final Map<String, ?>... imports )
		{
			this.type = type;
			this.bean = bean;
			// LOG.trace("Using imports: " + Arrays.asList(imports));
			Config config = null;
			if( Config.class.isAssignableFrom( type ) )
			{
				// always create fresh, never from cache
				config = ConfigFactory.create( type.asSubclass( Config.class ),
						imports );
				if( Mutable.class.isAssignableFrom( type ) )
				{
					final Mutable mutable = (Mutable) config;
					mutable.addPropertyChangeListener(
							new PropertyChangeListener()
							{
								@Override
								public void propertyChange(
									final PropertyChangeEvent change )
								{
									LOG.trace( "{} changed: {} = {} (was {})",
											type.getSimpleName(),
											change.getPropertyName(),
											change.getNewValue(),
											change.getOldValue() );

									// remove bean property in favor of changed
									// default config
									// bean.remove(change.getPropertyName());

									/*
									 * TODO parse actual value into bean try {
									 * final Method method =
									 * type.getMethod(change
									 * .getPropertyName()); final Object
									 * newValue = om.readValue(
									 * change.getNewValue().toString(),
									 * JsonUtil.checkRegistered(om,
									 * method.getReturnType(), imports));
									 * bean.set(change.getPropertyName(),
									 * newValue); } catch (final Throwable t) {
									 * LOG.warn(
									 * "Could not deserialize property: " +
									 * change.getPropertyName(), t); }
									 */
								}
							} );
				}
			} else if( imports != null ) for( Map<String, ?> imp : imports )
				this.bean.set( imp );
			this.config = config;

			// TODO use event listeners of Mutable interface to dynamically add
			// Converters at runtime
		}

//		@SuppressWarnings( "rawtypes" )
		@Override
		public Object invoke( final Object proxy, final Method method,
			final Object[] args ) throws Throwable
		{
			if( method.isDefault() )
				return ReflectUtil.invokeDefaultMethod( proxy, method, args );

			final String beanProp = method.getName();
//			LOG.trace( "Calling <{}> {}::{}({})",
//					method.getReturnType().getSimpleName(), this.type,
//					method.getName(), args == null ? "" : args );

			switch( args == null ? 0 : args.length )
			{
			case 0:
				if( beanProp.equals( Wrapper.UNWRAP_PROPERTY ) )
					return this.bean.get( Wrapper.WRAP_PROPERTY );

				if( beanProp.equals( "toString" ) )
				{
					if( Wrapper.class.isAssignableFrom( this.type ) )
						return this.bean.get( Wrapper.WRAP_PROPERTY )
								.toString();
					JsonUtil.checkRegistered( JsonUtil.getJOM(), this.type );
					return this.bean.toString();
				}

				if( beanProp.equals( "hashCode" ) ) return this.bean.hashCode();

				// ! can't intercept call to native method
				// if (method.getName().equals("getClass"))
				// return this.type;

				Object result = this.bean.any().get( beanProp );
				if( result == null ) // no value currently
				{
					if( this.config != null ) // obtain value from config
					{
						// cache (immutable) result
						result = method.invoke( this.config, args );
						if( this.config instanceof Mutable == false )
							this.bean.any().put( beanProp, result );
					} else
						try
						{
							return ReflectUtil.invokeAsBean(
									this.bean.dynamicProperties, this.type,
									method, args );
						} catch( final Exception e )
						{
							// ignoring non-bean method
						}
				}
				return result;

			case 1:
				if( beanProp.equals( "equals" ) )
					return this.bean.equals( args[0] );

				final DynaBean.BeanProxy annot = this.type
						.getAnnotation( DynaBean.BeanProxy.class );
				if( beanProp.equals( "compareTo" ) && annot != null )
					return DynaBean.getComparator( annot ).compare(
							(Comparable) this.bean, (Comparable) args[0] );

				// assume setter method, e.g. void setVal()
				if( method.getParameterTypes().length == 1
						&& method.getParameterTypes()[0]
								.isAssignableFrom( args[0].getClass() )
				//&& method.getName().startsWith( "set" ) )
				//&& method.getReturnType().equals( Void.TYPE ) 
				) try
				{
					return ReflectUtil.invokeAsBean(
							this.bean.dynamicProperties, this.type, method,
							args[0] );
				} catch( final Exception e )
				{
					// non-bean method, assume setter, e.g. val(..), withVal(..)
					return this.bean.set( beanProp, args[0] );
				}

				LOG.warn( "{} ({}) unknown: {}#{}({})",
						DynaBean.class.getSimpleName(),
						method.getReturnType().isPrimitive() ? "primitive"
								: "Object",
						this.type, beanProp, Arrays.asList( args ) );
				break;
			}

			if( this.config != null )
			{
				// LOG.trace("Passing call to Config");
				return method.invoke( this.config, args );
			}

//			if( method.getReturnType().equals( Void.TYPE ) )
//			{
//				LOG.warn( "Ignoring call to: void " + this.type.getSimpleName()
//						+ "#" + beanProp + "(" + Arrays.asList( args ) + ")" );
//				return null;
//			}

			throw ExceptionFactory.createUnchecked(
					"{} ({}) value not set: {}#{}({})",
					DynaBean.class.getSimpleName(),
					method.getReturnType().isPrimitive() ? "primitive"
							: "Object",
					this.type.getSimpleName(), beanProp,
					Arrays.asList( args ) );
		}
	}

	/**
	 * @param <T>
	 * @param wrapperType
	 * @return
	 */
	static final <T> JsonSerializer<T>
		createJsonSerializer( final Class<T> type )
	{
		return new JsonSerializer<T>()
		{
			@Override
			public void serialize( final T value, final JsonGenerator jgen,
				final SerializerProvider serializers )
				throws IOException, JsonProcessingException
			{
				// non-Proxy objects get default treatment
				if( !Proxy.isProxyClass( value.getClass() ) )
				{
					@SuppressWarnings( "unchecked" )
					final JsonSerializer<T> ser = (JsonSerializer<T>) serializers
							.findValueSerializer( value.getClass() );
					if( ser != this )
						ser.serialize( value, jgen, serializers );
					else
						LOG.warn( "Problem serializing: {}", value );
					return;
				}

				// BeanWrapper gets special treatment
				if( DynaBeanInvocationHandler.class
						.isInstance( Proxy.getInvocationHandler( value ) ) )
				{
					final DynaBeanInvocationHandler handler = (DynaBeanInvocationHandler) Proxy
							.getInvocationHandler( value );

					// Wrapper extensions get special treatment
					if( Wrapper.class.isAssignableFrom( handler.type ) )
					{
						final Object wrap = handler.bean.get( "wrap" );
						serializers.findValueSerializer( wrap.getClass(), null )
								.serialize( wrap, jgen, serializers );
						return;
					}
					// Config (Accessible) extensions get special treatment
					else if( Accessible.class.isAssignableFrom( handler.type ) )
					{
						final Map<String, Object> copy = new HashMap<>(
								handler.bean.any() );
						final Accessible config = (Accessible) handler.config;
						for( String key : config.propertyNames() )
							copy.put( key, config.getProperty( key ) );
						serializers.findValueSerializer( copy.getClass(), null )
								.serialize( copy, jgen, serializers );
						return;
					} else if( Config.class.isAssignableFrom( handler.type ) )
						throw new JsonGenerationException(
								"BeanWrapper should extend "
										+ Accessible.class.getName()
										+ " required for serialization: "
										+ Arrays.asList(
												handler.type.getInterfaces() ),
								jgen );

					// BeanWrappers that do not extend OWNER API's Config
					serializers
							.findValueSerializer( handler.bean.getClass(),
									null )
							.serialize( handler.bean, jgen, serializers );
					return;
				}

				// Config (Accessible) gets special treatment
				if( Accessible.class.isInstance( value ) )
				{
					final Accessible config = (Accessible) value;
					final Properties entries = new Properties();
					for( String key : config.propertyNames() )
						entries.put( key, config.getProperty( key ) );
					serializers.findValueSerializer( entries.getClass(), null )
							.serialize( entries, jgen, serializers );
					return;
				}

				if( Config.class.isInstance( value ) )
					throw new JsonGenerationException(
							"Config should extend " + Accessible.class.getName()
									+ " required for serialization: "
									+ Arrays.asList(
											value.getClass().getInterfaces() ),
							jgen );

				throw new JsonGenerationException(
						"No serializer found for proxy of: " + Arrays
								.asList( value.getClass().getInterfaces() ),
						jgen );
			}
		};
	}

	/**
	 * @param referenceType
	 * @param <S>
	 * @param <T>
	 * @return
	 */
	@SafeVarargs
	static final <S, T> JsonDeserializer<T> createJsonDeserializer(
		final ObjectMapper om, final Class<T> resultType,
		final Map<String, ?>... imports )
	{
		return new JsonDeserializer<T>()
		{

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
				if( jp.getCurrentToken() == JsonToken.VALUE_NULL ) return null;

//				if( Wrapper.class.isAssignableFrom( resultType ) )
//				{
//					// FIXME
//					LOG.trace( "deser wrapper intf of {}", jp.getText() );
//					return (T) Wrapper.Util.valueOf( jp.getText(),
//							resultType.asSubclass( Wrapper.class ) );
//				} 
				if( Config.class.isAssignableFrom( resultType ) )
				{
					final Map<String, Object> entries = jp.readValueAs(
							new TypeReference<Map<String, Object>>()
							{
							} );

					final Iterator<Entry<String, Object>> it = entries
							.entrySet().iterator();
					for( Entry<String, Object> next = null; it
							.hasNext(); next = it.next() )
						if( next != null && next.getValue() == null )
						{
							LOG.trace( "Ignoring null value: {}", next );
							it.remove();
						}
					return resultType.cast( ConfigFactory.create(
							resultType.asSubclass( Config.class ), entries ) );
				}
				// else if (Config.class.isAssignableFrom(resultType))
				// throw new JsonGenerationException(
				// "Config does not extend "+Mutable.class.getName()+" required for deserialization: "
				// + Arrays.asList(resultType
				// .getInterfaces()));

				// can't parse directly to interface type
				final DynaBean bean = new DynaBean();
				final TreeNode tree = jp.readValueAsTree();

				// override attributes as defined in interface getters
				final Set<String> attributes = new HashSet<>();
				for( Method method : resultType.getMethods() )
				{
					if( method.getReturnType().equals( Void.TYPE )
							|| method.getParameterTypes().length != 0 )
						continue;

					final String attribute = method.getName();
					if( attribute.equals( "toString" )
							|| attribute.equals( "hashCode" ) )
						continue;

					attributes.add( attribute );
					final TreeNode value = tree.get( attribute );// bean.any().get(attributeName);
					if( value == null ) continue;

					bean.set( method.getName(),
							om.treeToValue( value, JsonUtil.checkRegistered( om,
									method.getReturnType(), imports ) ) );
				}
				if( tree.isObject() )
				{
					// keep superfluous properties as TreeNodes, just in case
					final Iterator<String> fieldNames = tree.fieldNames();
					while( fieldNames.hasNext() )
					{
						final String fieldName = fieldNames.next();
						if( !attributes.contains( fieldName ) )
							bean.set( fieldName, tree.get( fieldName ) );
					}
				} else if( tree.isValueNode() )
				{
					for( Class<?> type : resultType.getInterfaces() )
						for( Method method : type.getDeclaredMethods() )
						{
//							LOG.trace( "Scanning {}", method );
							if( method
									.isAnnotationPresent( JsonProperty.class ) )
							{
								final String property = method
										.getAnnotation( JsonProperty.class )
										.value();
//								LOG.trace( "Setting {}: {}", property,
//										((ValueNode) tree).textValue() );
								bean.set( property,
										((ValueNode) tree).textValue() );
							}
						}
				} else
					throw ExceptionFactory.createUnchecked(
							"Expected {} but parsed: {}", resultType,
							tree.getClass() );

				return DynaBean.proxyOf( om, resultType, bean, imports );
			}
		};
	}

	/** */
	@SafeVarargs
	public static <T> void registerType( final ObjectMapper om,
		final Class<T> type, final Map<String, ?>... imports )
	{
		// TODO implement dynamic generic Converter(s) for JSON bean
		// properties ?

		// if (Config.class.isAssignableFrom(type))
		// {
		// final Class<?> editorType = new
		// JsonPropertyEditor<T>().getClass();
		// PropertyEditorManager.registerEditor(type, editorType);
		// LOG.trace("Registered " + editorType + " - "
		// + PropertyEditorManager.findEditor(type));
		// }

		om.registerModule( new SimpleModule()
				.addSerializer( type, createJsonSerializer( type ) )
				.addDeserializer( type,
						createJsonDeserializer( om, type, imports ) ) );
	}

	/** */
	private static final Map<BeanProxy, Comparator<?>> COMPARATOR_CACHE = new TreeMap<>();

	/**
	 * @param annot the {@link BeanProxy} instance for the type of wrapper of
	 *            {@link DynaBean}s containing the {@link Comparable} value type
	 *            in the annotated property key
	 * @return a (cached) comparator
	 */
	@SuppressWarnings( { "unchecked"/*, "rawtypes"*/ } )
	public static <S extends Comparable> Comparator<S>
		getComparator( final BeanProxy annot )
	{
		if( annot.comparableOn().isEmpty() ) return null;
		synchronized( COMPARATOR_CACHE )
		{
			Comparator<S> result = (Comparator<S>) COMPARATOR_CACHE
					.get( annot );
			if( result == null )
			{
				result = new Comparator<S>()
				{
					@Override
					public int compare( final S o1, final S o2 )
					{
						final S key1 = (S) ((DynaBeanInvocationHandler) Proxy
								.getInvocationHandler( o1 )).bean.any()
										.get( annot.comparableOn() );
						final S key2 = (S) ((DynaBeanInvocationHandler) Proxy
								.getInvocationHandler( o2 )).bean.any()
										.get( annot.comparableOn() );
						return key1.compareTo( key2 );
					}
				};
				LOG.trace( "Created comparator for " + annot );
				COMPARATOR_CACHE.put( annot, result );
			}
			return result;
		}
	}

	/**
	 * @param type the type of {@link Proxy} to generate
	 * @param imports default value {@link Properties} of the bean
	 * @return a {@link Proxy} instance backed by an empty {@link DynaBean}
	 */
	@SafeVarargs
	public static <T> T proxyOf( final Class<T> type,
		final Map<String, ?>... imports )
	{
		return proxyOf( type, new DynaBean(), imports );
	}

	/**
	 * @param type the type of {@link Proxy} to generate
	 * @param bean the (prepared) {@link DynaBean} for proxied getters/setters
	 * @param imports default value {@link Properties} of the bean
	 * @return a {@link Proxy} instance backed by an empty {@link DynaBean}
	 */
	@SafeVarargs
	protected static <T> T proxyOf( final Class<T> type, final DynaBean bean,
		final Map<String, ?>... imports )
	{
		return proxyOf( JsonUtil.getJOM(), type, bean, imports );
	}

	/**
	 * @param om the {@link ObjectMapper} for get and set de/serialization
	 * @param type the type of {@link Proxy} to generate
	 * @param bean the (prepared) {@link DynaBean} for proxied getters/setters
	 * @param imports default value {@link Properties} of the bean
	 * @return a {@link Proxy} instance backed by an empty {@link DynaBean}
	 */
	@SuppressWarnings( "unchecked" )
	protected static <T> T proxyOf( final ObjectMapper om, final Class<T> type,
		final DynaBean bean, final Map<String, ?>... imports )
	{
//		if( !type.isAnnotationPresent( BeanProxy.class ) )
//			throw ExceptionFactory.createUnchecked( "{} is not a @{}", type,
//					BeanProxy.class.getSimpleName() );

		return (T) Proxy.newProxyInstance( type.getClassLoader(),
				new Class[]
		{ type }, new DynaBeanInvocationHandler( om, type, bean, imports ) );
	}

	public static Class<?> typeOf( final Object proxy )
	{
		return ((DynaBeanInvocationHandler) Proxy
				.getInvocationHandler( proxy )).type;
	}

	/**
	 * {@link ProxyProvider}
	 * 
	 * @param <T>
	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
	 * @author Rick van Krevelen
	 */
	public static class ProxyProvider<T> implements Provider<T>
	{

		/** cache of type arguments for known {@link Proxy} sub-types */
		private static final Map<Class<?>, List<Class<?>>> PROXY_TYPE_ARGUMENT_CACHE = new HashMap<>();

		/**
		 * @param proxyType should be a non-abstract concrete {@link Class} that
		 *            has a public zero-arg constructor
		 * @return the new {@link ProxyProvider} instance
		 */
		@SafeVarargs
		public static <T> ProxyProvider<T> of( final Class<T> proxyType,
			final Map<String, ?>... imports )
		{
			return of( JsonUtil.getJOM(), proxyType, imports );
		}

		/**
		 * @param om the {@link ObjectMapper} for get and set de/serialization
		 * @param proxyType should be a non-abstract concrete {@link Class} that
		 *            has a public zero-arg constructor
		 * @return the new {@link ProxyProvider} instance
		 */
		@SafeVarargs
		public static <T> ProxyProvider<T> of( final ObjectMapper om,
			final Class<T> proxyType, final Map<String, ?>... imports )
		{
			return new ProxyProvider<T>( om, proxyType, new DynaBean(),
					imports );
		}

		/**
		 * @param om the {@link ObjectMapper} for get and set de/serialization
		 * @param beanType should be a non-abstract concrete {@link Class} that
		 *            has a public zero-arg constructor
		 * @param cache the {@link Map} of previously created instances
		 * @return the cached (new) {@link ProxyProvider} instance
		 */
		@SafeVarargs
		public static <T> ProxyProvider<T> of( final ObjectMapper om,
			final Class<T> beanType,
			final Map<Class<?>, ProxyProvider<?>> cache,
			final Map<String, ?>... imports )
		{
			if( cache == null ) return of( om, beanType, imports );

			synchronized( cache )
			{
				@SuppressWarnings( "unchecked" )
				ProxyProvider<T> result = (ProxyProvider<T>) cache
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
		private final Map<String, ?>[] imports;

		/**
		 * {@link ProxyProvider} constructor
		 * 
		 * @param om
		 * @param proxyType
		 * @param bean the (possibly prepared) {@link DynaBean}
		 * @param imports
		 */
		@SafeVarargs
		public ProxyProvider( final ObjectMapper om, final Class<T> proxyType,
			final DynaBean bean, final Map<String, ?>... imports )
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
						? (Class<T>) TypeArguments.of( ProxyProvider.class,
								getClass(), PROXY_TYPE_ARGUMENT_CACHE ).get( 0 )
						: this.proxyType;
				return DynaBean.proxyOf( this.om, proxyType, this.bean,
						this.imports );
			} catch( final Throwable e )
			{
				Thrower.rethrowUnchecked( e );
				return null;
			}
		}
	}
//	/**
//	 * {@link Builder}
//	 * 
//	 * @param <T> the result type
//	 * @param <THIS> the builder type
//	 * @version $Id: ef3fdccd265ebdcd1fa53df1afb494764cbf664c $
//	 * @author Rick van Krevelen
//	 */
//	public static class Builder<T, THIS extends Builder<T, THIS>>
//		extends ProxyProvider<T>
//	{
//
//		/** */
//		private final DynaBean bean;
//
//		/**
//		 * {@link Builder} constructor, to be extended by a public zero-arg
//		 * constructor in concrete sub-types
//		 */
//		protected Builder( final Properties... imports )
//		{
//			this( JsonUtil.getJOM(), new DynaBean(), imports );
//		}
//
//		/**
//		 * {@link Builder} constructor, to be extended by a public zero-arg
//		 * constructor in concrete sub-types
//		 */
//		protected Builder( final ObjectMapper om, final Properties... imports )
//		{
//			this( om, new DynaBean(), imports );
//		}
//
//		/**
//		 * {@link Builder} constructor, to be extended by a public zero-arg
//		 * constructor in concrete sub-types
//		 */
//		protected Builder( final ObjectMapper om, final DynaBean bean,
//			final Properties... imports )
//		{
//			super( om, null, bean, imports );
//			this.bean = bean;
//		}
//
//		/**
//		 * helper-method
//		 * 
//		 * @param key
//		 * @param returnType
//		 * @return the currently set value, or {@code null} if not set
//		 */
//		protected <S> S get( final String key, final Class<S> returnType )
//		{
//			return returnType.cast( this.bean.get( key ) );
//		}
//
//		/**
//		 * @param key
//		 * @param value
//		 * @return
//		 */
//		@SuppressWarnings( "unchecked" )
//		public THIS with( final String key, final Object value )
//		{
//			this.bean.set( key, value );
//			return (THIS) this;
//		}
//
//		public THIS with( final String key, final TreeNode value,
//			final Class<?> valueType )
//		{
//			return (THIS) with( key, JsonUtil.valueOf( value, valueType ) );
//		}
//
//		/**
//		 * @return this Builder with the immutable bean
//		 */
//		@SuppressWarnings( "unchecked" )
//		public THIS lock()
//		{
//			this.bean.lock();
//			return (THIS) this;
//		}
//
//		/**
//		 * @return the provided instance of <T>
//		 */
//		public T build()
//		{
//			return get();
//		}
//	}
>>>>>>> branch 'develop' of https://github.com/krevelen/coala-binder.git
}