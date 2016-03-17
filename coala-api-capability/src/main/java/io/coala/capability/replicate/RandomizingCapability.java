/* $Id: c5ec36ea22e53e30059884411628fe29276d8784 $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/service/randomizer/RandomizerService.java $
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
package io.coala.capability.replicate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;
import io.coala.config.CoalaProperty;
import io.coala.log.InjectLogger;
import io.coala.random.RandomNumberStream;

/**
 * {@link RandomizingCapability} provides random number generators or streams
 * 
 * @date $Date: 2014-06-03 14:26:09 +0200 (Tue, 03 Jun 2014) $
 * @version $Revision: 296 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <K> the type of key for persisted values
 * @param <V> the type of value to persist
 * @param <THIS> the (sub)type of {@link RandomizingCapability} to build
 */
public interface RandomizingCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 * 
	 * @version $Revision: 296 $
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 */
	interface Factory extends CapabilityFactory<RandomizingCapability>
	{
		// empty
	}

	/** identifier for the simulation model's main {@link RandomNumberStream} */
	RandomNumberStream.ID MAIN_RNG_ID = new RandomNumberStream.ID( "MAIN_RNG" );

	/**
	 * @return the main {@link RandomNumberStream}
	 * @see {@link #MAIN_RNG_ID}
	 */
	RandomNumberStream getRNG();

	/**
	 * @param rngID the {@link RandomNumberStreamID} of the returnable RNG
	 * @return the main {@link RandomNumberStream}
	 */
	RandomNumberStream getRNG( RandomNumberStream.ID rngID );

	/**
	 * {@link Math3RandomizingCapability} implements
	 * {@link RandomizingCapability} using APache's commons-math3 toolkit
	 * 
	 * @version $Id: 07298364b836980a619bb4ac47f836e573dd3796 $
	 * @author Rick van Krevelen
	 */
	public class Simple extends BasicCapability implements RandomizingCapability
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
		protected Simple( final Binder binder )
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

		private RandomNumberStream
			newRNG( final RandomNumberStream.ID streamID )
		{
			// add owner ID hash code for reproducible seeding variance across
			// owner agents
			return getBinder().inject( RandomNumberStream.Factory.class )
					.create( streamID,
							CoalaProperty.randomSeed.value().getLong()
									+ getID().getOwnerID().hashCode() );
		}

	}
}
