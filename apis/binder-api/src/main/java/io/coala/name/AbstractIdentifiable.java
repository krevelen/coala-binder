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
package io.coala.name;

import java.io.Serializable;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.coala.json.JsonUtil;
import io.coala.log.InjectLogger;

/**
 * {@link AbstractIdentifiable}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <ID> the {@link Identifier} type
 */
@JsonInclude(Include.NON_NULL)
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "class")
public abstract class AbstractIdentifiable<ID extends Identifier<?, ?>>
		implements Identifiable<ID>, Serializable
{

	/** */
	private static final long serialVersionUID = 1L;

	/** the injected {@link Logger} */
	@InjectLogger
	private transient Logger LOG;

	/** */
	@JsonProperty("id")
	private ID iD;

	/**
	 * {@link AbstractIdentifiable} zero-arg bean constructor
	 */
	protected AbstractIdentifiable()
	{
	}

	/**
	 * {@link AbstractIdentifiable} constructor
	 * 
	 * @param ID
	 */
	protected AbstractIdentifiable(final ID ID)
	{
		setID(ID);
	}

	@Override
	public ID getID()
	{
		return this.iD;
	}

	/**
	 * @param iD the {@link ID} identifying this object
	 */
	protected void setID(final ID iD)
	{
		if (this.iD == null)
			this.iD = iD;
		else
			LOG.warn("ID already set, ignoring: " + iD);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(final Identifiable<ID> other)
	{
		return ((Comparable<ID>) getID()).compareTo(other.getID());
	}

	@Override
	public String toString()
	{
		try
		{
			return getClass().getSimpleName() + JsonUtil.toString(this);
		} catch (final Throwable t)
		{
			t.printStackTrace();
			return getClass().getName();
		}
	}

	@Override
	public int hashCode()
	{
		return getID() == null ? super.hashCode() : getID().hashCode();
	}

	@Override
	public boolean equals(final Object other)
	{
		if (other == null || other.getClass() != getClass())
			return false;

		@SuppressWarnings("unchecked")
		final AbstractIdentifiable<ID> that = getClass().cast(other);
		return getID().equals(that.getID());
	}

}
