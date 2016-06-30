/* $Id: 4d29784a912cbf920c236fa1b26c4b868b271ff1 $
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
package io.coala.eve3;

import org.apache.logging.log4j.Logger;

import io.coala.agent.AgentID;
import io.coala.agent.BasicAgent;
import io.coala.bind.Binder;
import io.coala.capability.admin.DestroyingCapability;
import io.coala.capability.interact.ReceivingCapability;
import io.coala.log.InjectLogger;
import io.coala.message.AbstractMessage;
import io.coala.message.MessageID;
import io.coala.time.NanoInstant;
import rx.Observer;

/**
 * {@link CleanupTestAgent}
 * 
 * @version $Id: 4d29784a912cbf920c236fa1b26c4b868b271ff1 $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
@Deprecated
public class CleanupTestAgent extends BasicAgent
{

	public static class Harakiri extends AbstractMessage<MessageID<?, ?>>
	{
		/** */
		private static long count = 0L;

		public Harakiri( final AgentID ownerID )
		{
			super( MessageID.of( ownerID.getModelID(), count++,
					NanoInstant.ZERO ), ownerID, ownerID, ownerID );
		}
	}

	@InjectLogger
	private Logger LOG;

	/**
	 * {@link CleanupTestAgent} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	public CleanupTestAgent( )
	{
		getBinder().inject( ReceivingCapability.class ).getIncoming()
				.ofType( Harakiri.class )// .observeOn(Schedulers.trampoline())
				.subscribe( new Observer<Harakiri>()
				{
					@Override
					public void onCompleted()
					{
						LOG.trace(
								"Completed receiving messages, agent destroyed?" );
					}

					@Override
					public void onError( final Throwable e )
					{
						LOG.error( "Error", e );
						e.printStackTrace();
					}

					@Override
					public void onNext( final Harakiri kill )
					{
						System.err.println(
								"Received message for Harakiri, destroying now..." );

						try
						{
							getBinder().inject( DestroyingCapability.class )
									.destroy();
						} catch( final Exception e )
						{
							LOG.error( "Problem committing Hara Kiri", e );
							e.printStackTrace();
						}
					}
				} );
	}

	@Override
	public void initialize()
	{
		LOG.trace( getID() + " is initialized" );
	}

	@Override
	public void finish()
	{
		LOG.trace( getID() + " is done" );
	}

}
