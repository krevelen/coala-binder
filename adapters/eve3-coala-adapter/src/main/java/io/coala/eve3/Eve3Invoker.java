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

import java.lang.reflect.Method;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.coala.bind.LocalBinder;
import io.coala.inter.Invoker;
import rx.Observable;

/**
 * {@link Eve3Invoker}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Singleton
public class Eve3Invoker implements Invoker
{
	@Inject
	private LocalBinder binder;

	@Inject
	private Eve3Factory eve3;

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> Observable<T> invoke( final URI remoteAddress,
		final Method method, final Object... args )
	{
		return Observable.create( subscriber ->
		{
			final String addr = remoteAddress.toASCIIString();
			final URI uri = addr.indexOf( ':' ) < 0
					? URI.create( "local:" + addr ) : remoteAddress;
			this.eve3.getAgent( this.binder.id() ).call( uri, method, args,
					subscriber );
		} );
	}
}