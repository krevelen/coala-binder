/* $Id: c10f9093c822bd06fd2472f353550c2706dff575 $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/main/java/com/almende/coala/exception/CoalaRuntimeException.java $
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
package io.coala.exception;

import org.apache.logging.log4j.message.StringFormattedMessage;

/**
 * {@link CoalaRuntimeException}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 * @deprecated
 */
public class CoalaRuntimeException extends ExceptionBuilder.UncheckedException
{

	/** */
	private static final long serialVersionUID = 1L;

	/**
	 * {@link CoalaRuntimeException} constructor
	 * 
	 * @param message
	 */
	protected CoalaRuntimeException( final String message )
	{
		super( null, new StringFormattedMessage( message ) );
	}

	/**
	 * {@link CoalaRuntimeException} constructor
	 * 
	 * @param message
	 * @param cause
	 */
	protected CoalaRuntimeException( final String message,
		final Throwable cause )
	{
		super( null, new StringFormattedMessage( message ), cause );
	}

}
