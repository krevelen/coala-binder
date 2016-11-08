package io.coala.math;

import static org.junit.Assert.assertTrue;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Angle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.util.Compare;
import tec.uom.se.unit.Units;

/**
 * {@link QuantityUtilTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class QuantityUtilTest
{

	/** */
	private static final Logger LOG = LogManager
			.getLogger( QuantityUtilTest.class );

	@Test
	public void test()
	{
		final Number entropy = DecimalUtil.binaryEntropy( .1 );
		LOG.trace( "Binary entropy: {}", entropy );

		final Unit<Angle> unit = Units.DEGREE_ANGLE;
		final LatLong p0 = LatLong.of( 0.0001, 0.0001, unit );
		final LatLong p1 = LatLong.of( 1.0001, 1.0001, unit );
		final LatLong p2 = LatLong.of( 2.0002, 2.0002, unit );
		final LatLong p3 = LatLong.of( 3.0003, 3.0003, unit );
		final LatLong p4 = LatLong.of( -0.9999, -0.9999, unit );
		final Quantity<Angle> d21 = p2.angularDistance( p1, unit );
		final Quantity<Angle> d23 = p2.angularDistance( p3, unit );
		final Quantity<Angle> d01 = p0.angularDistance( p1, unit );
		final Quantity<Angle> d04 = p0.angularDistance( p4, unit );
		final int precision123 = Compare.min( QuantityUtil.precision( d21 ),
				QuantityUtil.precision( d23 ) ) - 1; // FIXME allow loss?
		LOG.trace(
				"Testing angular distance with precision {} between"
						+ "\n\t{} V {} = {} and\n\t{} V {} = {}",
				precision123, p1, p2, d21, p2, p3, d23 );
		assertTrue( QuantityUtil.approximates( d21, d23, precision123 ) );
		final int precision014 = Compare.min( QuantityUtil.precision( d01 ),
				QuantityUtil.precision( d04 ) );
		LOG.trace(
				"Testing angular distance with precision {} between"
						+ "\n\t{} V {} = {} and\n\t{} V {} = {}",
				precision014, p0, p1, d01, p4, p0, d04 );
		assertTrue( QuantityUtil.approximates( d01, d04, precision014 ) );
	}
}
