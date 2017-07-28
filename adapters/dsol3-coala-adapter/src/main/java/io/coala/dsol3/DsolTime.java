/* $Id: 3a709681cd90898a22ee40915e0915a641b8bbee $
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

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;
import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;

import io.coala.exception.ExceptionFactory;
import io.coala.json.Wrapper;
import io.coala.log.LogUtil;
import io.coala.math.DecimalUtil;
import io.coala.math.QuantityUtil;
import io.coala.time.Instant;
import io.coala.time.TimeUnits;
import io.coala.util.Instantiator;
import nl.tudelft.simulation.dsol.DSOLModel;
import nl.tudelft.simulation.dsol.experiment.Experiment;
import nl.tudelft.simulation.dsol.experiment.Replication;
import nl.tudelft.simulation.dsol.experiment.Treatment;
import nl.tudelft.simulation.dsol.simtime.SimTime;
import nl.tudelft.simulation.dsol.simtime.TimeUnit;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulator;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;
import nl.tudelft.simulation.dsol.simulators.Simulator;
import tec.uom.se.ComparableQuantity;

/**
 * {@link DsolTime} extends a DSOL {@link SimTime} to become a {@link Wrapper}
 * of {@link BigDecimal} time values for maximal compatibility with COALA
 * {@link Instant} values
 * 
 * @version $Id: 3a709681cd90898a22ee40915e0915a641b8bbee $
 * @author Rick van Krevelen
 */
@SuppressWarnings( "serial" )
public class DsolTime<Q extends Quantity<Q>>
	extends SimTime<DsolTime.DsolQuantity<Q>, BigDecimal, DsolTime<Q>>
	implements Wrapper<DsolTime.DsolQuantity<Q>>
{

	static class DsolQuantity<Q extends Quantity<Q>>
		extends Wrapper.Simple<ComparableQuantity<Q>>
		implements Comparable<DsolQuantity<Q>>
	{
		public static <Q extends Quantity<Q>> DsolQuantity<Q>
			of( final ComparableQuantity<Q> value )
		{
			return Util.of( value, new DsolQuantity<Q>() );
		}

		@Override
		public int compareTo( final DsolQuantity<Q> o )
		{
			return unwrap().compareTo( o.unwrap() );
		}
	}

	/** */
	private static final Logger LOG = LogUtil.getLogger( DsolTime.class );

	/**
	 * @param unit
	 * @return
	 */
	public static Unit<?> resolve( final TimeUnit unit )
	{
		switch( unit )
		{
		case DAY:
			return TimeUnits.DAYS;
		case HOUR:
			return TimeUnits.HOURS;
		case MILLISECOND:
			return TimeUnits.MILLIS;
		case MINUTE:
			return TimeUnits.MINUTE;
		case SECOND:
			return TimeUnits.SECOND;
		case WEEK:
			return TimeUnits.WEEK;
		case YEAR:
			return TimeUnits.ANNUM;
		case UNIT:
			return QuantityUtil.PURE;
		default:
			throw ExceptionFactory
					.createUnchecked( "Unsupported unit: " + unit );
		}
	}

	@SuppressWarnings( { /*"unchecked",*/ "rawtypes" } )
	public static DsolTime valueOf( final String value )
	{
		return valueOf( Instant.valueOf( value ) );
	}

	/**
	 * @param time
	 * @return
	 */
//	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q> & Comparable<Q>> DsolTime<Q> valueOf(
		final SimTime<? extends Number, ?, ?> time, final Unit<Q> unit )
	{
		return valueOf( time.get(), unit );
	}

	/**
	 * @param absoluteTime the {@link Instant}
	 * @return the new {@link DsolTime}
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static DsolTime valueOf( final Instant absoluteTime )
	{
		return valueOf( absoluteTime.unwrap() );
	}

	/**
	 * @param absoluteTime
	 * @return the new {@link DsolTime}
	 */
//	@SuppressWarnings( "unchecked" )
	public static DsolTime<Dimensionless> valueOf( final Number absoluteTime )
	{
		return valueOf( QuantityUtil.valueOf( absoluteTime ) );
	}

	/**
	 * @param time
	 * @return
	 */
//	@SuppressWarnings( "unchecked" )
	public static <Q extends Quantity<Q>> DsolTime<Q>
		valueOf( final Number time, final Unit<Q> unit )
	{
		return valueOf( QuantityUtil.valueOf( time, unit ) );
	}

	/**
	 * @param absoluteTime the {@link TimeSpan}
	 * @return the new {@link DsolTime}
	 */
	public static <Q extends Quantity<Q>> DsolTime<Q>
		valueOf( final Quantity<Q> absoluteTime )
	{
		return valueOf(
				DsolQuantity.<Q>of( QuantityUtil.valueOf( absoluteTime ) ) );
	}

	/**
	 * @param absoluteTime the {@link TimeSpan}
	 * @return the new {@link DsolTime}
	 */
	static <Q extends Quantity<Q>> DsolTime<Q>
		valueOf( final DsolQuantity<Q> absoluteTime )
	{
		return Util.of( absoluteTime, new DsolTime<Q>() );
	}

	public Instant toInstant()
	{
		return Instant.of( quantity() );
	}

	/**
	 * {@link DsolTime} zero-arg bean constructor
	 */
	protected DsolTime()
	{
		super( null ); // initialize empty
	}

	private DsolQuantity<Q> value;

	@Override
	public DsolQuantity<Q> unwrap()
	{
		return this.value;
	}

	@Override
	public DsolTime<Q> wrap( final DsolQuantity<Q> absoluteTime )
	{
		this.value = absoluteTime;
		return this;
	}

	public ComparableQuantity<Q> quantity()
	{
		return unwrap().unwrap();
	}

	public BigDecimal decimal()
	{
		return DecimalUtil.valueOf( unwrap().unwrap().getValue() );
	}

	public Unit<Q> unit()
	{
		return unwrap().unwrap().getUnit();
	}

	/**
	 * @deprecated please use {@link #plus(Number))}
	 */
	@Deprecated
	@Override
	public void add( final BigDecimal relativeTime )
	{
		wrap( DsolQuantity.of( quantity()
				.add( QuantityUtil.valueOf( relativeTime, unit() ) ) ) );
	}

//	@SuppressWarnings( "unchecked" )
	public DsolTime<Q> plus( final DsolTime<Q> relativeTime )
	{
		return valueOf( quantity().add( relativeTime.quantity() ) );
	}

	/**
	 * @deprecated please use {@link #minus(BigDecimal))}
	 */
	@Deprecated
	@Override
	public void subtract( final BigDecimal relativeTime )
	{
		LOG.warn( "Please use thread-safe minus(..)" );
		wrap( DsolQuantity.of( quantity()
				.subtract( QuantityUtil.valueOf( relativeTime, unit() ) ) ) );
	}

//	@SuppressWarnings( "unchecked" )
	public DsolTime<Q> subtract( final DsolTime<Q> relativeTime )
	{
		return valueOf( quantity().subtract( relativeTime.quantity() ) );
	}

//	@SuppressWarnings( "unchecked" )
	@Override
	public BigDecimal minus( final DsolTime<Q> absoluteTime )
	{
		return DecimalUtil.valueOf(
				quantity().subtract( absoluteTime.quantity() ).getValue() );
	}

	@Override
//	@SuppressWarnings( "unchecked" )
	public DsolTime<Q> copy()
	{
		return valueOf( get() );
	}

	@Override
	public DsolQuantity<Q> get()
	{
		return unwrap();
	}

	@Override
	public void set( final DsolQuantity<Q> absoluteTime )
	{
		wrap( absoluteTime );
	}

	@Override
//	@SuppressWarnings( "unchecked" )
	public DsolTime<Q> setZero()
	{
		return valueOf( BigDecimal.ZERO, unit() );
	}

	/**
	 * @return a {@link DEVSSimulator} scheduler for {@link DsolTime} amounts
	 */
	@SuppressWarnings( "rawtypes" )
	public static <T extends DEVSSimulatorInterface> T
		createDEVSSimulator( final Class<T> simType )
	{
		final T result = Instantiator.instantiate( simType );
		Runtime.getRuntime().addShutdownHook(
				new Thread( () -> ((Simulator) result).cleanUp() ) );
		return result;
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
	public static <Q extends Quantity<Q>>
		Replication<DsolQuantity<Q>, BigDecimal, DsolTime<Q>>
		createReplication( final String id, final DsolTime<Q> startTime,
			final BigDecimal warmupPeriod, final BigDecimal runLength,
			final DSOLModel<DsolQuantity<Q>, BigDecimal, DsolTime<Q>> model )
			throws NamingException
	{
		return new Replication<DsolQuantity<Q>, BigDecimal, DsolTime<Q>>( id,
				startTime, warmupPeriod, runLength, model );
	}
}