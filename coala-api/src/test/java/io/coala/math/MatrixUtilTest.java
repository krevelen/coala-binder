package io.coala.math;

import static io.coala.math.MatrixUtil.coordinates;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.StreamSupport;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.ujmp.core.Matrix;
import org.ujmp.core.SparseMatrix;
import org.ujmp.core.enums.ValueType;

import io.coala.util.MapBuilder;

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

	public enum EpiPop
	{
		/** susceptibles */
		S,
		/** infectives */
		I,
		/** recovered */
		R;
	}

	public interface EpiConfig extends Config
	{
		int POPULATION_DEFAULT = 1000;

		/** @return initial number of susceptibles */
		@DefaultValue( "" + (POPULATION_DEFAULT - 1) )
		Long susceptibles();

		/** @return initial number of infectives */
		@DefaultValue( "" + 1 )
		Long infectives();

		/** @return initial number of recovered */
		@DefaultValue( "" + 0 )
		Long recovered();

		default Map<EpiPop, Long> population()
		{
			return MapBuilder.ordered( EpiPop.class, Long.class )
					.fill( EpiPop.values(), 0L ).put( EpiPop.S, susceptibles() )
					.put( EpiPop.I, infectives() ).put( EpiPop.R, recovered() )
					.build();
		}

		@DefaultValue( "" + .1 )
		BigDecimal dt();

		/**
		 * @return <em>R</em><sub>0</sub>: basic reproduction ratio (eg.
		 *         {@link #growth} vs. {@link #recovery}, >1 : epidemic)
		 */
		@DefaultValue( "" + 14 )
		BigDecimal reproduction();

		/**
		 * @return &gamma;<sup>-1</sup>: mean period of recovery (in <em>R
		 *         &larr; &gamma; &middot; I</em>)
		 */
		@DefaultValue( "" + 12 )
		BigDecimal recovery();

	}

	@Test
	public void testSIRIntegrationForwardEuler()
	{
		final EpiConfig conf = ConfigFactory.create( EpiConfig.class );

		// basic reproduction ratio (dimensionless) = beta/gamma
		final BigDecimal recovery = conf.recovery(), dt = conf.dt(),
				dt_ratio = DecimalUtil.inverse( dt ), R_0 = conf.reproduction(),
				gamma = DecimalUtil.inverse( recovery ),
				beta = gamma.multiply( R_0 );
		// some matrix indices
		final int i_c = EpiPop.values().length, i_n = i_c + 2,
				i_S = EpiPop.S.ordinal(), i_I = EpiPop.I.ordinal(),
				i_R = EpiPop.R.ordinal(), i_SI = i_n - 2, i_N = i_n - 1;
		// initialize population structure
		final MatrixBuilder M_rate = MatrixBuilder
				.sparse( ValueType.BIGDECIMAL, i_n, i_c )
				.label( "Rates", "t", "dt" ).label( i_S, "S", "dS" )
				.label( i_I, "I", "dI" ).label( i_R, "R", "dR" )
				.label( i_SI, "SI/N" ).label( i_N, "N" )
				// infection: beta * SI/N, flows from S to I
				.subtract( beta, i_SI, i_S ).add( beta, i_SI, i_I )
				// recovery: gamma * I, flows from I to R
				.subtract( gamma, i_I, i_I ).add( gamma, i_I, i_R ),
				// population structure
				M_pop = MatrixBuilder.sparse( ValueType.BIGDECIMAL, 1, i_c )
						.withContent( conf.population() ),
				// SIR's ordinal differential equation (ODE) terms
				M_terms = MatrixBuilder.sparse( ValueType.BIGDECIMAL, 1, i_n )
						.labelColumns( EpiPop.values() ).label( i_SI, "SI/N" )
						.label( i_N, "N" );

		LOG.trace(
				"R_0: {}, recovery: {}, beta: {}, gamma: {}, dt: {}, M_rate:\n{}",
				R_0, recovery, beta, gamma, dt_ratio, M_rate );

		final int T = 100;
		final MatrixBuilder M_results = MatrixBuilder.sparse( T, 2 * i_c )
				.label( "Forw.Euler" )
				.labelColumns( i -> (i < i_c ? "" : "d")
						+ EpiPop.values()[(int) i % i_c] )
				.labelRows( i -> "t=" + dt.multiply( new BigDecimal( i ) ) )
				.withContent( M_pop );

		BigDecimal N_t, SI_t;
		for( int t = 1; t < T; t++ )
		{
			N_t = M_pop.calcSum();
			SI_t = DecimalUtil.divide(
					M_pop.getNumber( i_I ).multiply( M_pop.getNumber( i_S ) ),
					N_t );

			// update terms
			M_terms.withContent( M_pop ).with( SI_t, i_SI ).with( N_t, i_N );

			// calculate deltas
			final MatrixBuilder M_delta = M_terms.mtimes( M_rate )
					.multiply( dt );

			M_results.withContent( M_delta, t - 1, i_c );
			M_results.withContent( M_pop.add( M_delta ), t, 0 );
		}
		LOG.trace( "Results: \n{}", M_results );
	}
}
