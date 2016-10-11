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
package io.coala.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;

/**
 * {@link XmlTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class XmlTest
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( XmlTest.class );

	@Test
	public void testStAX() throws IOException
	{
		LOG.info( "Started StAX test" );
		final String[] path = { "RDF", "Person" };
		final URI uri = URI.create( "http://www.w3.org/People/EM/contact" );
		final HttpURLConnection con = (HttpURLConnection) uri.toURL()
				.openConnection();
		con.setRequestMethod( "GET" );
		con.setRequestProperty( "User-Agent", "Mozilla/5.0" );
		try( final InputStream is = con.getInputStream() )
		{
//			int responseCode = con.getResponseCode();

			XmlUtil.matchElementPath( is, path ).subscribe( xml ->
			{
				LOG.trace( "Matched path: {} for <{}:{}/> at uri: {} [{}:{}]",
						path, xml.getPrefix(), xml.getLocalName(), uri,
						xml.getLocation().getLineNumber(),
						xml.getLocation().getColumnNumber() );
			}, e ->
			{
				LOG.error( "Problem", e );
			}, () ->
			{
				LOG.trace( "Completed streaming XML from uri: {}", uri );
			} );
			LOG.info( "Completed StAX test" );
		} finally
		{
			con.disconnect();
		}
	}
}
