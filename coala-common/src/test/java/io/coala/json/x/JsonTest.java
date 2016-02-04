package io.coala.json.x;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;

import io.coala.json.x.Wrapper.Polymorph;
import io.coala.log.LogUtil;

/**
 * {@link JsonTest} tests the {@link DynaBean} used by {@link Wrapper}
 * 
 * @date $Date$
 * @version $Id$
 * @author <a href="mailto:rick@almende.org">rick</a>
 */
public class JsonTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( JsonTest.class );

	/**
	 * {@link MyWrapper} decorates any {@link Object}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	public static class MyWrapper extends Wrapper.Simple<Object>
	{
		public static MyWrapper valueOf( final Object value )
		{
			final MyWrapper result = new MyWrapper();
			result.wrap( value );
			return result;
		}

		@Override
		public boolean equals( final Object that )
		{
			return Util.equals( this, that );
		}
	}

	public static class MyWrappedWrapper extends Wrapper.Simple<MyWrapper>
	{
		public static MyWrappedWrapper valueOf( final MyWrapper value )
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
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	@Polymorph( stringAs = MyImaginaryNumber.class,
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
	 * @version $Id$
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
	public void jsonConversionTest()
	{
		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ Object.class.getSimpleName() + ">" );
		// TODO test wrapped wrapper
		final Object valueObject = "myValue";
		final MyWrapper valueWrap = MyWrapper.valueOf( valueObject );
		final String valueJSON = JsonUtil.toJSON( valueObject ); // "natural"
		assertEquals(
				MyWrapper.class.getSimpleName() + "<"
						+ valueObject.getClass().getSimpleName() + "<"
						+ valueJSON + ">> must stringify as " + valueJSON + "",
				valueJSON, JsonUtil.stringify( valueWrap ) );
		assertEquals(
				"JSON " + valueJSON + " must parse as "
						+ MyWrapper.class.getSimpleName() + "<"
						+ valueObject.getClass().getSimpleName() + "<"
						+ valueJSON + ">>",
				valueWrap, Wrapper.Util.valueOf( valueJSON, MyWrapper.class ) );

		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ Float.class.getSimpleName() + ">" );
		JsonUtil.getJOM()
				.disable( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS );
		final Float floatValue = 0.1f;
		final MyWrapper floatWrap = MyWrapper.valueOf( floatValue );
		final String floatJSON = JsonUtil.toJSON( floatValue );
		assertEquals(
				MyWrapper.class.getSimpleName() + "<"
						+ floatValue.getClass().getSimpleName() + "<"
						+ floatJSON + ">> must stringify as " + floatJSON,
				floatValue.toString(), JsonUtil.stringify( floatWrap ) );
		assertEquals(
				"JSON " + floatJSON + " must parse as "
						+ MyWrapper.class.getSimpleName() + "<"
						+ floatValue.getClass().getSimpleName() + "<"
						+ floatJSON + ">>",
				floatWrap, Wrapper.Util.valueOf( floatJSON, MyWrapper.class ) );

		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ Double.class.getSimpleName() + ">" );
		JsonUtil.getJOM()
				.disable( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS );
		final Double doubleValue = 1.2;
		final MyWrapper doubleWrap = MyWrapper.valueOf( doubleValue );
		final String doubleJSON = JsonUtil.toJSON( doubleValue );
		assertEquals(
				MyWrapper.class.getSimpleName() + "<"
						+ doubleValue.getClass().getSimpleName() + "<"
						+ doubleJSON + ">> must stringify as " + doubleJSON,
				doubleValue.toString(), JsonUtil.stringify( doubleWrap ) );
		assertEquals(
				"JSON " + doubleJSON + " must parse as "
						+ MyWrapper.class.getSimpleName() + "<"
						+ doubleValue.getClass().getSimpleName() + "<"
						+ doubleJSON + ">>",
				doubleWrap,
				Wrapper.Util.valueOf( doubleJSON, MyWrapper.class ) );

		LOG.trace( "Testing JSON de/serialization of " + Wrapper.class + "<"
				+ BigDecimal.class.getSimpleName() + ">" );
		JsonUtil.getJOM()
				.enable( DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS );
		final BigDecimal decimalValue = BigDecimal.valueOf( 0.1 );
		final MyWrapper decimalWrap = MyWrapper.valueOf( decimalValue );
		final String decimalJSON = JsonUtil.toJSON( decimalValue );
		assertEquals(
				MyWrapper.class.getSimpleName() + "<"
						+ decimalValue.getClass().getSimpleName() + "<"
						+ decimalJSON + ">> must stringify as " + decimalJSON,
				decimalValue.toString(), JsonUtil.stringify( decimalWrap ) );
		assertEquals(
				"JSON " + decimalJSON + " must parse as "
						+ MyWrapper.class.getSimpleName() + "<"
						+ decimalValue.getClass().getSimpleName() + "<"
						+ decimalJSON + ">>",
				decimalWrap,
				Wrapper.Util.valueOf( decimalJSON, MyWrapper.class ) );
	}

	@Ignore // FIXME
	@Test
	public void equalsTest()
	{
	}

	@Ignore // FIXME
	@Test
	public void compareTest()
	{
	}

	@Ignore // FIXME
	@Test
	public void polymorphicTest()
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
