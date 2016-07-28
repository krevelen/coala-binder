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
package io.coala.enterprise.transaction;

import javax.inject.Inject;

import io.coala.agent.AgentID;
import io.coala.bind.Binder;
import io.coala.capability.AbstractCapability;
import io.coala.capability.CapabilityID;
import io.coala.enterprise.fact.CoordinationFact;
import io.coala.enterprise.service.FactPersisterService;
import io.coala.time.SimTime;
import rx.Observable;

/**
 * {@link AbstractTransaction}
 * 
 * @version $Id$
 * 
 * @param <F> the (super)type of {@link CoordinationFact}
 * @param <THIS> the concrete type of {@link AbstractTransaction}
 */
@Deprecated
public class AbstractTransaction<F extends CoordinationFact, THIS extends AbstractTransaction<F, THIS>>
	extends AbstractCapability<CapabilityID> implements Transaction<F>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link AbstractTransaction} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@SuppressWarnings( "unchecked" )
	@Inject
	protected AbstractTransaction( final Binder binder )
	{
		super( null, binder );
		setID( new TransactionTypeID<F, THIS>( getBinder().getID(),
				(Class<THIS>) getClass() ) );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Observable<F> facts()
	{
		return getBinder().inject( FactPersisterService.class )
				.find( ((TransactionTypeID<F, THIS>) getID()).getFactType() );
	}

	@Override
	public AgentID getOwnerID()
	{
		return super.getID().getOwnerID();
	}

	@Override
	public SimTime getTime()
	{
		return (SimTime) null;
	}

}