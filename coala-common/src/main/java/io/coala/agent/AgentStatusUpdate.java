package io.coala.agent;

/**
 * {@link AgentStatusUpdate}
 */
public interface AgentStatusUpdate
{

	AgentID getAgentID();

	AgentStatus<?> getStatus();

}