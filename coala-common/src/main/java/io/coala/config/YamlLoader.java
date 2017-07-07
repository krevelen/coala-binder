/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Properties;

import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.loaders.Loader;

import io.coala.util.FileUtil;

/** {@link YamlLoader} is a {@link Loader} for YAML format configurations */
public class YamlLoader implements Loader
{
	private static final long serialVersionUID = 1L;

	private static boolean registered = false;

	public synchronized static void register()
	{
		if( !registered )
		{
			ConfigFactory.registerLoader( new YamlLoader() );
			registered = true;
		}
	}

	@Override
	public boolean accept( final URI uri )
	{
		final String path = uri.toASCIIString();
		if( !path.toLowerCase().contains( "yaml" ) ) return false;
		try
		{
			uri.toURL();
			return true;
		} catch( final MalformedURLException ex )
		{
			return new File( path ).exists() || Thread.currentThread()
					.getContextClassLoader().getResource( path ) != null;
		}
	}

	@Override
	public void load( final Properties result, final URI uri )
		throws IOException
	{
		try( final InputStream is = FileUtil.toInputStream( uri ) )
		{
			result.putAll( YamlUtil.flattenYaml( is ) );
		}
	}

	@Override
	public String defaultSpecFor( final String uriPrefix )
	{
		return uriPrefix + ".yaml";
	}

}