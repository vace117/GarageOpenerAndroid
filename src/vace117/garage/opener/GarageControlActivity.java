package vace117.garage.opener;

import java.net.InetSocketAddress;

import vace117.garage.opener.secure.channel.crypto.AESChannelClient;
import vace117.garage.opener.secure.channel.network.InternetCommunicationChannel;
import vace117.garage.opener.secure.channel.test.TestChannelClient;
import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Main Garage Opener activity.
 * <p>
 * This application uses one activity, with many Views stacked on top of each other. Switching of "screens"
 * is achieved by hiding some Views while showing others. The views are arranged as follows, from bottom to the top:
 * <ol>
 *  <li>activity_spinner</li>
 *  <li>activity_error_log</li>
 *  <li>activity_door_image</li>
 *  <li>activity_door_animation</li>
 * </ol>
 * 
 * @author Val Blant
 */
public class GarageControlActivity extends Activity {
    public static final int LAN_MODE_ID = Menu.FIRST;
    public static final int INTERNET_MODE_ID = Menu.FIRST + 1;
    public static final int TEST_MODE_ID = Menu.FIRST + 2;

	private static AssetManager assetManager;

	private GarageDoorController controller;

	ProgressBar connectionProgressSpinner; // The spinner on the bottom view
	View errorLogView; // The view that shows Stack Dumps
	TextView exceptionText; // The TextView where Exception stack is printed
	
	private InetSocketAddress internetSparkCore = new InetSocketAddress("vace.homelinux.com", 45666);
	private InetSocketAddress wifiSparkCore = new InetSocketAddress("192.168.7.121", 6666);

	
	static {
		// Resolving the hostname of the Spark Core happens in InternetCommunicationChannel constructor. I don't
		// want to make this asyncronous, so I am disabling the network ThreadPolicy check to avoid NetworkOnMainThreadException
		//
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(policy);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    //Remove title and notification bars
		//
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
	    // Start with a spinner screen
	    //
		setContentView(R.layout.activity_spinner);
		connectionProgressSpinner = (ProgressBar) findViewById(R.id.connectionProgressSpinner);
		
		// Add the Error Log view on top, but hide it so we can still see the spinner
		//
		errorLogView = getLayoutInflater().inflate(R.layout.activity_error_log, null);
		addContentView(errorLogView, new ViewGroup.LayoutParams(
            	ViewGroup.LayoutParams.MATCH_PARENT,
            	ViewGroup.LayoutParams.MATCH_PARENT));
		errorLogView.setVisibility(View.GONE);
		exceptionText = (TextView) findViewById(R.id.exceptionLog);
		exceptionText.setMovementMethod(new ScrollingMovementMethod());
		
		// Expose AssetManager so MasterKey has access to the Resources from a static initializer 
		assetManager = getResources().getAssets();
	}
	
	/**
	 * Starts the real controller that connects to the Spark Core 
	 */
	private void initRealGarageController(InetSocketAddress sparkCore) {
		controller = new GarageDoorController(this, new AESChannelClient(new InternetCommunicationChannel(sparkCore)));
	}

	/**
	 * Starts a test controller that connects to a state machine that simulates the garage door
	 */
	private void initTestGarageController() {
		controller = new GarageDoorController(this, new TestChannelClient(null));
	}

	/**
	 * Create the Options Menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		menu.add(0, LAN_MODE_ID, 0, "WiFi");
		menu.add(0, INTERNET_MODE_ID, 0, "Internet");
		menu.add(0, TEST_MODE_ID, 0, "Test Mode");
		
		return true;
	}

	/**
	 * Process Option Menu clicks
	 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	cleanupController();
    	
        switch ( item.getItemId() ) {
	        case LAN_MODE_ID:
	        	initRealGarageController(wifiSparkCore);
	            break;
	        case INTERNET_MODE_ID:
	        	initRealGarageController(internetSparkCore);
	            break;
	        case TEST_MODE_ID:
	        	initTestGarageController();
	            break;

        }
        
        if ( controller != null ) {
        	startController();
        	return true;
        }
        else {
        	return super.onOptionsItemSelected(item);
        }
    }




	private void cleanupController() {
		if (controller != null) {
			controller.stop();
			controller = null;
		}
	}

	private void startController() {
		if ( controller == null ) {
			initRealGarageController(internetSparkCore);
		}
		
		new Thread(new Runnable() {
	        public void run() {
	        	controller.start();
	        }
	    }).start();
	}

	/**
	 * Expose AssetManager so MasterKey has access to the Resources from a static initializer 
	 */
	public static AssetManager getAssetManager() {
		return assetManager;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		startController();
	}

    @Override
    protected void onPause() {
        super.onPause();
		cleanupController();
    }

	
}
