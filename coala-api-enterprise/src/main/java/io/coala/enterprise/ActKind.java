package io.coala.enterprise;

/**
 * {@link ActKind} represents the coordination act types that may
 * occur in some transaction
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public enum ActKind
{
	/**  */
	INITIATING( false, RoleKind.INITIATOR, null, FactKind.INITIATED ),

	/**  */
	INITIATING_REQUEST_CANCELLATION( true, RoleKind.INITIATOR, FactKind.REQUESTED, FactKind._INITIATED_REQUEST_CANCELLATION ),

	/**  */
	INITIATING_PROMISE_CANCELLATION( true, RoleKind.EXECUTOR, FactKind.PROMISED, FactKind._INITIATED_PROMISE_CANCELLATION ),

	/**  */
	INITIATING_STATE_CANCELLATION( true, RoleKind.EXECUTOR, FactKind.STATED, FactKind._INITIATED_STATE_CANCELLATION ),

	/**  */
	INITIATING_ACCEPT_CANCELLATION( true, RoleKind.INITIATOR, FactKind.ACCEPTED, FactKind._INITIATED_ACCEPT_CANCELLATION ),

	/**
	 * The "rq" coordination act (C-Act) that starts the order-phase (O-phase)
	 * of some transaction and may lead to a "rq" coordination fact.
	 */
	REQUESTING( false, RoleKind.INITIATOR, FactKind.INITIATED, FactKind.REQUESTED ),

	/**  */
	CANCELLING_REQUEST( true, RoleKind.INITIATOR, FactKind.REQUESTED, FactKind._CANCELLED_REQUEST ),

	/**  */
	REFUSING_REQUEST_CANCELLATION( true, RoleKind.EXECUTOR, FactKind._CANCELLED_REQUEST, FactKind._REFUSED_REQUEST_CANCELLATION ),

	/**  */
	ALLOWING_REQUEST_CANCELLATION( true, RoleKind.EXECUTOR, FactKind._CANCELLED_REQUEST, FactKind._ALLOWED_REQUEST_CANCELLATION ),

	/**  */
	DECLINING( false, RoleKind.EXECUTOR, FactKind.REQUESTED, FactKind.DECLINED ),

	/**  */
	QUITTING( false, RoleKind.INITIATOR, FactKind.DECLINED, FactKind.QUIT ),

	/** The "pm" coordination act that may lead to a "pm" coordination fact. */
	PROMISING( false, RoleKind.EXECUTOR, FactKind.REQUESTED, FactKind.PROMISED ),

	/**  */
	CANCELLING_PROMISE( true, RoleKind.EXECUTOR, FactKind.PROMISED, FactKind._CANCELLED_PROMISE ),

	/**  */
	REFUSING_PROMISE_CANCELLATION( true, RoleKind.INITIATOR, FactKind._CANCELLED_PROMISE, FactKind._REFUSED_PROMISE_CANCELLATION ),

	/**  */
	ALLOWING_PROMISE_CANCELLATION( true, RoleKind.INITIATOR, FactKind._CANCELLED_PROMISE, FactKind._ALLOWED_PROMISE_CANCELLATION ),

	/**
	 * The "ex" production act that may lead to the production fact of this
	 * transaction, completing the execution phase.
	 */
	EXECUTING( false, RoleKind.EXECUTOR, FactKind.PROMISED, FactKind.EXECUTED ),

	/** The "st" coordination act that may lead to a "st" coordination fact. */
	STATING( false, RoleKind.EXECUTOR, FactKind.EXECUTED, FactKind.STATED ),

	/**  */
	CANCELLING_STATE( true, RoleKind.EXECUTOR, FactKind.STATED, FactKind._CANCELLED_STATE ),

	/**  */
	REFUSING_STATE_CANCELLATION( true, RoleKind.INITIATOR, FactKind._CANCELLED_STATE, FactKind._REFUSED_STATE_CANCELLATION ),

	/**  */
	ALLOWING_STATE_CANCELLATION( true, RoleKind.INITIATOR, FactKind._CANCELLED_STATE, FactKind._ALLOWED_STATE_CANCELLATION ),

	/**  */
	REJECTING( false, RoleKind.INITIATOR, FactKind.STATED, FactKind.REJECTED ),

	/**  */
	STOPPING( false, RoleKind.EXECUTOR, FactKind.REJECTED, FactKind.STOPPED ),

	/** The "ac" coordination act that may lead to an "ac" coordination fact. */
	ACCEPTING( false, RoleKind.INITIATOR, FactKind.STATED, FactKind.ACCEPTED ),

	/**  */
	CANCELLING_ACCEPT( true, RoleKind.INITIATOR, FactKind.ACCEPTED, FactKind._CANCELLED_ACCEPT ),

	/**  */
	REFUSING_ACCEPT_CANCELLATION( true, RoleKind.EXECUTOR, FactKind._CANCELLED_ACCEPT, FactKind._REFUSED_ACCEPT_CANCELLATION ),

	/**  */
	ALLOWING_ACCEPT_CANCELLATION( true, RoleKind.EXECUTOR, FactKind._CANCELLED_ACCEPT, FactKind._ALLOWED_ACCEPT_CANCELLATION );

	/** true if this type of act is part of cancellations */
	private final boolean isCancellationAct;

	/** the generic actor role type that can perform this type of act */
	private final RoleKind performer;

	/** the fact type required for this act type to initiate, or null */
	private final FactKind condition;

	/** the fact possibly resulting from this act type */
	private final FactKind outcome;

	/**
	 * @param condition the fact type required for this act type to initiate
	 * @param outcomes the facts possibly resulting from this act type
	 */
	private ActKind( final boolean isCancellationAct,
		final RoleKind actorRole, final FactKind condition,
		final FactKind outcome )
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
	public RoleKind performer()
	{
		return this.performer;
	}

	/**
	 * @return the generic actor role type awaiting this type of act to result
	 *         in some fact
	 */
	public RoleKind listener()
	{
		return this.performer.equals( RoleKind.EXECUTOR )
				? RoleKind.INITIATOR : RoleKind.EXECUTOR;
	}

	/**
	 * @return the generic fact types that may trigger this type of act, or null
	 *         if undefined (e.g. initiation/termination)
	 */
	public FactKind condition()
	{
		return this.condition;
	}

	/** @return the generic fact type resulting from this type of act */
	public FactKind outcome()
	{
		return this.outcome;
	}

	/**
	 * @param factType the required fact type
	 * @return true if factType is the requirement of this type of act
	 */
	public boolean hasRequirement( final FactKind factType )
	{
		return condition().equals( factType );
	}

	/**
	 * @param factType the resulting fact type to check
	 * @return true if factType is the outcome/result of this type of act
	 */
	public boolean isOutcome( final FactKind factType )
	{
		return outcome().equals( factType );
	}

}
