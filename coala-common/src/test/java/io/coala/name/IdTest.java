/* $Id: e6295c63830424b277a9bdd125218a9c849801e8 $
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
package io.coala.name;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.coala.exception.ExceptionStream;
import io.coala.log.LogUtil;
import io.coala.name.Id;
import rx.Observer;

/**
 * {@link IdTest}
 * 
 * @version $Id: e6295c63830424b277a9bdd125218a9c849801e8 $
 * @author Rick van Krevelen
 */
public class IdTest
{

	public static class MyId extends Id<Object>
	{
		public static MyId valueOf( final Object value )
		{
			final MyId result = new MyId();
			result.wrap( value );
			return result;
		}
	}

	public static class MyWrappedId extends Id<MyId>
	{
		public static MyWrappedId valueOf( final MyId value )
		{
			final MyWrappedId result = new MyWrappedId();
			result.wrap( value );
			return result;
		}
	}

	public static class MyOrdinal extends Id.Ordinal<String>
	{
		public static MyOrdinal valueOf( final String value )
		{
			final MyOrdinal result = new MyOrdinal();
			result.wrap( value );
			return result;
		}
	}

	@SuppressWarnings( "rawtypes" )
	public static class MyChild extends Id.OrdinalChild<String, Comparable>
	{
		public static MyChild valueOf( final String value )
		{
			final MyChild result = new MyChild();
			result.wrap( value );
			return result;
		}

		public MyChild withParent( final Comparable value )
		{
			parent( value );
			return this;
		}
	}

	/** {@link String#toString()} */
	private static final Logger LOG = LogUtil.getLogger( IdTest.class );

	@BeforeClass
	public static void listenExceptions()
	{
		ExceptionStream.asObservable().subscribe( new Observer<Throwable>()
		{
			@Override
			public void onCompleted()
			{
				LOG.trace( "JVM COMPLETED" );
			}

			@Override
			public void onError( final Throwable e )
			{
				LOG.trace( "JVM FAILED" );
			}

			@Override
			public void onNext( final Throwable t )
			{
				LOG.error( "Intercept " + t.getClass().getSimpleName(), t );
			}
		} );
	}

	@Ignore // FIXME
	@Test
	public void equalsTest()
	{
		LOG.info( "Testing " + Id.class + "<" + Id.class.getSimpleName() + "<"
				+ Object.class.getSimpleName() + ">>" );
		final Object valueObject = "myValue";
		final MyId valueWrap = MyId.valueOf( valueObject );
		final MyId sameWrap = MyId.valueOf( valueObject );
		final MyWrappedId wrappedWrap = MyWrappedId.valueOf( sameWrap );
		final MyOrdinal similarWrap = MyOrdinal
				.valueOf( valueObject.toString() );

		assertEquals( "Id#equals() must return #equals() of wrapped Object",
				valueWrap, sameWrap );
		assertNotEquals( "Id#equals() must return false for different types",
				valueWrap, similarWrap );
		assertNotEquals( "Id#equals() must return false for different types",
				valueWrap, wrappedWrap );
	}

	@Test
	public void toStringTest()
	{
		LOG.info( "Testing " + Id.class + "<" + Object.class.getSimpleName()
				+ ">" );
		final Object valueObject = "myValue";
		final MyId valueWrap = MyId.valueOf( valueObject );
		final MyId sameWrap = MyId.valueOf( valueObject );
		final MyOrdinal similarWrap = MyOrdinal
				.valueOf( valueObject.toString() );
		final MyWrappedId wrappedWrap = MyWrappedId.valueOf( sameWrap );

		assertEquals( "Id#toString() must return #toString() of wrapped Object",
				valueObject.toString(), valueWrap.toString() );
		assertEquals( "Id#toString() must return same for same values",
				similarWrap.toString(), valueWrap.toString() );
		assertEquals( "Id#toString() must return #toString() of wrapped Object",
				valueObject.toString(), wrappedWrap.toString() );
		assertEquals( "Id#toString() must return same for same values",
				sameWrap.toString(), wrappedWrap.toString() );
	}

	@Test
	public void hashCodeTest()
	{
		LOG.info( "Testing " + Id.class + "<" + Id.class.getSimpleName() + "<"
				+ Object.class.getSimpleName() + ">>" );
		final Object valueObject = "myValue";
		final MyId valueWrap = MyId.valueOf( valueObject );
		final MyId sameWrap = MyId.valueOf( valueObject );
		final MyWrappedId wrappedWrap = MyWrappedId.valueOf( sameWrap );
		final MyOrdinal similarWrap = MyOrdinal
				.valueOf( valueObject.toString() );

		assertNotEquals(
				"Id#hashCode() must return different #hashCode() than wrapped Object",
				valueObject.hashCode(), valueWrap.hashCode() );
		assertNotEquals(
				"Id#hashCode() must return different for different types",
				similarWrap.hashCode(), valueWrap.hashCode() );
		assertNotEquals(
				"Id#hashCode() must return different #hashCode() than wrapped Object",
				valueObject.hashCode(), wrappedWrap.hashCode() );
		assertNotEquals(
				"Id#hashCode() must return different for different types",
				valueWrap.hashCode(), wrappedWrap.hashCode() );
	}

	@Test
	public void compareToTest()
	{
		LOG.trace( "Testing " + Id.Ordinal.class );
		final String value1String = "myValue1";
		final String value2String = "myValue2";
		final MyOrdinal value1Wrap = MyOrdinal.valueOf( value1String );
		final MyOrdinal value2Wrap = MyOrdinal.valueOf( value2String );
		final MyChild value2Orphan = MyChild.valueOf( value2String );
		final MyChild value2Child = MyChild.valueOf( value2String )
				.withParent( value1String );
		final MyChild value2Recur = MyChild.valueOf( value1String )
				.withParent( value2Orphan );
		assertThat( "Ordinal should defer to compareTo() of wrapped object",
				value1String.compareTo( value2String ),
				equalTo( value1Wrap.compareTo( value2Wrap ) ) );
		assertThat( "Ordinal should allow comparison with plain objects",
				value1String.compareTo( value2String ),
				equalTo( value1Wrap.compareTo( value2String ) ) );
		assertThat( "orphan(2) = wrap(2)", value2Orphan.compareTo( value2Wrap ),
				equalTo( 0 ) );
		assertThat( "wrap(2) = orphan(2)", value2Wrap.compareTo( value2Orphan ),
				equalTo( 0 ) );
		assertThat( "wrap(1) < orphan(2)", value1Wrap.compareTo( value2Orphan ),
				lessThan( 0 ) );
		assertThat( "orphan(2) > wrap(1)", value2Orphan.compareTo( value1Wrap ),
				greaterThan( 0 ) );
		assertThat( "wrap(2) < child(2 of 1)",
				value2Wrap.compareTo( value2Child ), lessThan( 0 ) );
		assertThat( "child(2 of 1) > wrap(2)",
				value2Child.compareTo( value2Wrap ), greaterThan( 0 ) );
		assertThat( "wrap(1) < child(1 of wrap(2))",
				value1Wrap.compareTo( value2Recur ), lessThan( 0 ) );
		assertThat( "child(1 of wrap(2)) > wrap(1)",
				value2Recur.compareTo( value1Wrap ), greaterThan( 0 ) );
	}

}
