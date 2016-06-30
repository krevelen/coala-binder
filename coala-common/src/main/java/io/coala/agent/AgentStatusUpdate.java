package io.coala.agent;

/**
 * {@link AgentStatusUpdate}
 */
@Deprecated
public interface AgentStatusUpdate
{

	AgentID getAgentID();

	AgentStatus<?> getStatus();

}