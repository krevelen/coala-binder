package io.coala.math;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import javax.measure.quantity.Angle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import tec.uom.se.ComparableQuantity;
import tec.uom.se.unit.Units;

/**
 * {@link MeasureUtilTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MeasureUtilTest
{

	/** */
	private static final Logger LOG = LogManager
			.getLogger( MeasureUtilTest.class );
//
//	public static final Unit<Angle> DEGREE_ANGLE = new TransformedUnit<>(
//			Units.RADIAN, new PiMultiplierConverter()
//					.concatenate( new RationalConverter( 1, 180 ) ) );

	@Test
	public void test()
	{
//		LOG.trace( "test deg->rad: {}",
//				Quantities.getQuantity( BigDecimal.ONE, Units.DEGREE_ANGLE )
//						.to( Units.RADIAN ) );

//		LOG.trace( "test rad->deg: {}",
//				Quantities.getQuantity( BigDecimal.ONE, Units.RADIAN )
//						.to( Units.DEGREE_ANGLE ) );

		final Number entropy = DecimalUtil.binaryEntropy( .1 );
		LOG.trace( "Binary entropy: {}", entropy );

		final LatLong p0 = LatLong.of( 0.0001, 0.0001, Units.DEGREE_ANGLE );
		final LatLong p1 = LatLong.of( 1.0001, 1.0001, Units.DEGREE_ANGLE );
		final LatLong p2 = LatLong.of( 2.0002, 2.0002, Units.DEGREE_ANGLE );
		final LatLong p3 = LatLong.of( 3.0003, 3.0003, Units.DEGREE_ANGLE );
		final ComparableQuantity<Angle> d12 = p1.angularDistance( p2 )
		//.to( Units.DEGREE_ANGLE )
		;
		final ComparableQuantity<Angle> d23 = p2.angularDistance( p3 )
		//.to( Units.DEGREE_ANGLE )
		;
		final LatLong p4 = LatLong.of( -0.9999, -0.9999, Units.DEGREE_ANGLE );
		final ComparableQuantity<Angle> d13 = p1.angularDistance( p0 )
		//.to( Units.DEGREE_ANGLE )
		;
		final ComparableQuantity<Angle> d43 = p4.angularDistance( p0 )
		//.to( Units.DEGREE_ANGLE )
		;
		LOG.trace(
				"Testing angular distance between\n\t{} V {} = {} {} and\n\t{} V {} = {} {}",
				p1, p2, d12, ((BigDecimal) d12.getValue()).precision(), p2, p3,
				d23, ((BigDecimal) d23.getValue()).precision() );
		assertTrue( d12.equals( d23 ) );
		LOG.trace(
				"Testing angular distance between {} V {} = {} and {} V {} = {}",
				p0, p1, d13, p4, p0, d43 );
		assertTrue( d13.equals( d43 ) );
	}

}
