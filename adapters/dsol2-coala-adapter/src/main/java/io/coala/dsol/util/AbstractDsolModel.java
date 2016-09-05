/* $Id: 1b993e308bdfba74cc238c6c2cd42e332197c917 $
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

import io.coala.log.LogUtil;
import io.coala.model.ModelID;
import io.coala.time.ClockID;
import nl.tudelft.simulation.dsol.ModelInterface;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEvent;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.event.Event;
import nl.tudelft.simulation.event.EventType;

/**
 * {@link AbstractDsolModel}
 * 
 * @param <S>
 * @param <THIS>
 * @version $Id: 1b993e308bdfba74cc238c6c2cd42e332197c917 $
 * @author Rick van Krevelen
 */
@Deprecated
public abstract class AbstractDsolModel<S extends SimulatorInterface, THIS extends AbstractDsolModel<S, THIS>>
	extends AbstractDsolModelComponent<S, THIS> implements DsolModel<S, THIS>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( AbstractDsolModel.class );

	/** */
	public static final EventType MODEL_INITIALIZED = new EventType(
			"Model initialized" );

	/** */
	private final ClockID sourceID;

	/** */
	private S simulator = null;

	/** */
	public AbstractDsolModel( final String sourceName )
	{
		// use simple class name as model's name
		super( null, sourceName );
		this.sourceID = new ClockID( new ModelID( sourceName ), sourceName );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public THIS getModel()
	{
		return (THIS) this;
	}

	/** @see ModelInterface#getSimulator() */
	@Override
	public S getSimulator()
	{
		return this.simulator;
	}

	/** @see ModelInterface#getSimulatorName() */
	@Override
	public ClockID getSimulatorName()
	{
		return this.sourceID;
	}

	/** @see ModelInterface#constructModel(SimulatorInterface) */
	@SuppressWarnings( "unchecked" )
	@Override
	public void constructModel( final SimulatorInterface simulator )
		throws SimRuntimeException, RemoteException
	{
		this.simulator = (S) simulator;

		if( simulator instanceof DEVSSimulatorInterface )
			((DEVSSimulatorInterface) simulator)
					.scheduleEvent( new SimEvent( simTime(), this, this,
							SET_THREAD_NAME_METHOD_ID, NO_ARGS ) );

		LOG.trace( "Constructing model..." );
		try
		{
			onInitialize();

			fireEvent( new Event( MODEL_INITIALIZED, this, null ) );

			LOG.trace( "Model constructed!" );
		} catch( final Throwable t )
		{
			LOG.error( "Problem constructing model", t );
		}
	}

	/** */
	protected static final String SET_THREAD_NAME_METHOD_ID = "setThreadName";

	/** */
	protected void setThreadName()
	{
		Thread.currentThread().setName( getSimulatorName().toString() );
	}

}
