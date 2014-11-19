package vace117.garage.opener.crypto;

import java.io.IOException;

/**
 * Obtaining an instance of <code>SecureConversation</code> executes conversation establishment handshake 
 * and provides the sendMessage() method to send and receive data from the server.
 *
 * @author Val Blant
 */
public class SecureConversation {
	
	private SecureChannelClient scc;
	private ConversationToken token;
	
	
	SecureConversation(SecureChannelClient scc, ConversationToken token) {
		this.scc = scc;
		this.token = token;
	}

	public String sendMessage(String message) throws ConversationExpiredException, InvalidHmacException, IOException {
		return scc.sendMessage(message, token);
	}

}
