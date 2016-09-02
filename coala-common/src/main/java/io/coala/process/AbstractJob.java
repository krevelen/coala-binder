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
 */
package io.coala.process;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.coala.config.CoalaProperty;
import io.coala.name.Identifier;

/**
 * {@link AbstractJob}
 */
@Deprecated
public abstract class AbstractJob<ID extends Identifier<?, ?>>
	extends AbstractProcess<ID> implements Job<ID>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private final String stackTrace;

	/**
	 * {@link AbstractJob} zero-arg bean constructor
	 */
	protected AbstractJob()
	{
		super();
		this.stackTrace = null;
	}

	/**
	 * {@link AbstractJob} constructor
	 * 
	 * @param id
	 */
	public AbstractJob( final ID id )
	{
		super( id );
		if( CoalaProperty.addOriginatorStackTrace.value().getBoolean( false ) )
		{
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter( sw );
			new Exception().printStackTrace( pw );

			this.stackTrace = sw.toString();
		} else
			this.stackTrace = null;
	}

	/** @see Job#getStackTrace() */
	@Override
	public String getStackTrace()
	{
		return this.stackTrace;
	}

}
