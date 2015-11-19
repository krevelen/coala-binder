/* $Id: 330674a63ce0cac9d247ae33499f3b412f93b729 $
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
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.guice;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import io.coala.agent.BasicAgent;
import io.coala.bind.Binder;
import io.coala.log.InjectLogger;

/**
 * {@link TestAgent}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class TestAgent extends BasicAgent
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link TestAgent} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	public TestAgent(final Binder host)
	{
		super(host);
	}

	@Override
	public void activate()
	{
		LOG.info("Executed");
	}

}
