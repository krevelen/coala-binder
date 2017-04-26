/* $Id: e1229e134cce702980c1bafb06b65a648d3778be $
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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import nl.tudelft.simulation.dsol.DSOLModel;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;
import nl.tudelft.simulation.event.EventInterface;
import nl.tudelft.simulation.event.EventListenerInterface;
import nl.tudelft.simulation.event.EventProducer;
import nl.tudelft.simulation.event.EventType;

/**
 * {@link DsolEventObservable}
 * 
 * @version $Id: e1229e134cce702980c1bafb06b65a648d3778be $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public class DsolEventObservable implements EventListenerInterface
{

	/** listeners is the collection of interested listeners. */
	private Map<EventListenerInterface, Disposable> subscriptions = Collections
			.synchronizedMap( new HashMap<>() );

	/** the relay {@link rx.subjects.Subject} */
	private final transient Subject<EventInterface> relay = PublishSubject
			.create();

	@Override
	public void notify( final EventInterface event )
	{
		fireEvent( event );
	}

	/** publish {@link EventInterface events} as {@link rx.Observable} */
	public final Observable<EventInterface> events()
	{
		return this.relay;
	}

	/**
	 * publish {@link EventInterface events} with specified {@link EventType} as
	 * {@link rx.Observable}
	 */
	public final Observable<EventInterface> events( final EventType eventType )
	{
		return events().filter( event -> event.getType().equals( eventType ) );
	}

	public <T extends DsolEvent<?>> DsolEventObservable
		subscribeTo( final EventProducer producer, final Class<T> eventType )
	{
		if( producer instanceof DSOLModel )
			try
			{
				subscribeTo( ((DSOLModel) producer).getSimulator() );
			} catch( final RemoteException e )
			{
				this.relay.onError( e );
			}
		else if( producer instanceof SimulatorInterface )
			subscribeTo( (SimulatorInterface) producer );

		producer.addListener( this, DsolEvent.resolveType( eventType ) );
		return this;
	}

	/**
	 * @param simulator
	 */
	public void subscribeTo( final SimulatorInterface simulator )
	{
		try
		{
			simulator.addListener( event -> relay.onComplete(),
					SimulatorInterface.END_OF_REPLICATION_EVENT );
		} catch( final RemoteException e )
		{
			this.relay.onError( e );
		}

	}

	public DsolEventObservable subscribeTo( final EventProducer producer,
		final EventType... eventTypes )
	{
		if( eventTypes != null && eventTypes.length != 0 )
			for( EventType eventType : eventTypes )
			producer.addListener( this, eventType );
		return this;
	}

	/**
	 * @param valueOf
	 */
	protected void fireEvent( final EventInterface event )
	{
		this.relay.onNext( event );
	}

	public final boolean addListener( final EventListenerInterface listener,
		final EventType eventType )
	{
		synchronized( this.subscriptions )
		{
			return listener != null
					&& !this.subscriptions.containsKey( listener )
					&& null == this.subscriptions.put( listener,
							events( eventType ).subscribe( listener::notify ) );
		}
	}

	public final boolean removeListener( final EventListenerInterface listener )
	{
		final Disposable sub = this.subscriptions.get( listener );
		if( sub == null || sub.isDisposed() ) return false;
		sub.dispose();
		return true;
	}

}
