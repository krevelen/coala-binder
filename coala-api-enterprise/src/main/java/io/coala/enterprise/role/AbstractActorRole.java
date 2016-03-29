/* $Id: 9d35c3a08c36d3cbbec8c503f95caff69836e379 $
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
package io.coala.enterprise.role;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
//import javax.inject.Named;
import javax.inject.Named;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.coala.agent.Agent;
import io.coala.agent.AgentID;
import io.coala.agent.AgentStatusUpdate;
import io.coala.bind.Binder;
import io.coala.capability.AbstractCapability;
import io.coala.capability.CapabilityID;
import io.coala.capability.admin.CreatingCapability;
import io.coala.capability.admin.DestroyingCapability;
import io.coala.capability.configure.ConfiguringCapability;
import io.coala.capability.interact.ReceivingCapability;
import io.coala.capability.interact.SendingCapability;
import io.coala.capability.know.ReasoningCapability;
import io.coala.capability.plan.SchedulingCapability;
import io.coala.capability.replicate.RandomizingCapability;
import io.coala.capability.replicate.ReplicatingCapability;
import io.coala.config.PropertyGetter;
import io.coala.enterprise.fact.CoordinationFact;
import io.coala.enterprise.fact.CoordinationFactType;
import io.coala.enterprise.organization.Organization;
import io.coala.exception.CoalaExceptionFactory;
import io.coala.invoke.ProcedureCall;
import io.coala.invoke.Schedulable;
import io.coala.log.InjectLogger;
import io.coala.log.LogUtil;
import io.coala.message.Message;
import io.coala.model.ModelComponent;
import io.coala.model.ModelComponentIDFactory;
import io.coala.process.Job;
import io.coala.random.ProbabilityDistribution;
import io.coala.time.SimTime;
import io.coala.time.TimeUnit;
import io.coala.time.Trigger;
import io.coala.util.ClassUtil;
import rx.Observable;
import rx.Observer;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

/**
 * {@link AbstractActorRole}
 * 
 * @param <F> the concrete {@link CoordinationFact} type being handled
 * @version $Id: 9d35c3a08c36d3cbbec8c503f95caff69836e379 $
 * @author Rick van Krevelen
 */
public abstract class AbstractActorRole<F extends CoordinationFact>
	extends AbstractCapability<CapabilityID> implements ActorRole<F>
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/** */
	@SuppressWarnings( "rawtypes" )
	@Inject
	@Named( Binder.AGENT_TYPE )
	private Class ownerType;

	/** the type of {@link CoordinationFact} */
	@JsonIgnore
	private Class<F> factType;

	/** */
	@JsonIgnore
	private final Observable<F> facts;

	/**
	 * {@link AbstractActorRole} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@SuppressWarnings( "unchecked" )
	@Inject
	protected AbstractActorRole( final Binder binder )
	{
		super( null, binder );
		setID( new ActorRoleID( binder.getID(), getClass() ) );

		final List<Class<?>> typeArgs = ClassUtil
				.getTypeArguments( AbstractActorRole.class, getClass() );
		this.factType = (Class<F>) typeArgs.get( 0 );
		// LOG.trace("Listening for messages of type: " +
		// this.factType.getName());
		this.facts = getReceiver().getIncoming().ofType( this.factType );
		this.facts.subscribe( new Observer<F>()
		{
			@Override
			public void onNext( final F fact )
			{
				final SimTime now = getTime();
				// LOG.trace("SCHEDULING FACT: " + fact + " AT " + now);
				getScheduler().schedule(
						ProcedureCall.create( AbstractActorRole.this,
								AbstractActorRole.this, FACT_HANDLER, fact ),
						Trigger.createAbsolute( now ) );
				// LOG.trace("SCHEDULED FACT: " + fact + " AT " + now);
			}

			@Override
			public void onCompleted()
			{
				//
			}

			@Override
			public void onError( final Throwable e )
			{
				e.printStackTrace();
			}
		} );
	}

	/**
	 * @param fact the ignored {@link CoordinationFact} to log
	 */
	protected void logIgnore( final F fact, final boolean expired )
	{
		final CoordinationFactType factType = fact.getID().getType();
		final ActorRoleType roleType = expired ? factType.originatorRoleType()
				: factType.responderRoleType();
		final CoordinationFactType proceedType = factType
				.getDefaultResponse( roleType, true ).outcome();
		final CoordinationFactType receedType = factType
				.getDefaultResponse( roleType, false ).outcome();
		LOG.trace( String.format(
				"%s ignoring %s (%s), default response type: "
						+ "%s to proceed or %s otherwise",
				roleType, (expired ? "expiration of " : "") + factType,
				fact.getClass().getSimpleName(), proceedType, receedType ) );
	}

	@Override
	public ActorRoleID getID()
	{
		return (ActorRoleID) super.getID();
	}

	/**
	 * @see ModelComponent#getOwnerID()
	 */
	@Override
	public AgentID getOwnerID()
	{
		return getID().getOwnerID();
	}

	/**
	 * @return the type of this {@link ActorRole}'s owner {@link Organization}
	 */
	@SuppressWarnings( "unchecked" )
	public Class<? extends Organization> getOwnerType()
	{
		return (Class<? extends Organization>) this.ownerType;
	}

	/*
	 * private class FactHandler implements Observer<F> {
	 *//**
		 * @see Observer#onError(Throwable)
		 */
	/*
	 * @Override public void onError(final Throwable t) { t.printStackTrace(); }
	 * 
	 *//**
		 * @see Observer#onNext(Object)
		 */

	/*
	 * @Override public void onNext(final F fact) { // don't handle immediately,
	 * may still be constructing the role! final SimTime now = getTime();
	 * LOG.info("SCHEDULING FACT:" + fact + " AT " + now);
	 * getSimulator().schedule( ProcedureCall.create(AbstractActorRole.this,
	 * AbstractActorRole.this, FACT_HANDLER, fact),
	 * Trigger.createAbsolute(now)); };
	 * 
	 *//**
		 * @see Observer#onCompleted()
		 */
	/*
	 * @Override public void onCompleted() { // empty } }
	 */
	private static final String FACT_HANDLER = "factHandler";

	@Schedulable( FACT_HANDLER )
	public void handleFact( F fact )
	{
		// System.err.println("HANDLING FACT:" + fact);
		try
		{
			switch( fact.getID().getType() )
			{
			case ACCEPTED:
				asExecutor().onAccepted( fact );
				break;
			case QUIT:
				asExecutor().onQuit( fact );
				break;
			case REJECTED:
				asExecutor().onRejected( fact );
				break;
			case REQUESTED:
				asExecutor().onRequested( fact );
				break;
			case STOPPED:
				asExecutor().onStopped( fact );
				break;
			case _ALLOWED_PROMISE_CANCELLATION:
				asExecutor().onAllowedPromiseCancellation( fact );
				break;
			case _ALLOWED_STATE_CANCELLATION:
				asExecutor().onAllowedStateCancellation( fact );
				break;
			case _CANCELLED_ACCEPT:
				asExecutor().onCancelledAccept( fact );
				break;
			case _CANCELLED_REQUEST:
				asExecutor().onCancelledRequest( fact );
				break;
			case _REFUSED_PROMISE_CANCELLATION:
				asExecutor().onRefusedPromiseCancellation( fact );
				break;
			case _REFUSED_STATE_CANCELLATION:
				asExecutor().onRefusedStateCancellation( fact );
				break;

			case DECLINED:
				asInitiator().onDeclined( fact );
				break;
			case PROMISED:
				asInitiator().onPromised( fact );
				break;
			case STATED:
				asInitiator().onStated( fact );
				break;
			case _ALLOWED_ACCEPT_CANCELLATION:
				asInitiator().onAllowedAcceptCancellation( fact );
				break;
			case _ALLOWED_REQUEST_CANCELLATION:
				asInitiator().onAllowedRequestCancellation( fact );
				break;
			case _CANCELLED_PROMISE:
				asInitiator().onCancelledPromise( fact );
				break;
			case _CANCELLED_STATE:
				asInitiator().onCancelledState( fact );
				break;
			case _REFUSED_ACCEPT_CANCELLATION:
				asInitiator().onRefusedAcceptCancellation( fact );
				break;
			case _REFUSED_REQUEST_CANCELLATION:
				asInitiator().onRefusedRequestCancellation( fact );
				break;

			default:
				throw CoalaExceptionFactory.VALUE_NOT_ALLOWED
						.createRuntime( "factType", fact.getID().getType() );
			}
		} catch( final Throwable t )
		{
			onError( t );
		}
	}

	/**
	 * @param value
	 * @return
	 */
	protected AgentID newAgentID( final String value )
	{
		return getBinder().inject( ModelComponentIDFactory.class )
				.createAgentID( value );
	}

	/**
	 * @return
	 */
	protected ProbabilityDistribution.Factory newDist()
	{
		return getBinder().inject( ProbabilityDistribution.Factory.class );
	}

	/**
	 * @param value
	 * @param unit
	 * @return
	 */
	protected SimTime newTime( final Number value, final TimeUnit unit )
	{
		return getBinder().inject( SimTime.Factory.class ).create( value,
				unit );
	}

	/**
	 * @see ActorRole#getTime()
	 */
	@Override
	public SimTime getTime()
	{
		return getScheduler().getTime();
	}

	/**
	 * @see ActorRole#replayFacts()
	 */
	@Override
	public Observable<F> replayFacts()
	{
		return this.facts.asObservable();
	}

	private Logger LOG()
	{

		// @InjectLogger doesn't work on injected (abstract) super types
		if( LOG == null )
		{
			LOG = LogUtil.getLogger( AbstractActorRole.class, this );
			LOG.info( "Logger NOT INJECTED" );
		}
		return LOG;
	}

	/**
	 * @see ActorRole#onStopped(CoordinationFact)
	 */
	protected void onStopped( final F fact )
	{
		LOG().warn( "Ignoring " + fact.getID().getType() + ": " + fact );
	}

	/**
	 * @see ActorRole#onQuit(CoordinationFact)
	 */
	protected void onQuit( final F fact )
	{
		LOG().warn( "Ignoring " + fact.getID().getType() + ": " + fact );
	}

	private AbstractInitiator<F> asInitiator()
	{
		return (AbstractInitiator<F>) this;
	}

	private AbstractExecutor<F> asExecutor()
	{
		return (AbstractExecutor<F>) this;
	}

	private static final String ADD_PROCESS_MANAGER_AGENT = "addProcessManagerAgent";

	@Schedulable( ADD_PROCESS_MANAGER_AGENT )
	protected synchronized Observable<AgentStatusUpdate> bootAgent(
		final AgentID agentID, final Class<? extends Agent> agentType,
		// final BasicAgentStatus blockSimUntilState,
		final Job<?> next ) throws Exception
	{
		if( next == null ) // no need to sleep sim, nothing to schedule next
			return getBooter().createAgent( agentID, agentType );

		final CountDownLatch latch = new CountDownLatch( 1 );
		final Subject<AgentStatusUpdate, AgentStatusUpdate> status = ReplaySubject
				.create();
		status.subscribe( new Observer<AgentStatusUpdate>()
		{
			/** */
			private boolean success = false;

			@Override
			public void onNext( final AgentStatusUpdate update )
			{
				LOG().trace( "Got child agent update: " + update );
				if( update.getStatus().isFailedStatus() )
				{
					LOG().warn( "Child agent failed: " + update.getAgentID() );
					latch.countDown();
				} else if( update.getStatus().isInitializedStatus()// .equals(blockSimUntilState)
				)
				{
					LOG().info( "Child agent " + agentID
							+ " reached unblock status: "
							+ update.getStatus() );
					// first schedule/block, then countdown/yield
					getScheduler().schedule( next,
							Trigger.createAbsolute( getTime() ) );
					success = true;
					latch.countDown(); // yield
				}
			}

			@Override
			public void onCompleted()
			{
				if( success ) return;
				LOG().warn(
						"Child agent died but never reached blockable status"
								+ ", scheduling next job now" );
				getScheduler().schedule( next,
						Trigger.createAbsolute( getTime() ) );
				latch.countDown();
			}

			@Override
			public void onError( final Throwable e )
			{
				e.printStackTrace();
			}
		} );

		getScheduler()
				.schedule(
						ProcedureCall.create( this, this, AWAIT_METHOD_ID,
								latch, agentID ),
				Trigger.createAbsolute( getTime() ) );

		getBooter().createAgent( agentID, agentType ).subscribe( status );

		return status.asObservable();
	}

	private static final String AWAIT_METHOD_ID = "holdSimUntilAgentInitializes";

	@Schedulable( AWAIT_METHOD_ID )
	protected void holdSimUntilLatchCompletes( final CountDownLatch latch,
		final AgentID agentID )
	{
		if( latch.getCount() == 0 ) return;

		// wait for other thread to make scheduling
		// attempt and block until this thread
		// yields
		try
		{
			LOG().trace(
					"Sleeping simulator thread until the other agent/thread "
							+ "attempts to schedule too, then yield" );
			Thread.sleep( 10 );
		} catch( final InterruptedException ignore )
		{
		}
		Thread.yield();
		getScheduler()
				.schedule(
						ProcedureCall.create( this, this, AWAIT_METHOD_ID,
								latch, agentID ),
				Trigger.createAbsolute( getTime() ) );
	}

	/**
	 * @return the (super)type of {@link CoordinationFact}
	 */
	protected Class<F> getFactType()
	{
		return this.factType;
	}

	/**
	 * @see ActorRole#send(CoordinationFact)
	 */
	protected <M extends Message<?>> M send( final M fact ) throws Exception
	{
		return send( 0, fact );
	}

	/**
	 * @see ActorRole#send(CoordinationFact)
	 */
	protected <M extends Message<?>> M send( final Number delay, final M fact )
		throws Exception
	{
		return send( newTime( delay, getTime().getUnit() ), fact );
	}

	/**
	 * @see ActorRole#send(CoordinationFact)
	 */
	protected <M extends Message<?>> M send( final SimTime delay, final M fact )
		throws Exception
	{
		// LOG.trace("Sending fact: " + fact);
		getScheduler().schedule(
				ProcedureCall.create( this, this, SEND_METHOD_ID, fact ),
				Trigger.createAbsolute( getTime().plus( delay ) ) );
		return fact;
	}

	private static final String SEND_METHOD_ID = "actorRoleSend";

	/**
	 * @see ActorRole#send(CoordinationFact)
	 */
	@Schedulable( SEND_METHOD_ID )
	private <M extends Message<?>> M doSend( final M fact ) throws Exception
	{
		getMessenger().send( fact );
		return fact;
	}

	/**
	 * @return the agent's local {@link CreatingCapability}
	 */
	@JsonIgnore
	protected CreatingCapability getBooter()
	{
		return getBinder().inject( CreatingCapability.class );
	}

	/**
	 * @return the agent's local {@link SchedulingCapability}
	 */
	@SuppressWarnings( "unchecked" )
	@JsonIgnore
	protected SchedulingCapability<SimTime> getScheduler()
	{
		return getBinder().inject( SchedulingCapability.class );
	}

	/**
	 * @return the agent's local {@link ReplicatingCapability}
	 */
	@JsonIgnore
	protected ReplicatingCapability getSimulator()
	{
		return getBinder().inject( ReplicatingCapability.class );
	}

	/**
	 * @return the agent's local {@link SendingCapability}
	 */
	@JsonIgnore
	protected SendingCapability getMessenger()
	{
		return getBinder().inject( SendingCapability.class );
	}

	/**
	 * @return the agent's local {@link ReceivingCapability}
	 */
	@JsonIgnore
	protected ReceivingCapability getReceiver()
	{
		return getBinder().inject( ReceivingCapability.class );
	}

	/**
	 * @param key the configuration value to get
	 * @return the {@link PropertyGetter} from agent's local
	 *         {@link ConfigurerService}
	 */
	protected PropertyGetter getProperty( final String key )
	{
		return getBinder().inject( ConfiguringCapability.class )
				.getProperty( key );
	}

	/**
	 * @return the agent's local {@link ReasonerService}
	 */
	@JsonIgnore
	protected ReasoningCapability getReasoner()
	{
		return getBinder().inject( ReasoningCapability.class );
	}

	@JsonIgnore
	protected DestroyingCapability getFinalizer()
	{
		return getBinder().inject( DestroyingCapability.class );
	}

	/**
	 * @return the agent's local {@link RandomizingCapability}
	 */
	@JsonIgnore
	protected RandomizingCapability getRandomizer()
	{
		return getBinder().inject( RandomizingCapability.class );
	}

	// /** @return the agent's local {@link EmbodierService} */
	// @JsonIgnore
	// protected EmbodierService getWorld()
	// {
	// return getBinder().inject(EmbodierService.class);
	// }

}
