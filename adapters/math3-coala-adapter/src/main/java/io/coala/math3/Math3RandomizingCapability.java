/* $Id: 07298364b836980a619bb4ac47f836e573dd3796 $
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
package io.coala.math3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.replicate.RandomizingCapability;
import io.coala.config.CoalaProperty;
import io.coala.log.InjectLogger;
import io.coala.random.RandomNumberStream;

/**
 * {@link Math3RandomizingCapability} implements {@link RandomizingCapability}
 * using APache's commons-math3 toolkit
 * 
 * @version $Id: 07298364b836980a619bb4ac47f836e573dd3796 $
 * @author Rick van Krevelen
 */
public class Math3RandomizingCapability extends BasicCapability
	implements RandomizingCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private final Map<RandomNumberStream.ID, RandomNumberStream> rng = Collections
			.synchronizedMap(
					new HashMap<RandomNumberStream.ID, RandomNumberStream>() );

	@InjectLogger
	private Logger LOG;

	/**
	 * {@link Math3RandomizingCapability} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	protected Math3RandomizingCapability( final Binder binder )
	{
		super( binder );
	}

	@Override
	public RandomNumberStream getRNG()
	{
		return getRNG( MAIN_RNG_ID );
	}

	@Override
	public RandomNumberStream getRNG( RandomNumberStream.ID rngID )
	{
		if( !this.rng.containsKey( rngID ) )
			this.rng.put( rngID, newRNG( rngID ) );
		return this.rng.get( rngID );
	}

	private RandomNumberStream newRNG( final RandomNumberStream.ID streamID )
	{
		// add owner ID hash code for reproducible seeding variance across
		// owner agents
		return getBinder().inject( RandomNumberStream.Factory.class )
				.create( streamID, CoalaProperty.randomSeed.value().getLong()
						+ getID().getOwnerID().hashCode() );
	}

}
