package vace117.garage.opener.secure.channel;

import java.io.IOException;

/**
 * Obtaining an instance of <code>Conversation</code> executes conversation establishment handshake 
 * and provides the sendMessage() method to send and receive data from the server.
 *
 *
 * @author Val Blant
 */
public interface Conversation {
	public String sendMessage(String message) throws ConversationExpiredException, SecurityException, IOException;
}
