package io.coala.math;

import static io.coala.math.MatrixUtil.coordinates;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.ujmp.core.Matrix;
import org.ujmp.core.SparseMatrix;

/**
 * {@link MatrixUtilTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MatrixUtilTest
{

	/** */
	private static final Logger LOG = LogManager
			.getLogger( MatrixUtilTest.class );

	@Test
	public void testParallel()
	{
		LOG.info( "start parallellism test" );
		final TreeMap<String, Object> seq = new TreeMap<>(),
				par = new TreeMap<>(), fix = new TreeMap<>();

		final Matrix m = SparseMatrix.Factory.eye( 3, 2 );
		StreamSupport.stream( m.availableCoordinates().spliterator(), false )
				.forEach( x -> seq.put( Arrays.asList( x[0], x[1] ).toString(),
						m.getAsObject( x ) ) );
		LOG.trace( "seq: {}", seq );
		StreamSupport.stream( m.availableCoordinates().spliterator(), true )
				.forEach( x -> par.put( Arrays.asList( x[0], x[1] ).toString(),
						m.getAsObject( x ) ) );
		LOG.trace( "par: {}", par );
		StreamSupport.stream( coordinates( m, false ).spliterator(), true )
				.forEach( x -> fix.put( Arrays.asList( x[0], x[1] ).toString(),
						m.getAsObject( x ) ) );
		LOG.trace( "fix: {}", fix );
//		MatrixUtil.forEach( MatrixUtil.toStream( m.allCoordinates(), true ),
//				m::getAsBigDecimal, ( bd, x ) -> LOG.trace( "{}->{}", x, bd ) );
//		LOG.trace( "X: {}", m, X );
		assertThat( "size <= 1", fix.size(), greaterThan( 1 ) );
	}
}
