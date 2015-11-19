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
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.event;

import org.slf4j.Logger;

import io.coala.agent.AgentID;
import io.coala.lifecycle.ActivationType;
import io.coala.lifecycle.LifeCycleHooks;
import io.coala.log.InjectLogger;
import io.coala.model.ModelComponent;
import io.coala.model.ModelComponentID;
import io.coala.process.AbstractJob;

/**
 * {@link AbstractEvent}
 * 
 * @version $Id $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <ID> the concrete {@link EventID} type
 * @param <THIS> the concrete {@link AbstractEvent} type
 */
public abstract class AbstractEvent<ID extends EventID<?>> extends
		AbstractJob<ID> implements Event<ID>, LifeCycleHooks
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private transient Logger log;

	/** */
	private ModelComponentID<?> producerID;

	/** */
	private AgentID ownerID;

	/**
	 * {@link AbstractEvent} constructor
	 * 
	 * @param id
	 * @param producerID
	 */
	protected AbstractEvent(final ID id, final ModelComponent<?> producer)
	{
		this(id, producer.getOwnerID(), producer.getID());
	}

	/**
	 * {@link AbstractEvent} constructor
	 * 
	 * @param id
	 * @param producerID
	 */
	protected AbstractEvent(final ID id, final AgentID ownerID,
			final ModelComponentID<?> producerID)
	{
		super(id);
		this.ownerID = ownerID;
		this.producerID = producerID;
	}

	/**
	 * {@link AbstractEvent} zero-arg bean constructor
	 */
	protected AbstractEvent()
	{
		super();
	}

	/** @param producerID */
	protected synchronized void setProducerID(
			final ModelComponentID<?> producerID)
	{
		this.producerID = producerID;
	}

	@Override
	public AgentID getOwnerID()
	{
		return this.ownerID;
	}

	@Override
	public synchronized ModelComponentID<?> getProducerID()
	{
		return this.producerID;
	}

	@Override
	public void initialize()
	{
		// override me
	}

	@Override
	public void activate()
	{
		// empty
	}

	@Override
	public void deactivate()
	{
		// empty
	}

	@Override
	public void finish()
	{
		// override me
	}

	@Override
	public ActivationType getActivationType()
	{
		return ActivationType.ACTIVATE_ONCE;
	}

}
