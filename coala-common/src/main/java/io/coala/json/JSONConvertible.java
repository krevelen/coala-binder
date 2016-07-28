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
package io.coala.json;

/**
 * {@link JSONConvertible} tags POJOs that have a JSON representation,
 * {@link #toSL()}
 */
@Deprecated
public interface JSONConvertible<THIS extends JSONConvertible<?>>
{

	/** @return the JSON representation of this object */
	String toJSON();

	/** @return a new object deserialized from a JSON representation */
	THIS fromJSON( String jsonValue );

}
