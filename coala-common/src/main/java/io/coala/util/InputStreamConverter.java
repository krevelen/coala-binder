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
package io.coala.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.aeonbits.owner.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link AgentConfigConverter}
 */
public class InputStreamConverter implements Converter<InputStream>
{
	/** */
	private static final Logger LOG = LogManager
			.getLogger( InputStreamConverter.class );

	@Override
	public InputStream convert( final Method method, final String input )
	{
		try
		{
			return input == null ? null : FileUtil.toInputStream( input );
		} catch( final IOException e )
		{
			LOG.info( "Ignoring resource {}: {}", input, e.getMessage() );
			return null;
		}
	}
}