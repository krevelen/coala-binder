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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link BinTuple}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class BinTuple extends Tuple
{
	public static <T extends Bin<?>> Tuple of( final T o )
	{
		return of( Collections.singletonList( o ) );
	}

	@SafeVarargs
	public static <T extends Bin<?>> Tuple of( final T... o )
	{
		return of( Arrays.asList( o ) );
	}

	public static <T extends Bin<?>> BinTuple ofList( final List<T> values )
	{
		return new BinTuple( values );
	}

	/**
	 * {@link BinTuple} constructor
	 * 
	 * @param values
	 */
	public BinTuple( final List<? extends Bin<?>> values )
	{
		super( values );
	}

	public boolean contains( final Tuple tuple )
	{
		@SuppressWarnings( "rawtypes" )
		final List<Comparable> bins = super.values();
		for( int i = 0, n = bins.size(); i < n; i++ )
			if( !contains( (Bin<?>) bins.get( i ), tuple.values().get( i ) ) )
				return false;
		return true;
	}

	@SuppressWarnings( "unchecked" )
	static <T extends Comparable<?>> boolean contains( final Bin<T> bin,
		final Object value )
	{
		return bin.contains( (T) value );
	}
}