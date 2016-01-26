package io.coala.eve3;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.fasterxml.jackson.databind.JsonNode;

import io.coala.exception.CoalaException;

/**
 * {@link EveReceiverAgent}
 * 
 * @date $Date: 2014-05-05 09:27:49 +0200 (Mon, 05 May 2014) $
 * @version $Revision: 248 $
 * @author <a href="mailto:suki@almende.org">suki</a>
 * 
 */
public interface EveReceiverAgent extends EveWrapper
{

	/** */
	String RECEIVE_METHOD_NAME = "doReceive";

	/**
	 * @param payload
	 * @throws CoalaException
	 */
	@Access(AccessType.PUBLIC)
	void doReceive(@Name(PAYLOAD_FIELD_NAME) JsonNode payload);

}
