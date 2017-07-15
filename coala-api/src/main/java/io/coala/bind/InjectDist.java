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
package io.coala.bind;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.function.Supplier;

import javax.inject.Qualifier;
import javax.measure.Unit;

import org.aeonbits.owner.Config;

import io.coala.exception.Thrower;
import io.coala.random.QuantityDistribution;
import io.coala.random.ProbabilityDistribution;
import io.coala.random.ProbabilityDistribution.Parser;
import tec.uom.se.AbstractUnit;

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

	class Util
	{

		/**
		 * @param encloser
		 * @param field
		 * @param binder
		 */
		public static void injectDistribution( final Object encloser,
			final Field field, final Supplier<Parser> parser )
		{
			if( !ProbabilityDistribution.class
					.isAssignableFrom( field.getType() ) )
				Thrower.throwNew( UnsupportedOperationException::new,
						() -> "@" + InjectDist.class.getSimpleName()
								+ " only injects extensions of "
								+ Config.class );
			field.setAccessible( true );
			final InjectDist annot = field.getAnnotation( InjectDist.class );
			try
			{
				final ProbabilityDistribution<?> parsedDist = parser.get()
						.parse( annot.value(), annot.paramType() );
				if( field.getType().isAssignableFrom( parsedDist.getClass() ) )
				{
					field.set( encloser, parsedDist );
					return;
				} else if( QuantityDistribution.class
						.isAssignableFrom( field.getType() ) )
				{
					final Unit<?> unit = annot.unit().isEmpty()
							? AbstractUnit.ONE
							: AbstractUnit.parse( annot.unit() );
//					final Class<?> fieldDim = TypeArguments
//							.of( AmountDistribution.class, field.getType()
//									.asSubclass( AmountDistribution.class ) )
//							.get( 0 );
//					final Class<?> parsedDim = TypeArguments.of( Unit.class, unit
//							.getStandardUnit().getClass().asSubclass( Unit.class ) )
//							.get( 0 );
					//
//					LogUtil.getLogger( ProbabilityDistribution.class ).trace(
//							"Convert amounts from parsed {} to injected {}",
//							parsedDim, fieldDim );
//					if( fieldDim == parsedDim )

					// FIXME injects raw, check unit compatibility in helper method?
					field.set( encloser, parsedDist.toQuantities( unit ) );

//					else
//						Thrower.throwNew( UnsupportedOperationException.class,
//								"Can't convert amounts from parsed {} to injected {}",
//								parsedDim.getTypeName(), fieldDim.getTypeName() );
				} else
					Thrower.throwNew( UnsupportedOperationException::new,
							() ->
							"Can't convert values from parsed "+parsedDist.getClass().getTypeName()+" to injected "+field.getType().getTypeName() );
//				final Class<?> fieldValueType = TypeArguments
//						.of( ProbabilityDistribution.class, field.getType()
//								.asSubclass( ProbabilityDistribution.class ) )
//						.get( 0 );
//				final Class<?> parsedValueType = TypeArguments
//						.of( ProbabilityDistribution.class, parsedDist.getClass() )
//						.get( 0 );
//				if( fieldValueType == null
//						|| fieldValueType.isAssignableFrom( parsedValueType ) )
//					field.set( encloser, parsedDist );
//				else if( Amount.class.isAssignableFrom( fieldValueType )
//						&& Number.class.isAssignableFrom( parsedValueType ) )
//				{
//					final Unit<?> unit = annot.unit().isEmpty() ? Unit.ONE
//							: Unit.valueOf( annot.unit() );
//					field.set( encloser, parsedDist.toAmounts( unit ) );
//				} else
//					Thrower.throwNew( UnsupportedOperationException.class,
//							"Can't convert values from parsed {} to injected {}",
//							InjectDist.class.getSimpleName(), parsedValueType,
//							fieldValueType );
			} catch( final Exception e )
			{
				Thrower.rethrowUnchecked( e );
			}
		}
	}
}
