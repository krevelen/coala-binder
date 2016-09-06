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
package io.coala.eve3;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentInterface;
import com.almende.eve.instantiation.Initable;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Namespace;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.transport.Receiver;
import com.almende.util.callback.AsyncCallback;
import com.fasterxml.jackson.annotation.JsonIgnore;

import rx.Observer;

/**
 * {@link Eve3Container} TODO allow multiple name-spaces and services
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Eve3Container extends Receiver, Initable, AgentInterface
{

	String NAMESPACE = "exposed";

	/** @return the exposed object, or {@code null} if none (yet) */
	@JsonIgnore
	@Access( AccessType.PUBLIC )
	@Namespace( NAMESPACE )
	Object exposed();

	@JsonIgnore
	@Access( AccessType.SELF )
	List<URI> expose( Object exposed );

	/**
	 * exposes/wraps the package-protected
	 * {@link Agent#call(URI, Method, Object[], AsyncCallback)} method
	 * 
	 * @param <T> the return type, e.g. {@link Void}
	 * @param uri the remote procedure end-point
	 * @param method the remote procedure to call
	 * @param params the remote procedure parameters
	 * @param callback an {@link Observer} to call back (asynchronously)
	 */
	<T> void call( final URI uri, final Method method, final Object[] params,
		final Observer<T> callback );

	/**
	 * {@link Simple}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Simple extends Agent implements Eve3Container
	{
		/** the exposed referent */
		private transient WeakReference<Object> exposed = null;

		@Override
		public Object exposed()
		{
			return this.exposed == null ? null : this.exposed.get();
		}

		@Override
		public List<URI> expose( final Object exposed )
		{
			this.exposed = new WeakReference<Object>( exposed );
			return getUrls();
		}

		@Override
		public <T> void call( final URI url, final Method method,
			final Object[] args, final Observer<T> observer )
		{
			try
			{
				call( url, NAMESPACE + "." + method.getName(),
						new JSONRequest( method, args, null ).getParams(),
						new AsyncCallback<T>()
						{
							@Override
							public void onSuccess( final T result )
							{
								observer.onNext( result );
								observer.onCompleted(); // just one result expected
							}

							@Override
							public void onFailure( final Exception e )
							{
								observer.onError( e );
							}
						} );
			} catch( final IOException e )
			{
				observer.onError( e );
			}
		}
	}
}