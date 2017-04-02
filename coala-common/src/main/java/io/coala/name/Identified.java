/* $Id: 86b29591d7882848883604bf4871baced695fb20 $
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
 * @version $Id$
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
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@SuppressWarnings( "rawtypes" )
	interface Ordinal<T extends Comparable>
		extends Identified<T>, Comparable<Identified<T>>
	{
		@SuppressWarnings( "unchecked" )
		@Override
		default int compareTo( final Identified<T> o )
		{
			return id().compareTo( (T) o.id() );
		}
	}

	class Simple<T> implements Identified<T>
	{

		protected static <T extends Simple<ID>, ID> T of( final T result,
			final ID id )
		{
			result.id = id;
			return result;
		}

		public static <T> Simple<T> of( final T id )
		{
			return of( new Simple<T>(), id );
		}

		protected T id;

		@Override
		public T id()
		{
			return this.id;
		}

		@Override
		public int hashCode()
		{
			return Identified.hashCode( this );
		}

		@Override
		public boolean equals( final Object that )
		{
			return Identified.equals( this, that );
		}

		@Override
		public String toString()
		{
			return Identified.toString( this );
		}
	}

	@SuppressWarnings( "rawtypes" )
	class SimpleOrdinal<T extends Comparable> extends Simple<T>
		implements Ordinal<T>
	{
		public static <T extends Comparable> SimpleOrdinal<T>
			of( final T id )
		{
			return of( new SimpleOrdinal<T>(), id );
		}
	}
}