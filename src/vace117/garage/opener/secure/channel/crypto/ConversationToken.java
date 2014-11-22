package vace117.garage.opener.secure.channel.crypto;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;

/**
 * Represents a negotiated Conversation Token. It is valid for a limited amount of time, determined by the server.
 * 
 * The Token is calculated from a 16-byte random Nonce sent to us by the server like so: 
 * 		<pre>conversationToken = HMAC(MasterKey, Nonce[16])</pre>
 * 
 * This conversationToken will be sent with every message in the conversation. The same calculation is done on the 
 * server to verify that the message is authentic.
 *
 * @author Val Blant
 */
public class ConversationToken {
	private byte[] conversationToken;

	public ConversationToken(byte[] challengeNonce) {
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
	        mac.init(MasterKey.getMasterKey());
	        
			this.conversationToken = mac.doFinal(challengeNonce);
			
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Unable to compute conversationToken, b/c HMAC algorithm failed.", e);
		}

	}

	public byte[] getBytes() {
		return conversationToken;
	}
	
}
