package vace117.garage.opener;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Main Garage Opener activity.
 *
 * @author Val Blant
 */
public class GarageControlActivity extends Activity {
	
	Button mainButton;
	ProgressBar connectionProgressSpinner;
	TextView doorStatusText;

	LinearLayout errorContainer;
	TextView exceptionText;
	
	GarageDoorController controller;
	
	private static AssetManager assetManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_garage_control);
		
		doorStatusText = (TextView) findViewById(R.id.doorStatusText);
		
		errorContainer = (LinearLayout) findViewById(R.id.errorContainer);
		exceptionText = (TextView) findViewById(R.id.exceptionLog);
		connectionProgressSpinner = (ProgressBar) findViewById(R.id.connectionProgressSpinner);
		
		mainButton = (Button) findViewById(R.id.mainButton);
		
		controller = new GarageDoorController(this);
		mainButton.setOnClickListener( controller );
		
		
		assetManager = getResources().getAssets();
	}
	
	public static AssetManager getAssetManager() {
		return assetManager;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		new Thread(new Runnable() {
	        public void run() {
	        	controller.start();
	        }
	    }).start();
	}



	@Override
	protected void onStop() {
		super.onStop();
		
		controller.stop();
	}

}
