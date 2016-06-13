/* $Id: 78bdc3495417601286a847a1b6061eb5bfe8f023 $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/lifecycle/AbstractLifeCycle.java $
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
package io.coala.lifecycle;

import io.coala.name.Identifier;

/**
 * {@link AbstractLifeCycle}
 * 
 * @date $Date: 2014-06-03 14:26:09 +0200 (Tue, 03 Jun 2014) $
 * @version $Revision: 296 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 */
public abstract class AbstractLifeCycle<ID extends Identifier<?, ?>, S extends LifeCycleStatus<S>>
	extends AbstractMachine<ID, S> implements LifeCycle<S>
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link AbstractLifeCycle} zero-arg bean constructor
	 */
	protected AbstractLifeCycle()
	{
		super();
	}

	/**
	 * {@link AbstractLifeCycle} constructor
	 * 
	 * @param id
	 */
	public AbstractLifeCycle( final ID id )
	{
		super( id );
	}

	/** @param status */
	protected void forceStatus( final S status )
	{
		super.setStatus( status,
				status.isFinishedStatus() || status.isFailedStatus() );
	}

	/** @param status */
	protected void setStatus( final S status )
	{
		forceStatus( status );
	}

	@Override
	@Deprecated
	protected void setStatus( final S status, final boolean completed )
	{
		super.setStatus( status, completed );
	}

}