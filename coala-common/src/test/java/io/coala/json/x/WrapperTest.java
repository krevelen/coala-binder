package io.coala.json.x;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;

import io.coala.json.x.Wrapper.JavaPolymorph;
import io.coala.log.LogUtil;

/**
 * {@link WrapperTest} tests various {@link Wrapper} usagess
 * 
 * @version $Id: 20a533399b4b4e3055bdb44d7a92137e63ea1c6f $
 * @author Rick van Krevelen
 */
public class WrapperTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( WrapperTest.class );

	/**
	 * {@link MySimpleWrapper} decorates any {@link Object}
	 * 
	 * @version $Id: 20a533399b4b4e3055bdb44d7a92137e63ea1c6f $
	 * @author Rick van Krevelen
	 */
	public static class MySimpleWrapper extends Wrapper.Simple<Object>
	{
		public static MySimpleWrapper valueOf( final Object value )
		{
			final MySimpleWrapper result = new MySimpleWrapper();
			result.wrap( value );
			return result;
		}

		@Override
		public boolean equals( final Object that )
		{
			return Util.equals( this, that );
		}
	}

	@SuppressWarnings( "rawtypes" )
	public static class MySimpleOrdinalWrapper
		extends Wrapper.SimpleOrdinal<Comparable>
	{
		public static MySimpleOrdinalWrapper valueOf( final Comparable value )
		{
			final MySimpleOrdinalWrapper result = new MySimpleOrdinalWrapper();
			result.wrap( value );
			return result;
		}
	}

	public static class MyWrappedWrapper extends Wrapper.Simple<MySimpleWrapper>
	{
		public static MyWrappedWrapper valueOf( final MySimpleWrapper value )
		{
			final MyWrappedWrapper result = new MyWrappedWrapper();
			result.wrap( value );
			return result;
		}

		@Override
		public boolean equals( final Object that )
		{
			return Util.equals( this, that );
		}
	}

	/**
	 * {@link MyPolymorphNumberWrapper} decorates any {@link Number}
	 * 
	 * @version $Id: 20a533399b4b4e3055bdb44d7a92137e63ea1c6f $
	 * @author Rick van Krevelen
	 */
	@JavaPolymorph( stringAs = MyImaginaryNumber.class,
		objectAs = MyImaginaryNumber.class )
	public static class MyPolymorphNumberWrapper extends Wrapper.Simple<Number>
	{
		public MyPolymorphNumberWrapper()
		{
		}

		// FIXME allow Jackson's "natural" polymorphism
		public MyPolymorphNumberWrapper( final double value )
		{
			wrap( value );
		}

		public static MyPolymorphNumberWrapper valueOf( final Number value )
		{
			final MyPolymorphNumberWrapper result = new MyPolymorphNumberWrapper();
			result.wrap( value );
			return result;
		}
	}

	/**
	 * {@link MyImaginaryNumber} decorates imaginary number values
	 * 
	 * @version $Id: 20a533399b4b4e3055bdb44d7a92137e63ea1c6f $
	 * @author Rick van Krevelen
	 */
	public static class MyImaginaryNumber extends Number
	{
		/** */
		private static final long serialVersionUID = 1L;

		/** */
		private static final Pattern parts = Pattern
				.compile( "(\\d+[.,]?\\d*)(\\+(\\d+[.,]?\\d*)){1}i" );

		private static final String REAL_PART = "realPart",
				IMAGINARY_PART = "imaginaryPart";

		/** */
		@JsonProperty( REAL_PART )
		public BigDecimal realPart = null;

		/** */
		@JsonProperty( IMAGINARY_PART )
		public BigDecimal imaginaryPart = null;

		/**
		 * {@link MyImaginaryNumber} zero-arg bean constructor, for JSON-object
		 * deserialization
		 */
		public MyImaginaryNumber()
		{
			// empty
		}

		/**
		 * {@link MyImaginaryNumber} constructor, for {@link String}
		 * deserialization
		 * 
		 * @param value the {@link String} representation
		 */
		public MyImaginaryNumber( final String value )
		{
			final Matcher matcher = parts.matcher( value );
			if( !matcher.find() ) throw new IllegalArgumentException(
					value + ", expected: <v1>+<v2>i" );

			this.realPart = new BigDecimal( matcher.group( 1 ) );
			this.imaginaryPart = new BigDecimal( matcher.group( 3 ) );
		}

		@Override
		public String toString()
		{
			return String.format( "%s+%si", this.realPart, this.imaginaryPart );
		}

		@Override
		public boolean equals( final Object o )
		{
			return o != null && o instanceof MyImaginaryNumber
					&& this.realPart.equals( ((MyImaginaryNumber) o).realPart )
					&& this.imaginaryPart
							.equals( ((MyImaginaryNumber) o).imaginaryPart );
		}

		@Override
		public int intValue()
		{
			return this.realPart.intValue();
		}

		@Override
		public long longValue()
		{
			return this.realPart.longValue();
		}

		@Override
		public float floatValue()
		{
			return this.realPart.floatValue();
		}

		@Override
		public double doubleValue()
		{
			return this.realPart.doubleValue();
		}
	}

	@Test
	public void equalsTest()
	{
		LOG.trace( "Testing equals() on " + Wrapper.Util.class  );
		final Object valueObject = "myValue";
		final MySimpleWrapper valueWrap = MySimpleWrapper
				.valueOf( valueObject );
		final MySimpleWrapper sameWrap = MySimpleWrapper.valueOf( valueObject );
		final MySimpleOrdinalWrapper ordinalWrap = MySimpleOrdinalWrapper
				.valueOf( valueObject.toString() );
		final MyWrappedWrapper wrappedWrap = MyWrappedWrapper
				.valueOf( sameWrap );
		assertThat(
				"Wrapper#equals() must defer to #equals() of wrapped Object",
				valueWrap, equalTo( sameWrap ) );
		assertThat( "Wrapper#equals() must return false for different types",
				valueWrap, not( equalTo( (Object) ordinalWrap ) ) );
		assertThat( "Wrapper#equals() must return false for different types",
				valueWrap, not( equalTo( (Object) wrappedWrap ) ) );
		assertThat( "Wrapper#equals() must return false for different types",
				ordinalWrap, not( equalTo( (Object) wrappedWrap ) ) );
	}

	@Test
	public void compareToTest()
	{
		LOG.trace( "Testing compareTo() on " + Wrapper.Util.class );
//		final Object valueObject = "myValue";
//		final MySimpleWrapper valueWrap = MySimpleWrapper
//				.valueOf( valueObject );
//		final MySimpleOrdinalWrapper ordinalWrap = MySimpleOrdinalWrapper
//				.valueOf( valueObject.toString() );
//		final MyWrappedWrapper wrappedWrap = MyWrappedWrapper
//				.valueOf( valueWrap );
//		assertThat(
//				"Wrapper#equals() must defer to #equals() of wrapped Object",
//				valueWrap, equalTo( sameWrap ) );
//		assertThat( "Wrapper#equals() must return false for different types",
//				valueWrap, not( equalTo( (Object) similarWrap ) ) );

//		final String value1String = "myValue1";
//		final String value2String = "myValue2";
//		final MyOrdinal value1Wrap = MyOrdinal.valueOf( value1String );
//		final MyOrdinal value2Wrap = MyOrdinal.valueOf( value2String );
//		assertEquals( "Ordinal should defer to compareTo() of wrapped object",
//				value1String.compareTo( value2String ),
//				value1Wrap.compareTo( value2Wrap ) );
//		assertEquals( "Ordinal should allow comparison with plain objects",
//				value1String.compareTo( value2String ),
//				value1Wrap.compareTo( value2String ) );
//		assertTrue( value1Wrap.compareTo( value2Wrap ) < 0 );
//		assertTrue( value1Wrap.compareTo( value2Wrap ) < 0 );
	}

	@Test
	public void hashCodeTest()
	{
		LOG.trace( "Testing hashCode() on " + Wrapper.Util.class );
//		final Object valueObject = "myValue";
//		final MyId valueWrap = MyId.valueOf( valueObject );
//		final MyOrdinal similarWrap = MyOrdinal
//				.valueOf( valueObject.toString() );
//
//		assertNotEquals(
//				"Id#hashCode() must return different #hashCode() than wrapped Object",
//				valueObject.hashCode(), valueWrap.hashCode() );
//		assertNotEquals(
//				"Id#hashCode() must return different for different types",
//				similarWrap.hashCode(), valueWrap.hashCode() );
	}

	@Test
	public void jsonConversionTest()
	{
		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ Object.class.getSimpleName() + ">" );
		// TODO test wrapped wrapper
		final Object valueObject = "myValue";
		final MySimpleWrapper valueWrap = MySimpleWrapper
				.valueOf( valueObject );
		final String valueJSON = JsonUtil.toJSON( valueObject ); // "natural"
		assertEquals(
				MySimpleWrapper.class.getSimpleName() + "<"
						+ valueObject.getClass().getSimpleName() + "<"
						+ valueJSON + ">> must stringify as " + valueJSON + "",
				valueJSON, JsonUtil.stringify( valueWrap ) );
		assertEquals(
				"JSON " + valueJSON + " must parse as "
						+ MySimpleWrapper.class.getSimpleName() + "<"
						+ valueObject.getClass().getSimpleName() + "<"
						+ valueJSON + ">>",
				valueWrap,
				Wrapper.Util.valueOf( valueJSON, MySimpleWrapper.class ) );

		/* FIXME
		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ Float.class.getSimpleName() + ">" );
		JsonUtil.getJOM()
				.disable( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS );
		final Float floatValue = 0.1f;
		final MySimpleWrapper floatWrap = MySimpleWrapper.valueOf( floatValue );
		final String floatJSON = JsonUtil.toJSON( floatValue );
		assertEquals(
				MySimpleWrapper.class.getSimpleName() + "<"
						+ floatValue.getClass().getSimpleName() + "<"
						+ floatJSON + ">> must stringify as " + floatJSON,
				floatValue.toString(), JsonUtil.stringify( floatWrap ) );
		assertEquals(
				"JSON " + floatJSON + " must parse as "
						+ MySimpleWrapper.class.getSimpleName() + "<"
						+ floatValue.getClass().getSimpleName() + "<"
						+ floatJSON + ">>",
				floatWrap,
				Wrapper.Util.valueOf( floatJSON, MySimpleWrapper.class ) );
		*/

		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ Double.class.getSimpleName() + ">" );
		JsonUtil.getJOM()
				.disable( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS );
		final Double doubleValue = 1.2;
		final MySimpleWrapper doubleWrap = MySimpleWrapper
				.valueOf( doubleValue );
		final String doubleJSON = JsonUtil.toJSON( doubleValue );
		assertEquals(
				MySimpleWrapper.class.getSimpleName() + "<"
						+ doubleValue.getClass().getSimpleName() + "<"
						+ doubleJSON + ">> must stringify as " + doubleJSON,
				doubleValue.toString(), JsonUtil.stringify( doubleWrap ) );
		assertEquals(
				"JSON " + doubleJSON + " must parse as "
						+ MySimpleWrapper.class.getSimpleName() + "<"
						+ doubleValue.getClass().getSimpleName() + "<"
						+ doubleJSON + ">>",
				doubleWrap,
				Wrapper.Util.valueOf( doubleJSON, MySimpleWrapper.class ) );

		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ BigDecimal.class.getSimpleName() + ">" );
		JsonUtil.getJOM()
				.enable( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS );
		final BigDecimal decimalValue = BigDecimal.valueOf( 0.1 );
		final MySimpleWrapper decimalWrap = MySimpleWrapper
				.valueOf( decimalValue );
		final String decimalJSON = JsonUtil.toJSON( decimalValue );
		assertEquals(
				MySimpleWrapper.class.getSimpleName() + "<"
						+ decimalValue.getClass().getSimpleName() + "<"
						+ decimalJSON + ">> must stringify as " + decimalJSON,
				decimalValue.toString(), JsonUtil.stringify( decimalWrap ) );
		assertEquals(
				"JSON " + decimalJSON + " must parse as "
						+ MySimpleWrapper.class.getSimpleName() + "<"
						+ decimalValue.getClass().getSimpleName() + "<"
						+ decimalJSON + ">>",
				decimalWrap,
				Wrapper.Util.valueOf( decimalJSON, MySimpleWrapper.class ) );
	}

	@Ignore // FIXME
	@Test
	public void jsonPolymorphTest()
	{
		final Number v0 = 3;
		final String s0 = v0.toString();

		LOG.trace(
				"Testing wrapped de/serialization of " + v0 + " <--> " + s0 );

		assertThat( "unwrap-serializer",
				JsonUtil.stringify( MyPolymorphNumberWrapper.valueOf( v0 ) ),
				equalTo( s0 ) );

		assertThat( "wrap-deserializer", v0, equalTo( JsonUtil
				.valueOf( s0, MyPolymorphNumberWrapper.class ).unwrap() ) );

		final String s1 = "2.1+1.2i";

		LOG.trace( "Testing wrapped polymorphic de/serialization of: " + s1 );

		// deserialize wrapped value subtype from string
		final MyPolymorphNumberWrapper n1 = JsonUtil.valueOf( "\"" + s1 + "\"",
				MyPolymorphNumberWrapper.class );
		assertThat( "polymorphic-object", n1.unwrap(),
				instanceOf( MyImaginaryNumber.class ) );
		assertThat( "polymorphic-string", n1.toString(), equalTo( s1 ) );

		// deserialize wrapped value subtype from object
		final MyPolymorphNumberWrapper n2 = JsonUtil.valueOf(
				"{\"" + MyImaginaryNumber.REAL_PART + "\":2.1,\""
						+ MyImaginaryNumber.IMAGINARY_PART + "\":1.2}",
				MyPolymorphNumberWrapper.class );

		assertThat( "polymorphic-object", n2.unwrap(),
				instanceOf( MyImaginaryNumber.class ) );
		assertThat( "polymorphic-object", n2.unwrap(), equalTo( n1.unwrap() ) );
	}
}
