package vace117.garage.opener;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Timer;
import java.util.TimerTask;

import vace117.garage.opener.DoorAnimationManager.DoorAnimationListener;
import vace117.garage.opener.DoorPictureManager.DoorImageListener;
import vace117.garage.opener.secure.channel.Conversation;
import vace117.garage.opener.secure.channel.ConversationExpiredException;
import vace117.garage.opener.secure.channel.SecureChannelClient;
import android.util.Log;
import android.view.View;

/**
 * Controller that displays Garage Door status using some animations and pictures,
 * as well as sending commands to the Spark Core that controls the actual garage door. 
 *
 * @author Val Blant
 */
public class GarageDoorController implements DoorImageListener, DoorAnimationListener {
	private static final String TAG = "GarageDoorController";
	
	private GarageControlActivity activity;
	
	private SecureChannelClient secureChannel;
	
	private DoorAnimationManager doorAnimationManager;
	private DoorPictureManager doorPictureManager;

	enum GarageDoorState {OPEN, CLOSED, MOVING};
	private GarageDoorState doorState;
	
	
	public GarageDoorController(GarageControlActivity activity, SecureChannelClient secureChannel) {
		this.activity = activity;
		this.secureChannel = secureChannel;
		
		doorAnimationManager = new DoorAnimationManager(activity, this);
		doorPictureManager = new DoorPictureManager(activity, this);
	}
	
	
	public void start() {
		try {
			resetScreenStates();
			updateDoorStatus( sendCommand("GET_STATUS") );
		}
		catch (Throwable e) {
			displayErrorLog(e);
		}
	}
	
	private synchronized void updateDoorStatus(String newStatus) throws IOException {
		if ( "DOOR_MOVING".equals(newStatus) ) {
			if ( doorState == GarageDoorState.CLOSED || doorState == GarageDoorState.OPEN) {
				startProgressBar();
			}
			
			doorState = GarageDoorState.MOVING;
		}
		else if ( "DOOR_OPEN".equals(newStatus) ) {
			stopTimer();
			if ( doorState == GarageDoorState.MOVING) {
				doorAnimationManager.openDoor();
			}
			else {
				doorPictureManager.showOpenDoor();
			}
			
			doorState = GarageDoorState.OPEN;
		}
		else if ( "DOOR_CLOSED".equals(newStatus) ) {
			stopTimer();
			if ( doorState == GarageDoorState.MOVING) {
				doorAnimationManager.closeDoor();
			}
			else {
				doorPictureManager.showClosedDoor();
			}

			doorState = GarageDoorState.CLOSED;
		}
	}
	

	private int knockCount = 0; 
	private Timer knockTimer;
	
	@Override
	public void doorClicked() {
		// Enforce that the clicks happen within 800ms
		//
		if ( knockTimer == null ) {
			knockTimer = new Timer("Knock Timer");
			knockTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					Log.v(TAG, "Knock too slow - expiring wait period.");
					knockCount = 0;
					knockTimer = null;
				}
			}, 800);
		}
		
		if ( ++knockCount == 3 ) {
		    new Thread(new Runnable() {
		        public void run() {
		        	try {
						doorPictureManager.setEnabled(false); // Disable clicks
						
			        	if ( GarageDoorState.OPEN.equals( doorState ) ) {
			    			updateDoorStatus( sendCommand("CLOSE") );
			        	}
			        	else if ( GarageDoorState.CLOSED.equals( doorState ) ) {
			    			updateDoorStatus( sendCommand("OPEN") );
			        	}
			        	
			        	knockCount = 0;
		        	}
		        	catch (Throwable e) {
		        		displayErrorLog(e);
		        	}
		        }
		    }).start();
		}
	}

	@Override
	public void animationCompleted() {
		if ( doorState == GarageDoorState.OPEN ) {
			doorPictureManager.showOpenDoor();
		}
		if ( doorState == GarageDoorState.CLOSED ) {
			doorPictureManager.showClosedDoor();
		}
	}


	
	
	
	private void resetScreenStates() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				activity.connectionProgressSpinner.setVisibility(View.VISIBLE);				
				activity.errorLogView.setVisibility(View.GONE);
				if (doorAnimationManager != null) doorAnimationManager.hide();
				if (doorPictureManager != null) doorPictureManager.hide();
			}
		});
	}
	
	private void disconnectFromGarage() throws IOException {
		secureChannel.closeCommunicationChannel();
		resetScreenStates();
		doorPictureManager.removeView();
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
			
			Conversation conversation = secureChannel.createConversation();
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
	
	private Timer movingDoorTimer;
	private void stopTimer() {
		if ( movingDoorTimer != null) movingDoorTimer.cancel();
	}
	
	private void startProgressBar() {
		doorAnimationManager.startProgressBar();
		
		// Start a timer that will query the door status while the door is moving
		// and the progress bar video is playing.
		//
		movingDoorTimer = new Timer();
		movingDoorTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					Log.v(TAG, "Scheduled GET_STATUS");
					updateDoorStatus( sendCommand("GET_STATUS") );
				}
		    	catch (Throwable e) {
		    		displayErrorLog(e);
		    	}
			}
		}, 0, 2000); // Every 2 seconds
		
	}
	
	private void displayErrorLog(final Throwable e) {
		activity.runOnUiThread(new Runnable() {
	        public void run() {
	        	activity.connectionProgressSpinner.setVisibility(View.GONE);
	        	doorAnimationManager.hide();
	        	doorPictureManager.hide();
	    		activity.errorLogView.setVisibility(View.VISIBLE);
	    		
	    		StringWriter errors = new StringWriter();
	    		e.printStackTrace(new PrintWriter(errors));
	    		activity.exceptionText.setText( errors.toString() );
	        }
	    });
	}
	
	
}
