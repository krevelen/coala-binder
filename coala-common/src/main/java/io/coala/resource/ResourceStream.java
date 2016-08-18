/* $Id: eaf4f804beb5113e463a5e5f6c5ea9962fdb2740 $
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
package io.coala.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.coala.function.ThrowableUtil;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.xml.XmlContext;

/**
 * {@link ResourceStream}
 * 
 * @version $Id: eaf4f804beb5113e463a5e5f6c5ea9962fdb2740 $
 * @author Rick van Krevelen
 */
@Deprecated
public class ResourceStream
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( ResourceStream.class );

	/** */
	private final InputStream stream;

	/** */
	private final ResourceType type;

	/** */
	private final String path;

	/**
	 * {@link ResourceStream} constructor
	 * 
	 * @param stream
	 * @param type
	 * @param path
	 */
	private ResourceStream( final InputStream stream, final ResourceType type,
		final String path )
	{
		this.stream = stream;
		this.type = type;
		this.path = path;
	}

	// private StackTraceElement[] read = null;

	/**
	 * @return the {@link InputStream}
	 */
	public InputStream getStream()
	{
		// if (this.read != null)
		// throw new RuntimeException("Already read, stack trace: "
		// + Arrays.asList(this.read).toString()
		// .replace(", ", "\n\t\tat ") + "\n\n");
		// this.read = Thread.currentThread().getStackTrace();
		return this.stream;
	}

	/**
	 * @return the {@link ResourceType}
	 */
	public ResourceType getType()
	{
		return this.type;
	}

	/**
	 * @return the path
	 */
	public String getPath()
	{
		return this.path;
	}

	/** Writes the {@link InputStream} to a {@link String} */
	@Override
	public String toString()
	{
		InputStream stream = null;
		try
		{
			stream = getStream();
			return String.format( "[source: %s%s >>> %s]", getPath(),
					getType() == null ? "" : ", type: " + getType(),
					IOUtils.toString( stream ) );
		} catch( final Throwable e )
		{
			LOG.error( "Problem while streaming from " + getPath(), e );
			return e.getMessage();
		} finally
		{
			if( stream != null ) try
			{
				stream.close();
			} catch( final IOException e )
			{
				LOG.error( "Problem with stream cleanup", e );
			}
		}
	}

	/**
	 * @return the {@link InputStream} unmarshalled into a {@link JsonNode}
	 */
	public JsonNode toJSON()
	{
		try
		{
			return JsonUtil.toTree( IOUtils.toString( getStream() ) );
		} catch( final IOException e )
		{
			ThrowableUtil.throwAsUnchecked( e );
			return null;
		}
	}

	/**
	 * TODO verify {@link ResourceType}
	 * 
	 * @return the {@link InputStream} unmarshalled into an object of the
	 *         specified {@code resultType}
	 */
	public <T> T toJSON( final Class<T> resultType )
	{
		try
		{
			return JsonUtil.valueOf( IOUtils.toString( getStream() ),
					resultType );
			// valueOf(getStream(), resultType);
		} catch( final IOException e )
		{
			ThrowableUtil.throwAsUnchecked( e );
			return null;
		}
	}

	/**
	 * TODO verify {@link ResourceType}
	 * 
	 * @return the unmarshalled {@code resultType} instance using the specified
	 *         {@link XmlContext}
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T toXML( final XmlContext<?> context, final Class<T> resultType )
	{
		try
		{
			return (T) JAXBIntrospector.getValue( context.getUnmarshaller()
					.unmarshal( new StreamSource( getStream() ), resultType ) );
		} catch( final JAXBException e )
		{
			ThrowableUtil.throwAsUnchecked( e );
			return null;
		}
	}

	/**
	 * @param obj
	 * @return the default {@link Object#toString()} result
	 */
	private static String toHashString( final Object obj )
	{
		return obj.getClass().getName() + "@"
				+ Integer.toHexString( obj.hashCode() );
	}

	/**
	 * @param is
	 * @param type
	 * @param path
	 * @return
	 */
	public static ResourceStream of( final InputStream is,
		final ResourceType type, final String path )
	{
		return new ResourceStream( is, type, path );
	}

	/**
	 * @param bytes
	 * @param type
	 * @return
	 */
	public static ResourceStream of( final byte[] bytes,
		final ResourceType type )
	{
		return of( new ByteArrayInputStream( bytes ), type,
				toHashString( bytes ) );
	}

	/**
	 * @param string
	 * @param type
	 * @return
	 */
	public static ResourceStream of( final String string,
		final ResourceType type )
	{
		return of( string, type, string );
	}

	/**
	 * @param source
	 * @param type
	 * @param stringRepr
	 * @return
	 */
	public static ResourceStream of( final Object source,
		final ResourceType type, final String stringRepr )
	{
		return of( new ByteArrayInputStream( stringRepr.getBytes() ), type,
				toHashString( source ) );
	}
}