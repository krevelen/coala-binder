/* $Id: 4230e66f10a519cf8f246e639d2ff072c214a715 $
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.agent.AgentStatusObserver;
import io.coala.agent.AgentStatusUpdate;
import io.coala.bind.Binder;
import io.coala.bind.BinderFactory;
import io.coala.capability.admin.CreatingCapability;
import io.coala.capability.interact.ReceivingCapability;
import io.coala.capability.replicate.ReplicationConfig;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.message.Message;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import rx.functions.Action1;

/**
 * {@link ConwayTest}
 * 
 * @version $Id: 4230e66f10a519cf8f246e639d2ff072c214a715 $
 * @author Rick van Krevelen
 */
public class ConwayTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( ConwayTest.class );

	/** */
	private static final String CONFIG_FILE = "conway.properties";

	// @Test
	public void testBasicMethods() throws Exception
	{
		final Binder binder = BinderFactory.Builder.fromFile( CONFIG_FILE )
				.withProperty( ReplicationConfig.class,
						ReplicationConfig.MODEL_NAME_KEY,
						"testModel" + System.currentTimeMillis() )
				.build().create( "methodTester" );

		final SimTime.Factory timer = binder.inject( SimTime.Factory.class );
		final SimTime t1 = timer.create( 1.5, TimeUnit.TICKS );
		final SimTime t2 = timer.create( 1.6, TimeUnit.TICKS );

		final CellID cellID1 = new CellID( binder.getID(), 1, 1 );
		final CellState state1 = new CellState( t1, cellID1, LifeStatus.ALIVE );
		LOG.trace( "Created: " + state1 );

		final CellState state2 = new CellState( t2, cellID1, LifeStatus.ALIVE );
		LOG.trace( "Created: " + state2 );

		final CellID cellID2 = new CellID( binder.getID(), 1, 2 );
		LOG.trace( "Booted agent with id: " + cellID1 );

		final CellState state3a = new CellState( t2, cellID2, LifeStatus.DEAD );
		final CellState state3b = new CellState( t2, cellID2, LifeStatus.DEAD );
		LOG.trace( "Created: " + state3a );

		assertNotEquals(
				"Hash codes should not match for " + state1 + " and " + state2,
				state1.hashCode(), state2.hashCode() );
		assertNotEquals(
				"Hash codes should not match for " + state2 + " and " + state3a,
				state2.hashCode(), state3a.hashCode() );
		assertNotEquals(
				"Hash codes should not match for " + state1 + " and " + state3a,
				state1.hashCode(), state3a.hashCode() );

		assertTrue( "Should be smaller", state1.compareTo( state2 ) < 0 );
		assertTrue( "Should be smaller", state2.compareTo( state3a ) < 0 );
		assertTrue( "Should be smaller", state1.compareTo( state3a ) < 0 );

		assertEquals( "Should be equal", state3a, state3b );

		// final CellStateTransition tran = new CellStateTransition(state1,
		// state2);
		LOG.trace( "Created: " + state2 );
	}

	@Test
	public void testCellWorld() throws Exception
	{
		final Binder binder = BinderFactory.Builder.fromFile( CONFIG_FILE )
				.withProperty( ReplicationConfig.class,
						ReplicationConfig.MODEL_NAME_KEY, "torus1" )
				.build().create( "booter" );

		final CellID source = new CellID( binder.getID(), 1, 2 );
		final String sourceSer = JsonUtil.toTree( source ).toString();
		final CellID sourceDeser = JsonUtil.valueOf( sourceSer, CellID.class );
		assertEquals( "De/serialization failed for " + CellID.class, source,
				sourceDeser );
		// LOG.trace("De/serialization worked for " + source);

		final CellState state = new CellState( binder
				.inject( SimTime.Factory.class ).create( 1, TimeUnit.TICKS ),
				source, LifeStatus.ALIVE );
		final String stateSer = JsonUtil.toTree( state ).toString();
		final CellState stateDeser = JsonUtil.valueOf( stateSer,
				CellState.class );
		assertEquals( "De/serialization failed for " + CellState.class, state,
				stateDeser );
		// LOG.trace("De/serialization worked for " + stateDeser);

		final List<List<CellID>> cellStates = CellWorld.Util
				.createLatticeLayout( binder );
		final int total = cellStates.size() * cellStates.get( 0 ).size();
		LOG.trace( "Initial states: " + JsonUtil
				.toJSON( CellWorld.Util.importInitialValues( binder ) ) );

		final CountDownLatch initializedLatch = new CountDownLatch( total );
		final CountDownLatch completedLatch = new CountDownLatch( total );
		final Set<CellID> initialized = new HashSet<CellID>();
		final Set<CellID> completed = new HashSet<CellID>();

		/** global sniffing */
		binder.inject( ReceivingCapability.class ).getIncoming()
				.subscribe( new Action1<Message<?>>()
				{
					@Override
					public void call( final Message<?> msg )
					{
						// System.err.println("Received child message: " + msg);
					}
				} );

		for( List<CellID> row : cellStates )
			for( CellID cellID : row )
			{
				final CellID myCellID = cellID;
				binder.inject( CreatingCapability.class )
						.createAgent( myCellID, BasicCell.class )
						.subscribe( new AgentStatusObserver()
						{
							@Override
							public void onNext( final AgentStatusUpdate update )
							{
								if( update.getStatus().isInitializedStatus() )
								{
									if( initialized.add(
											(CellID) update.getAgentID() ) )
										initializedLatch.countDown();
									else
										LOG.warn( "already initialized "
												+ update.getAgentID() );
								} else if( update.getStatus()
										.isCompleteStatus() )
								{
									if( completed.add(
											(CellID) update.getAgentID() ) )
										completedLatch.countDown();
									else
										LOG.warn( "already completed "
												+ update.getAgentID() );
								} else if( update.getStatus().isFailedStatus() )
									fail( "Cell failed: "
											+ update.getAgentID() );
							}

							@Override
							public void onCompleted()
							{
								LOG.trace( "Cell finished: " + myCellID );
							}

							@Override
							public void onError( final Throwable e )
							{
								LOG.trace( "Cell failed: " + myCellID, e );
							}
						} );
			}
		initializedLatch.await( 2, java.util.concurrent.TimeUnit.SECONDS );
		assertEquals( "Cells should have initialized in 2sec", 0,
				initializedLatch.getCount() );
		LOG.trace( "All cells initialized: " + initialized );

		completedLatch.await( 10, java.util.concurrent.TimeUnit.SECONDS );
		assertEquals( "Cells should have completed in 10sec", 0,
				completedLatch.getCount() );
		LOG.trace( "All cells completed: " + completed );
	}
}
