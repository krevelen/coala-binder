package io.coala.eve3;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@link EveReceiverAgent}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface EveReceiverAgent extends EveWrapper
{

	/** */
	String RECEIVE_METHOD_NAME = "doReceive";

	/**
	 * @param payload
	 */
	@Access( AccessType.PUBLIC )
	void doReceive( @Name( PAYLOAD_FIELD_NAME ) JsonNode payload );

}
