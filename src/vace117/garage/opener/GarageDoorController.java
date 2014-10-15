package vace117.garage.opener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.graphics.LightingColorFilter;
import android.view.View;

public class GarageDoorController implements View.OnClickListener {
	private GarageControlActivity activity;
	
	private Socket socket;
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	
	private InetSocketAddress sparkCore = new InetSocketAddress("192.168.7.121", 6666);
	private static final int CONNECT_TIMEOUT = 5000; //ms
	private static final int READ_TIMEOUT = 5000; //ms. Allows the app to throw an error if the connection is lost.
	
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
		socket = new Socket();
		socket.setSoTimeout(READ_TIMEOUT);
		socket.connect(sparkCore, CONNECT_TIMEOUT);
		
		outToServer = new DataOutputStream( socket.getOutputStream() );
		inFromServer = new BufferedReader( new InputStreamReader(socket.getInputStream()) );
	}
	
	private void disconnectFromGarage() throws IOException {
		outToServer.close();
		inFromServer.close();
		socket.close();
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
	
	private String sendCommand(String command) throws IOException {
		if ( socket.isConnected() && !socket.isInputShutdown() && !socket.isOutputShutdown() ) {
			outToServer.writeBytes(command + "\n");
			return inFromServer.readLine();
		}
		else {
			throw new IOException("Lost connection to the garage!");
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
