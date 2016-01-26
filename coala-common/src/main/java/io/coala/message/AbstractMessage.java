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
package io.coala.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.agent.AgentID;
import io.coala.event.AbstractTimedEvent;
import io.coala.model.ModelComponent;
import io.coala.model.ModelComponentID;

/**
 * {@link AbstractMessage}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <ID> the {@link MessageID} type
 */
public abstract class AbstractMessage<ID extends MessageID<?, ?>>
		extends AbstractTimedEvent<ID>implements Message<ID>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private AgentID senderID;

	/** */
	private AgentID receiverID;

	/**
	 * {@link AbstractMessage} zero-arg bean constructor
	 */
	protected AbstractMessage()
	{
		super();
	}

	/**
	 * {@link AbstractMessage} constructor
	 * 
	 * @param id
	 * @param producerID
	 * @param senderID
	 * @param receiverID
	 */
	// @Inject
	protected AbstractMessage(final ID id, final ModelComponentID<?> producerID,
			final AgentID senderID, final AgentID receiverID)
	{
		super(id, null, null);
		this.senderID = senderID;
		this.receiverID = receiverID;
	}

	/**
	 * {@link AbstractMessage} constructor
	 * 
	 * @param modelID
	 * @param producerID
	 * @param receiverID
	 */
	public AbstractMessage(final ID id, final ModelComponent<?> producer,
			final AgentID receiverID)
	{
		super(id, producer);
		this.senderID = producer.getOwnerID();
		this.receiverID = receiverID;
	}

	@JsonIgnore
	@Override
	public AgentID getProducerID()
	{
		return getSenderID();
	}

	@JsonIgnore
	@Override
	public AgentID getOwnerID()
	{
		return getSenderID();
	}

	@Override
	public AgentID getSenderID()
	{
		return this.senderID;
	}

	@Override
	public AgentID getReceiverID()
	{
		return this.receiverID;
	}

}
