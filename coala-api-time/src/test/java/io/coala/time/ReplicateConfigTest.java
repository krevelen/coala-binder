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
package io.coala.time;

import java.util.Date;

import javax.measure.format.ParserException;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.json.DynaBean.BeanProxy;
import io.coala.log.LogUtil;
import io.coala.name.Id;
import io.coala.name.Identified;

/**
 * {@link ReplicateConfigTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class ReplicateConfigTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( ReplicateConfigTest.class );

	@BeanProxy
	interface TestRepl extends Identified<TestId>
	{

	}

	public static class TestId extends Id<String>
	{

	}

	@Test
	public void test() throws Exception
	{
		final ReplicateConfig conf = ReplicateConfig.getOrCreate();
		LOG.info( "Testing {} defaults: {}",
				ReplicateConfig.class.getSimpleName(), conf );

		try
		{
			LOG.trace( "Id as static TestId  : {}", conf.id( TestId.class ) );
			LOG.trace( "Time unit as Unit    : {}", conf.timeUnit() );
			LOG.trace( "Offset as local Date : {}",
					Date.from( conf.offset() ) );
			LOG.trace( "Duration as Quantity : {}", conf.duration() );
		} catch( final Exception e )
		{
			if( e instanceof ParserException
					&& ((ParserException) e).getParsedString() != null )
				LOG.error( ((ParserException) e).getParsedString() );
			else if( e.getCause() instanceof ParserException )
				LOG.error( ((ParserException) e.getCause()).getParsedString() );
			throw e;
		}
	}

}
