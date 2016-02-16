/* $Id: f48a01d4e370494501065e39d20eb1ffb9f7c070 $
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
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.experiment.Experiment;
import nl.tudelft.simulation.dsol.experiment.Replication;
import nl.tudelft.simulation.dsol.experiment.Treatment;
import nl.tudelft.simulation.jstats.streams.MersenneTwister;
import nl.tudelft.simulation.jstats.streams.StreamInterface;

/**
 * {@link ReplicationBuilder}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class ReplicationBuilder extends Replication
{

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( ReplicationBuilder.class );

	/** */
	private static final String REPLICATION_CONTEXT_PREFIX = "/repl=";

	/** */
	public static Context getReplicationContext( final Experiment experiment,
		final String name ) throws NamingException
	{
		return experiment.getContext()
				.createSubcontext( REPLICATION_CONTEXT_PREFIX + name );
	}

	/**
	 * {@link ReplicationBuilder} constructor
	 * 
	 * @param treatment
	 * @throws NamingException
	 */
	public ReplicationBuilder( final Treatment treatment )
		throws NamingException
	{
		this( treatment, new UUID().toString() );
	}

	/**
	 * {@link ReplicationBuilder} constructor
	 * 
	 * @param treatment
	 * @param name the object's name in the RMI context
	 * @throws NamingException
	 */
	public ReplicationBuilder( final Treatment treatment, final String name )
		throws NamingException
	{
		super( getReplicationContext( treatment.getExperiment(), name ),
				treatment.getExperiment() );
		setDescription( name );
		setStreams( new HashMap<String, StreamInterface>() );
	}

	public ReplicationBuilder withDescription( final String description )
	{
		setDescription( description );
		return this;
	}

	public ReplicationBuilder withStream()
	{
		return withStream( System.currentTimeMillis() );
	}

	public ReplicationBuilder withStream( final long seed )
	{
		return withStream( DsolModelComponent.RNG_ID, seed );
	}

	public ReplicationBuilder withStream( final String name, final long seed )
	{
		return withStream( name, new MersenneTwister( seed ) );
	}

	public ReplicationBuilder withStream( final String name,
		final StreamInterface rng )
	{
		final Map<String, StreamInterface> streams = getStreams();
		if( streams == null )
			throw new NullPointerException( "RNG stream map not initialized" );
		else if( streams.put( name, rng ) != null )
			LOG.info( "Replaced stream " + name );
		return this;
	}

	@Override
	@JsonIgnore
	public Context getContext()
	{
		return super.getContext();
	}

	@Override
	@JsonIgnore
	public ExperimentBuilder getExperiment()
	{
		return (ExperimentBuilder) super.getExperiment();
	}

	@Override
	public TreatmentBuilder getTreatment()
	{
		return (TreatmentBuilder) super.getTreatment();
	}

	@Override
	public String toString()
	{
		try
		{
			// final JsonNode node = JsonUtil.getJOM().valueToTree(this);
			return JsonUtil.getJOM().writerWithDefaultPrettyPrinter()
					.writeValueAsString( this );
		} catch( final JsonProcessingException e )
		{
			LOG.warn( "Problem marshalling " + getClass().getName(), e );
			try
			{
				return getContext().getNameInNamespace();
			} catch( final NamingException e1 )
			{
				return super.toString();
			}
		}
	}

	public ReplicationBuilder initialize() throws SimRuntimeException
	{
		try
		{
			getExperiment().getSimulator().initialize( this,
					getTreatment().getReplicationMode() );
		} catch( final RemoteException e )
		{
			LOG.fatal( "Problem initializing DSOL replication", e );
			throw new NullPointerException( "Replication uninitializable" );
		}
		return this;
	}

	public ReplicationBuilder start() throws SimRuntimeException
	{
		try
		{
			getExperiment().getSimulator().start();
		} catch( final RemoteException e )
		{
			LOG.fatal( "Problem initializing DSOL replication", e );
			throw new NullPointerException( "Replication uninitializable" );
		}
		return this;
	}
}
