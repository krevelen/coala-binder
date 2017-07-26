/* $Id: 9b29d6b5d0ccbad74c81ea92abc2e5ff6ec760a3 $
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
import io.reactivex.Observable;

/**
 * {@link Eve3Invoker}
 * 
 * @version $Id: 9b29d6b5d0ccbad74c81ea92abc2e5ff6ec760a3 $
 * @author Rick van Krevelen
 */
@Singleton
public class Eve3Invoker implements Invoker
{
	@Inject
	private LocalBinder binder;

	@Inject
	private Eve3Factory eve3;

//	@SuppressWarnings( "unchecked" )
	@Override
	public <T> Observable<T> invoke( final URI target, final Method method,
		final Object... args )
	{
		return Observable.unsafeCreate( subscriber ->
		{
			this.eve3.getAgent( this.binder.id() ).call( target, method, args,
					subscriber );
		} );
	}
}