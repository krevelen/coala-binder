/* $Id: 671a7409b1683c5e4597c38bf9f6f4248800b94e $
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
 */
package io.coala.dsol3;

import java.math.BigDecimal;

import javax.measure.Measurable;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;

import io.coala.exception.ExceptionFactory;
import io.coala.json.Wrapper;
import io.coala.log.LogUtil;
import io.coala.time.Instant;
import io.coala.time.TimeSpan;
import io.coala.util.Instantiator;
import nl.tudelft.simulation.dsol.ModelInterface;
import nl.tudelft.simulation.dsol.experiment.Experiment;
import nl.tudelft.simulation.dsol.experiment.Replication;
import nl.tudelft.simulation.dsol.experiment.Treatment;
import nl.tudelft.simulation.dsol.simtime.SimTime;
import nl.tudelft.simulation.dsol.simtime.TimeUnit;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;

/**
 * {@link DsolTime} extends a DSOL {@link SimTime} to become a {@link Wrapper}
 * of {@link BigDecimal} time values for maximal compatibility with COALA
 * {@link Instant} values
 * 
 * @version $Id: 671a7409b1683c5e4597c38bf9f6f4248800b94e $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class DsolTime<Q extends Quantity> extends
	SimTime<Measurable<Q>, BigDecimal, DsolTime<Q>> implements Wrapper<TimeSpan>
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( DsolTime.class );

	/** the ZERO constant */
//	public static final DsolTime ZERO = valueOf( 0d );

	/** the DEFAULT_UNIT - TODO read from config */
//	public static final TimeUnit DEFAULT_UNIT = TimeUnit.DAY;

//	private static final Unit<?> DEFAULT_QUANTITY_UNIT = resolve(
//			DEFAULT_UNIT );

	public static Unit<?> resolve( final TimeUnit unit )
	{
		switch( unit )
		{
		case DAY:
			return NonSI.DAY;
		case HOUR:
			return NonSI.HOUR;
		case MILLISECOND:
			return TimeSpan.MILLIS;
		case MINUTE:
			return NonSI.MINUTE;
		case SECOND:
			return SI.SECOND;
		case WEEK:
			return NonSI.WEEK;
		case YEAR:
			return NonSI.YEAR;
		case UNIT:
			return Unit.ONE;
		default:
			throw ExceptionFactory
					.createUnchecked( "Unsupported unit: " + unit );
		}
	}

	/**
	 * TODO infer unit or apply {@link #resolve( unit )}
	 * 
	 * @param time
	 * @return
	 */
	public static <Q extends Quantity> DsolTime<Q>
		valueOf( final SimTime<?, ?, ?> time, final Unit<Q> unit )
	{
		try
		{
			return valueOf( TimeSpan.of( (Number) time.get(), unit ) );
		} catch( final Throwable t )
		{
			throw ExceptionFactory.createUnchecked( t,
					"Problem converting time: ", time );
		}
	}

	/**
	 * @param absoluteTime the {@link Instant}
	 * @return the new {@link DsolTime}
	 */
	public static DsolTime<?> valueOf( final Instant absoluteTime )
	{
		return valueOf( absoluteTime.unwrap() );
	}

	/**
	 * @param absoluteTime the {@link TimeSpan}
	 * @return the new {@link DsolTime}
	 */
	public static <Q extends Quantity> DsolTime<Q>
		valueOf( final TimeSpan absoluteTime )
	{
		return Util.of( absoluteTime, new DsolTime<Q>() );
	}

	/**
	 * @param absoluteTime
	 * @return the new {@link DsolTime}
	 */
	public static DsolTime<Dimensionless> valueOf( final Number absoluteTime )
	{
		return valueOf( TimeSpan.of( absoluteTime, Unit.ONE ) );
	}

	/**
	 * @return the zero constant as {@link DsolTime}
	 */
//	public static DsolTime zero()
//	{
//		return ZERO;
//	}

	public Instant toInstant()
	{
		return Instant.of( unwrap() );
	}

	/**
	 * {@link DsolTime} zero-arg bean constructor
	 */
	protected DsolTime()
	{
		super( null ); // initialize empty
	}

	private TimeSpan value;

	@Override
	public TimeSpan unwrap()
	{
		return this.value;
	}

	@Override
	public DsolTime<Q> wrap( final TimeSpan absoluteTime )
	{
		this.value = absoluteTime;
		return this;
	}

	/**
	 * @deprecated please use {@link #plus(Number))}
	 */
	@Deprecated
	@Override
	public void add( final BigDecimal relativeTime )
	{
		wrap( unwrap().add( relativeTime ) );
	}

	@SuppressWarnings( "unchecked" )
	public DsolTime<Q> plus( final DsolTime<Q> relativeTime )
	{
		return plus(
				relativeTime.unwrap().to( unwrap().getUnit() ).getValue() );
	}

	/**
	 * @deprecated please use {@link #minus(BigDecimal))}
	 */
	@Deprecated
	@Override
	public void subtract( final BigDecimal relativeTime )
	{
		LOG.warn( "Please use thread-safe minus(..)" );
		wrap( unwrap().subtract( relativeTime ) );
	}

	@SuppressWarnings( "unchecked" )
	public DsolTime<Q> subtract( final DsolTime<Q> relativeTime )
	{
		return minus(
				relativeTime.unwrap().to( unwrap().getUnit() ).getValue() );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public BigDecimal minus( final DsolTime<Q> absoluteTime )
	{
		return unwrap().getValue().subtract(
				absoluteTime.unwrap().to( unwrap().getUnit() ).getValue() );
	}

	@Override
	public DsolTime<Q> copy()
	{
		return valueOf( get() );
	}

	@Override
	public TimeSpan get()
	{
		return unwrap();
	}

	@Override
	public void set( final Measurable<Q> absoluteTime )
	{
		wrap( (TimeSpan) absoluteTime );
	}

	@Override
	public DsolTime<Q> setZero()
	{
		return DsolTime
				.valueOf( TimeSpan.of( BigDecimal.ZERO, unwrap().getUnit() ) );
	}

	/**
	 * @return a {@link DEVSSimulator} scheduler for {@link DsolTime} amounts
	 */
	@SuppressWarnings( "rawtypes" )
	public static <T extends DEVSSimulatorInterface> T
		createDEVSSimulator( final Class<T> simType )
	{
		return Instantiator.instantiate( simType );
	}

	/**
	 * constructs a stand-alone {@link Replication} along with a
	 * {@link Treatment} and {@link Experiment}.
	 * 
	 * @param id the id of the replication.
	 * @param startTime the start time as a time object.
	 * @param warmupPeriod the warmup period, included in the runlength (!)
	 * @param runLength the total length of the run, including the warm-up
	 *            period.
	 * @param model the model for which this is the replication
	 * @throws NamingException in case a context for the replication cannot be
	 *             created
	 */
	public static <Q extends Quantity>
		Replication<Measurable<Q>, BigDecimal, DsolTime<Q>>
		createReplication( final String id, final DsolTime<Q> startTime,
			final BigDecimal warmupPeriod, final BigDecimal runLength,
			final ModelInterface<Measurable<Q>, BigDecimal, DsolTime<Q>> model )
			throws NamingException
	{
		return new Replication<Measurable<Q>, BigDecimal, DsolTime<Q>>( id,
				startTime, warmupPeriod, runLength, model );
	}

	/**
	 * @param time the DSOL {@link SimTime}, e.g. a {@link DsolTime},
	 *            {@code SimTime}<? extends {@link BigDecimal},?,?>, or
	 *            {@code SimTime}<? extends {@link Number},?,?>
	 * @return the {@link Instant} representation
	 * @throws ClassCastException if concrete parameter type for absolute times
	 *             does not extend {@link BigDecimal} or {@link Number}
	 */
//	public static <T extends SimTime<?, ?, T>> Instant toInstant( final T time )
//	{
//		return time instanceof DsolTime ? ((DsolTime<?>) time).toInstant()
//				: time.get() instanceof BigDecimal
//						? Instant.of( (BigDecimal) time.get() )
//						: Instant.of( (Number) time.get() );
//	}

//	public static class DsolReplication
//		extends Replication<BigDecimal, BigDecimal, DsolTime>
//	{
//		public DsolReplication( final DsolExperiment experiment )
//			throws NamingException
//		{
//			super( experiment );
//		}
//
//		protected DsolReplication( final String id, final DsolTime startTime,
//			final BigDecimal warmupPeriod, final BigDecimal runLength,
//			final ModelInterface<BigDecimal, BigDecimal, DsolTime> model )
//				throws NamingException
//		{
//			super( id, startTime, warmupPeriod, runLength, model );
//		}
//	}

//	public static class DsolTreatment
//		extends Treatment<BigDecimal, BigDecimal, DsolTime>
//	{
//		/**
//		 * constructs a {@link DsolTreatment}
//		 * 
//		 * @param experiment reflects the experiment
//		 * @param id an id to recognize the treatment
//		 * @param startTime the absolute start time of a run (can be zero)
//		 * @param warmupPeriod the relative warm-up time of a run (can be zero),
//		 *            <i>included</i> in the runLength
//		 * @param runLength the run length of a run (relative to the start time)
//		 * @param replicationMode the replication mode of this treatment
//		 */
//		public DsolTreatment( final DsolExperiment experiment, final String id,
//			final DsolTime startTime, final BigDecimal warmupPeriod,
//			final BigDecimal runLength, final ReplicationMode replicationMode )
//		{
//			super( experiment, id, startTime, warmupPeriod, runLength,
//					replicationMode );
//		}
//
//		/**
//		 * constructs a {@link DsolTreatment}
//		 * 
//		 * @param experiment reflects the experiment
//		 * @param id an id to recognize the treatment
//		 * @param startTime the absolute start time of a run (can be zero)
//		 * @param warmupPeriod the relative warm-up time of a run (can be zero),
//		 *            <i>included</i> in the runLength
//		 * @param runLength the run length of a run (relative to the start time)
//		 */
//		public DsolTreatment( final DsolExperiment experiment, final String id,
//			final DsolTime startTime, final BigDecimal warmupPeriod,
//			final BigDecimal runLength )
//		{
//			super( experiment, id, startTime, warmupPeriod, runLength );
//		}
//	}

//	public static class DsolExperiment
//		extends Experiment<BigDecimal, BigDecimal, DsolTime>
//	{
//
//		/**
//		 * constructs a new {@link DsolExperiment}
//		 * 
//		 * @param treatment the treatment for this experiment
//		 * @param simulator the simulator
//		 * @param model the model to experiment with
//		 */
//		public DsolExperiment( final DsolTreatment treatment,
//			final DsolDEVSSimulator simulator, final DsolModel model )
//		{
//			super( treatment, simulator, model );
//		}
//	}

}