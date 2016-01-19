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
package io.coala.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.name.AbstractIdentifier;

/**
 * {@link ModelComponentID}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class ModelComponentID<T extends Serializable & Comparable<T>>
		extends AbstractIdentifier<T>
{

	/** */
	public static final String ORPHAN_MODEL_ID = "<orphan>";

	/** */
	public static final String PATH_SEP_PROPERTY = "io.coala.model-id.path-sep";

	/** */
	public static final char PATH_SEP_DEFAULT = '-';

	/** */
	public static final char PATH_SEP = Character
			.valueOf(System.getProperty(PATH_SEP_PROPERTY,
					Character.toString(PATH_SEP_DEFAULT)).charAt(0));

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private ModelID modelID = null;

	/** */
	private ModelComponentID<?> parentID = null;

	/**
	 * {@link ModelComponentID} zero-arg bean constructor
	 */
	protected ModelComponentID()
	{
		super();
	}

	/**
	 * {@link ModelComponentID} constructor for orphans
	 * 
	 * @param modelID
	 * @param value
	 */
	// @Inject
	public ModelComponentID(final ModelID modelID, final T value)
	{
		super(value);
		setModelID(modelID);
	}

	/**
	 * {@link ModelComponentID} constructor for children
	 * 
	 * @param parentID
	 * @param value
	 */
	// @Inject
	public ModelComponentID(final ModelComponentID<?> parentID, final T value)
	{
		super(value);
		setParentID(parentID);
	}

	/**
	 * @param modelID
	 */
	protected synchronized final void setModelID(final ModelID modelID)
	{
		this.modelID = modelID;
	}

	/**
	 * @param parentID
	 */
	protected synchronized final void setParentID(
			final ModelComponentID<?> parentID)
	{
		this.parentID = parentID;
		setModelID(parentID.getModelID());
	}

	/**
	 * @return the {@link ModelID} identifying this {@link ModelComponent}'s
	 *         owner model
	 */
	public synchronized ModelID getModelID()
	{
		return this.modelID;
	}

	/**
	 * @return the {@link ModelComponentID} identifying this
	 *         {@link ModelComponent}'s parent {@link ModelComponent}
	 */
	public synchronized ModelComponentID<?> getParentID()
	{
		return this.parentID;
	}

	@JsonIgnore
	public boolean isOrphan()
	{
		return getParentID() == null;
	}

	@Override
	public String toString()
	{
		return getModelID() == this ? getValue().toString()
				: (/*!isOrphan() ? getParentID().toString()
						:*/ getModelID() == null ? ORPHAN_MODEL_ID
								: getModelID().getValue())
						+ PATH_SEP + getValue();
	}

	@Override
	public int hashCode()
	{
		// FIXME apply some common strategy via Visitor design pattern

		final int prime = 31;
		int result = super.hashCode(); // ID value hash code
		result = prime * result
				+ ((getModelID() == null || getModelID() == this) ? 0
						: getModelID().hashCode());
		result = prime * result
				+ ((getParentID() == null || getParentID() == this) ? 0
						: getParentID().hashCode());
		result = prime * result + ((getValue() == null || getValue() == this)
				? 0 : getValue().hashCode());

		return result;
	}

	@Override
	public boolean equals(final Object other)
	{
		// FIXME apply some common strategy via Visitor design pattern

		if (this == other)
			return true;

		if (!super.equals(other) || getClass() != other.getClass())
			return false;

		@SuppressWarnings("unchecked")
		final ModelComponentID<T> that = (ModelComponentID<T>) other;
		if (getModelID() == null)
		{
			if (that.getModelID() != null)
				return false;
		} else
			if (getModelID() != this && !getModelID().equals(that.getModelID()))
			return false;
		if (getParentID() == null)
		{
			if (that.getParentID() != null)
				return false;
		} else if (getParentID() != this
				&& !getParentID().equals(that.getParentID()))
			return false;

		return super.equals(other);
	}

	@Override
	public int compareTo(final AbstractIdentifier<T> other)
	{
		// FIXME apply some common strategy via Visitor design pattern

		if (getModelID() != this && getModelID() != null)
		{
			final int modelIDCompare = getModelID()
					.compareTo(((ModelComponentID<?>) other).getModelID());
			if (modelIDCompare != 0)
				return modelIDCompare;
		}

		if (getValue() == null)
			throw new NullPointerException(
					getClass() + ": Can't compare with this null value");

		if (other.getValue() == null)
			throw new NullPointerException(
					getClass() + ": Can't compare with other null value");

		return getValue().compareTo(other.getValue());
	}

}
