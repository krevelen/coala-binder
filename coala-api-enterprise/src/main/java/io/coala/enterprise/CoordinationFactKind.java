/* $Id$
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
package io.coala.enterprise;

import java.util.Arrays;

import io.coala.machine.StateNode;

/**
 * {@link CoordinationFactKind}
 */
public enum CoordinationFactKind implements StateNode<CoordinationFactKind>
{

	/** the moment that a transaction's order phase is initiated */
	INITIATED( false, CoordinationRoleKind.INITIATOR, CoordinationActKind.REQUESTING ), // no
	// choice

	/** the moment that a request cancellation phase is initiated */
	_INITIATED_REQUEST_CANCELLATION( true, CoordinationRoleKind.INITIATOR, CoordinationActKind.CANCELLING_REQUEST ), // no choice

	/** the moment that a promise cancellation phase is initiated */
	_INITIATED_PROMISE_CANCELLATION( true, CoordinationRoleKind.EXECUTOR, CoordinationActKind.CANCELLING_PROMISE ), // no choice

	/** the moment that a promise cancellation phase is initiated */
	_INITIATED_STATE_CANCELLATION( true, CoordinationRoleKind.EXECUTOR, CoordinationActKind.CANCELLING_STATE ), // no choice

	/** the moment that an accept cancellation phase is initiated */
	_INITIATED_ACCEPT_CANCELLATION( true, CoordinationRoleKind.INITIATOR, CoordinationActKind.CANCELLING_ACCEPT ), // no choice

	/** */
	QUIT( false, null ), // both roles terminate this exchange

	/** */
	_CANCELLED_REQUEST( true, CoordinationRoleKind.EXECUTOR, CoordinationActKind.ALLOWING_REQUEST_CANCELLATION, // permission
			CoordinationActKind.REFUSING_REQUEST_CANCELLATION ), // forbidden, e.g. timed-out

	/** terminal state for request cancellation */
	_ALLOWED_REQUEST_CANCELLATION( true, CoordinationRoleKind.INITIATOR, CoordinationActKind.QUITTING ), // no choice

	/**
	 * The (intersubjective) "rq" coordination fact (C-fact) that a request has
	 * been made in some transaction by its initiator to its executor. This fact
	 * is an agendum for the executor to either promise or decline to produce
	 * its P-fact.
	 */
	REQUESTED( false, CoordinationRoleKind.EXECUTOR, CoordinationActKind.PROMISING, // permission
			CoordinationActKind.DECLINING ), // forbidden, e.g. timed-out

	/** terminal state for request cancellation */
	_REFUSED_REQUEST_CANCELLATION( true, null ), // both roles terminate this
	// exchange

	/** */
	DECLINED( false, CoordinationRoleKind.INITIATOR, CoordinationActKind.REQUESTING, // permission
			CoordinationActKind.QUITTING ), // forbidden, e.g. timed-out

	/** */
	_CANCELLED_PROMISE( true, CoordinationRoleKind.INITIATOR, CoordinationActKind.ALLOWING_PROMISE_CANCELLATION, // permission
			CoordinationActKind.REFUSING_PROMISE_CANCELLATION ), // forbidden, e.g. timed-out

	/** terminal state for promise cancellation */
	_ALLOWED_PROMISE_CANCELLATION( true, CoordinationRoleKind.EXECUTOR, CoordinationActKind.DECLINING ), // no choice

	/**
	 * The (intersubjective) "pm" coordination fact (C-fact) that a promise was
	 * made in some transaction by its executor to its initiator. This fact is
	 * an agendum for the executor to produce the P-Fact of the transaction and
	 * state the result.
	 */
	PROMISED( false, CoordinationRoleKind.EXECUTOR, CoordinationActKind.EXECUTING ), // no
	// choice

	/**
	 * terminal state for promise cancellation. promised is still the case,
	 * therefore refusal ranks higher
	 */
	_REFUSED_PROMISE_CANCELLATION( true, null ), // both roles terminate this
	// exchange

	/**
	 * The (subjective) production fact (P-fact) that the executor produced the
	 * P-Fact of some transaction. Production is handled subjectively, i.e. its
	 * processes and result are principally not knowable to the initiator.
	 */
	EXECUTED( false, CoordinationRoleKind.EXECUTOR, CoordinationActKind.STATING ), // no choice

	/**
	 * The executor cancelled a state (e.g. to avoid rejection), causing the
	 * initiator to stop rejecting/accepting and allow or refuse this
	 * cancellation (Dietz, 2006:97)
	 */
	_CANCELLED_STATE( true, CoordinationRoleKind.INITIATOR, CoordinationActKind.ALLOWING_STATE_CANCELLATION, // permission
			CoordinationActKind.REFUSING_STATE_CANCELLATION ), // forbidden, e.g. timed-out

	/**
	 * terminal state for state cancellation. The initiator allowed the
	 * executor's state cancellation (e.g. due to execution flaws) causing the
	 * executor to re-execute (Dietz, 2006:97)
	 */
	_ALLOWED_STATE_CANCELLATION( true, null ), // both roles terminate this
	// exchange

	/**
	 * The (intersubjective) "st" coordination fact (C-fact) that the P-fact of
	 * some transaction was stated by its executor to its initiator. This fact
	 * is an agendum for the initiator to either accept or refuse the stated
	 * result (P-fact) of this transaction process.
	 */
	STATED( false, CoordinationRoleKind.INITIATOR, CoordinationActKind.ACCEPTING, // permission
			CoordinationActKind.REJECTING ), // forbidden, e.g. timed-out

	/**
	 * Terminal state for state cancellation. When a state cancellation was
	 * refused by the initiator (e.g. the execution flaws accepted), the
	 * transaction remains 'stated' (Dietz, 2006:97)
	 */
	_REFUSED_STATE_CANCELLATION( true, null ), // both roles terminate this
	// exchange

	/**
	 * The (intersubjective) "rj" coordination fact (C-fact) that the P-fact
	 * stated by the executor of some transaction was rejected by its initiator.
	 * This fact is an agendum for the executor to either state a new result
	 * (P-fact) or stop this transaction process.
	 */
	REJECTED( false, CoordinationRoleKind.EXECUTOR, CoordinationActKind.STATING, // permission
			CoordinationActKind.STOPPING ), // forbidden, e.g. timed-out

	/**
	 * agendum for executor to allow or refuse the accept cancelation by the
	 * initiator
	 */
	_CANCELLED_ACCEPT( true, CoordinationRoleKind.EXECUTOR, CoordinationActKind.ALLOWING_ACCEPT_CANCELLATION, // permission
			CoordinationActKind.REFUSING_ACCEPT_CANCELLATION ), // forbidden, e.g. timed-out

	/**
	 * terminal state for accept cancellation. The executor allowed the
	 * initiator's accept cancellation (e.g. due to payment problems) causing
	 * the initiator to reject (Dietz, 2006:97)
	 */
	_ALLOWED_ACCEPT_CANCELLATION( true, CoordinationRoleKind.INITIATOR, CoordinationActKind.REJECTING ), // no choice

	/** terminal state for P-fact transaction for both initiator and executor */
	STOPPED( false, null ), // both roles terminate this exchange

	/**
	 * The (intersubjective) "ac" coordination fact (C-fact) that the P-fact of
	 * some transaction as stated by its executor was accepted by its initiator.
	 * This fact is a terminal state of this transaction process.
	 */
	ACCEPTED( false, null ), // both roles terminate this exchange

	/** terminal state for accept cancellation, P-fact remains 'accepted' */
	_REFUSED_ACCEPT_CANCELLATION( true, null ), // both roles terminate this
												// exchange

	;

	/** true if this type of fact is part of cancellations */
	private final boolean isCancellationStep;

	/**
	 * the generic actor role type making the follow-up decision, or null for
	 * both
	 */
	private final CoordinationRoleKind responderRole;

	/**
	 * The generic act types possibly triggered by this fact type decider's
	 * response
	 */
	private final CoordinationActKind[] proceedActs;

	/**
	 * @param isCancellationFact true if this type of fact is part of
	 *            cancellations
	 * @param sourceAct the generic act type responsible for generating this
	 *            fact type
	 * @param deciderRole the generic actor role type making the follow-up
	 *            decision, or null for both
	 * @param proceedActs the acts possibly resulting from this fact type
	 */
	private CoordinationFactKind( final boolean isCancellationFact,
		final CoordinationRoleKind deciderRole,
		final CoordinationActKind... proceedActs )
	{
		this.isCancellationStep = isCancellationFact;
		this.responderRole = deciderRole;
		this.proceedActs = proceedActs;
		if( this.proceedActs.length > 2 ) // undefined
			new IllegalArgumentException( "Proceed act undefined for "
					+ this.name() + ", too many options: "
					+ Arrays.asList( this.proceedActs ) ).printStackTrace();
	}

	/** @return true if this type of fact is part of cancellations */
	public boolean isCancellationStep()
	{
		return this.isCancellationStep;
	}

	/**
	 * @return the generic actor role type making the follow-up decision, or
	 *         null if this is fact type leads to a terminal state in the
	 *         transaction/cancellation process for BOTH actor roles
	 */
	public CoordinationRoleKind responderRoleKind()
	{
		return this.responderRole;
	}

	/**
	 * @return the generic actor role type sending the fact type
	 */
	public CoordinationRoleKind originatorRoleType()
	{
		return CoordinationRoleKind.values()[1 - responderRoleKind().ordinal()];
	}

	/**
	 * @return {@code true} if this fact type comes from the executor,
	 *         {@code false} otherwise
	 */
	public boolean isFromInitiator()
	{
		return CoordinationRoleKind.EXECUTOR.equals( responderRoleKind() );
	}

	/**
	 * @return {@code true} if this fact type comes from the executor,
	 *         {@code false} otherwise
	 */
	public boolean isFromExecutor()
	{
		return CoordinationRoleKind.INITIATOR.equals( responderRoleKind() );
	}

	/**
	 * @param roleType the actor role type for which to determine the proceed
	 *            act type options
	 * @return an array of generic act types possibly triggered by this fact
	 *         type, or null if terminal
	 */
	public CoordinationActKind[]
		responseKind( final CoordinationRoleKind roleType )
	{
		return this.proceedActs;
	}

	/**
	 * @param roleType the role type to check whether it should respond
	 * @return true if actions need/can be taken by specified actor role type
	 */
	public boolean isAgendum( final CoordinationRoleKind roleType )
	{
		return defaultResponseKind( roleType, true ) != null;
	}

	/**
	 * @return true if no more actions need/can be taken by either actor role
	 */
	public boolean isTerminal()
	{
		return this.proceedActs.length == 0;
	}

	/**
	 * @param response the act type to check as legal response for this fact
	 *            type
	 * @return true if specified act type is a legal response to this fact type
	 */
	public boolean isValidResponseKind( final CoordinationActKind response )
	{
		if( !this.responderRoleKind().equals( response.performer() ) )
			return true; // any response is good for the party that has to wait
		for( CoordinationActKind actType : this
				.responseKind( response.performer() ) )
			if( actType.equals( response ) ) return true;
		return false;
	}

	/**
	 * @param roleType the role type who performs the response act
	 * @param proceed whether to continue or stop the process
	 * @return the default response act type for specified role and actor type
	 *         given specified permission to continue or not, or null if only
	 *         options are to await reply or to terminate the exchange
	 */
	@SuppressWarnings( "incomplete-switch" )
	public CoordinationActKind defaultResponseKind(
		final CoordinationRoleKind roleType, final boolean proceed )
	{
		if( responderRoleKind() != null
				&& !responderRoleKind().equals( roleType ) )
		{
			if( !proceed && !isCancellationStep() )
				// what the other party can do to roll-back the exchange
				switch( this )
				{
				case REQUESTED:
				return CoordinationActKind.CANCELLING_REQUEST;
				case PROMISED:
				return CoordinationActKind.CANCELLING_REQUEST;
				case EXECUTED:
				return CoordinationActKind.CANCELLING_REQUEST;
				case STATED:
				return CoordinationActKind.CANCELLING_STATE;
				case REJECTED:
				return CoordinationActKind.CANCELLING_REQUEST;
				}
			return null;
		}
		if( this.proceedActs.length == 0 ) // terminal
			return null;
		if( this.proceedActs.length == 1 ) // no choice
			return this.proceedActs[0];

		return proceed ? this.proceedActs[0] : this.proceedActs[1];
	}

	@Override
	public boolean mayPrecede( final CoordinationFactKind factType )
	{
		return isValidResponseKind( factType
				.defaultResponseKind( CoordinationRoleKind.INITIATOR, true ) )
				|| isValidResponseKind( factType.defaultResponseKind(
						CoordinationRoleKind.INITIATOR, false ) )
				|| isValidResponseKind( factType.defaultResponseKind(
						CoordinationRoleKind.EXECUTOR, true ) )
				|| isValidResponseKind( factType.defaultResponseKind(
						CoordinationRoleKind.EXECUTOR, false ) );
	}

}
