package vace117.garage.opener;

import java.io.IOException;

import vace117.garage.opener.crypto.ConversationExpiredException;
import vace117.garage.opener.crypto.SecureChannelClient;
import vace117.garage.opener.crypto.SecureConversation;
import vace117.garage.opener.network.InternetCommunicationChannel;
import android.graphics.LightingColorFilter;
import android.view.View;

/**
 * Controller that displays Garage Door status on the screen and handles button clicks
 * by sending messages to the Spark Core.
 *
 * @author Val Blant
 */
public class GarageDoorController implements View.OnClickListener {
	private GarageControlActivity activity;
	
	private SecureChannelClient secureChannel;
	
	
	private enum ButtonState {
		OPEN("Open"), CLOSE("Close");
		
		private final String text;
		private ButtonState(String text) { this.text = text; }
	};
	private ButtonState buttonState;
	
	
	public GarageDoorController(GarageControlActivity activity) {
		this.activity = activity;
		
		activity.connectionProgressSpinner.setVisibility(View.VISIBLE);
		activity.doorStatusText.setVisibility(View.GONE);
		
		activity.mainButton.setVisibility(View.GONE);
		activity.errorContainer.setVisibility(View.GONE);
		
		activity.mainButton.getBackground().setColorFilter(new LightingColorFilter(0xFF888888, 0xFFFF0000));
	}
	
	private void connectToGarage() throws IOException {
		secureChannel = new SecureChannelClient(new InternetCommunicationChannel());
	}
	
	private void disconnectFromGarage() throws IOException {
		secureChannel.closeCommunicationChannel();
		secureChannel = null;
	}

	
	public void start() {
		try {
			connectToGarage();
			refreshDoorStatus( sendCommand("GET_STATUS") );
		}
		catch (Exception e) {
			displayErrorLog(e);
		}
	}
	
	public void stop() {
		try {
			disconnectFromGarage();
		}
		catch (Exception e) {
			displayErrorLog(e);
		}
	}
	
	private synchronized String sendCommand(String command) {
		try {
			secureChannel.openCommunicationChannel();
			
			SecureConversation conversation = secureChannel.createConversation();
			String response = conversation.sendMessage(command);
			return response;
		} catch (ConversationExpiredException e) {
			throw new IllegalStateException("Conversation Token not accepted", e);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to send message", e);
		}
		finally {
			try {
				secureChannel.closeCommunicationChannel();
			} catch (IOException e) {
				throw new IllegalStateException("Unable to close Communication Channel", e);
			}
		}
		
	}
	
	private void refreshDoorStatus(String status) throws IOException {
		if ( "DOOR_MOVING".equals(status) ) {
			setStatusAndButtonText("Moving", null);
		}
		else if ( "DOOR_OPEN".equals(status) ) {
			setStatusAndButtonText("Open", ButtonState.CLOSE);
		}
		else if ( "DOOR_CLOSED".equals(status) ) {
			setStatusAndButtonText("Closed", ButtonState.OPEN);
		}
	}
	
	private void setStatusAndButtonText(final String statusText, final ButtonState buttonState) {
		this.buttonState = buttonState;
		
		activity.runOnUiThread(new Runnable() {
	        public void run() {
	    		activity.connectionProgressSpinner.setVisibility(View.GONE);
	    		activity.doorStatusText.setVisibility(View.VISIBLE);
	    		activity.doorStatusText.setText(statusText);
	    		
	    		activity.mainButton.setEnabled(true);
	    		
	    		if ( buttonState != null ) {
	    			activity.mainButton.setVisibility(View.VISIBLE);
	    			activity.mainButton.setText(buttonState.text);
	    		}
	    		else {
	    			activity.mainButton.setVisibility(View.GONE);
	    		}
	        }
	    });
	}
	
	private void displayErrorLog(final Exception e) {
		activity.runOnUiThread(new Runnable() {
	        public void run() {
	        	activity.connectionProgressSpinner.setVisibility(View.GONE);
	    		activity.mainButton.setVisibility(View.GONE);
	    		
	    		activity.errorContainer.setVisibility(View.VISIBLE);
	    		activity.exceptionText.setText( e.toString() );
	        }
	    });
	}
	
	

	@Override
	public void onClick(View v) {
	    new Thread(new Runnable() {
	        public void run() {
	        	try {
	        		activity.runOnUiThread(new Runnable() {
						public void run() {
							activity.mainButton.setEnabled(false);
						}
					});
	        		
		        	if ( ButtonState.OPEN.equals( buttonState ) ) {
		    			refreshDoorStatus( sendCommand("OPEN") );
		        	}
		        	else if ( ButtonState.CLOSE.equals( buttonState ) ) {
		    			refreshDoorStatus( sendCommand("CLOSE") );
		        	}
	        	}
	        	catch (Exception e) {
	        		displayErrorLog(e);
	        	}
	        }
	    }).start();
	}

}
