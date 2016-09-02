/* $Id: 6906eff0d90d13e4805e609859895520fb71412a $
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

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.agent.BasicAgent;
import io.coala.capability.interact.ReceivingCapability;
import rx.Observable;
import rx.Observer;

/**
 * {@link BasicCell}
 * 
 * @version $Id: 6906eff0d90d13e4805e609859895520fb71412a $
 * @author Rick van Krevelen
 */
@Deprecated
public class BasicCell extends BasicAgent implements Cell
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@Inject
	private transient Logger LOG;

	@Override
	public void initialize() throws Exception
	{
		super.initialize();

		// link to inputs / observable percepts
		final Observable<CellState> incoming = getBinder()
				.inject( ReceivingCapability.class ).getIncoming()
				.ofType( CellState.class );
//		incoming.subscribe(new Observer<CellState>()
//		{
//
//			@Override
//			public void onCompleted()
//			{
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void onError(Throwable e)
//			{
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void onNext(CellState t)
//			{
//				// LOG.trace("Received " + t.getClass().getSimpleName() + ": "
//				// + t.getSenderID().getValue() + " > "
//				// + getID().getValue());
//
//			}
//		});

		getBinder().inject( CellWorld.class ).myStates( incoming )
				.subscribe( new Observer<CellState>()
				{
					@Override
					public void onCompleted()
					{
						LOG.trace( "My world has ended, simulation done!" );
//						die();
					}

					@Override
					public void onError( final Throwable e )
					{
						LOG.error( "Problem in my state transition stream", e );
					}

					@Override
					public void onNext( final CellState t )
					{
					}
				} );
	}

//	@Override
//	public SimTime getTime()
//	{
//		return getBinder().inject( CellWorld.class ).getTime();
//	}

}
