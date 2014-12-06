package vace117.garage.opener.secure.channel;

import java.io.IOException;


/**
 * Provides a secure request/response style communication with a server. 
 * <p>
 * The specifics of talking to the server are abstracted into <code>CommunicationChannel</code>.
 *
 * @author Val Blant
 */
public abstract class AbstractSecureChannelClient {
	
	protected CommunicationChannel commChannel;
	
	
	public AbstractSecureChannelClient(CommunicationChannel commChannel) {
		this.commChannel = commChannel;
	}

	public void openCommunicationChannel() throws IOException {
		commChannel.open();
	}

	public void closeCommunicationChannel() throws IOException {
		commChannel.close();
	}
	
	public abstract Conversation createConversation() throws SecurityException, IOException;

}
