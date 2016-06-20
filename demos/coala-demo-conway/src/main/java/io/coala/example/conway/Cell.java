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

import io.coala.agent.Agent;
import io.coala.time.SimTime;
import io.coala.time.Timed;

/**
 * {@link Cell} simply exchanges with the cell's environment its own state
 * updates for those from its neighboring Cells
 * 
 * @version $Id$
 */
public interface Cell extends Agent, Timed<SimTime>
{

	/**
	 * @param time the {@link SimTime} to return the state of
	 * @return the {@link Cell}'s state at specified {@link SimTime}
	 */
	// CellState getStateAt(SimTime time);

	/**
	 * @return {@link Observable} stream of my {@link CellState} transitions
	 */
	// Observable<CellState> myStates();

	// CellWorld getWorld();

	/**
	 * for explicit initialization override
	 * 
	 * @param initialState
	 */
	// void initialize(CellState initialState);
}
