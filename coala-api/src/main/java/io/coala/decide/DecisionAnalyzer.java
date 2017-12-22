/* $Id: 1ac169bfed866f1a47a313954024859ad5112a32 $
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
package io.coala.decide;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.logging.log4j.Logger;

import io.coala.log.LogUtil;

/**
 * {@link DecisionAnalyzer} is a {@link Comparator} to analyze/order
 * alternatives
 * 
 * @param <V> the type of option being compared
 * @version $Id: 1ac169bfed866f1a47a313954024859ad5112a32 $
 * @author Rick van Krevelen
 */
public interface DecisionAnalyzer<A> extends Comparator<A>
{

	/**
	 * @param options a {@link Set} of options
	 * @return a new {@link NavigableSet} of the options, ordered according to
	 *         this {@link DecisionAnalyzer}
	 */
	<E extends A> NavigableSet<E> apply( Collection<E> options );

	/**
	 * @param options a {@link Map} of options
	 * @return a new {@link NavigableMap} of the options, ordered by this
	 *         {@link DecisionAnalyzer}
	 */
	<K extends A, V> NavigableMap<K, V> apply( Map<K, V> options );

	/**
	 * {@link MultiCriteriaDecisionAnalyzer} is a {@link DecisionAnalyzer} that
	 * orders {@link MultiCriteriaWeightedAlternative}s based on their
	 * individual criteria values and the policy's selection criteria weights
	 * 
	 * @param <A> the type of weight {@link MultiCriteriaWeightedAlternative}
	 * @param <C> the type of selection criterion
	 * @version $Id: 1ac169bfed866f1a47a313954024859ad5112a32 $
	 * @author Rick van Krevelen
	 */
	interface MultiCriteriaDecisionAnalyzer<A extends MultiCriteriaWeightedAlternative<C>, C>
		extends DecisionAnalyzer<A>
	{

		/**
		 * @return the weights of this {@link DecisionAnalyzer} per criterion
		 */
		Map<C, Number> getWeights();

	}

	/**
	 * {@link MultiCriteriaWeightedAlternative} has multiple values, one per
	 * selection criterion, for use in {@link MultiCriteriaDecisionAnalyzer}
	 * selection policies
	 * 
	 * @param <C> the type of weighing criterion
	 * @version $Id: 1ac169bfed866f1a47a313954024859ad5112a32 $
	 * @author Rick van Krevelen
	 */
	public interface MultiCriteriaWeightedAlternative<C>
	{

		/**
		 * @param criterion
		 * @return the value of specified criterion for this
		 *         {@link MultiCriteriaWeightedAlternative}
		 */
		Number evaluate( C criterion );

	}

	/**
	 * {@link WeightedProduct} implements a
	 * {@link MultiCriteriaDecisionAnalyzer} that compares
	 * {@link MultiCriteriaWeightedAlternative} s by the product of each
	 * criterion's weight with the policy's respective
	 * {@link #getNormalizedWeights()} (see
	 * <a href="https://www.wikiwand.com/en/Weighted_product_model">the method
	 * by Bridgman (1922) and Miller & Starr (1969)</a>)
	 * 
	 * @param <C>
	 * @version $Id: 1ac169bfed866f1a47a313954024859ad5112a32 $
	 * @author Rick van Krevelen
	 */
	class WeightedProduct<A extends MultiCriteriaWeightedAlternative<C>, C>
		implements MultiCriteriaDecisionAnalyzer<A, C>
	{

		/** */
		private static final Logger LOG = LogUtil
				.getLogger( WeightedProduct.class );

		/** */
		private final Map<C, Number> weights;

		/** */
		private final Map<C, Double> normalizedWeights = new HashMap<>();

		/** */
		public WeightedProduct()
		{
			this( null );
		}

		/** */
		public WeightedProduct( final Map<C, Number> weights )
		{
			this.weights = weights;
		}

		@Override
		public Map<C, Number> getWeights()
		{
			return this.weights;
		}

		//@Override
		public Map<C, Double> getNormalizedWeights()
		{
			return this.normalizedWeights;
		}

		public void normalize()
		{
			double total = 0.0d;
			for( Number weight : getWeights().values() )
				total += Math.abs( weight.doubleValue() );
			getNormalizedWeights().clear();
			for( Map.Entry<C, Number> entry : getWeights().entrySet() )
				getNormalizedWeights().put( entry.getKey(),
						total == 0.0 ? 1.0d / getWeights().size()
								: entry.getValue().doubleValue() / total );
			LOG.trace( "Normalized the criteria weights: " + getWeights()
					+ " to: " + getNormalizedWeights() );
		}

		@Override
		public String toString()
		{
			return String.format( "%s[ weights: %s, normalized: %s ]",
					getClass().getSimpleName(), getWeights(),
					getNormalizedWeights() );
		}

		@Override
		public int compare( final A o1, final A o2 )
		{
			double weightedProduct = 1.0d;
			for( Map.Entry<C, Double> entry : getNormalizedWeights()
					.entrySet() )
				try
				{
					final C criterion = entry.getKey();
					final double value1 = o1.evaluate( criterion )
							.doubleValue();
					final double value2 = o2.evaluate( criterion )
							.doubleValue();
					final double weight = entry.getValue().doubleValue();
					final double factor = value1 == 0.0d || value2 == 0.0d
							? 1.0d : Math.pow( value1 / value2, weight );
					LOG.trace( String.format(
							"%s factor: ( %.3f / %.3f ) ^ %.3f = %.3f",
							criterion, value1, value2, weight, factor ) );
					weightedProduct *= factor;
				} catch( final Throwable e )
				{
					LOG.error( "Problem comparing {} with {}", o1, o2, e );
				}
			final int result = Double.compare( weightedProduct, 1.0d );
			LOG.trace( "Comparing {} with {}: {} >>> {}", o1, o2,
					weightedProduct, result );
			return result;
		}

		@Override
		public <K extends A, V> NavigableMap<K, V>
			apply( final Map<K, V> alternatives )
		{
			normalize();
			final NavigableMap<K, V> result = new ConcurrentSkipListMap<>(
					this );
			result.putAll( alternatives );
			return result;
		}

		@Override
		public <E extends A> NavigableSet<E>
			apply( final Collection<E> alternatives )
		{
			normalize();
			final NavigableSet<E> result = new ConcurrentSkipListSet<>( this );
			result.addAll( alternatives );
			return result;
		}
	}
}