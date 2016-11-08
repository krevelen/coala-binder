package io.coala.time;

import javax.measure.Unit;
import javax.measure.format.UnitFormat;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Time;

import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.function.RationalConverter;
import tec.uom.se.unit.TransformedUnit;
import tec.uom.se.unit.Units;

/**
 * {@link TimeUnits}
 * 
 * @version $Id: 057e778f9917d1396ec12c9b4c88843f3bfcf479 $
 * @author Rick van Krevelen
 */
public class TimeUnits extends Units
{

	/** */
	public static final String MILLIS_LABEL = "ms";
	public static final Unit<Time> MILLIS = new TransformedUnit<>( MILLIS_LABEL,
			SECOND, new RationalConverter( 1, 1000 ) );

	/** */
	public static final String NANOS_LABEL = "ns";
	public static final Unit<Time> NANOS = new TransformedUnit<>( NANOS_LABEL,
			SECOND, new RationalConverter( 1, 1000000000 ) );

	/** */
	public static final String HOURS_LABEL = "hrs";
	public static final Unit<Time> HOURS = new TransformedUnit<>( HOURS_LABEL,
			SECOND, new RationalConverter( 60 * 60, 1 ) );

	/**
	 * a {@link Time} expressed as an amount of {@link #DAY}
	 */
	public static final String DAYS_LABEL = "days";
	public static final Unit<Time> DAYS = new TransformedUnit<>( DAYS_LABEL,
			SECOND, new RationalConverter( 60 * 60 * 24, 1 ) );

	/** a {@link Frequency} expressed as an amount per {@link #DAY} */
	public static final String DAILY_LABEL = "daily";
	public static final Unit<Frequency> DAILY = new TransformedUnit<>(
			DAILY_LABEL, HERTZ, new RationalConverter( 1, 60 * 60 * 24 ) );

	public static final String ANNUM_LABEL = "yr";
	public static final Unit<Time> ANNUM = new TransformedUnit<>( ANNUM_LABEL,
			SECOND, new RationalConverter( 60 * 60 * 24 * 365, 1 ) );

	/**
	 * a {@link Frequency} expressed as an amount per {@link #ANNUM} (= 365 d)
	 */
	public static final String ANNUAL_LABEL = "annual";
	public static final Unit<Frequency> ANNUAL = new TransformedUnit<>(
			ANNUAL_LABEL, HERTZ,
			new RationalConverter( 1, 60 * 60 * 24 * 365 ) );

	public static final UnitFormat UNIT_FORMAT = SimpleUnitFormat.getInstance();

	static
	{
		((SimpleUnitFormat) UNIT_FORMAT).alias( HOUR, HOURS.getSymbol() );
		UNIT_FORMAT.label( DAY, DAY.getSymbol() );
		((SimpleUnitFormat) UNIT_FORMAT).alias( DAY, DAYS.getSymbol() );
		UNIT_FORMAT.label( DAILY, DAILY.getSymbol() );
		UNIT_FORMAT.label( ANNUM, ANNUM.getSymbol() );
		UNIT_FORMAT.label( ANNUAL, ANNUAL.getSymbol() );
	}
}
