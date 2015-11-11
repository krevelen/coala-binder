/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-examples/src/test/java/io/coala/example/conway/ConwayTest.java $
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
 * Copyright (c) 2010-2014 Almende B.V. 
 */
package io.coala.example.conway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.coala.agent.AgentStatusObserver;
import io.coala.agent.AgentStatusUpdate;
import io.coala.bind.Binder;
import io.coala.bind.BinderFactory;
import io.coala.capability.admin.CreatingCapability;
import io.coala.capability.replicate.ReplicationConfig;
import io.coala.exception.CoalaException;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import rx.functions.Action1;

/**
 * {@link ConwayTest}
 * 
 * @version $Revision: 312 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 */
// @Ignore
public class ConwayTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger(ConwayTest.class);

	/** */
	private static final String CONFIG_FILE = "conway.properties";

	@BeforeClass
	public static void setupBinderFactory() throws CoalaException
	{
		CellWorld.GLOBAL_TRANSITION_SNIFFER.subscribe(new Action1<CellState>()
		{
			@Override
			public void call(final CellState transition)
			{
				System.err.println("Observed transition: " + transition);
			}
		});
	}

	// @Test
	public void testBasicMethods() throws Exception
	{
		final Binder binder = BinderFactory.Builder.fromFile(CONFIG_FILE)
				.withProperty(ReplicationConfig.class,
						ReplicationConfig.MODEL_NAME_KEY,
						"testModel" + System.currentTimeMillis())
				.build().create("methodTester");

		final SimTime.Factory timer = binder.inject(SimTime.Factory.class);
		final SimTime t1 = timer.create(1.5, TimeUnit.TICKS);
		final SimTime t2 = timer.create(1.6, TimeUnit.TICKS);

		final CellID cellID1 = new CellID(binder.getID().getModelID(), 1, 1);
		final CellState state1 = new CellState(t1, cellID1, LifeStatus.ALIVE);
		LOG.trace("Created: " + state1);

		final CellState state2 = new CellState(t2, cellID1, LifeStatus.ALIVE);
		LOG.trace("Created: " + state2);

		final CellID cellID2 = new CellID(binder.getID().getModelID(), 1, 2);
		LOG.trace("Booted agent with id: " + cellID1);

		final CellState state3a = new CellState(t2, cellID2, LifeStatus.DEAD);
		final CellState state3b = new CellState(t2, cellID2, LifeStatus.DEAD);
		LOG.trace("Created: " + state3a);

		Assert.assertNotEquals(
				"Hash codes should not match for " + state1 + " and " + state2,
				state1.hashCode(), state2.hashCode());
		Assert.assertNotEquals(
				"Hash codes should not match for " + state2 + " and " + state3a,
				state2.hashCode(), state3a.hashCode());
		Assert.assertNotEquals(
				"Hash codes should not match for " + state1 + " and " + state3a,
				state1.hashCode(), state3a.hashCode());

		Assert.assertTrue("Should be smaller", state1.compareTo(state2) < 0);
		Assert.assertTrue("Should be smaller", state2.compareTo(state3a) < 0);
		Assert.assertTrue("Should be smaller", state1.compareTo(state3a) < 0);

		Assert.assertEquals("Should be equal", state3a, state3b);

		// final CellStateTransition tran = new CellStateTransition(state1,
		// state2);
		LOG.trace("Created: " + state2);
	}

	@Test
	public void testCellWorld() throws Exception
	{
		final Binder binder = BinderFactory.Builder.fromFile(CONFIG_FILE)
				.withProperty(ReplicationConfig.class,
						ReplicationConfig.MODEL_NAME_KEY, "torus1")
				.build().create("conwayBooter");

		final List<List<CellID>> cellStates = CellWorld.Util
				.createLatticeLayout(binder);
		final int total = cellStates.size() * cellStates.get(0).size();
		assertEquals("Should import all initial cell states", 9, total);
		LOG.trace("Initial states: " + JsonUtil
				.toPrettyJSON(CellWorld.Util.importInitialValues(binder)));

		final CountDownLatch initializedLatch = new CountDownLatch(total);
		final CountDownLatch completedLatch = new CountDownLatch(total);
		final Set<CellID> initialized = new HashSet<CellID>();
		final Set<CellID> completed = new HashSet<CellID>();

		for (List<CellID> row : cellStates)
			for (CellID cellID : row)
			{
				final CellID myCellID = cellID;
				binder.inject(CreatingCapability.class)
						.createAgent(cellID, BasicCell.class)
						.subscribe(new AgentStatusObserver()
						{
							@Override
							public void onNext(final AgentStatusUpdate update)
							{
								if (update.getStatus().isInitializedStatus())
								{
									if (initialized
											.add((CellID) update.getAgentID()))
										initializedLatch.countDown();
									else
										LOG.warn("already initialized "
												+ update.getAgentID());
								} else
									if (update.getStatus().isCompleteStatus())
								{
									if (completed
											.add((CellID) update.getAgentID()))
										completedLatch.countDown();
									else
										LOG.warn("already completed "
												+ update.getAgentID());
								} else if (update.getStatus().isFailedStatus())
									fail("Cell failed: " + update.getAgentID());
							}

							@Override
							public void onCompleted()
							{
								LOG.trace("Cell finished: " + myCellID);
							}

							@Override
							public void onError(final Throwable e)
							{
								e.printStackTrace();
							}
						});
			}
		initializedLatch.await(2, java.util.concurrent.TimeUnit.SECONDS);
		assertEquals("Cells should have initialized in <2s", 0,
				initializedLatch.getCount());
		LOG.trace("All cells initialized: " + initialized);

		completedLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
		assertEquals("Cells should have completed in <10s", 0,
				completedLatch.getCount());
		LOG.trace("All cells completed: " + completed);
	}
}
