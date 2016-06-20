package io.coala.eve;

import io.coala.exception.CoalaException;
import io.coala.message.Message;

import com.almende.eve.rpc.annotation.Name;

/**
 * {@link EveReceiverAgent}
 */
public interface EveReceiverAgent extends EveWrapper
{

	/** */
	String RECEIVE_METHOD_NAME = "doReceive";

	/**
	 * @param payload
	 * @throws CoalaException
	 */
	//@Access(AccessType.PUBLIC)
	void doReceive( @Name( PAYLOAD_FIELD_NAME ) Message<?> payload );

}
