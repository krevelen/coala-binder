/* $Id$
 * 
 * Part of ZonMW project no. 50-53000-98-156
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
 * 
 * Copyright (c) 2016 RIVM National Institute for Health and Environment 
 */
package io.coala.math;

import static org.apfloat.ApfloatMath.abs;
import static org.apfloat.ApfloatMath.asin;
import static org.apfloat.ApfloatMath.cos;
import static org.apfloat.ApfloatMath.pow;
import static org.apfloat.ApfloatMath.sin;
import static org.apfloat.ApfloatMath.sqrt;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Angle;

import org.apfloat.Apfloat;
import org.apfloat.Apint;

import com.fasterxml.jackson.annotation.JsonIgnore;

import tec.uom.se.ComparableQuantity;
import tec.uom.se.unit.Units;

public class LatLong implements Serializable
{

	/** the serialVersionUID */
	private static final long serialVersionUID = 1L;

	public static LatLong of( final Number lat, final Number lon,
		final Unit<Angle> unit )
	{
		return of( QuantityUtil.valueOf( lat, unit ),
				QuantityUtil.valueOf( lon, unit ) );
	}

	public static LatLong of( final Quantity<Angle> lat,
		final Quantity<Angle> lon )
	{
		System.err.println( lat + " by " + lon );
		return new LatLong( lat, lon.to( lat.getUnit() ) );
	}

	private static final Apfloat TWO = new Apint( 2 );

	private final List<Quantity<Angle>> coordinates;

	private transient List<Apfloat> radians = null;

	public LatLong( final Quantity<Angle> lat, final Quantity<Angle> lon )
	{
		this.coordinates = Arrays.asList( lat, lon );
	}

	@Override
	public String toString()
	{
		return this.coordinates.toString();
	}

	/**
	 * @return
	 */
	public List<Quantity<Angle>> getCoordinates()
	{
		return this.coordinates;
	}

	@JsonIgnore
	public List<Apfloat> getRadians()
	{
		return this.radians != null ? this.radians
				: (this.radians = this.coordinates.stream()
						.map( c -> c.getUnit().equals( Units.DEGREE_ANGLE )
								? DecimalUtil.toRadians( c.getValue() )
								: c.to( Units.RADIAN ).getValue() )
						.map( DecimalUtil::toApfloat )
						.collect( Collectors.toList() ));
	}

	/**
	 * Calculates the angular distance or central angle between two points on a
	 * sphere, using the half-versed-sine or
	 * <a href="https://www.wikiwand.com/en/Haversine_formula">haversine
	 * formula</a> for great-circle distance
	 * <p>
	 * TODO maintain {@link BigDecimal} math precision for sqrt, pow, sin, cos,
	 * asin, abs arithmetic functions
	 * 
	 * 
	 * @param that another {@link LatLong}
	 * @param unit the {@link Unit} of {@link Angle} measurement
	 * @return the {@link Amount} of central {@link Angle}
	 */
	public ComparableQuantity<Angle> angularDistance( final LatLong that )
	{
		final Apfloat lat1 = this.getRadians().get( 0 );
		final Apfloat lon1 = this.getRadians().get( 1 );
		final Apfloat lat2 = that.getRadians().get( 0 );
		final Apfloat lon2 = that.getRadians().get( 1 );
		final Apfloat dist = TWO.multiply( asin( sqrt( pow(
				sin( abs( lat1.subtract( lat2 ) ).divide( TWO ) ),
				TWO ).add( cos( lat1 ).multiply( cos( lat2 ) ).multiply(
						pow( sin( abs( lon1.subtract( lon2 ) ).divide( TWO ) ),
								TWO ) ) ) ) ) );
		return QuantityUtil.valueOf( dist.precision( 4 ), Units.RADIAN );
	}
}