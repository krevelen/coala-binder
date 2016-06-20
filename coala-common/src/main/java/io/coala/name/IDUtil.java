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
package io.coala.name;

import io.coala.util.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * {@link IDUtil} provides identifier helpr methods
 */
@Deprecated
public class IDUtil implements Util
{

	/**
	 * @param identifiables
	 * @return the string representations of the wrapped identifiers
	 */
	public static String[] toStringArray(
		final Collection<? extends Identifiable<?>> identifiables )
	{
		final Collection<String> result = toString( identifiables );
		return result.toArray( new String[result.size()] );
	}

	/**
	 * @param identifiables
	 * @return the string representations of the wrapped identifiers
	 */
	public static Collection<String>
		toString( final Collection<? extends Identifiable<?>> identifiables )
	{
		final Collection<String> result = new ArrayList<String>();
		for( Identifiable<?> identifier : identifiables )
			result.add( identifier.getID().toString() );
		return result;
	}

}
