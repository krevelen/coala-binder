/* $Id: 6ff1fbf35cf8a0e61d916b594a95a94e27dff7e0 $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/util/FileUtil.java $
 *  
 * Part of the EU project All4Green, see http://www.all4green-project.eu/
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
 * Copyright © 2010-2013 Almende B.V.
 */
package io.coala.resource.x;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import io.coala.exception.x.ExceptionBuilder;
import io.coala.log.LogUtil;

/**
 * {@link FileUtil} provides some file related utilities
 * 
 * @date $Date: 2014-08-08 07:08:29 +0200 (Fri, 08 Aug 2014) $
 * @version $Revision: 349 $ $Author: krevelen $
 * @author <a href="mailto:rick@almende.org">Rick van Krevelen</a>
 */
public class FileUtil // implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger(FileUtil.class);

	/**
	 * {@link FileUtil} constructor
	 */
	private FileUtil()
	{
		// empty
	}

	/** */
	private static final String DEFAULT_CHARSET = "UTF-8";

	/**
	 * @param data
	 * @return
	 */
	public static String urlEncode(final String data)
	{
		return urlEncode(data, DEFAULT_CHARSET);
	}

	/**
	 * @param data
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String urlEncode(final String data, final String charset)
	{
		try
		{
			return URLEncoder.encode(data, charset);
		} catch (final UnsupportedEncodingException e)
		{
			LOG.warn("Problem encoding agent id using: " + charset, e);
			return URLEncoder.encode(data);
		}
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream(final File path) throws IOException
	{
		return toInputStream(path.getPath());
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream(final URI path) throws IOException
	{
		try
		{
			return toInputStream(path.toURL());
		} catch (final MalformedURLException e)
		{
			throw ExceptionBuilder.unchecked("ILLEGAL: uri " + path, e).build();
		}
	}

	/**
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream(final URL path) throws IOException
	{
		return toInputStream(path.toExternalForm());
	}

	/**
	 * Searches the file system first and then the context class path for a file
	 * 
	 * @param path an absolute path in the file system or (context) classpath
	 * @return an {@link InputStream} for the specified {@code path}
	 * @throws IOException
	 */
	public static InputStream toInputStream(final String path)
			throws IOException
	{
		if (path == null)
			throw ExceptionBuilder.unchecked("NOT SET: path").build();

		final File file = new File(path);
		if (file.exists())
		{
			LOG.trace("Found '" + path + "' at location: "
					+ file.getAbsolutePath());

			// if (path.exists() && path.isFile())
			return new FileInputStream(file);
		}

		final File userFile = new File(System.getProperty("user.dir") + path);
		if (userFile.exists())
		{
			LOG.trace("Found '" + path + "' at location: "
					+ userFile.getAbsolutePath());

			// if (path.exists() && path.isFile())
			return new FileInputStream(userFile);
		}

		try
		{
			final URL url = new URL(path);
			LOG.trace("Downloading file from " + path);
			return url.openStream();
		} catch (final MalformedURLException e)
		{
			// ignore
		}

		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		// FileUtil.class.getClassLoader()
		final URL resourcePath = cl.getResource(path);
		if (resourcePath == null)
		{
			throw new FileNotFoundException("File not found " + path
					+ ", tried " + file.getAbsolutePath() + " and classpath");
		}
		LOG.trace("Found '" + path + "' in classpath: " + resourcePath);
		return cl.getResourceAsStream(path);
	}

	/**
	 * @param path
	 * @return
	 */
	public static OutputStream toOutputStream(final String path)
	{
		return toOutputStream(new File(path), true);
	}

	/**
	 * @param path
	 * @return
	 */
	public static OutputStream toOutputStream(final File file,
			final boolean append)
	{
		if (file == null)
			throw ExceptionBuilder.unchecked("NOT SET: file").build();

		try
		{
			if (file.createNewFile())
				LOG.info("Created '" + file.getName() + "' at location: "
						+ file.getAbsolutePath());
			else
				LOG.debug("Found '" + file.getName() + "' at location: "
						+ file.getAbsolutePath());
			return new FileOutputStream(file, append);
		} catch (final IOException e)
		{
			throw ExceptionBuilder.unchecked("NOT AVAILABLE: file " + file, e)
					.build();
		}
	}

}
