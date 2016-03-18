/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-examples/src/main/java/io/coala/example/conway/LifeState.java $
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

import java.util.Map;

/**
 * {@link LifeStatus} tells whether a {@link Cell} is (or transitions to being)
 * alive or dead
 * 
 * @date $Date: 2014-06-03 13:55:16 +0200 (Tue, 03 Jun 2014) $
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public enum LifeStatus
{
	/** the {@link Cell} is alive */
	ALIVE,

	/** the {@link Cell} is dead */
	DEAD,

	;

	public static void blockUntilTotalStatesReached(
			final Map<LifeStatus, Integer> states, final int target)
					throws InterruptedException
	{
		int total = 0;
		while (total != target)
			synchronized (states)
			{
				states.wait();
				total = states.get(LifeStatus.ALIVE)
						+ states.get(LifeStatus.DEAD);
			}
	}

	/**
	 * @param states counts per the neighboring {@link LifeStatus}
	 * @return the next state as prescribed by Conway's rules given specified
	 *         neighbor states
	 */
	public LifeStatus getTransition(final Map<LifeStatus, Integer> states)
	{
		final int aliveCount, deadCount, total;
		synchronized (states)
		{
			aliveCount = states.containsKey(ALIVE) ? states.get(ALIVE) : 0;
			deadCount = states.containsKey(DEAD) ? states.get(DEAD) : 0;
		}
		total = aliveCount + deadCount;

		// sanity check
		if (total != 8)
			throw new IllegalStateException(String.format(
					"Incorrect neighbor count: %d (<>8) with "
							+ "%d living and %d dead",
					total, aliveCount, deadCount));

		if (this == ALIVE && (aliveCount == 2 || aliveCount == 3))
			return ALIVE;

		if (this == DEAD && aliveCount == 3)
			return ALIVE;

		return DEAD;
	}
}