/* $Id: ce69ef0e4d5d608cd33e74ddbf22ff8ca8be7ec6 $
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

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.agent.AgentID;
import io.coala.capability.CapabilityID;
import io.coala.enterprise.fact.CoordinationFact;
import io.coala.util.TypeArguments;

/**
 * {@link TransactionTypeID}
 * 
 * @param <F>
 * @param <T>
 * @version $Id$
 * @author Rick van Krevelen
 */
public class TransactionTypeID<F extends CoordinationFact, T extends Transaction<F>>
	extends CapabilityID
{

	/** */
	private static final long serialVersionUID = 1L;

	/** the type of {@link CoordinationFact} */
	@JsonIgnore
	private Class<F> factType;

	/** the concrete type of {@link Transaction} */
	@JsonIgnore
	private Class<T> transactionType;

	/**
	 * {@link TransactionTypeID} CDI constructor
	 * 
	 * @param clientID the owner {@link AgentID}
	 * @param cls the concrete {@link T} type
	 */
	@SuppressWarnings( "unchecked" )
	@Inject
	public TransactionTypeID( final AgentID clientID, final Class<T> cls )
	{
		super( clientID, cls );
		final List<Class<?>> typeArgs = TypeArguments
				.of( TransactionTypeID.class, getClass() );
		this.factType = (Class<F>) typeArgs.get( 0 );
		this.transactionType = (Class<T>) typeArgs.get( 1 );
	}

	/**
	 * @return the concrete type {@link F}
	 */
	protected Class<F> getFactType()
	{
		return this.factType;
	}

	/**
	 * @return the concrete type {@link T}
	 */
	protected Class<T> getTransactionType()
	{
		return this.transactionType;
	}
}
