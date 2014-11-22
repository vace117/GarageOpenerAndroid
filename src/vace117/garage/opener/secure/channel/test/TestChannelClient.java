package vace117.garage.opener.secure.channel.test;

import java.io.IOException;

import vace117.garage.opener.secure.channel.Conversation;
import vace117.garage.opener.secure.channel.ConversationExpiredException;
import vace117.garage.opener.secure.channel.SecurityException;
import vace117.garage.opener.secure.channel.SecureChannelClient;

/**
 * Just for testing the Android code w/o having to connect to anything or do any crypto. 
 *
 * @author Val Blant
 */
public class TestChannelClient implements SecureChannelClient {
	private static int counter = 0;

	@Override
	public Conversation createConversation() throws SecurityException, IOException {
		return new OpenCloseSequenceTest();
	}
	
	/**
	 * Allows repetetive testing of open/close 
	 *
	 * @author Val Blant
	 */
	public class OpenCloseSequenceTest implements Conversation {
		
		@Override
		public String sendMessage(String message) throws ConversationExpiredException, SecurityException, IOException {
			if ( counter == 0 ) {
				if ( message.equals("GET_STATUS") )	return "DOOR_CLOSED";
				if ( message.equals("OPEN") ) {	counter = 1; return "DOOR_MOVING"; }
			}
			else if ( counter <= 6 ) {
				counter++;
				return "DOOR_MOVING";
			}
			else if ( counter == 7 ) {
				counter = 10;
				return "DOOR_OPEN";
			}
			else if ( counter == 10 ) {
				if ( message.equals("CLOSE") ) {
					counter = 11; 
					return "DOOR_MOVING"; 
				}
				else {
					new IllegalStateException("I expected CLOSE");
				}
			}
			else if ( counter <= 16 ) {
				counter++;
				return "DOOR_MOVING";
			}
			else if ( counter == 17 ) {
				counter = 0;
				return "DOOR_CLOSED";
			}
			
			
			throw new IllegalStateException("Counter out of whack");
		}
	}
	

	public class DoorMovingTest implements Conversation {
		@Override
		public String sendMessage(String message) throws ConversationExpiredException, SecurityException, IOException {
			return "DOOR_MOVING";
		}
	}
	
	public class DoorOpenTest implements Conversation {
		@Override
		public String sendMessage(String message) throws ConversationExpiredException, SecurityException, IOException {
			return "DOOR_OPEN";
		}
	}

	public class DoorClosedTest implements Conversation {
		@Override
		public String sendMessage(String message) throws ConversationExpiredException, SecurityException, IOException {
			return "DOOR_CLOSED";
		}
	}

	

	@Override
	public void openCommunicationChannel() throws IOException {
	}

	@Override
	public void closeCommunicationChannel() throws IOException {
	}

}
