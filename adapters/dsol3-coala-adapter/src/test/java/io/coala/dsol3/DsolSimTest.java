/* $Id: db00600b42b2236dcc5f74a2e1c4d3a46c3dbb44 $
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
package io.coala.dsol3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.measure.Measurable;
import javax.measure.quantity.Quantity;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.log.LogUtil;
import nl.tudelft.simulation.dsol.DSOLModel;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.experiment.ReplicationMode;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.Executable;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.event.EventProducer;
import rx.Observer;

/**
 * {@link DsolSimTest}
 * 
 * @version $Id: db00600b42b2236dcc5f74a2e1c4d3a46c3dbb44 $
 * @author Rick van Krevelen
 */
public class DsolSimTest
{

	/** */
	static final Logger LOG = LogUtil.getLogger( DsolSimTest.class );

	/** */
	@SuppressWarnings( { "serial", "rawtypes" } )
	public static class TestModel<Q extends Quantity> extends EventProducer
		implements DSOLModel<Measurable<Q>, BigDecimal, DsolTime<Q>>
	{
		/** the scheduler {@link DEVSSimulator} */
		private DEVSSimulator<Measurable<Q>, BigDecimal, DsolTime<Q>> scheduler;

		private int jobCount = 0;

		@Override
		public final DEVSSimulator<Measurable<Q>, BigDecimal, DsolTime<Q>>
			getSimulator()
		{
			return this.scheduler;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public void constructModel( final SimulatorInterface simulator )
			throws SimRuntimeException, RemoteException
		{
			this.scheduler = (DEVSSimulator<Measurable<Q>, BigDecimal, DsolTime<Q>>) simulator;

			LOG.trace( "Schedulable job count: " + this.jobCount );
			for( int i = 0; i < this.jobCount; i++ )
			{
				final DsolTime<Q> t = (DsolTime<Q>) DsolTime
						.valueOf( 1.0d * i );
				this.scheduler.scheduleEventAbs( t, new Executable()
				{
					@Override
					public void execute()
					{
						fireEvent( DsolEvent
								.valueOf( "SimEvent at t=" + getTime() ) );
					}
				} );
				LOG.trace( "Scheduled execution at t=" + t );
			}
		}

		public DsolTime<Q> getTime()
		{
			return getSimulator().getSimulatorTime();
		}

		public TestModel withJobCount( final int jobCount )
		{
			this.jobCount += jobCount;
			return this;
		}
	}

	@Test
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public void testDEVSSimulator()
		throws SimRuntimeException, RemoteException, NamingException
	{
		// create test condition
		final CountDownLatch latch = new CountDownLatch( 1 );

		LOG.trace( "Create model" );
		final TestModel model = new TestModel().withJobCount( 10 );

		LOG.trace( "Initialize sim" );
		final DEVSSimulator sim = DsolTime
				.createDEVSSimulator( DEVSSimulator.class );
		sim.initialize( DsolTime.createReplication( "rep1",
				DsolTime.valueOf( 0.0 ), BigDecimal.valueOf( 0.0 ),
				BigDecimal.valueOf( 100.0 ), model ),
				ReplicationMode.TERMINATING );

		// register listeners
		new DsolEventObservable().subscribeTo( model, DsolEvent.class ).events()
				.ofType( DsolEvent.class ).subscribe( new Observer<DsolEvent>()
				{
					@Override
					public void onCompleted()
					{
						LOG.trace( "Simulation complete" );
						latch.countDown();
					}

					@Override
					public void onError( final Throwable e )
					{
						LOG.error( "Simulation failed", e );
						fail( e.getMessage() );
					}

					@Override
					public void onNext( final DsolEvent t )
					{
						LOG.trace( "Observed event at t="
								+ model.getSimulator().getSimulatorTime() );
					}
				} );

		LOG.trace( "Starting sim" );
		sim.start();
		try
		{
			latch.await( 10, TimeUnit.SECONDS );
		} catch( final InterruptedException e )
		{
			fail( "Unexpected interrupt" );
		}
		assertEquals( "Simulation hasn't finished yet", 0, latch.getCount() );
	}

}
