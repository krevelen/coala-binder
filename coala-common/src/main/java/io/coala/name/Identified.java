/* $Id: ebe93594e8299023d7bbd41b8f6a183ee72ef89b $
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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link Identified} wraps an identifier *
 * 
 * @version $Id: ebe93594e8299023d7bbd41b8f6a183ee72ef89b $
 * @author Rick van Krevelen
 */
//@JsonTypeInfo( use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY,
//	property = "@class" )
public interface Identified<T>
{

	String ID_KEY = "id";

	/** @return the (ordinal) identifier */
	@JsonProperty( ID_KEY )
	T id();

	static int hashCode( final Identified<?> self )
	{
		return self.id().hashCode();
	}

	static boolean equals( final Identified<?> self, final Object other )
	{
		return self.id().equals( other );
	}

	static String toString( final Identified<?> self )
	{
		return self.id().toString();
	}

	/**
	 * {@link Ordinal} kind of {@link Identified}
	 * 
	 * @param <T> the {@link Comparable} identifier value type
	 * @version $Id: ebe93594e8299023d7bbd41b8f6a183ee72ef89b $
	 * @author Rick van Krevelen
	 */
	interface Ordinal<T extends Comparable<? super T>>
		extends Identified<T>, Comparable<Identified<T>>
	{
		@Override
		default int compareTo( final Identified<T> o )
		{
			return id().compareTo( (T) o.id() );
		}
	}
}