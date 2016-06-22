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
package io.coala.config;

/**
 * {@link ConfigDefaultConverter} inspired by
 * <a href="http://java-taste.blogspot.nl/2011/10/guiced-configuration.html" >
 * here</a>
 */
@Deprecated
public class ConfigDefaultConverter implements ConfigConverter<Object>
{

	/** @see ConfigConverter#convert(String) */
	@Override
	public Object convert( final String property )
	{
		return property.toString();
	}

}
