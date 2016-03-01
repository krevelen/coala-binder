/* $Id: 120c5197e02b88b84a7e00635af67656539fc770 $
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
package io.coala.optimize;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import io.coala.log.LogUtil;

/**
 * {@link WeightedProductComparator}
 * 
 * @param <C>
 * @version $Id: 120c5197e02b88b84a7e00635af67656539fc770 $
 * @author Rick van Krevelen
 */
public class WeightedProductComparator<C extends WeightedCriterion>
	implements WeightedComparator<C>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( WeightedProductComparator.class );

	/** */
	private final Map<C, Number> weights;

	/** */
	private final Map<C, Double> normalizedWeights;

	/** */
	public WeightedProductComparator( final Map<C, Number> weights )
	{
		this.weights = weights;
		double total = 0.0d;
		final Map<C, Double> normalized = new HashMap<C, Double>();
		for( Number weight : weights.values() )
			total += Math.abs( weight.doubleValue() );
		for( Map.Entry<C, Number> entry : weights.entrySet() )
			normalized.put( entry.getKey(), total == 0.0 ? 1.0d / weights.size()
					: entry.getValue().doubleValue() / total );
		LOG.debug( "Normalized the weighted criteria: " + weights + " to: "
				+ normalized );
		this.normalizedWeights = Collections.unmodifiableMap( normalized );
	}

	@Override
	public Map<C, Number> getWeights()
	{
		return this.weights;
	}

	@Override
	public Map<C, Double> getNormalizedWeights()
	{
		return this.normalizedWeights;
	}

	@Override
	public int compare( final WeightedComparable<C> o1,
		final WeightedComparable<C> o2 )
	{
		// apply the weighted product model (Bridgman 1922, Miller & Starr 1969)
		double weightedProduct = 1.0d;
		for( Map.Entry<C, Double> entry : getNormalizedWeights().entrySet() )
			try
			{
				final C criterion = entry.getKey();
				final double value1 = o1.getValue( criterion ).doubleValue();
				final double value2 = o2.getValue( criterion ).doubleValue();
				final double weight = entry.getValue().doubleValue();
				final double factor = value1 == 0.0d || value2 == 0.0d ? 1.0d
						: Math.pow( value1 / value2, weight );
				LOG.debug( String.format(
						"%s factor: ( %.3f / %.3f ) ^ %.3f = %.3f", criterion,
						value1, value2, weight, factor ) );
				weightedProduct *= factor;
			} catch( final Throwable e )
			{
				LOG.error( "Problem comparing " + o1 + " with " + o2, e );
			}
		final int result = Double.compare( weightedProduct, 1.0d );
		LOG.debug( "Comparing " + o1 + " with " + o2 + ": " + weightedProduct
				+ " >>> " + result );
		return result;
	}

	@Override
	public String toString()
	{
		return String.format( "%s[ weights: %s, normalized: %s ]",
				getClass().getSimpleName(), getWeights(),
				getNormalizedWeights() );
	}

}