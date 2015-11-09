/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-examples/src/main/java/io/coala/example/conway/CellWorld.java $
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
package io.coala.example.conway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.coala.capability.CapabilityFactory;
import io.coala.capability.configure.ConfiguringCapability;
import io.coala.capability.embody.GroundingCapability;
import io.coala.exception.CoalaException;
import io.coala.model.ModelID;
import io.coala.time.Timed;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link CellWorld} observes percepts from, and performs actions in, an
 * individual {@link Cell}'s environment
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public interface CellWorld extends GroundingCapability
{

	/**
	 * @version $Id$
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	interface Factory extends CapabilityFactory<CellWorld>
	{
		// empty
	}

	/**
	 * 
	 */
	void proceed() throws Exception;

	/**
	 * @return my {@link Timed} {@link CellState}, or {@code null} if not
	 *         initialized
	 */
	CellState myState();

	/**
	 * @return my {@link CellLink}s with neighbors
	 */
	Collection<CellID> myNeighbors();

	class Util
	{

		/** global cache */
		private static final Map<ModelID, List<List<CellID>>> CELL_IDS = Collections
				.synchronizedMap(new HashMap<ModelID, List<List<CellID>>>());

		/** global sniffing */
		public static final Subject<CellState, CellState> GLOBAL_TRANSITIONS = PublishSubject
				.create();

		/** global cache */
		private static final Map<ModelID, List<Map<CellID, LifeStatus>>> INITIAL_STATE_CACHE = Collections
				.synchronizedMap(
						new HashMap<ModelID, List<Map<CellID, LifeStatus>>>());

		public static synchronized List<Map<CellID, LifeStatus>> importLattice(
				final ModelID modelID, final ConfiguringCapability config)
						throws CoalaException
		{
			List<Map<CellID, LifeStatus>> result = INITIAL_STATE_CACHE
					.get(modelID);
			if (result != null)
				return result;

			result = new ArrayList<Map<CellID, LifeStatus>>();
			INITIAL_STATE_CACHE.put(modelID,
					Collections.unmodifiableList(result));

			final int[][] initialStates = config.getProperty("initialStates")
					.getJSON(int[][].class);

			for (List<CellID> row : getLatticeCellIDs(modelID,
					initialStates.length, initialStates[0].length))
			{
				final Map<CellID, LifeStatus> rowStates = new HashMap<CellID, LifeStatus>();
				result.add(rowStates);

				for (CellID cellID : row)
					rowStates
							.put(cellID,
									initialStates[cellID.getRow()][cellID
											.getCol()] == 0 ? LifeStatus.DEAD
													: LifeStatus.ALIVE);
			}
			return result;
		}

		/**
		 * @param agentIDFactory
		 * @param rows
		 * @param cols
		 * @return
		 */
		public static List<List<CellID>> getLatticeCellIDs(final ModelID modelID,
				final int rows, final int cols)
		{
			List<List<CellID>> result = CELL_IDS.get(modelID);
			if (result != null)
				return result;

			result = new ArrayList<List<CellID>>(rows);
			for (int row = 0; row < rows; row++)
			{
				final List<CellID> rowMap = new ArrayList<CellID>(cols);
				result.add(Collections.unmodifiableList(rowMap));

				for (int col = 0; col < cols; col++)
					rowMap.add(new CellID(modelID, row, col));
			}
			result = Collections.unmodifiableList(result);
			CELL_IDS.put(modelID, result);
			return result;
		}

		/**
		 * @param cellID the local {@link CellID}
		 * @param config the local {@link ConfiguringCapability}
		 * @return
		 * @throws CoalaException
		 */
		public static Collection<CellID> getTorusNeighborIDs(
				final CellID cellID, final ConfiguringCapability config)
						throws CoalaException
		{
			final ModelID modelID = cellID.getModelID();
			final List<Map<CellID, LifeStatus>> lattice = importLattice(modelID,
					config);
			final int rows = lattice.size();
			final int cols = lattice.get(0).size();
			List<List<CellID>> cellIDs = getLatticeCellIDs(modelID, rows, cols);

			final int row = cellID.getRow();
			final int col = cellID.getCol();
			final int north = (rows + row - 1) % rows;
			final int south = (rows + row + 1) % rows;
			final int west = (cols + col - 1) % cols;
			final int east = (cols + col + 1) % cols;
			final Collection<CellID> result = Arrays.asList(
					cellIDs.get(north).get(west), // northwest
					cellIDs.get(row).get(west), // west
					cellIDs.get(south).get(west), // south-west
					cellIDs.get(north).get(col), // north
					cellIDs.get(south).get(col), // south
					cellIDs.get(north).get(east), // northeast
					cellIDs.get(row).get(east), // east
					cellIDs.get(south).get(east)); // south-east
			return result;
		}
	}

}
