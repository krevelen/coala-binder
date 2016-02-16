/* $Id: 7c77cd2cc5cc0098bd38e4d8e5a80a88e5c8368a $
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
package io.coala.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.logging.log4j.Logger;

import io.coala.log.LogUtil;
import io.coala.util.Util;

/**
 * {@link WebUtil} contains internet-related utility methods
 * 
 * @version $Revision: 296 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class WebUtil implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger(WebUtil.class);

	/** */
	private static final String DEFAULT_CHARSET = "UTF-8";

	/**
	 * @param data
	 * @return
	 */
	public static String urlEncode(final String data)
	{
		return urlEncode(data,DEFAULT_CHARSET);
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
			LOG.warn("Problem encoding agent id using: " + DEFAULT_CHARSET, e);
			return URLEncoder.encode(data);
		}
	}

	/**
	 * {@link WebUtil} constructor
	 */
	private WebUtil()
	{
		// utility class should not produce protected/public instances
	}

}
