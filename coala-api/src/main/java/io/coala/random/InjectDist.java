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
package io.coala.random;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

import javax.inject.Qualifier;
import javax.measure.unit.Unit;

/**
 * {@link InjectDist}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Qualifier
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface InjectDist
{
	String value();

	Class<?> paramType() default BigDecimal.class;

	/**
	 * FIXME may trigger {@link javax.measure.converter.ConversionException} as
	 * injection does not check unit compatibility in
	 * {@link io.coala.random.ProbabilityDistribution#injectDistribution(Object, Field, Parser)}
	 * 
	 * @return the unit label, to be parsed by
	 *         {@link Unit#valueOf(CharSequence)}, default is {@link Unit#ONE}
	 */
	String unit() default "";
}
