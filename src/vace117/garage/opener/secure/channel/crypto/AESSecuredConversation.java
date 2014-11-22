package vace117.garage.opener.secure.channel.crypto;

import java.io.IOException;

import vace117.garage.opener.secure.channel.Conversation;
import vace117.garage.opener.secure.channel.ConversationExpiredException;
import vace117.garage.opener.secure.channel.SecurityException;

/**
 * Asking for an instance executes conversation establishment handshake by negotiating a <code>ConversationToken</code> with the server
 * and provides the sendMessage() method to send and receive data from the server.
 *
 * @author Val Blant
 */
public class AESSecuredConversation implements Conversation {
	
	private AESChannelClient scc;
	private ConversationToken token;
	
	
	AESSecuredConversation(AESChannelClient scc, ConversationToken token) {
		this.scc = scc;
		this.token = token;
	}

	public String sendMessage(String message) throws ConversationExpiredException, SecurityException, IOException {
		return scc.sendMessage(message, token);
	}

}
