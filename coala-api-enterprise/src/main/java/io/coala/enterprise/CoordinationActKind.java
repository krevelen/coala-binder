package io.coala.enterprise;

/**
 * {@link CoordinationActKind} represents the coordination act types that may
 * occur in some transaction
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public enum CoordinationActKind
{
	/**  */
	INITIATING( false, PerformerType.INITIATOR, null, CoordinationFactKind.INITIATED ),

	/**  */
	INITIATING_REQUEST_CANCELLATION( true, PerformerType.INITIATOR, CoordinationFactKind.REQUESTED, CoordinationFactKind._INITIATED_REQUEST_CANCELLATION ),

	/**  */
	INITIATING_PROMISE_CANCELLATION( true, PerformerType.EXECUTOR, CoordinationFactKind.PROMISED, CoordinationFactKind._INITIATED_PROMISE_CANCELLATION ),

	/**  */
	INITIATING_STATE_CANCELLATION( true, PerformerType.EXECUTOR, CoordinationFactKind.STATED, CoordinationFactKind._INITIATED_STATE_CANCELLATION ),

	/**  */
	INITIATING_ACCEPT_CANCELLATION( true, PerformerType.INITIATOR, CoordinationFactKind.ACCEPTED, CoordinationFactKind._INITIATED_ACCEPT_CANCELLATION ),

	/**
	 * The "rq" coordination act (C-Act) that starts the order-phase (O-phase)
	 * of some transaction and may lead to a "rq" coordination fact.
	 */
	REQUESTING( false, PerformerType.INITIATOR, CoordinationFactKind.INITIATED, CoordinationFactKind.REQUESTED ),

	/**  */
	CANCELLING_REQUEST( true, PerformerType.INITIATOR, CoordinationFactKind.REQUESTED, CoordinationFactKind._CANCELLED_REQUEST ),

	/**  */
	REFUSING_REQUEST_CANCELLATION( true, PerformerType.EXECUTOR, CoordinationFactKind._CANCELLED_REQUEST, CoordinationFactKind._REFUSED_REQUEST_CANCELLATION ),

	/**  */
	ALLOWING_REQUEST_CANCELLATION( true, PerformerType.EXECUTOR, CoordinationFactKind._CANCELLED_REQUEST, CoordinationFactKind._ALLOWED_REQUEST_CANCELLATION ),

	/**  */
	DECLINING( false, PerformerType.EXECUTOR, CoordinationFactKind.REQUESTED, CoordinationFactKind.DECLINED ),

	/**  */
	QUITTING( false, PerformerType.INITIATOR, CoordinationFactKind.DECLINED, CoordinationFactKind.QUIT ),

	/** The "pm" coordination act that may lead to a "pm" coordination fact. */
	PROMISING( false, PerformerType.EXECUTOR, CoordinationFactKind.REQUESTED, CoordinationFactKind.PROMISED ),

	/**  */
	CANCELLING_PROMISE( true, PerformerType.EXECUTOR, CoordinationFactKind.PROMISED, CoordinationFactKind._CANCELLED_PROMISE ),

	/**  */
	REFUSING_PROMISE_CANCELLATION( true, PerformerType.INITIATOR, CoordinationFactKind._CANCELLED_PROMISE, CoordinationFactKind._REFUSED_PROMISE_CANCELLATION ),

	/**  */
	ALLOWING_PROMISE_CANCELLATION( true, PerformerType.INITIATOR, CoordinationFactKind._CANCELLED_PROMISE, CoordinationFactKind._ALLOWED_PROMISE_CANCELLATION ),

	/**
	 * The "ex" production act that may lead to the production fact of this
	 * transaction, completing the execution phase.
	 */
	EXECUTING( false, PerformerType.EXECUTOR, CoordinationFactKind.PROMISED, CoordinationFactKind.EXECUTED ),

	/** The "st" coordination act that may lead to a "st" coordination fact. */
	STATING( false, PerformerType.EXECUTOR, CoordinationFactKind.EXECUTED, CoordinationFactKind.STATED ),

	/**  */
	CANCELLING_STATE( true, PerformerType.EXECUTOR, CoordinationFactKind.STATED, CoordinationFactKind._CANCELLED_STATE ),

	/**  */
	REFUSING_STATE_CANCELLATION( true, PerformerType.INITIATOR, CoordinationFactKind._CANCELLED_STATE, CoordinationFactKind._REFUSED_STATE_CANCELLATION ),

	/**  */
	ALLOWING_STATE_CANCELLATION( true, PerformerType.INITIATOR, CoordinationFactKind._CANCELLED_STATE, CoordinationFactKind._ALLOWED_STATE_CANCELLATION ),

	/**  */
	REJECTING( false, PerformerType.INITIATOR, CoordinationFactKind.STATED, CoordinationFactKind.REJECTED ),

	/**  */
	STOPPING( false, PerformerType.EXECUTOR, CoordinationFactKind.REJECTED, CoordinationFactKind.STOPPED ),

	/** The "ac" coordination act that may lead to an "ac" coordination fact. */
	ACCEPTING( false, PerformerType.INITIATOR, CoordinationFactKind.STATED, CoordinationFactKind.ACCEPTED ),

	/**  */
	CANCELLING_ACCEPT( true, PerformerType.INITIATOR, CoordinationFactKind.ACCEPTED, CoordinationFactKind._CANCELLED_ACCEPT ),

	/**  */
	REFUSING_ACCEPT_CANCELLATION( true, PerformerType.EXECUTOR, CoordinationFactKind._CANCELLED_ACCEPT, CoordinationFactKind._REFUSED_ACCEPT_CANCELLATION ),

	/**  */
	ALLOWING_ACCEPT_CANCELLATION( true, PerformerType.EXECUTOR, CoordinationFactKind._CANCELLED_ACCEPT, CoordinationFactKind._ALLOWED_ACCEPT_CANCELLATION );

	/** true if this type of act is part of cancellations */
	private final boolean isCancellationAct;

	/** the generic actor role type that can perform this type of act */
	private final PerformerType performer;

	/** the fact type required for this act type to initiate, or null */
	private final CoordinationFactKind condition;

	/** the fact possibly resulting from this act type */
	private final CoordinationFactKind outcome;

	/**
	 * @param condition the fact type required for this act type to initiate
	 * @param outcomes the facts possibly resulting from this act type
	 */
	private CoordinationActKind( final boolean isCancellationAct,
		final PerformerType actorRole, final CoordinationFactKind condition,
		final CoordinationFactKind outcome )
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
		return this.performer.equals( PerformerType.EXECUTOR )
				? PerformerType.INITIATOR : PerformerType.EXECUTOR;
	}

	/**
	 * @return the generic fact types that may trigger this type of act, or null
	 *         if undefined (e.g. initiation/termination)
	 */
	public CoordinationFactKind condition()
	{
		return this.condition;
	}

	/** @return the generic fact type resulting from this type of act */
	public CoordinationFactKind outcome()
	{
		return this.outcome;
	}

	/**
	 * @param factType the required fact type
	 * @return true if factType is the requirement of this type of act
	 */
	public boolean hasRequirement( final CoordinationFactKind factType )
	{
		return condition().equals( factType );
	}

	/**
	 * @param factType the resulting fact type to check
	 * @return true if factType is the outcome/result of this type of act
	 */
	public boolean isOutcome( final CoordinationFactKind factType )
	{
		return outcome().equals( factType );
	}

}
