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

import javax.inject.Inject;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.coala.json.JsonUtil;

/**
 * {@link AbstractIdentifiable}
 * 
 * @param <ID> the type of {@link Identifier}
 * @version $Id$
 * @author Rick van Krevelen
 * @deprecated please use {@link io.coala.name.Id}
 */
@JsonInclude( Include.NON_NULL )
@JsonTypeInfo( use = Id.CLASS, include = As.PROPERTY, property = "class" )
@Deprecated
public abstract class AbstractIdentifiable<ID extends Identifier<?, ?>>
	implements Identifiable<ID>, Serializable
{

	/** */
	private static final long serialVersionUID = 1L;

	/** the injected {@link Logger} */
	@Inject
	private transient Logger LOG;

	/** */
	@JsonProperty( "id" )
	private ID iD;

	/**
	 * {@link AbstractIdentifiable} zero-arg bean constructor
	 */
	protected AbstractIdentifiable()
	{
	}

	/**
	 * {@link AbstractIdentifiable} constructor
	 * 
	 * @param ID
	 */
	protected AbstractIdentifiable( final ID ID )
	{
		setID( ID );
	}

	@Override
	public ID getID()
	{
		return this.iD;
	}

	/**
	 * @param iD the {@link ID} identifying this object
	 */
	protected void setID( final ID iD )
	{
		if( this.iD == null )
			this.iD = iD;
		else
			LOG.warn( "ID already set, ignoring: " + iD );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public int compareTo( final Identifiable<ID> other )
	{
		return ((Comparable<ID>) getID()).compareTo( other.getID() );
	}

	@Override
	public String toString()
	{
		try
		{
			return getClass().getSimpleName() + JsonUtil.toJSON( this );
		} catch( final Throwable t )
		{
			t.printStackTrace();
			return getClass().getName();
		}
	}

	/**
	 * Apply {@link ID#hashCode()} if available (assuming unique identifiers),
	 * or {@link Object#hashCode()} otherwise
	 */
	@Override
	public int hashCode()
	{
		return getID() == null ? super.hashCode() : getID().hashCode();
	}

	/**
	 * Apply {@link ID#equals()} if available. Must be extended to consider
	 * equality of other properties in sub-types (if relevant)
	 */
	@Override
	public boolean equals( final Object other )
	{
		if( other == null || other.getClass() != getClass() ) return false;

		@SuppressWarnings( "unchecked" )
		final AbstractIdentifiable<ID> that = getClass().cast( other );
		return (getID() == null && that.getID() == null)
				|| (getID() != null && getID().equals( that.getID() ));
	}

	/**
	 * {@link AbstractBuilder} is an example approach to implementing builders
	 * for your {@link Identifiable} objects
	 * 
	 * @version $Id$
	 * @author <a href="mailto:rick.van.krevelen@rivm.nl">Rick van Krevelen</a>
	 *
	 * @param <T> the concrete {@link AbstractIdentifiable} result type
	 * @param <THIS> the concrete {@link AbstractBuilder} type
	 */
	protected class AbstractBuilder<T extends AbstractIdentifiable<ID>, THIS extends AbstractBuilder<T, THIS>>
		implements Builder<ID, T, THIS>
	{

		/** the result */
		private final T result;

		/**
		 * {@link AbstractBuilder} constructor
		 * 
		 * @param result the resulting object being built
		 */
		protected AbstractBuilder( final T result )
		{
			this.result = result;
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public THIS withID( final ID id )
		{
			this.result.setID( id );
			return (THIS) this;
		}

		@Override
		public T build()
		{
			return result;
		}

	}

}
