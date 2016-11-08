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
package io.coala.time.persist;

import java.math.BigDecimal;
import java.util.Date;

import javax.measure.Unit;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import io.coala.bind.BindableDao;
import io.coala.bind.LocalBinder;
import io.coala.math.QuantityUtil;
import io.coala.time.Instant;

/**
 * {@link InstantDao}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Embeddable
public class InstantDao implements BindableDao<Instant, InstantDao>
{
	/** the Java attribute name for the POSIX time value */
	public static final String POSIX_ATTR_NAME = "posix";

	/** the Java attribute name for the numeric virtual time value */
	public static final String NUM_ATTR_NAME = "num";

	/** the Java attribute name for the exact virtual time descriptor */
	public static final String STR_ATTR_NAME = "str";

	/** derived, based on replication UTC offset */
	@Temporal( TemporalType.TIMESTAMP )
	@Column
	protected Date posix;

	/** derived, based on replication time unit */
	@Column
	protected BigDecimal num;

	/** exact precision, scale and unit description */
	@Column
	protected String str;

	/**
	 * @param expiration
	 * @param offset
	 * @param unit
	 * @return
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static InstantDao of( final Instant instant, final Date offset,
		final Unit unit )
	{
		final InstantDao result = new InstantDao();
		if( instant == null )
		{
			result.posix = null;
			result.num = null;
			result.str = null;
		} else
		{
			result.posix = instant.toDate( offset );
			result.num = QuantityUtil.toBigDecimal( instant.unwrap() );
			result.str = instant.toString();
		}
		return result;
	}

	@Override
	public Instant restore( final LocalBinder binder )
	{
		return this.str == null ? null
				: Instant.of( QuantityUtil.valueOf( this.str ) );
	}
}