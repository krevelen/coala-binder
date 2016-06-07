/* $Id: 1de92762f6b7fa1576b680ebac1d734f9d591c3a $
 * $URL: https://dev.almende.com/svn/abms/coala-common/src/test/java/io/coala/WeightedProductComparatorTest.java $
 * 
 * Part of the EU project Adapt4EE, see http://www.adapt4ee.eu/
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
 * Copyright (c) 2010-2014 Almende B.V. 
 */
package io.coala.decide;

import static org.junit.Assert.assertEquals;

import java.util.EnumMap;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.decide.DecisionAnalyzer.MultiCriteriaDecisionAnalyzer;
import io.coala.decide.DecisionAnalyzer.MultiCriteriaWeightedAlternative;
import io.coala.decide.DecisionAnalyzer.WeightedProduct;
import io.coala.exception.ExceptionFactory;
import io.coala.log.LogUtil;

/**
 * {@link DecisionTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class DecisionTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( DecisionTest.class );

	/**
	 * {@link MyCriterion}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	enum MyCriterion // implements WeightedCriterion
	{
		/** */
		MIN_OCCUPANT_DISTANCE,

		/** */
		MIN_OCCUPANT_LOAD,

		;
	}

	/**
	 * {@link MyAlternative}
	 * 
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class MyAlternative implements MultiCriteriaWeightedAlternative<MyCriterion>
	{

		/** */
		private final Number totalOccupantDistance;

		/** */
		private final Number totalOccupantLoad;

		/**
		 * {@link MyAlternative} constructor
		 * 
		 * @param totalOccupantDistance
		 * @param totalOccupantLoad
		 */
		public MyAlternative( final Number totalOccupantDistance,
			final Number totalOccupantLoad )
		{
			this.totalOccupantDistance = totalOccupantDistance;
			this.totalOccupantLoad = totalOccupantLoad;
		}

		@Override
		public Number evaluate( final MyCriterion criterion )
		{
			switch( criterion )
			{
			case MIN_OCCUPANT_DISTANCE:
				return this.totalOccupantDistance;
			case MIN_OCCUPANT_LOAD:
				return this.totalOccupantLoad;
			default:
				throw ExceptionFactory.createUnchecked(
						"Unexcpected criterion: " + criterion );
			}
		}

		@Override
		public boolean equals( final Object rhs )
		{
			if( rhs == null || rhs instanceof MyAlternative == false )
				return false;
			final MyAlternative that = (MyAlternative) rhs;
			return this.totalOccupantDistance == that.totalOccupantDistance
					&& this.totalOccupantLoad == that.totalOccupantLoad;
		}

		@Override
		public String toString()
		{
			return String.format( "(totDist=%1.1f, totLoad=%1.1f)",
					this.totalOccupantDistance.doubleValue(),
					this.totalOccupantLoad.doubleValue() );
		}
	}

	@Test
	public void testMCDA()
	{
		final EnumMap<MyCriterion, Number> weights = new EnumMap<>(
				MyCriterion.class );
		weights.put( MyCriterion.MIN_OCCUPANT_DISTANCE, .4 );
		weights.put( MyCriterion.MIN_OCCUPANT_LOAD, .6 );

		final MultiCriteriaDecisionAnalyzer<MyAlternative, MyCriterion> mcda = new WeightedProduct<>(
				weights );
		final MyAlternative option1 = new MyAlternative( 14, 0.8 );
		final MyAlternative option2 = new MyAlternative( 12.5, 1.1 );
		final Decision<?, ?> decision = new Decision.Simple<>( mcda, true,
				option1, option2 );

		LOG.trace( "Testing decision: {}", decision );
		assertEquals( "intial weights should prefer option1", option1,
				decision.decide() );

		mcda.getWeights().put( MyCriterion.MIN_OCCUPANT_DISTANCE, 2 );
		assertEquals( "reweighted weights should prefer option2", option2,
				decision.decide() );
	}

}
