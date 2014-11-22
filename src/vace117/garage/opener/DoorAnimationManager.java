package vace117.garage.opener;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Manages the SurfaceView we use to display the video sequences of opening, closing door and progress bar sequence.
 * 
 * The view is destroyed and recreated for every playback.
 *
 * @author Val Blant
 */
public class DoorAnimationManager implements OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, SurfaceHolder.Callback {

	private static final String TAG = "DoorAnimationManager";
	private MediaPlayer mediaPlayer;
	private SurfaceView surface;
	private SurfaceHolder surfaceHolder;
	private boolean isVideoSizeSet = false;
	private boolean isVideoReadyToBePlayed = false;
	
	private Activity activity;

	private View mediaPlayerView;
	
	private int currentVideoId;
	
	private DoorAnimationListener doorAnimationListener; 
	
	
	public DoorAnimationManager(Activity activity, DoorAnimationListener dal) {
		this.activity = activity;
		this.doorAnimationListener = dal;
	}

	/**
	 * This is how we notify caller that the video is over
	 */
	public interface DoorAnimationListener {
		public void animationCompleted();
	}

	/**
	 * Plays the open video to completion
	 */
	public void openDoor() {
		startAnimationSequence(R.raw.open_video);
	}

	/**
	 * Starts the progress bar video. This one is intended to be interrupted by the caller when the task is done.
	 */
	public void startProgressBar() {
		startAnimationSequence(R.raw.loading_screen_loop_flipped);
	}

	/**
	 * Plays the close video to completion
	 */
	public void closeDoor() {
		startAnimationSequence(R.raw.close_video);
	}
	
	public void hide() {
		removeView();
	}


	
	
	private void startAnimationSequence(int videoId) {
		currentVideoId = videoId;

		activity.runOnUiThread(new Runnable() {
	        public void run() {
		
				removeView(); // Make sure our SurfaceView is removed before we try adding another
		
				// Inflate the view from XML
				//
				LayoutInflater inflater = activity.getLayoutInflater();
				mediaPlayerView = inflater.inflate(R.layout.activity_door_animation, null);
		
				// Add the view and make it fill the activity
				//
				activity.addContentView(mediaPlayerView, new ViewGroup.LayoutParams(
		                	ViewGroup.LayoutParams.MATCH_PARENT,
		                	ViewGroup.LayoutParams.MATCH_PARENT));
		
				
		        surface = (SurfaceView) activity.findViewById(R.id.doorsOpeningVideo);
		        surfaceHolder = surface.getHolder();
		        surfaceHolder.addCallback(DoorAnimationManager.this);		
		        
				
				mediaPlayerView.setVisibility(View.VISIBLE);
				
	        }
		});
		
	}
	
    private void startVideoPlayback() {
        Log.v(TAG, "startVideoPlayback");
        if ( isVideoReadyToBePlayed ) {
	        mediaPlayer.start();
        }
    }



    /**
     * Here we size the SurfaceView to the appropriate size. We set the width  to fill the activity,
     * and calculate the height based on video's aspect ratio.
     */
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged called");
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        isVideoSizeSet = true;
        
        float aspectRatio = (float) height / (float) width;
        
        // Size the ViewSurface to take up the full width of the phone
        //
        android.view.ViewGroup.LayoutParams lp = surface.getLayoutParams();
        lp.width = activity.getWindow().getDecorView().getWidth();
        lp.height = Math.round(lp.width * aspectRatio);
        surface.setLayoutParams(lp);
        
        if (isVideoReadyToBePlayed && isVideoSizeSet) {
            startVideoPlayback();
        }
    }

	
	/**
	 * Called when our SurfaceView is created.
	 * 
	 * Reads the video file from Resources and sets up the MediaPlayer
	 */
    private void playVideo() {
	        doCleanUp();
	        try {
	
	            // Create a new media player and set the listeners
	            AssetFileDescriptor afd = activity.getResources().openRawResourceFd(currentVideoId);
	            mediaPlayer = new MediaPlayer();
	            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
	            afd.close();
	            mediaPlayer.setDisplay(surfaceHolder);
	            mediaPlayer.prepare();
	            mediaPlayer.setOnCompletionListener(this);
	            mediaPlayer.setOnPreparedListener(this);
	            mediaPlayer.setOnVideoSizeChangedListener(this);
	            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	
	
	        } catch (Exception e) {
	            Log.e(TAG, "error: " + e.getMessage(), e);
	        }
    }

    /**
     * Called when our SurfaceView is destroyed, which happens when the view is removed from the Activity
     */
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void doCleanUp() {
        isVideoReadyToBePlayed = false;
        isVideoSizeSet = false;
    }
    
    /**
     * Removes our SurfaceView from the Activity. This causes the view to be destroyed.
     */
    private void removeView() {
    	if ( mediaPlayer != null ) {
    		mediaPlayer.stop();

    		activity.runOnUiThread(new Runnable() {
    	        public void run() {
    	    		((ViewGroup)mediaPlayerView.getParent()).removeView(mediaPlayerView);
    	        	mediaPlayerView.setVisibility(View.GONE);
    	        }
    		});
    	}
    	
    }


    public void onCompletion(MediaPlayer arg0) {
        Log.d(TAG, "onCompletion called");
        
        // Tell our caller that video is over
        //
        doorAnimationListener.animationCompleted();
        
    }

    public void onPrepared(MediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        isVideoReadyToBePlayed = true;
        if (isVideoReadyToBePlayed && isVideoSizeSet) {
            startVideoPlayback();
        }
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d(TAG, "surfaceChanged called");

    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.d(TAG, "surfaceDestroyed called");
        releaseMediaPlayer();
        doCleanUp();
    }


    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called");
        playVideo();
    }
	
}
