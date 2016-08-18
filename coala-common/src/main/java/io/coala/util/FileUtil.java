/* $Id: 1fb88305289e7f66f752387a4991fc8f82820c4d $
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.logging.log4j.Logger;

import io.coala.exception.ExceptionFactory;
import io.coala.function.ThrowableUtil;
import io.coala.log.LogUtil;

/**
 * {@link FileUtil} provides some file related utilities
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class FileUtil implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( FileUtil.class );

	/**
	 * {@link FileUtil} constructor
	 */
	private FileUtil()
	{
		// empty
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream( final File path )
		throws IOException
	{
		return toInputStream( path.getPath() );
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream( final URI path ) throws IOException
	{
		try
		{
			return toInputStream( path.toURL() );
		} catch( final IOException e )
		{
			throw e;
		} catch( final Exception e )
		{
			ThrowableUtil.throwAsUnchecked( e );
			return null;
		}
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream( final URL path ) throws IOException
	{
		return toInputStream( path.toExternalForm() );
	}

	/**
	 * Searches the file system first and then the context class path for a file
	 * 
	 * @param path an absolute path in the file system or (context) classpath
	 * @return an {@link InputStream} for the specified {@code path}
	 * @throws IOException e.g. if the file was not found
	 */
	public static InputStream toInputStream( final String path )
		throws IOException
	{
		final File file = new File( path );
		if( file.exists() )
		{
			LOG.debug( "Found '" + path + "' at location: "
					+ file.getAbsolutePath() );

			// if (path.exists() && path.isFile())
			return new FileInputStream( file );
		}

		try
		{
			final URL url = new URL( path );
			LOG.trace( "Downloading '" + path + "'" );
			return url.openStream();
		} catch( final MalformedURLException e )
		{
			// ignore
		}

		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		// FileUtil.class.getClassLoader()
		final URL resourcePath = cl.getResource( path );
		if( resourcePath == null ) { throw ExceptionFactory.createUnchecked(
				"File not found, looked in {} and classpath: {}",
				file.getAbsolutePath(), path ); }
		LOG.trace( "Found '" + path + "' in classpath: " + resourcePath );
		return cl.getResourceAsStream( path );
	}

	/**
	 * @param path
	 * @return
	 */
	public static OutputStream toOutputStream( final String path )
		throws IOException
	{
		return toOutputStream( new File( path ), true );
	}

	/**
	 * @param path
	 * @return
	 */
	public static OutputStream toOutputStream( final File file,
		final boolean append ) throws IOException
	{
		if( file.createNewFile() )
			LOG.info( "Created '" + file.getName() + "' at location: "
					+ file.getAbsolutePath() );
		else
			LOG.debug( "Found '" + file.getName() + "' at location: "
					+ file.getAbsolutePath() );
		return new FileOutputStream( file, append );
	}

}
