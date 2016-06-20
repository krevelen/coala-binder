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
 */
package io.coala.enterprise.role;

import io.coala.bind.Binder;
import io.coala.enterprise.fact.CoordinationFact;
import io.coala.log.InjectLogger;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * {@link AbstractExecutor}
 * 
 * @version $Id$
 * 
 * @param <F> the {@link CoordinationFact} type being handled
 */
public abstract class AbstractExecutor<F extends CoordinationFact>
	extends AbstractActorRole<F> implements Executor<F>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link AbstractExecutor} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected AbstractExecutor( final Binder binder )
	{
		super( binder );
	}

	protected abstract void onRequested( F request );

	protected void onCancelledRequest( final F cancel )
	{
		logIgnore( cancel, false );
	}

	protected void onExpiredPromise( final F promise )
	{
		logIgnore( promise, true );
	}

	protected void onExpiredPromiseCancellation( final F cancel )
	{
		logIgnore( cancel, true );
	}

	protected void onAllowedPromiseCancellation( final F allow )
	{
		logIgnore( allow, false );
	}

	protected void onRefusedPromiseCancellation( final F refuse )
	{
		logIgnore( refuse, false );
	}

	protected void onExpiredState( final F state )
	{
		logIgnore( state, true );
	}

	protected void onExpiredStateCancellation( final F cancel )
	{
		logIgnore( cancel, true );
	}

	protected void onAllowedStateCancellation( final F allow )
	{
		logIgnore( allow, false );
	}

	protected void onRefusedStateCancellation( final F refuse )
	{
		logIgnore( refuse, false );
	}

	protected void onAccepted( final F accept )
	{
		logIgnore( accept, false );
	}

	protected void onCancelledAccept( final F cancel )
	{
		logIgnore( cancel, false );
	}

	protected void onRejected( final F reject )
	{
		logIgnore( reject, false );
	}

}
