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
package io.coala.capability.interact;

import org.joda.time.Duration;

import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import io.coala.message.Message;
import rx.Observer;

/**
 * {@link SendingCapability}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public interface SendingCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 * 
	 * @version $Id$
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	interface Factory extends CapabilityFactory<SendingCapability>
	{
		// empty
	}

	/**
	 * @param msg the {@link Message} to transport
	 * @throws Exception
	 */
	void send(Message<?> msg) throws Exception;

	/**
	 * @param msg the {@link Message} to transport
	 * @param timeout any non-{@code null} positive {@link Duration} indicates
	 *            an allowed delivery timeout
	 * @throws Exception
	 */
	void send(Message<?> msg, Duration timeout) throws Exception;

	/**
	 * @param timeout any non-{@code null} positive {@link Duration} indicates
	 *            an allowed delivery timeout
	 * @return an {@link Observer} of outgoing {@link Message}s
	 */
	<T extends Message<?>> Observer<T> outgoing(Duration timeout);

}
