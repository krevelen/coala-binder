/* $Id: f72f19c87e997baef47903c8d71facb597eea58b $
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
package io.coala.config;

import java.lang.reflect.Method;

import org.aeonbits.owner.Converter;

import io.coala.json.JsonUtil;

/**
 * {@link JsonConverter} is a JSON-string {@link Converter} to the expected type
 * 
 * @version $Id: f72f19c87e997baef47903c8d71facb597eea58b $
 * @author Rick van Krevelen
 */
public class JsonConverter implements Converter<Object>
{
//	@SuppressWarnings( "unchecked" )
	@Override
	public Object convert( final Method method, final String input )
	{
//		final Class<?> returnType = (Class<T>) TypeArguments
//				.of( JsonConverter.class, getClass() ).get( 0 );
//		Objects.requireNonNull( returnType );
		return JsonUtil.valueOf( /* "\"" + */ input /* + "\"" */,
				method.getReturnType() );
	}
}
