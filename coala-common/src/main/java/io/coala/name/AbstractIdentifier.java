/* $Id: 78c567fb2863e8dd31b4656a4f7cca82a3ffbccb $
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.coala.json.JSONConvertible;
import io.coala.json.JsonUtil;

/**
 * {@link AbstractIdentifier} contains some identifier content type
 * 
 * @param <T> the type of {@link Comparable} and {@link Serializable}
 * @version $Id: 78c567fb2863e8dd31b4656a4f7cca82a3ffbccb $
 * @author Rick van Krevelen
 * @deprecated please use {@link io.coala.name.Id}
 */
@Deprecated
@JsonInclude( Include.NON_NULL )
@JsonTypeInfo( use = Id.CLASS, include = As.PROPERTY, property = "class" )
public abstract class AbstractIdentifier<T extends Comparable<T> & Serializable>
	implements Identifier<T, AbstractIdentifier<T>>,
	JSONConvertible<AbstractIdentifier<T>>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** the identifier value */
	private T value = null;

	/**
	 * {@link AbstractIdentifier} zero-arg bean constructor
	 */
	protected AbstractIdentifier()
	{
		super();
	}

	/**
	 * {@link AbstractIdentifier} constructor
	 * 
	 * @param value the (unique) {@link T} value
	 */
	protected AbstractIdentifier( final T value )
	{
		setValue( value );
	}

	/**
	 * @param value the (unique) {@link T} value of this
	 *            {@link AbstractIdentifier} object
	 */
	protected void setValue( final T value )
	{
		this.value = value;
	}

	/**
	 * @return the identifier value
	 */
	public T getValue()
	{
		return this.value;
	}

	@Override
	public int compareTo( final AbstractIdentifier<T> other )
	{
		return getValue().compareTo( other.getValue() );
	}

	@Override
	public String toString()
	{
		return getValue().toString();
	}

	@Override
	public int hashCode()
	{
		return getValue() == null ? super.hashCode() : getValue().hashCode();
	}

	@Override
	public boolean equals( final Object other )
	{
		if( other == null || other.getClass() != getClass() ) return false;

		@SuppressWarnings( "unchecked" )
		final AbstractIdentifier<T> that = (AbstractIdentifier<T>) other;
		return (getValue() == null && that.getValue() == null)
				|| (getValue() != null && getValue().equals( that.getValue() ));
	}

	@Override
	public String toJSON()
	{
		return JsonUtil.toJSON( this );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public AbstractIdentifier<T> fromJSON( final String jsonValue )
	{
		return (AbstractIdentifier<T>) JsonUtil.valueOf( jsonValue,
				getClass() );
	}

}
