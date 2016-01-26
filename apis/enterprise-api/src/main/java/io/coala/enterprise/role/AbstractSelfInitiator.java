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
package io.coala.enterprise.role;

import io.coala.bind.Binder;
import io.coala.enterprise.fact.CoordinationFact;
import io.coala.log.InjectLogger;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * {@link AbstractSelfInitiator}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <F> the {@link CoordinationFact} type being handled
 */
public abstract class AbstractSelfInitiator<F extends CoordinationFact>
		extends AbstractExecutor<F>implements Initiator<F>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link AbstractSelfInitiator} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected AbstractSelfInitiator(final Binder binder)
	{
		super(binder);
	}

	protected void onExpiredRequest(final F request)
	{
		logIgnore(request, true);
	}

	protected void onExpiredRequestCancellation(final F cancel)
	{
		logIgnore(cancel, true);
	}

	protected void onAllowedRequestCancellation(final F allow)
	{
		logIgnore(allow, false);
	}

	protected void onRefusedRequestCancellation(final F refuse)
	{
		logIgnore(refuse, false);
	}

	protected void onPromised(final F promise)
	{
		logIgnore(promise, false);
	}

	protected void onCancelledPromise(final F cancel)
	{
		logIgnore(cancel, false);
	}

	protected void onDeclined(final F decline)
	{
		logIgnore(decline, false);
	}

	protected abstract void onStated(F state);

	protected void onCancelledState(final F cancel)
	{
		logIgnore(cancel, false);
	}

	protected void onExpiredAcceptCancellation(final F cancel)
	{
		logIgnore(cancel, true);
	}

	protected void onAllowedAcceptCancellation(final F allow)
	{
		logIgnore(allow, false);
	}

	protected void onRefusedAcceptCancellation(final F refuse)
	{
		logIgnore(refuse, false);
	}

}
