/* $Id: ef2f5743ead6eb81e64b6a19925cad9b9418bb1d $
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
package io.coala.dsol.util;

import java.rmi.RemoteException;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import io.coala.log.LogUtil;
import io.coala.time.ClockID;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import nl.tudelft.simulation.dsol.experiment.TimeUnitInterface;
import nl.tudelft.simulation.dsol.experiment.Treatment;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.event.EventProducer;
import nl.tudelft.simulation.jstats.streams.StreamInterface;

/**
 * {@link AbstractDsolModelComponent}
 * 
 * @param <S> the type of {@link SimulatorInterface}
 * @param <M> the type of {@link DsolModel}
 * @version $Id: ef2f5743ead6eb81e64b6a19925cad9b9418bb1d $
 * @author Rick van Krevelen
 */
@Deprecated
public class AbstractDsolModelComponent<S extends SimulatorInterface, M extends DsolModel<S, M>>
	extends EventProducer implements DsolModelComponent<S, M>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( AbstractDsolModelComponent.class );

	/** */
	private final M model;

	/** */
	private final String name;

	/**
	 * @param model
	 * @param name
	 */
	public AbstractDsolModelComponent( final M model, final String name )
	{
		this.model = model;
		this.name = name;
	}

	/** @see DsolModelComponent#getName() */
	@Override
	public String getName()
	{
		return this.name;
	}

	/** @see DsolModelComponent#getModel() */
	@Override
	public M getModel()
	{
		return this.model;
	}

	/** @see DsolModelComponent#getTreatment() */
	@Override
	public Treatment getTreatment()
	{
		try
		{
			return getModel().getSimulator().getReplication().getTreatment();
		} catch( final RemoteException e )
		{
			LOG.fatal( "Problem reaching DSOL replication", e );
			throw new NullPointerException( "Replication unreachable" );
		}
	}

	/** @see DsolModelComponent#getRNG() */
	@Override
	public StreamInterface getRNG()
	{
		return getRNG( RNG_ID );
	}

	/** @see DsolModelComponent#getRNG(String) */
	@Override
	public StreamInterface getRNG( final String name )
	{
		try
		{
			return getModel().getSimulator().getReplication().getStream( name );
		} catch( RemoteException e )
		{
			LOG.fatal( "Problem reaching DSOL replication", e );
			throw new NullPointerException( "RNG stream unreachable" );
		}
	}

	/** @see DsolModelComponent#getSimulator() */
	@Override
	public S getSimulator()
	{
		return (S) getModel().getSimulator();
	}

	/** @see DsolModelComponent#getSimulatorName() */
	@Override
	public ClockID getSimulatorName()
	{
		return getModel().getSimulatorName();
	}

	/** @see DsolModelComponent#simTime() */
	@Override
	public double simTime()
	{
		try
		{
			return getModel().getSimulator().getSimulatorTime();
		} catch( final RemoteException e )
		{
			LOG.fatal( "Problem reaching DSOL replication RMI context", e );
			throw new NullPointerException( "RMI context unreachable" );
		}
	}

	/** @see DsolModelComponent#simTime(TimeUnit) */
	@Override
	public double simTime( final TimeUnit timeUnit )
	{
		return simTime( DsolUtil.toTimeUnit( timeUnit ) );
	}

	/** @see DsolModelComponent#simTime(TimeUnitInterface) */
	@Override
	public double simTime( final TimeUnitInterface timeUnit )
	{
		return DsolUtil
				.toTimeUnit( timeUnit, simTime(), getTreatment().getTimeUnit() )
				.doubleValue();
	}

	/** @see DsolModelComponent#simTime(DateTime) */
	@Override
	public double simTime( final DateTime time )
	{
		return DsolUtil.simTime( time, getTreatment() );
	}

	/** @see DsolModelComponent#simTime(Duration) */
	@Override
	public double simTime( final Duration duration )
	{
		return DsolUtil.simTime( duration, getTreatment() );
	}

	/** @see DsolModelComponent#getSimTime() */
	@Override
	public SimTime getSimTime()
	{
		return DsolUtil.getSimTime( getSimulatorName(), getSimulator() );
	}

	/** @see DsolModelComponent#getSimTime(double) */
	@Override
	public SimTime getSimTime( final double simTime )
	{
		return DsolUtil.getSimTime( getSimulatorName(), getSimulator(),
				simTime );
	}

	/** @see DsolModelComponent#getSimTime(DateTime) */
	@Override
	public SimTime getSimTime( final DateTime time )
	{
		return DsolUtil.getSimTime( getSimulatorName(), getSimulator(), time );
	}

	/** @see DsolModelComponent#getDateTime() */
	@Override
	public DateTime getDateTime()
	{
		return DsolUtil.getDateTime( getSimulator() );
	}

	private Interval interval = null;

	/** @see DsolModelComponent#getInterval() */
	@Override
	public Interval getInterval()
	{
		if( this.interval == null )
			this.interval = DsolUtil.getRunInterval( getTreatment() );
		return this.interval;
	}

}
