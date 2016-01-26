/* $Id$
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
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.lifecycle;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.exception.CoalaExceptionFactory;
import io.coala.name.AbstractIdentifiable;
import io.coala.name.Identifier;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

/**
 * {@link AbstractMachine}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <ID> the (super)type of {@link Identifier}
 * @param <S> the (super)type of {@link MachineStatus}
 */
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
	public AbstractMachine(final ID id)
	{
		super(id);
	}

	/**
	 * @param status
	 */
	protected void setStatus(final S status, final boolean completed)

	{
		final S currentStatus = getStatus();

		// sanity check
		if (!status.permitsTransitionFrom(currentStatus))
			throw CoalaExceptionFactory.VALUE_NOT_ALLOWED.createRuntime(
					"status", status, "Not permitted from current status: "
							+ currentStatus);

		MachineUtil.setStatus(this, status, completed);
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
