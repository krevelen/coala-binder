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
package io.coala.eve;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.Capability;
import io.coala.capability.interact.ExposingCapability;
import io.coala.exception.CoalaException;
import io.coala.log.InjectLogger;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * {@link EveExposingCapability}
 */
public class EveExposingCapability extends BasicCapability implements
		ExposingCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link EveExposingCapability} constructor
	 * 
	 * @param clientID
	 */
	@Inject
	private EveExposingCapability(final Binder binder)
	{
		super(binder);
	}

	@Override
	public <T extends Serializable> void expose(final Class<T> api,
			final T implementation)
	{
		try
		{
			EveAgentManager.getInstance(getBinder()).setExposed(
					getID().getOwnerID(), implementation);
			LOG.trace("Exposed object: " + implementation);
		} catch (final Exception e)
		{
			throw new RuntimeException("Problem exposing object: "
					+ implementation, e);
		}
	}

	@Override
	public <T extends Capability<?> & Serializable> void expose(
			final Class<T> api)
	{
		expose(api, getBinder().inject(api));
	}

	@Override
	public List<URI> getAddresses()
	{
		final List<URI> result = new ArrayList<>();
		try
		{
			for (String address : EveUtil.getAddresses(getID().getOwnerID()))
				result.add(URI.create(address));
		} catch (final CoalaException e)
		{
			LOG.warn("Problem getting/parsing owner address(es)", e);
		}
		return result;
	}
}
