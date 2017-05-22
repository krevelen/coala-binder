/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.coala.util.Comparison;

/**
 * {@link Tuple} is a fixed-size vector of any {@link Comparable} entries, made
 * ordinal with a piece-wise comparison implementation
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@SuppressWarnings( "rawtypes" )
public class Tuple implements Comparable<Tuple>
{
	public static Tuple of( final Comparable<?> o )
	{
		return of( Collections.singletonList( o ) );
	}

	@SafeVarargs
	public static Tuple of( final Comparable<?>... o )
	{
		return of( Arrays.asList( o ) );
	}

	public static <T extends Comparable<?>> Tuple of( final List<T> values )
	{
		if( values instanceof List ) return new Tuple( (List<T>) values );
		final List<T> list = new ArrayList<>();
		for( T value : values )
			list.add( value );
		return new Tuple( list );
	}

	private List<Comparable> list;

	public Tuple( final List<? extends Comparable<?>> values )
	{
		this.list = Collections.unmodifiableList( values );
	}

	public List<Comparable> values()
	{
		return this.list;
	}

	@Override
	public int hashCode()
	{
		return values().hashCode();
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public int compareTo( final Tuple that )
	{
		int result = 0;
		for( int i = 0, n = values().size(); result == 0 && i < n; i++ )
			result = Comparison.compare( this.values().get( i ),
					that.values().get( i ) );
		return result;
	}

	@Override
	public boolean equals( final Object rhs )
	{
		if( rhs == this ) return true;
		if( rhs == null || rhs instanceof Tuple == false ) return false;
		final Tuple that = (Tuple) rhs;
		return values() == null ? that.values() == null
				: values().equals( that.values() );
	}

	@Override
	public String toString()
	{
		return values().toString();
	}
}