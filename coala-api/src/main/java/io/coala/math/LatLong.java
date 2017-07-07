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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Angle;

import org.apfloat.Apfloat;
import org.apfloat.Apint;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.util.Compare;
import tec.uom.se.ComparableQuantity;
import tec.uom.se.unit.Units;

/**
 * {@link LatLong}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
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
		return new LatLong( lat, lon );
	}

	private static final Apfloat TWO = new Apint( 2 );

	private final Unit<Angle> unit;

	private final List<Quantity<Angle>> coordinates;

	private transient List<Apfloat> radians = null;

	public LatLong( final Quantity<Angle> lat, final Quantity<Angle> lon )
	{
		this( lat, lon, lat.getUnit() );
	}

	public LatLong( final Quantity<Angle> lat, final Quantity<Angle> lon,
		final Unit<Angle> unit )
	{
		this.unit = unit;
		this.coordinates = Arrays.asList( QuantityUtil.valueOf( lat, unit ),
				QuantityUtil.valueOf( lon, unit ) );
	}

	@Override
	public String toString()
	{
		return "("
				+ getCoordinates().stream()
						.map( q -> DecimalUtil.valueOf( q.getValue() )
								.toPlainString() )
						.collect( Collectors.joining( " by " ) )
				+ " " + this.unit + ")";
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
						.map( c -> QuantityUtil.valueOf( c, Units.RADIAN ) )
						.map( Quantity::getValue ).map( DecimalUtil::toApfloat )
						.collect( Collectors.toList() ));
	}

	/**
	 * Calculates the angular distance or central angle between two points on a
	 * sphere, using the half-versed-sine or
	 * <a href="https://www.wikiwand.com/en/Haversine_formula">haversine
	 * formula</a> for great-circle distance
	 * <p>
	 * NOTE does not take into account any (standard) <a
	 * href=https://www.wikiwand.com/en/Geodesy>geodesy</a> or any geoidal
	 * undulation from the ideal or <a
	 * href=https://www.wikiwand.com/en/Reference_ellipsoid>reference
	 * ellipsoid</a>
	 * 
	 * @param that another {@link LatLong}
	 * @param unit the {@link Unit} of {@link Angle} measurement
	 * @return the {@link Amount} of central {@link Angle}
	 */
	public ComparableQuantity<Angle> angularDistance( final LatLong that )
	{
		return QuantityUtil.valueOf( angularDistance( that ), this.unit );
	}

	/**
	 * Calculates the angular distance or central angle between two points on a
	 * sphere, using the half-versed-sine or
	 * <a href="https://www.wikiwand.com/en/Haversine_formula">haversine
	 * formula</a> for great-circle distance
	 * <p>
	 * NOTE does not take into account any (standard) <a
	 * href=https://www.wikiwand.com/en/Geodesy>geodesy</a> or any geoidal
	 * undulation from the ideal or <a
	 * href=https://www.wikiwand.com/en/Reference_ellipsoid>reference
	 * ellipsoid</a>
	 * 
	 * @param that another {@link LatLong}
	 * @param unit the {@link Unit} of {@link Angle} measurement
	 * @return the {@link ComparableQuantity} of central {@link Angle}
	 * @see QuantityUtil#toUnit(Quantity, Unit)
	 */
	@SuppressWarnings( "deprecation" )
	public ComparableQuantity<Angle> angularDistance( final LatLong that,
		final Unit<Angle> unit )
	{
		final Apfloat lat1 = this.getRadians().get( 0 );
		final Apfloat lon1 = this.getRadians().get( 1 );
		final Apfloat lat2 = that.getRadians().get( 0 );
		final Apfloat lon2 = that.getRadians().get( 1 );
		final Apfloat dist = TWO
				.multiply( asin( sqrt( pow( sin(
						abs( lat1.subtract( lat2 ) ).divide( TWO ) ), TWO ).add(
								cos( lat1 ).multiply( cos( lat2 ) ).multiply(
										pow( sin( abs( lon1.subtract( lon2 ) )
												.divide( TWO ) ), TWO ) ) ) ) ) )
				.precision( Compare.min( lat1.precision(), lon1.precision(),
						lat2.precision(), lon2.precision() ) );
		// NOTE avoid using (standard) ComparableQuantity#to(Unit) !!
		return QuantityUtil.toUnit( QuantityUtil
				.valueOf( DecimalUtil.valueOf( dist ), Units.RADIAN ), unit );
	}

	/**
	 * @return
	 * @see QuantityUtil#precision(Quantity)
	 */
	public int precision()
	{
		return getCoordinates().stream().map( QuantityUtil::precision )
				.min( ( v1, v2 ) -> v1.compareTo( v2 ) ).orElse( 0 );
	}
}