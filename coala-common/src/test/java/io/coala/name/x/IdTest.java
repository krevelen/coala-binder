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
package io.coala.name.x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.coala.exception.x.Contextualized;
import io.coala.log.LogUtil;
import rx.Observer;

/**
 * {@link IdTest}
 * 
 * @version $Id$
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

	public static class MyChild extends Id.OrdinalChild<String, String>
	{
		public static MyChild valueOf( final String value )
		{
			final MyChild result = new MyChild();
			result.wrap( value );
			return result;
		}

		public MyChild withParent( final String value )
		{
			setParent( value );
			return this;
		}
	}

	public static class MyRecurrentChild
		extends Id.OrdinalChild<String, MyRecurrentChild>
	{

	}

	/** {@link String#toString()} */
	private static final Logger LOG = LogUtil.getLogger( IdTest.class );

	@BeforeClass
	public static void listenExceptions()
	{
		Contextualized.ThrowablePublisher.asObservable()
				.subscribe( new Observer<Throwable>()
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
						LOG.error( "Intercept " + t.getClass().getSimpleName(),
								t );
					}
				} );
	}

	@Test
	public void idTest()
	{
		LOG.trace( "Testing " + Id.class + "<" + Object.class.getSimpleName()
				+ ">" );
		final Object valueObject = "myValue";
		final MyId valueWrap = MyId.valueOf( valueObject );
		final MyId sameWrap = MyId.valueOf( valueObject );
		final MyOrdinal similarWrap = MyOrdinal
				.valueOf( valueObject.toString() );

		assertEquals( "Id#toString() must return #toString() of wrapped Object",
				valueObject.toString(), valueWrap.toString() );
		assertEquals( "Id#toString() must return same for same values",
				similarWrap.toString(), valueWrap.toString() );
		assertNotEquals(
				"Id#hashCode() must return different #hashCode() than wrapped Object",
				valueObject.hashCode(), valueWrap.hashCode() );
		assertNotEquals(
				"Id#hashCode() must return different for different types",
				similarWrap.hashCode(), valueWrap.hashCode() );
		assertEquals( "Id#equals() must return #equals() of wrapped Object",
				valueWrap, sameWrap );
		assertNotEquals( "Id#equals() must return false for different types",
				valueWrap, similarWrap );
	}

	@Test
	public void idWrappedTest()
	{
		LOG.trace( "Testing " + Id.class + "<" + Id.class.getSimpleName() + "<"
				+ Object.class.getSimpleName() + ">>" );
		final Object valueObject = "myValue";
		final MyId valueWrap = MyId.valueOf( valueObject );
		final MyId sameWrap = MyId.valueOf( valueObject );
		final MyWrappedId wrappedWrap = MyWrappedId.valueOf( sameWrap );

		assertEquals( "Id#toString() must return #toString() of wrapped Object",
				valueObject.toString(), wrappedWrap.toString() );
		assertEquals( "Id#toString() must return same for same values",
				sameWrap.toString(), wrappedWrap.toString() );
		assertNotEquals(
				"Id#hashCode() must return different #hashCode() than wrapped Object",
				valueObject.hashCode(), wrappedWrap.hashCode() );
		assertNotEquals(
				"Id#hashCode() must return different for different types",
				valueWrap.hashCode(), wrappedWrap.hashCode() );
// FIXME
//		assertEquals( "Id#equals() must return #equals() of wrapped Object",
//				valueWrap, wrappedWrap );
		assertNotEquals( "Id#equals() must return false for different types",
				valueWrap, wrappedWrap );
	}

	@Test
	public void idOrdinalTest()
	{
		LOG.trace( "Testing " + Id.Ordinal.class );
		final String value1String = "myValue1";
		final String value2String = "myValue2";
		final MyOrdinal value1Wrap = MyOrdinal.valueOf( value1String );
		final MyOrdinal value2Wrap = MyOrdinal.valueOf( value2String );
		assertEquals( "Ordinal should defer to compareTo() of wrapped object",
				value1String.compareTo( value2String ),
				value1Wrap.compareTo( value2Wrap ) );
		assertEquals( "Ordinal should allow comparison with plain objects",
				value1String.compareTo( value2String ),
				value1Wrap.compareTo( value2String ) );
		assertTrue( value1Wrap.compareTo( value2Wrap ) < 0 );
		assertTrue( value1Wrap.compareTo( value2Wrap ) < 0 );
	}

	@Ignore // FIXME
	@Test
	public void idOrdinalChildTest()
	{
		LOG.trace( "Testing " + Id.class );
//		final Object valueObject = "myValue";
//		final MyId valueWrap = MyId.valueOf( valueObject );
	}

	@Ignore // FIXME
	@Test
	public void idOrdinalChildRecurrencyTest()
	{
		LOG.trace( "Testing " + Id.class );
//		final Object valueObject = "myValue";
//		final MyId valueWrap = MyId.valueOf( valueObject );
	}

}
