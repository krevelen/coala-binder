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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.coala.bind.Binder;
import io.coala.capability.CapabilityFactory;
import io.coala.capability.configure.ConfiguringCapability;
import io.coala.capability.embody.GroundingCapability;
import io.coala.model.ModelID;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import io.coala.time.Proactive;
import rx.Observable;

/**
 * {@link CellWorld} observes percepts from, and performs actions in, an
 * individual {@link Cell}'s environment
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Deprecated
public interface CellWorld extends GroundingCapability//, Timed<SimTime>
{

	interface Factory extends CapabilityFactory<CellWorld>
	{
		// empty
	}

	/** TODO use Config */
	String INITIAL_STATES_CONFIG_KEY = "initialStates";

	/** TODO use Config */
	String CYCLE_DURATION_CONFIG_KEY = "cycleDuration";

	/** TODO use Config */
	Number CYCLE_DURATION_DEFAULT = 1;

	/** TODO use Config */
	String CYCLE_TOTAL_CONFIG_KEY = "cycleTotal";

	/** TODO use Config */
	Number CYCLE_TOTAL_DEFAULT = 100;

	/**
	 * @param neighborStates the source stream of incoming neighbor states
	 * @return a stream of outgoing local states
	 */
	Observable<CellState> myStates( Observable<CellState> neighborStates );

	/**
	 * @return my {@link Proactive} {@link CellState}, or {@code null} if not
	 *         initialized
	 */
	CellState myState();

	/**
	 * @return my {@link CellLink}s with neighbors
	 */
	Collection<CellID> myNeighbors();

	class Util
	{

		/** caches {@link CellID} layout per lattice's {@link ModelID} */
		private static final Map<ModelID, List<List<CellID>>> CELL_ID_CACHE = new HashMap<>();

		/** caches initial {@link LifeStatus}es per lattice's {@link ModelID} */
		private static final Map<ModelID, int[][]> INITIAL_STATE_CACHE = new HashMap<>();

		/**
		 * @param binder
		 * @param modelID
		 * @return
		 * @throws Exception
		 */
		public static int[][] importInitialValues( final Binder binder )
			throws Exception
		{
			final int[][] result, tempStates;
			final ModelID modelID = binder.getID().getModelID();
			synchronized( INITIAL_STATE_CACHE )
			{
				tempStates = INITIAL_STATE_CACHE.get( modelID );
				if( tempStates == null )
				{
					result = binder.inject( ConfiguringCapability.class )
							.getProperty( INITIAL_STATES_CONFIG_KEY )
							.getJSON( int[][].class );
					INITIAL_STATE_CACHE.put( modelID, result );
				} else
					result = tempStates;
			}
			return result;
		}

		/**
		 * @param binder the local {@link Binder}
		 * @return the initial {@link CellState}
		 * @throws Exception if parsing failed
		 */
		public static CellState parseInitialState( final Binder binder )
			throws Exception
		{
			final int[][] values = importInitialValues( binder );
			final CellID myID = (CellID) binder.getID();
			final LifeStatus startState = values[myID.getRow()][myID
					.getCol()] == 0 ? LifeStatus.DEAD : LifeStatus.ALIVE;
			final SimTime startCycle = binder.inject( SimTime.Factory.class )
					.create( 0, TimeUnit.TICKS );
			return new CellState( startCycle, myID, startState );
		}

		/**
		 * @param modelID the {@link ModelID} for new cells
		 * @param rows lattice height
		 * @param cols lattice width
		 * @return a matrix (row-{@link List} containing column-{@link List}s)
		 *         of respective {@link CellID}s
		 * @throws Exception
		 */
		public static List<List<CellID>>
			createLatticeLayout( final Binder binder ) throws Exception
		{
			synchronized( CELL_ID_CACHE )
			{
				List<List<CellID>> result = CELL_ID_CACHE
						.get( binder.getID().getModelID() );
				if( result != null ) return result;

				final int[][] values = importInitialValues( binder );
				final int rows = values.length;
				final int cols = values[0].length;
				final List<List<CellID>> tempLayout = new ArrayList<>( rows );
				result = Collections.unmodifiableList( tempLayout );
				CELL_ID_CACHE.put( binder.getID().getModelID(), result );

				for( int row = 0; row < rows; row++ )
				{
					final List<CellID> rowMap = new ArrayList<>( cols );
					tempLayout.add( Collections.unmodifiableList( rowMap ) );

					for( int col = 0; col < cols; col++ )
						rowMap.add( new CellID( binder.getID(), row, col ) );
				}
				return result;
			}
		}

		/**
		 * @param binder the local {@link Binder}
		 * @return a {@link Collection} of neighboring {@link CellID}s
		 * @throws Exception
		 */
		public static Collection<CellID>
			determineTorusNeighbors( final Binder binder ) throws Exception
		{

			final CellID cellID = (CellID) binder.getID();
			final List<List<CellID>> cellIDs = createLatticeLayout( binder );

			final int rows = cellIDs.size();
			final int cols = cellIDs.get( 0 ).size();
			final int row = cellID.getRow();
			final int col = cellID.getCol();
			final int north = (rows + row - 1) % rows;
			final int south = (rows + row + 1) % rows;
			final int west = (cols + col - 1) % cols;
			final int east = (cols + col + 1) % cols;
			return Arrays.asList( //
					cellIDs.get( north ).get( west ), // northwest
					cellIDs.get( row ).get( west ), // west
					cellIDs.get( south ).get( west ), // south-west
					cellIDs.get( north ).get( col ), // north
					cellIDs.get( south ).get( col ), // south
					cellIDs.get( north ).get( east ), // northeast
					cellIDs.get( row ).get( east ), // east
					cellIDs.get( south ).get( east ) ); // south-east
		}
	}

}
