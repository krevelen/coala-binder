package io.coala.eve3;

import java.io.IOException;
import java.net.URI;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.formats.JSONRPCException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@link EveSenderAgent}
 */
public interface EveSenderAgent extends EveWrapper
{

	/** */
	String SEND_METHOD_NAME = "doSend";

	/**
	 * @param payload
	 * @param receiverURI
	 * @throws JSONRPCException
	 * @throws IOException
	 */
	@Access( AccessType.SELF )
	void doSend( JsonNode payload, URI receiverURI )
		throws IOException, JSONRPCException;

}
