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
package io.coala.json;

import java.beans.PropertyEditorSupport;

import io.coala.exception.Thrower;

// TODO find a use case with BeanUtils.getPropertyDescriptors ...
public class JsonPropertyEditor extends PropertyEditorSupport
{
	@Override
	public void setAsText( final String json ) throws IllegalArgumentException
	{
		try
		{
			Class<?> type = // TypeArguments.of( JsonPropertyEditor.class, getClass() ).get( 0 );
					getValue().getClass();
			setValue( JsonUtil.valueOf( json, type ) );
		} catch( final Throwable e )
		{
			Thrower.throwNew( IllegalArgumentException::new,
					() -> "Problem editing property from JSON: " + json );
		}
	}
}