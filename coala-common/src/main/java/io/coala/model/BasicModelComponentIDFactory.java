/* $Id: 793ed58b1615e778bef02de7b8c357f8e45c374c $
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
package io.coala.model;

import java.util.Objects;

import io.coala.agent.AgentID;
import io.coala.time.ClockID;

/**
 * {@link BasicModelComponentIDFactory}
 * 
 * @version $Id: 793ed58b1615e778bef02de7b8c357f8e45c374c $
 * @author Rick van Krevelen
 */
@Deprecated
public class BasicModelComponentIDFactory implements ModelComponentIDFactory
{

	/** */
	private ModelID modelID;

	/**
	 * {@link BasicModelComponentIDFactory} zero-arg bean constructor
	 */
	public BasicModelComponentIDFactory()
	{
		// empty
	}

	@Override
	public ModelID getModelID()
	{
		return this.modelID;
	}

	/**
	 * @return
	 */
	protected void checkInitialized( final boolean notNull )
	{
		if( notNull ) Objects.requireNonNull( getModelID() );
	}

	@Override
	public ModelComponentIDFactory initialize( final ModelID modelID )
	{
		this.modelID = modelID;
		return this;
	}

	@Override
	public AgentID createAgentID( final String value )
	{
		checkInitialized( true );
		final String modelPrefix = getModelID().getValue()
				+ ModelComponentID.PATH_SEP;
		if( value.startsWith( modelPrefix ) ) return new AgentID( getModelID(),
				value.substring( modelPrefix.length() ) );
		return new AgentID( getModelID(), value );
	}

	@Override
	public ClockID createClockID( final String value )
	{
		checkInitialized( true );
		return new ClockID( getModelID(),
				value == null ? getModelID().getValue() : value );
	}

}
