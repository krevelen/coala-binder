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
 * 
 * Copyright (c) 2010-2013 Almende B.V. 
 */
package io.coala.name;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * {@link Identifiable} owns an {@link AbstractIdentifier}
 * 
 * @version $Id$
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 * 
 * @param <ID> the {@link Identifier} type
 * @param <THIS> the concrete {@link Identifiable} type
 */
@JsonInclude(Include.NON_NULL)
@JsonTypeInfo(use = Id.CLASS, include = As.PROPERTY, property = "class")
public interface Identifiable<ID extends Identifier<?, ?>>
		extends Comparable<Identifiable<ID>>
{

	/**
	 * @return the {@link ID} value
	 */
	ID getID();

	/**
	 * {@link Builder}
	 * 
	 * @version $Id$
	 * @author <a href="mailto:Rick@almende.org">Rick</a>
	 * 
	 * @param <ID> the {@link Identifiable}'s {@link Identifier} type
	 * @param <T> the {@link Identifiable} type
	 * @param <THIS> the concrete {@link Builder} type
	 */
	public interface Builder<ID extends Identifier<?, ?>, T extends Identifiable<ID>, THIS extends Builder<ID, T, THIS>>
	{
		/**
		 * @param id the {@link ID} value to set
		 * @return this {@link Builder} for chaining
		 */
		THIS withID(ID id);

		/**
		 * @return the built {@link T} result
		 */
		T build();
	}

}
