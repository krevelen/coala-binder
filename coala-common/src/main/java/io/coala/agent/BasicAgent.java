package io.coala.agent;

import javax.inject.Inject;

import io.coala.bind.Binder;
import io.coala.lifecycle.AbstractLifeCycle;
import io.coala.lifecycle.ActivationType;
import io.coala.lifecycle.LifeCycleHooks;
import io.coala.lifecycle.LifeCycleManaged;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

@Deprecated
public class BasicAgent extends AbstractLifeCycle<AgentID, BasicAgentStatus>
	implements Agent, LifeCycleHooks
{

	/** the serialVersionUID */
	private static final long serialVersionUID = 1L;

	@Inject
	private Binder binder;

	@LifeCycleManaged
	private BasicAgentStatus status;
	
	private ActivationType activationType;

	private final transient Subject<BasicAgentStatus, BasicAgentStatus> history = ReplaySubject
			.create();

	@Override
	public BasicAgentStatus getStatus()
	{
		return this.status;
	}

	@Override
	public Observable<BasicAgentStatus> getStatusHistory()
	{
		return this.history.asObservable();
	}

	@Override
	public Binder getBinder()
	{
		return this.binder;
	}

	@Override
	public ActivationType getActivationType()
	{
		return this.activationType;
	}

	@Override
	public void initialize() throws Exception
	{
	}

	@Override
	public void activate() throws Exception
	{
	}

	@Override
	public void deactivate() throws Exception
	{	
	}

	@Override
	public void finish() throws Exception
	{
	}

}
