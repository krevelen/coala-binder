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

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.coala.bind.LocalBinder;
import io.coala.inter.Exposer;

/**
 * {@link Eve3Exposer}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Singleton
public class Eve3Exposer implements Exposer
{
	@Inject
	private LocalBinder binder;

	@Inject
	private Eve3Factory eve3;

	@Override
	public <T> List<URI> exposeAs( final String id, final Class<T> serviceIntf,
		final T serviceImpl )
	{
		return this.eve3.getAgent( id == null ? this.binder.id() : id ).expose(
				// FIXME apply http://stackoverflow.com/a/30287201 to 
				// annotate each method as Access(PUBLIC)  
				Proxy.newProxyInstance( serviceIntf.getClassLoader(),
						new Class[]
				{ serviceIntf }, ( proxy, method, args ) ->
				{
					return method.invoke( serviceImpl, args );
				} ) );
	}
}