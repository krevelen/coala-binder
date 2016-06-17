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
package io.coala.event.grant;

import io.coala.name.Identifiable;
import io.coala.name.Identifier;
import rx.Observable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link Grantable}
 */
@Deprecated
public interface Grantable<ID extends Identifier<?, ?>> extends
		Identifiable<ID>
{

	/**
	 * @return the {@link Observable} {@link Grant}s that have been released by
	 *         this {@link Grantable} object
	 */
	@JsonIgnore
	Observable<Grant> getReleased();
}
