/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vace117.garage.opener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Handles the ImageView we use to display images of open and closed door.
 * 
 * This view is created once and reused when images need to change.
 *
 * @author Val Blant
 */
public class DoorPictureManager  {
	private GarageControlActivity activity;
	
	private Bitmap openDoorBitmap;
    private Bitmap closedDoorBitmap;
    
    private View doorImageView;    
    private ImageView doorImage;
    // Used to ensure that we don't try to display anything until the view is properly inititalized
    private CountDownLatch initLatch = new CountDownLatch(1);
    
    private DoorImageListener doorImageListener;

    /**
     * This is how we tell the caller that our ImageView was clicked by the user.
     */
	public interface DoorImageListener {
		public void doorClicked();
	}

	public DoorPictureManager(GarageControlActivity activity, DoorImageListener doorImageListener) {
		this.activity = activity;
		this.doorImageListener = doorImageListener;
		
		createImageView();
	}
	
	public void showOpenDoor() {
		waitForInit();
		showDoor(openDoorBitmap);
	}
	
	public void showClosedDoor() {
		waitForInit();
		showDoor(closedDoorBitmap);
	}
	
	/**
	 * Enforces init lock
	 */
	private void waitForInit() {
		try {
			initLatch.await(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param door The image to display
	 */
	private void showDoor(final Bitmap door) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				doorImage.setImageBitmap(door);
				doorImageView.setVisibility(View.VISIBLE);
				setEnabled(true);
			}
		});
	}
	
	/**
	 * Used to enable or disable the clicks on this ImageView
	 * 
	 * @param state
	 */
	public void setEnabled(final boolean state) {
		activity.runOnUiThread(new Runnable() {
			public void run() {
				doorImage.setEnabled(state);
			}
		});
	}
	
	/**
	 * Hides the ImageView w/o destroying it
	 */
	public void hide() {
		if ( doorImageView != null ) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					doorImageView.setVisibility(View.GONE);
				}
			});
		}
	}
	
	/**
	 * This must be called if we want to make this class re-entrant.
	 */
    public void removeView() {
    	if ( doorImageView != null ) {

    		activity.runOnUiThread(new Runnable() {
    	        public void run() {
    	    		((ViewGroup)doorImageView.getParent()).removeView(doorImageView);
    	    		doorImageView.setVisibility(View.GONE);
    	        }
    		});
    	}
    	
    }

    /**
     * Initializes the view and the images
     */
	private void createImageView() {
		activity.runOnUiThread(new Runnable() {
			public void run() {
		
				doorImageView = activity.getLayoutInflater().inflate(R.layout.activity_door_image, null);
				activity.addContentView(doorImageView, new ViewGroup.LayoutParams(
		                	ViewGroup.LayoutParams.MATCH_PARENT,
		                	ViewGroup.LayoutParams.MATCH_PARENT));
		
				
				doorImage = (ImageView) activity.findViewById(R.id.doorImage);
				doorImage.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {
		            	doorImageListener.doorClicked();
		            }
		        });
				
		        openDoorBitmap = createBitmap(R.drawable.open_door);
		        closedDoorBitmap = createBitmap(R.drawable.closed_door);
		        
		        hide();
		        
		        initLatch.countDown();
			}
		});
		        
	}
	
	/**
	 * Reads specified bitmap from Resources
	 * 
	 * @param bitmapId
	 * @return
	 */
    private Bitmap createBitmap(int bitmapId) {
    	return BitmapFactory.decodeStream( activity.getResources().openRawResource(bitmapId) );
    }	


}

