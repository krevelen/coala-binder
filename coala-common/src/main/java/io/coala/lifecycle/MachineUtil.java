/* $Id: aef5e30b392038b499b5be50a5e2057238304171 $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/lifecycle/MachineUtil.java $
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
package io.coala.lifecycle;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Logger;

import io.coala.exception.CoalaException;
import io.coala.exception.CoalaExceptionFactory;
import io.coala.factory.ClassUtil;
import io.coala.log.LogUtil;
import io.coala.name.Identifiable;
import io.coala.name.Identifier;
import io.coala.util.Util;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * {@link MachineUtil} utility class, e.g. to manage the {@link MachineStatus}
 * in {@link Machine}s
 * 
 * @version $Revision: 296 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class MachineUtil implements Util
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( MachineUtil.class );

	/**
	 * {@link MachineUtil} constructor
	 */
	private MachineUtil()
	{
		// utility class should not provide protected/public instances
	}

	/** */
	private static final Scheduler STATUS_NOTIFIER = Schedulers
			.from( Executors.newCachedThreadPool() );

	/** */
	private static final Map<String, StatusListeners> LISTENER_CACHE = new HashMap<>();

	/**
	 * @param target the error generating {@link Object}
	 * @return the {@link Class#getSimpleName()} possibly extended with
	 *         respective the {@link Identifiable}'s {@link Identifier}
	 */
	private static String nameOf( final Object target )
	{
		return target instanceof Identifiable
				? String.format( "%s[%s]", target.getClass().getSimpleName(),
						((Identifiable<?>) target).getID() )
				: target.getClass().getSimpleName();
	}

	private static String listenerFieldKey( final Class<?> ownerType,
		final Class<?> valueType )
	{
		return ownerType.getName() + valueType.getName();
	}

	protected static class StatusListeners
	{

		private final StatusListeners parent;

		private final Set<Field> valueFields = new HashSet<>();

		private final Set<Field> observerFields = new HashSet<>();

		public <S extends MachineStatus<S>> StatusListeners(
			final Class<?> targetType, final Class<S> valueType )
		{
			this.parent = from( targetType.getSuperclass(), valueType );

			for( final Field field : targetType.getDeclaredFields() )
			{
				if( !field.isAnnotationPresent( LifeCycleManaged.class ) )
					continue;

				try
				{
					field.setAccessible( true );
				} catch( final Throwable t )
				{
					LOG.warn(
							"Problem accessing Field annotated with @"
									+ LifeCycleManaged.class.getSimpleName(),
							t );
				}

				if( ClassUtil.isAssignableFrom( Observer.class,
						field.getType() )
				/*
				 * && valueType .isAssignableFrom(ClassUtil
				 * .getTypeArguments(Observer.class, field.getType().asSubclass(
				 * Observer.class)) .get(0))
				 */ )
					this.observerFields.add( field );
				else if( ClassUtil.isAssignableFrom( field.getType(),
						valueType ) )
					this.valueFields.add( field );
			}
		}

		public boolean isEmpty()
		{
			return (this.parent == null || this.parent.isEmpty())
					&& this.valueFields.isEmpty()
					&& this.observerFields.isEmpty();
		}

		public <S extends MachineStatus<S>> void
			notify( final Machine<S> target, final S newValue )
		{
			if( this.parent != null ) this.parent.notify( target, newValue );
			for( final Field field : this.valueFields )
				try
				{
					@SuppressWarnings( "unchecked" )
					final S oldValue = (S) field.get( target );
					if( oldValue != null && newValue != null
							&& !oldValue.permitsTransitionTo( newValue ) )
					{
						LOG.warn( String.format(
								"Condoning illegal transition from "
										+ "%s to %s at %s (field %s)",
								oldValue, newValue, nameOf( target ), field ) );
						// return;
					}
					// TODO apply locking, or use available setter a la Jackson?
					field.set( target, newValue );
				} catch( final Throwable t )
				{
					LOG.error(
							String.format( "Problem accessing field %s of %s",
									field, nameOf( target ) ),
							t );
					t.printStackTrace();
				}
			for( final Field field : this.observerFields )
				try
				{
					@SuppressWarnings( "unchecked" )
					final Observer<S> obs = (Observer<S>) field.get( target );

					if( newValue == null )
						obs.onCompleted();
					else
						obs.onNext( newValue );
					// System.err.println(String.format(
					// "Notified %s at " + "%s (field %s.%s)", newValue,
					// targetName, observerType.getSimpleName(),
					// observerField.getName()));
				} catch( final Throwable t )
				{
					LOG.error( String.format(
							"Problem notifying %s at " + "%s (field %s)",
							newValue == null ? "COMPLETED" : newValue,
							nameOf( target ), field.toString() ), t );
				}
		}

		public static <S extends MachineStatus<S>> StatusListeners
			from( final Class<?> targetType, final Class<S> valueType )
		{
			if( !Machine.class.isAssignableFrom( targetType ) ) return null;

			final String typeKey = listenerFieldKey( targetType, valueType );
			StatusListeners result = LISTENER_CACHE.get( typeKey );

			if( result != null ) return result;

			result = new StatusListeners( targetType, valueType );
			LISTENER_CACHE.put( typeKey, result );

			if( result.isEmpty() ) LOG.warn( String.format(
					"No %s listener fields found to update in target type: %s",
					valueType.getSimpleName(), targetType.getName() ) );

			return result;
		}
	}

	/**
	 * @param target
	 * @param newStatus
	 * @throws CoalaException
	 */
	public static <S extends MachineStatus<S>> void setStatus(
		final Machine<S> target, final S newValue, final boolean completed )
	{
		if( target == null )
			throw CoalaExceptionFactory.VALUE_NOT_SET.createRuntime( "target" );

		@SuppressWarnings( "unchecked" )
		final StatusListeners listeners = StatusListeners
				.from( target.getClass(), newValue.getClass() );

		synchronized( target )
		{
			STATUS_NOTIFIER.createWorker().schedule( new Action0()
			{
				@Override
				public void call()
				{
					if( newValue != null ) listeners.notify( target, newValue );
					if( completed ) listeners.notify( target, null );
				}
			} );
		}
	}
}
