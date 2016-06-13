/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/event/AbstractTimedEvent.java $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.agent.AgentID;
import io.coala.model.ModelComponent;
import io.coala.model.ModelComponentID;
import io.coala.time.Instant;

/**
 * {@link AbstractTimedEvent}
 * 
 * @date $Date: 2014-08-04 14:19:04 +0200 (Mon, 04 Aug 2014) $
 * @version $Revision: 336 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <ID> the type of {@link TimedEventID} for time-ordered event identity
 * @param <THIS> the (sub)type of {@link TimedEvent} to build
 */
public abstract class AbstractTimedEvent<ID extends TimedEventID<?, ?>>
		extends AbstractEvent<ID>implements TimedEvent<ID>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link AbstractTimedEvent} zero-arg bean constructor
	 */
	protected AbstractTimedEvent()
	{
		super();
	}

	/**
	 * {@link AbstractTimedEvent} constructor
	 * 
	 * @param id
	 * @param producer
	 */
	public AbstractTimedEvent(final ID id, final ModelComponent<?> producer)
	{
		super(id, producer);
	}

	/**
	 * {@link AbstractTimedEvent} constructor
	 * 
	 * @param id
	 * @param ownerID
	 * @param producerID
	 */
	public AbstractTimedEvent(final ID id, final AgentID ownerID,
			final ModelComponentID<?> producerID)
	{
		super(id, ownerID, producerID);
	}

	@JsonIgnore
	@Override
	public Instant<?> getTime()
	{
		return getID().getTime();
	}

}