package io.coala.eve;

import io.coala.message.Message;

import java.io.IOException;

import com.almende.eve.rpc.jsonrpc.JSONRPCException;

/**
 * {@link EveSenderAgent}
 */
public interface EveSenderAgent extends EveWrapper
{

	/**
	 * @param payload
	 * @throws JSONRPCException
	 * @throws IOException
	 */
	void doSend(Message<?> payload) throws IOException, JSONRPCException;

}
