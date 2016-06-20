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
package io.coala.example.conway;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import io.coala.agent.AgentID;
import io.coala.name.AbstractIdentifier;

/**
 * {@link CellID}
 */
public class CellID extends AgentID
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final String CELL_ID_FORMAT = "cell_%d_%d";

	/** */
	private int row;

	/** */
	private int col;

	/**
	 * {@link CellID} zero-arg bean constructor
	 */
	protected CellID()
	{
		super();
	}

	/**
	 * {@link CellID} constructor
	 * 
	 * @param modelID
	 * @param row the {@link Cell}'s row in the lattice
	 * @param col the {@link Cell}'s column in the lattice
	 */
	@AssistedInject
	public CellID(@Assisted final AgentID parentID, final int row,
			final int col)
	{
		super(parentID, String.format(CELL_ID_FORMAT, row, col));
		this.row = row;
		this.col = col;
	}

	public int getRow()
	{
		return this.row;
	}

	public int getCol()
	{
		return this.col;
	}

	@Override
	public int compareTo(final AbstractIdentifier<String> other)
	{
		super.hashCode();
		if (other instanceof CellID)
		{
			final CellID that = (CellID) other;
			final int rowCompare = Integer.compare(getRow(), that.getRow());
			if (rowCompare != 0)
				return rowCompare;
			return Integer.compare(getCol(), that.getCol());
		}
		return super.compareTo(other);
	}

	// @Override
	// public String toString()
	// {
	// return String.format("%s[%02d,%02d]", getModelID(), getRow(), getCol());
	// }

}