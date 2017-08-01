/* $Id: 02eeac1c68425728db678e58488bef882bb4248b $
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
package io.coala.dsol3.legacy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import javax.measure.Quantity;

import com.eaio.uuid.UUID;

import io.coala.json.Wrapper;
import io.coala.name.Id;
import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface;

/**
 * {@link DsolSimEvent} wraps a {@link Callable} (with {@link Void} return
 * value) inside a DSOL {@link SimEventInterface}
 * 
 * @version $Id: 02eeac1c68425728db678e58488bef882bb4248b $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class DsolSimEvent<Q extends Quantity<Q>>
	extends Wrapper.SimpleOrdinal<DsolSimEvent.ID>
	implements SimEventInterface<DsolTime<Q>>
{
	public static class ID extends Id.Ordinal<UUID>
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

	private final DsolTime<Q> time;

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
	public DsolSimEvent( final DsolTime<Q> time, final Callable<Void> call )
	{
		this.time = time;
		this.call = call;
	}

	@Override
	public DsolTime<Q> getAbsoluteExecutionTime()
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

	public static <Q extends Quantity<Q>> DsolSimEvent<Q>
		of( final DsolTime<Q> when, final Callable<Void> call )
	{
		return new DsolSimEvent<Q>( when, call );
	}

	public static <Q extends Quantity<Q>> DsolSimEvent<Q>
		of( final DsolTime<Q> when, final Runnable runnable )
	{
		return of( when, new Callable<Void>()
		{
			@Override
			public Void call() throws Exception
			{
				runnable.run();
				return null;
			}
		} );
	}

	public static <Q extends Quantity<Q>> DsolSimEvent<Q> of(
		final DsolTime<Q> when, final Object target, final Method method,
		final Object... args )
	{
		return of( when, new Callable<Void>()
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

}