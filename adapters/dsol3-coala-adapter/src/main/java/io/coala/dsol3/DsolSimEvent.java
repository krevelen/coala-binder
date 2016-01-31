/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.dsol3;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import com.eaio.uuid.UUID;

import io.coala.json.x.Wrapper;
import io.coala.name.x.Id;
import io.coala.time.x.Instant;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface;
import nl.tudelft.simulation.dsol.simtime.SimTime;

/**
 * {@link DsolSimEvent} wraps a {@link Callable} (with {@link Void} return
 * value) inside a DSOL {@link SimEventInterface}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class DsolSimEvent extends Wrapper.SimpleComparable<DsolSimEvent.ID>
	implements SimEventInterface<DsolTime>
{
	public static class ID extends Id<UUID>
	{
		protected ID()
		{
			// empty bean constructor
		}

		protected ID( final UUID value )
		{
			wrap( value );
		}

		public static DsolSimEvent.ID create()
		{
			return new ID( new UUID() );
		}
	}

	private final DsolTime time;

	/**
	 * the local procedure call, to be reconstructed upon remote deserialization
	 */
	private final transient Callable<Void> call;

	/**
	 * {@link DsolSimEvent} constructor
	 * 
	 * @param time
	 * @param call
	 */
	public DsolSimEvent( final DsolTime time, final Callable<Void> call )
	{
		this.time = time;
		this.call = call;
	}

	@Override
	public DsolTime getAbsoluteExecutionTime()
	{
		return this.time;
	}

	@Override
	public void execute() throws SimRuntimeException
	{
		try
		{
			this.call.call();
		} catch( final Throwable t )
		{
			throw new SimRuntimeException( "Problem executing event", t );
		}
	}

	@Override
	public short getPriority()
	{
		return NORMAL_PRIORITY;
	}

	/**
	 * {@link Builder}
	 * 
	 * @param <THIS> the concrete sub-type
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class Builder<THIS extends Builder<THIS>>
	{
		private DsolTime time;

		private Callable<Void> call;

		@SuppressWarnings( "rawtypes" )
		public THIS withTime( final SimTime time )
		{
			return withTime( DsolTime.valueOf( time ) );
		}

		public THIS withTime( final Instant time )
		{
			return withTime( DsolTime.valueOf( time ) );
		}

		@SuppressWarnings( "unchecked" )
		public THIS withTime( final DsolTime time )
		{
			this.time = time;
			return (THIS) this;
		}

		@SuppressWarnings( "unchecked" )
		public THIS withCall( final Callable<Void> call )
		{
			this.call = call;
			return (THIS) this;
		}

		public THIS withCall( final Object target, final Method method,
			final Object... args )
		{
			return withCall( new Callable<Void>()
			{
				@Override
				public Void call() throws Exception
				{
					method.setAccessible( true );
					method.invoke( target, args );
					return null;
				}
			} );
		}

		public DsolSimEvent build()
		{
			return new DsolSimEvent( this.time, this.call );
		}
	}
}