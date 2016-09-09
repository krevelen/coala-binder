package io.coala.enterprise;

/**
 * The (coordination/production) act types that may occur in some transaction
 * 
 * <p>
 * Copyright &copy; 2011 Delft University of Technology, Jaffalaan 5, 2628 BX
 * Delft, the Netherlands. All rights reserved. <br/>
 * For project information, see <a
 * href="http://www.simulation.tudelft.nl/">www.simulation.tudelft.nl</a> and <a
 * href="http://www.gscg.org/">www.gscg.org</a>. <br/>
 * The source code and binary code of this software is proprietary information
 * of Delft University of Technology.
 * 
 * @author <a href="http://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @author <a href="http://www.tudelft.nl/dwfvankrevelen">Rick van Krevelen</a>
 * @date $Date: 2014-05-23 16:55:45 +0200 (Fri, 23 May 2014) $
 * @version SVN $Revision: 279 $ $Author: krevelen $
 */
public enum CoordinationActType
{
	/**  */
	INITIATING(false, PerformerType.INITIATOR, null,
			CoordinationFactType.INITIATED),

	/**  */
	INITIATING_REQUEST_CANCELLATION(true, PerformerType.INITIATOR,
			CoordinationFactType.REQUESTED,
			CoordinationFactType._INITIATED_REQUEST_CANCELLATION),

	/**  */
	INITIATING_PROMISE_CANCELLATION(true, PerformerType.EXECUTOR,
			CoordinationFactType.PROMISED,
			CoordinationFactType._INITIATED_PROMISE_CANCELLATION),

	/**  */
	INITIATING_STATE_CANCELLATION(true, PerformerType.EXECUTOR,
			CoordinationFactType.STATED,
			CoordinationFactType._INITIATED_STATE_CANCELLATION),

	/**  */
	INITIATING_ACCEPT_CANCELLATION(true, PerformerType.INITIATOR,
			CoordinationFactType.ACCEPTED,
			CoordinationFactType._INITIATED_ACCEPT_CANCELLATION),

	/**
	 * The "rq" coordination act (C-Act) that starts the order-phase (O-phase)
	 * of some transaction and may lead to a "rq" coordination fact.
	 */
	REQUESTING(false, PerformerType.INITIATOR,
			CoordinationFactType.INITIATED, CoordinationFactType.REQUESTED),

	/**  */
	CANCELLING_REQUEST(true, PerformerType.INITIATOR,
			CoordinationFactType.REQUESTED,
			CoordinationFactType._CANCELLED_REQUEST),

	/**  */
	REFUSING_REQUEST_CANCELLATION(true, PerformerType.EXECUTOR,
			CoordinationFactType._CANCELLED_REQUEST,
			CoordinationFactType._REFUSED_REQUEST_CANCELLATION),

	/**  */
	ALLOWING_REQUEST_CANCELLATION(true, PerformerType.EXECUTOR,
			CoordinationFactType._CANCELLED_REQUEST,
			CoordinationFactType._ALLOWED_REQUEST_CANCELLATION),

	/**  */
	DECLINING(false, PerformerType.EXECUTOR,
			CoordinationFactType.REQUESTED, CoordinationFactType.DECLINED),

	/**  */
	QUITTING(false, PerformerType.INITIATOR,
			CoordinationFactType.DECLINED, CoordinationFactType.QUIT),

	/** The "pm" coordination act that may lead to a "pm" coordination fact. */
	PROMISING(false, PerformerType.EXECUTOR,
			CoordinationFactType.REQUESTED, CoordinationFactType.PROMISED),

	/**  */
	CANCELLING_PROMISE(true, PerformerType.EXECUTOR,
			CoordinationFactType.PROMISED,
			CoordinationFactType._CANCELLED_PROMISE),

	/**  */
	REFUSING_PROMISE_CANCELLATION(true, PerformerType.INITIATOR,
			CoordinationFactType._CANCELLED_PROMISE,
			CoordinationFactType._REFUSED_PROMISE_CANCELLATION),

	/**  */
	ALLOWING_PROMISE_CANCELLATION(true, PerformerType.INITIATOR,
			CoordinationFactType._CANCELLED_PROMISE,
			CoordinationFactType._ALLOWED_PROMISE_CANCELLATION),

	/**
	 * The "ex" production act that may lead to the production fact of this
	 * transaction, completing the execution phase.
	 */
	EXECUTING(false, PerformerType.EXECUTOR,
			CoordinationFactType.PROMISED, CoordinationFactType.EXECUTED),

	/** The "st" coordination act that may lead to a "st" coordination fact. */
	STATING(false, PerformerType.EXECUTOR, CoordinationFactType.EXECUTED,
			CoordinationFactType.STATED),

	/**  */
	CANCELLING_STATE(true, PerformerType.EXECUTOR,
			CoordinationFactType.STATED, CoordinationFactType._CANCELLED_STATE),

	/**  */
	REFUSING_STATE_CANCELLATION(true, PerformerType.INITIATOR,
			CoordinationFactType._CANCELLED_STATE,
			CoordinationFactType._REFUSED_STATE_CANCELLATION),

	/**  */
	ALLOWING_STATE_CANCELLATION(true, PerformerType.INITIATOR,
			CoordinationFactType._CANCELLED_STATE,
			CoordinationFactType._ALLOWED_STATE_CANCELLATION),

	/**  */
	REJECTING(false, PerformerType.INITIATOR,
			CoordinationFactType.STATED, CoordinationFactType.REJECTED),

	/**  */
	STOPPING(false, PerformerType.EXECUTOR,
			CoordinationFactType.REJECTED, CoordinationFactType.STOPPED),

	/** The "ac" coordination act that may lead to an "ac" coordination fact. */
	ACCEPTING(false, PerformerType.INITIATOR,
			CoordinationFactType.STATED, CoordinationFactType.ACCEPTED),

	/**  */
	CANCELLING_ACCEPT(true, PerformerType.INITIATOR,
			CoordinationFactType.ACCEPTED,
			CoordinationFactType._CANCELLED_ACCEPT),

	/**  */
	REFUSING_ACCEPT_CANCELLATION(true, PerformerType.EXECUTOR,
			CoordinationFactType._CANCELLED_ACCEPT,
			CoordinationFactType._REFUSED_ACCEPT_CANCELLATION),

	/**  */
	ALLOWING_ACCEPT_CANCELLATION(true, PerformerType.EXECUTOR,
			CoordinationFactType._CANCELLED_ACCEPT,
			CoordinationFactType._ALLOWED_ACCEPT_CANCELLATION);

	/** true if this type of act is part of cancellations */
	private final boolean isCancellationAct;

	/** the generic actor role type that can perform this type of act */
	private final PerformerType performer;

	/** the fact type required for this act type to initiate, or null */
	private final CoordinationFactType condition;

	/** the fact possibly resulting from this act type */
	private final CoordinationFactType outcome;

	/**
	 * @param condition the fact type required for this act type to initiate
	 * @param outcomes the facts possibly resulting from this act type
	 */
	private CoordinationActType(final boolean isCancellationAct,
			final PerformerType actorRole,
			final CoordinationFactType condition,
			final CoordinationFactType outcome)
	{
		this.isCancellationAct = isCancellationAct;
		this.performer = actorRole;
		this.condition = condition;
		this.outcome = outcome;
	}

	/** @return true if this type of act is part of cancellations */
	public boolean isCancellationAct()
	{
		return this.isCancellationAct;
	}

	/** @return the generic actor role type performing this type of act */
	public PerformerType performer()
	{
		return this.performer;
	}

	/**
	 * @return the generic actor role type awaiting this type of act to result
	 *         in some fact
	 */
	public PerformerType listener()
	{
		return this.performer.equals(PerformerType.EXECUTOR) ? PerformerType.INITIATOR
				: PerformerType.EXECUTOR;
	}

	/**
	 * @return the generic fact types that may trigger this type of act, or null
	 *         if undefined (e.g. initiation/termination)
	 */
	public CoordinationFactType condition()
	{
		return this.condition;
	}

	/** @return the generic fact type resulting from this type of act */
	public CoordinationFactType outcome()
	{
		return this.outcome;
	}

	/**
	 * @param factType the required fact type
	 * @return true if factType is the requirement of this type of act
	 */
	public boolean hasRequirement(final CoordinationFactType factType)
	{
		return condition().equals(factType);
	}

	/**
	 * @param factType the resulting fact type to check
	 * @return true if factType is the outcome/result of this type of act
	 */
	public boolean isOutcome(final CoordinationFactType factType)
	{
		return outcome().equals(factType);
	}

}
