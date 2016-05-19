package io.coala.decide;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import io.coala.decide.DecisionAnalyzer.MultiCriteriaDecisionAnalyzer;
import io.coala.decide.DecisionAnalyzer.MultiCriteriaWeightedAlternative;
import io.coala.exception.ExceptionFactory;

/**
 * {@link Decision}
 * 
 * @param <A> the type of {@link DecisionAnalyzer} to order alternatives
 * @param <O> the type of option alternatives to order
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface Decision<A extends DecisionAnalyzer<O>, O>
{

	A getAnalyzer();

	/**
	 * @return {@code true} iff the analyzer orders the best options first (i.e.
	 *         the least or lowest)
	 */
	boolean isMinimize();

	/**
	 * @return the {@link Collection} of options
	 */
	Collection<O> getOptions();

	/** @return the best alternative given current alternatives and analyzer */
	O decide();

	/**
	 * {@link WeightedDecision} extends {@link Decision} with a
	 * {@link MultiCriteriaDecisionAnalyzer} type of {@link DecisionAnalyzer} to
	 * select among respective {@link MultiCriteriaWeightedAlternative}s
	 * 
	 * @param <A> the type of {@link DecisionAnalyzer} to order alternatives
	 * @param <O> the type of option alternatives to order
	 * @param <C> the type of weighing criterion
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	interface WeightedDecision<A extends MultiCriteriaDecisionAnalyzer<O, C>, O extends MultiCriteriaWeightedAlternative<C>, C>
		extends Decision<A, O>
	{

		// tag

	}

	/**
	 * {@link Simple} implementation of {@link Decision}
	 * 
	 * @param <A> the type of {@link DecisionAnalyzer} to order alternatives
	 * @param <O> the type of option alternatives to order
	 * @version $Id$
	 * @author Rick van Krevelen
	 */
	class Simple<A extends DecisionAnalyzer<O>, O> implements Decision<A, O>
	{

		private final A analyzer;

		private final boolean minimize;

		private final Collection<O> alternatives = Collections
				.synchronizedSet( new HashSet<O>() );

		@SafeVarargs
		public Simple( final A analyzer, final boolean minimize,
			final O... alternatives )
		{
			this.analyzer = analyzer;
			this.minimize = minimize;
			if( alternatives != null ) for( O alternative : alternatives )
				this.alternatives.add( alternative );
		}

		@Override
		public A getAnalyzer()
		{
			return this.analyzer;
		}

		@Override
		public boolean isMinimize()
		{
			return this.minimize;
		}

		@Override
		public Collection<O> getOptions()
		{
			return this.alternatives;
		}

		@Override
		public O decide()
		{
			if( this.alternatives.isEmpty() ) throw ExceptionFactory
					.createUnchecked( "No alternatives given" );
			return this.minimize
					? this.analyzer.apply( this.alternatives ).first()
					: this.analyzer.apply( this.alternatives ).last();
		}

		@Override
		public String toString()
		{
			return String.format(
					"%s[ analyzer: %s, firstBest: %s, options: %s ]",
					getClass().getSimpleName(),
					getAnalyzer().getClass().getSimpleName(), isMinimize(),
					getOptions() );
		}
	}

}
