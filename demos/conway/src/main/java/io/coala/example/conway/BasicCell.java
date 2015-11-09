/* $Id$
 * $URL: https://dev.almende.com/svn/abms/coala-examples/src/main/java/io/coala/example/conway/BasicCell.java $
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
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.example.conway;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import io.coala.agent.BasicAgent;
import io.coala.bind.Binder;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.log.InjectLogger;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link BasicCell}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
public class BasicCell extends BasicAgent implements Cell
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private transient Logger LOG;

	/** */
	private final transient Subject<CellState, CellState> states = PublishSubject
			.create();

	/**
	 * {@link BasicCell} constructor, even works when {@code private} ?!?
	 * 
	 * @param binder
	 */
	@Inject
	private BasicCell(final Binder binder)
	{
		super(binder);
	}

	@Override
	public void initialize() throws Exception
	{
		super.initialize();

		getBinder().inject(CellWorld.class).getStatusHistory()
				.subscribe(new Observer<BasicCapabilityStatus>()
				{
					@Override
					public void onCompleted()
					{
						// world has ended
						die();
					}

					@Override
					public void onError(final Throwable e)
					{
						e.printStackTrace();
					}

					@Override
					public void onNext(final BasicCapabilityStatus ignore)
					{
						// ignore
					}
				});
	}

	@Override
	public Observable<CellState> myStates()
	{
		return this.states.asObservable();
	}

}
