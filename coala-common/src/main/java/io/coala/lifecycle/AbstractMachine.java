/* $Id: d05fd438ee4a0b9216fb13a2f22aefc816628808 $
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
package io.coala.lifecycle;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.exception.ExceptionFactory;
import io.coala.name.AbstractIdentifiable;
import io.coala.name.Identifier;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

/**
 * {@link AbstractMachine}
 * 
 * @param <ID> the (super)type of {@link Identifier}
 * @param <S> the (super)type of {@link MachineStatus}
 */
@Deprecated
public abstract class AbstractMachine<ID extends Identifier<?, ?>, S extends MachineStatus<S>>
	extends AbstractIdentifiable<ID> implements Machine<S>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** the status */
	@LifeCycleManaged
	private S status;

	/** */
	@LifeCycleManaged
	private transient Subject<S, S> statusHistory = ReplaySubject.create();

	/**
	 * {@link AbstractMachine} zero-arg bean constructor
	 */
	protected AbstractMachine()
	{
		super();
	}

	/**
	 * {@link AbstractMachine} constructor
	 * 
	 * @param id
	 */
	public AbstractMachine( final ID id )
	{
		super( id );
	}

	/**
	 * @param status
	 * @param status
	 */
	protected void setStatus( final S status, final boolean completed )
	{
		final S current = getStatus();

		// sanity check
		if( !status.permitsTransitionFrom( current ) ) throw ExceptionFactory
				.createUnchecked( "Status {} not permitted from current: {}",
						status, current );

		MachineUtil.setStatus( this, status, completed );
	}

	@Override
	public synchronized S getStatus()
	{
		return this.status;
	}

	@Override
	@JsonIgnore
	public synchronized Observable<S> getStatusHistory()
	{
		return this.statusHistory.asObservable();
	}

}
