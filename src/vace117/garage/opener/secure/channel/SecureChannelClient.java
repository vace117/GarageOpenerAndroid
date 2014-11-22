package vace117.garage.opener.secure.channel;

import java.io.IOException;


/**
 * Provides a secure request/response style communication with a server. 
 * <p>
 * The specifics of talking to the server are abstracted into <code>CommunicationChannel</code>.
 *
 * @author Val Blant
 */
public interface SecureChannelClient {
	
	public void openCommunicationChannel() throws IOException;

	public void closeCommunicationChannel() throws IOException;
	
	public Conversation createConversation() throws SecurityException, IOException;

}
