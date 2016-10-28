package io.coala.time;

import javax.measure.quantity.Duration;
import javax.measure.quantity.Frequency;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

/**
 * {@link Units}
 * 
 * @version $Id: 057e778f9917d1396ec12c9b4c88843f3bfcf479 $
 * @author Rick van Krevelen
 */
public class Units
{

	/** */
	public static final Unit<Duration> MILLIS = SI.MILLI( SI.SECOND );

	/** */
	public static final Unit<Duration> NANOS = SI.NANO( SI.SECOND );

	/** */
	public static final Unit<Duration> HOURS = NonSI.HOUR;

	/** */
	public static final Unit<Duration> DAYS = NonSI.DAY;

	/** */
	public static final String HOURS_ALIAS = "hrs";

	/** */
	public static final String DAYS_ALIAS = "days";

	/** */
	public static final String DAILY_ALIAS = "daily";

//	interface InverseArea extends Quantity
//	{
//	    Unit<InverseArea> UNIT = SI.METER.pow( -2 );
//	}

	public static final Unit<?> PER_KM2 = SI.KILOMETER.pow( -2 );

	/** a {@link Frequency} expressed as an amount per {@link NonSI#DAY} */
	public static final Unit<Frequency> DAILY = DAYS.inverse()
			.asType( Frequency.class );

	public static final String ANNUM_ALIAS = "yr";

	public static final String ANNUALLY_ALIAS = "annual";

	public static final Unit<Duration> ANNUM = NonSI.YEAR_CALENDAR;

	/**
	 * a {@link Frequency} expressed as an amount per
	 * {@link NonSI#YEAR_CALENDAR} (annum = 365 days)
	 */
	public static final Unit<Frequency> ANNUAL = ANNUM.inverse()
			.asType( Frequency.class );

	private static boolean registered = false;

	public static void registerAliases()
	{
		if( registered ) return;
		UnitFormat.getInstance().alias( HOURS, HOURS_ALIAS );
		UnitFormat.getInstance().label( HOURS, HOURS_ALIAS );
		UnitFormat.getInstance().alias( DAYS, DAYS_ALIAS );
		UnitFormat.getInstance().label( DAYS, DAYS_ALIAS );
		UnitFormat.getInstance().alias( DAILY, DAILY_ALIAS );
		UnitFormat.getInstance().label( DAILY, DAILY_ALIAS );
		UnitFormat.getInstance().alias( ANNUM, ANNUM_ALIAS );
		UnitFormat.getInstance().label( ANNUM, ANNUM_ALIAS );
		UnitFormat.getInstance().alias( ANNUAL, ANNUALLY_ALIAS );
		UnitFormat.getInstance().label( ANNUAL, ANNUALLY_ALIAS );
		registered = true;
	}

}
