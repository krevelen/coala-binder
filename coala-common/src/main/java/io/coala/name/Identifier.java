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

import java.io.Serializable;

/**
 * {@link Identifier}
 * 
 * @version $Id$
 * @param <T> the {@link Comparable} and {@link Serializable} content type
 * @param <THIS> the concrete {@link Identifier} type
 * @deprecated please use {@link io.coala.json.Wrapper}
 */
@Deprecated
public interface Identifier<T extends Comparable<T> & Serializable, THIS extends Identifier<T, ?>>
		extends Serializable, Comparable<THIS>
{

}
