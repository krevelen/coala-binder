/* $Id$
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
 * Copyright (c) 2010-2014 Almende B.V. 
 */
package io.coala.enterprise.test.impl;

import io.coala.bind.Binder;
import io.coala.enterprise.organization.AbstractOrganization;
import io.coala.enterprise.test.TestExecutorOrganization;
import io.coala.enterprise.test.TestFact;
import io.coala.log.InjectLogger;

import javax.inject.Inject;

import org.apache.log4j.Logger;

/**
 * {@link TestExecutorOrganizationImpl}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class TestExecutorOrganizationImpl extends AbstractOrganization
		implements TestExecutorOrganization
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link TestExecutorOrganizationImpl} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected TestExecutorOrganizationImpl(final Binder binder)
	{
		super(binder);
	}

	// /** @see TestInitiatorOrganization#getWorld() */
	// @Override
	// public TestExecutorWorld getWorld()
	// {
	// return this.world;
	// }

	/** @see TestExecutorOrganization#getTestFactExecutor() */
	@Override
	public TestFact.Executor getTestFactExecutor()
	{
		return getBinder().inject(TestFact.Executor.class);
	}
}
