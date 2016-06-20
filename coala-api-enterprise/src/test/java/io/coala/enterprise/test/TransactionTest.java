/* $Id: 6e0b7fb8270250df9be3773d972d23338a303397 $
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
package io.coala.enterprise.test;

import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.Ignore;

import io.coala.agent.AgentStatusObserver;
import io.coala.agent.AgentStatusUpdate;
import io.coala.bind.Binder;
import io.coala.bind.BinderFactory;
import io.coala.capability.admin.CreatingCapability;
import io.coala.capability.plan.ClockStatusUpdate;
import io.coala.capability.plan.SchedulingCapability;
import io.coala.capability.replicate.ReplicationConfig;
import io.coala.dsol.DsolSimulatorStatus;
import io.coala.enterprise.test.impl.TestExecutorOrganizationImpl;
import io.coala.enterprise.test.impl.TestInitiatorOrganizationImpl;
import io.coala.log.LogUtil;
import io.coala.time.SimTime;
import rx.Observer;

/**
 * {@link TransactionTest}
 */
// @Ignore
public class TransactionTest
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( TransactionTest.class );

	@Ignore // FIXME !
	@Test
	public void testTransaction() throws Exception
	{
		LOG.trace( "Transaction test started!" );

		final Binder binder = BinderFactory.Builder.fromFile()
				.withProperty( ReplicationConfig.class,
						ReplicationConfig.MODEL_NAME_KEY,
						"testModel" + System.currentTimeMillis() )
				.build().create( "_unittestboot_" );

		final CountDownLatch initLatch = new CountDownLatch( 2 );

		final CountDownLatch doneLatch = new CountDownLatch( 1 );

		final AgentStatusObserver obs = new AgentStatusObserver()
		{

			@Override
			public void onNext( final AgentStatusUpdate update )
			{
				LOG.trace( "Observed: " + update );
				// System.err.println("Observed: " + update);

				if( update.getStatus().isInitializedStatus() )
				{
					initLatch.countDown();
					LOG.trace( "Agent initialized, remaining: "
							+ initLatch.getCount() );
				}
			}

			@Override
			public void onError( final Throwable e )
			{
				LOG.error( "Problem while observing agent status", e );
			}

			@Override
			public void onCompleted()
			{
				LOG.trace( "Agent status updates COMPLETED" );
			}
		};

		final CreatingCapability booterSvc = binder
				.inject( CreatingCapability.class );

		booterSvc.createAgent( "executorOrg",
				TestExecutorOrganizationImpl.class ).subscribe( obs );

		// FIXME boot initiator after executor initialized
		booterSvc.createAgent( "initiatorOrg",
				TestInitiatorOrganizationImpl.class ).subscribe( obs );

		LOG.trace( "Waiting for all organizations to initialize..." );
		initLatch.await();

		@SuppressWarnings( "unchecked" )
		final SchedulingCapability<SimTime> sim = binder
				.inject( SchedulingCapability.class );

		sim.getStatusUpdates().subscribe( new Observer<ClockStatusUpdate>()
		{

			@Override
			public void onCompleted()
			{
				System.err.println( "DSOL sim completed!" );
			}

			@Override
			public void onError( final Throwable e )
			{
				e.printStackTrace();
			}

			@Override
			public void onNext( final ClockStatusUpdate update )
			{
				System.err.println(
						"[t=" + sim.getTime() + "] DSOL status: " + update );
				switch( (DsolSimulatorStatus) update.getStatus() )
				{
				case FAILED:
				case FINISHED:
					doneLatch.countDown();
					break;
				default:
				}
			}
		} );

		LOG.trace( "Waiting for simulation to complete (or fail)..." );
		doneLatch.await();

		LOG.trace( "Transaction test complete!" );
	}
}
