/* $Id: 2070bb85b6ee8891e4612011dac89dfe69f9fa38 $
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
package io.coala.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.logging.log4j.Logger;

import io.coala.exception.Thrower;
import io.coala.log.LogUtil;

/**
 * {@link ClassUtil}
 * 
 * @version $Id: 2070bb85b6ee8891e4612011dac89dfe69f9fa38 $
 * @author Rick van Krevelen
 */
public class ClassUtil implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( ClassUtil.class );

	/**
	 * {@link ClassUtil} singleton constructor
	 */
	private ClassUtil()
	{
		// empty
	}

	/**
	 * @param primitive
	 * @param wrapper
	 * @return <tt>true</tt> if x is the primitive type wrapped by y,
	 *         <tt>false</tt> otherwise
	 */
	public static boolean isPrimitiveOf( final Class<?> primitive,
		final Class<?> wrapper )
	{
		if( !primitive.isPrimitive() || wrapper.isPrimitive() ) return false;
		// System.err.println( primitive+" =?= "+wrapper );

		if( primitive.equals( byte.class ) && wrapper.equals( Byte.class ) )
			return true;

		if( primitive.equals( short.class ) && wrapper.equals( Short.class ) )
			return true;

		if( primitive.equals( int.class ) && wrapper.equals( Integer.class ) )
			return true;

		if( primitive.equals( long.class ) && wrapper.equals( Long.class ) )
			return true;

		if( primitive.equals( double.class ) && wrapper.equals( Double.class ) )
			return true;

		if( primitive.equals( float.class ) && wrapper.equals( Float.class ) )
			return true;

		if( primitive.equals( boolean.class )
				&& wrapper.equals( Boolean.class ) )
			return true;

		if( primitive.equals( char.class )
				&& wrapper.equals( Character.class ) )
			return true;

		return false;
	}

	public static boolean isAbstract( final Class<?> type )
	{
		return Modifier.isAbstract( type.getModifiers() ) || type.isInterface()
				|| Proxy.isProxyClass( type );
	}

	/**
	 * @param x
	 * @param y
	 * @return <tt>true</tt> if x is an ancestor of y or they wrap/represent the
	 *         same primitive type, <tt>false</tt> otherwise
	 */
	public static boolean isAssignableFrom( final Class<?> x, final Class<?> y )
	{
		return x.isAssignableFrom( y ) || isPrimitiveOf( x, y )
				|| isPrimitiveOf( y, x );
	}

	/**
	 * Get the underlying class for a type, or null if the type is a variable
	 * type. See
	 * <a href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860" >
	 * description</a>
	 * 
	 * @param type the type
	 * @return the underlying class
	 */
	public static Class<?> getClass( final Type type )
	{
		return toClass( type );
	}

	/**
	 * Get the underlying class for a type, or null if the type is a variable
	 * type. See
	 * <a href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860" >
	 * description</a>
	 * 
	 * @param type the type
	 * @return the underlying class
	 */
	public static Class<?> toClass( final Type type )
	{
		if( type instanceof Class )
		{
			// LOG.trace("Type is a class/interface: {}", type);
			return (Class<?>) type;
		}

		if( type instanceof ParameterizedType )
		{
			// LOG.trace("Type is a ParameterizedType: {}", type);
			return toClass( ((ParameterizedType) type).getRawType() );
		}

		if( type instanceof GenericArrayType )
		{
			// LOG.trace("Type is a GenericArrayType: {}", type);
			final Type componentType = ((GenericArrayType) type)
					.getGenericComponentType();
			final Class<?> componentClass = toClass( componentType );
			if( componentClass != null )
				return Array.newInstance( componentClass, 0 ).getClass();
		}
		LOG.warn( "Type is a variable type: {}", type.toString() );
		return null;
	}

	/**
	 * @param returnType the type of the stored property value
	 * @param args the arguments for construction
	 * @return the property value's class instantiated
	 * @deprecated
	 */
	public static <T> T instantiate( final Class<T> returnType,
		final Object... args )
	{
		return Instantiator.instantiate( returnType, args );
	}

	/**
	 * @param serializable the {@link String} representation of the
	 *            {@link Serializable} object
	 * @return the deserialized {@link Object}
	 * @deprecated
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> T deserialize( final String serializable,
		final Class<T> returnType )
	{
		try
		{
			return (T) SerializableUtil.deserialize( serializable,
					returnType.asSubclass( Serializable.class ) );
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * Write the object to a Base64 string
	 * 
	 * @param object the {@link Serializable} object to serialize
	 * @return the {@link String} representation of the {@link Serializable}
	 *         object
	 * @deprecated
	 */
	public static String serialize( final Serializable object )
	{
		try
		{
			return SerializableUtil.serialize( object );
		} catch( final Throwable e )
		{
			return Thrower.rethrowUnchecked( e );
		}
	}

	/**
	 * Get the actual type arguments a child class has used to extend a generic
	 * base class. See
	 * <a href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860" >
	 * description</a>
	 * 
	 * @param genericAncestorType the base class
	 * @param concreteDescendantType the child class
	 * @return a list of the raw classes for the actual type arguments.
	 * @deprecated
	 */
	public static <T> List<Class<?>> getTypeArguments(
		final Class<T> genericAncestorType,
		final Class<? extends T> concreteDescendantType )
	{
		return TypeArguments.of( genericAncestorType, concreteDescendantType );
	}
}
