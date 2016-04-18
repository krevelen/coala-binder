package io.coala.math;

import static io.coala.math.MeasureUtil.angularDistance;
import static org.junit.Assert.assertTrue;

import javax.measure.quantity.Angle;
import javax.measure.unit.NonSI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jscience.geography.coordinates.LatLong;
import org.jscience.physics.amount.Amount;
import org.junit.Test;

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

	@Test
	public void test()
	{
		final LatLong p0 = LatLong.valueOf( 0.0001, 0.0001,
				NonSI.DEGREE_ANGLE );
		final LatLong p1 = LatLong.valueOf( 1.0001, 1.0001,
				NonSI.DEGREE_ANGLE );
		final LatLong p2 = LatLong.valueOf( 2.0002, 2.0002,
				NonSI.DEGREE_ANGLE );
		final LatLong p3 = LatLong.valueOf( 3.0003, 3.0003,
				NonSI.DEGREE_ANGLE );
		final Amount<Angle> d12 = angularDistance( p1, p2 )
				.to( NonSI.DEGREE_ANGLE );
		final Amount<Angle> d23 = angularDistance( p2, p3 )
				.to( NonSI.DEGREE_ANGLE );
		final LatLong p4 = LatLong.valueOf( -0.9999, -0.9999,
				NonSI.DEGREE_ANGLE );
		final Amount<Angle> d13 = angularDistance( p1, p0 )
				.to( NonSI.DEGREE_ANGLE );
		final Amount<Angle> d43 = angularDistance( p4, p0 )
				.to( NonSI.DEGREE_ANGLE );
		LOG.trace(
				"Testing angular distance between {} V {} = {} and {} V {} = {}",
				p1, p2, d12, p2, p3, d23 );
		assertTrue( d12.approximates( d23 ) );
		LOG.trace(
				"Testing angular distance between {} V {} = {} and {} V {} = {}",
				p0, p1, d13, p4, p0, d43 );
		assertTrue( d13.approximates( d43 ) );
	}

}
