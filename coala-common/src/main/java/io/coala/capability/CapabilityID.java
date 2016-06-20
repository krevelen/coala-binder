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
package io.coala.capability;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.agent.AgentID;
import io.coala.model.ModelComponentID;
import io.coala.model.ModelID;
import io.coala.name.AbstractIdentifier;

/**
 * {@link CapabilityID}
 */
@Deprecated
public class CapabilityID extends ModelComponentID<String>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private AgentID clientID;

	/** the concrete type of {@link Capability} */
	@JsonIgnore
	private Class<? extends Capability<?>> serviceType;

	/**
	 * {@link CapabilityID} constructor
	 * 
	 * @param clientID
	 * @param serviceType
	 */
	public <S extends Capability<?>> CapabilityID( final AgentID clientID,
		final Class<S> serviceType )
	{
		this( clientID, serviceType.getName() );
		this.serviceType = serviceType;
	}

	/**
	 * {@link CapabilityID} constructor
	 * 
	 * @param clientID
	 * @param value
	 */
	public CapabilityID( final AgentID clientID, final String value )
	{
		super( clientID.getModelID(), value );
		this.clientID = clientID;
	}

	@JsonIgnore
	@Override
	public ModelID getModelID()
	{
		return this.clientID.getModelID();
	}

	/**
	 * @return the identifier of the {@link AgentID} using this
	 *         {@link Capability}
	 */
	public AgentID getOwnerID()
	{
		return this.clientID;
	}

	/**
	 * @return
	 */
	public Class<? extends Capability<?>> getType()
	{
		return this.serviceType;
	}

	/** @see AbstractIdentifier#toString() */
	@Override
	public String toString()
	{
		return getOwnerID().toString() + "::" + getType().getSimpleName();
	}

	/** @see AbstractIdentifier#hashCode() */
	@Override
	public int hashCode()
	{
		// FIXME apply some common strategy via Visitor design pattern

		final int prime = 31;
		int result = super.hashCode(); // ID value hash code
		result = prime * result
				+ (getOwnerID() == null ? 0 : getOwnerID().hashCode());
		return result;
	}

	/** @see AbstractIdentifier#equals(Object) */
	@Override
	public boolean equals( final Object other )
	{
		// FIXME apply some common strategy via Visitor design pattern

		if( this == other ) return true;

		if( !super.equals( other ) || getClass() != other.getClass() )
			return false;

		final CapabilityID that = (CapabilityID) other;
		if( getOwnerID() == null )
		{
			if( that.getOwnerID() != null ) return false;
		} else if( !getOwnerID().equals( that.getOwnerID() ) ) return false;
		return super.equals( other );
	}

	/** @see Comparable#compareTo(Object) */
	@Override
	public int compareTo( final AbstractIdentifier<String> other )
	{
		// FIXME apply some common strategy via Visitor design pattern

		final int modelIDCompare = getOwnerID()
				.compareTo( ((CapabilityID) other).getOwnerID() );
		if( modelIDCompare != 0 ) return modelIDCompare;
		return getValue().compareTo( other.getValue() );
	}

}
