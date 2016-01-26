package io.coala.eve3;

import java.io.IOException;
import java.net.URI;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.formats.JSONRPCException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@link EveSenderAgent}
 * 
 * @date $Date: 2014-04-18 16:38:34 +0200 (Fri, 18 Apr 2014) $
 * @version $Revision: 235 $
 * @author <a href="mailto:suki@almende.org">suki</a>
 * 
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
	@Access(AccessType.SELF)
	void doSend(JsonNode payload, URI receiverURI) throws IOException, JSONRPCException;

}
