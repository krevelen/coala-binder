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
package io.coala.jsa.sl;

import java.io.Serializable;

// Type for requirements
public class SLParsableSerializable implements SLParsable, Serializable
{

	/** */
	private static final long serialVersionUID = 1L;

	protected SLParsableSerializable()
	{
		// zero argument constructor;
	}

	public SLParsableSerializable( final String string )
	{
		this.value = string;
	}

	private String value;

	/**
	 * @return the value
	 */
	public String getValue()
	{
		return this.value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue( final String value )
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}

	@Override
	public boolean equals( Object o )
	{
		return toString().equals( o.toString() );
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

}