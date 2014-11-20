package ri.cmu.edu.vehiclestateyi;


import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CameraController {

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;



	private static double captureRate = 10;


	public volatile Camera mCamera = null;
	public Camera.Parameters myCamParam = null;
	public MediaRecorder mMediaRecorder = null;
	private CameraPreview mPreview = null;
	private ZoomControls zoomControl;

	private static StateMediator sm = null;


	public Queue<byte[]> cache = null;
	File pictureFile = null;

	private String[] videoResolutions = {"1920, 1080", "1280, 720", "720, 480"};

	public WriteThread write_thread;
	public Alarm alarm = null;
	public MainActivity ctx;
	public Handler cHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == Protocol.TAKE_PICTURE){
				if(mCamera !=null);
				capturePicture();
			}
		}
	};

	public CameraController(MainActivity activity) {
		ctx = activity;

		Camera c = getCameraInstance();
		if (c != null) {
			myCamParam = c.getParameters();
			c.release();
		}
		
		cache = new LinkedList<byte[]>();
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		}
		catch (Exception e){
			Log.e("getCamera", e.getMessage());
		}
		return c; // returns null if camera is unavailable
	}
	/** 
	 * Enables zoom feature in native camera .  Called from listener of the view 
	 * used for zoom in  and zoom out.
	 * 
	 * 
	 * @param zoomInOrOut  "false" for zoom in and "true" for zoom out
	 */
	public void zoomCamera(boolean zoomInOrOut) {
		if(mCamera!=null) {
			Parameters parameter = mCamera.getParameters();

			if(parameter.isZoomSupported()) {
				int MAX_ZOOM = parameter.getMaxZoom();
				int currnetZoom = parameter.getZoom();
				if(zoomInOrOut && (currnetZoom <MAX_ZOOM && currnetZoom >=0)) {
					parameter.setZoom(++currnetZoom);
				}
				else if(!zoomInOrOut && (currnetZoom <=MAX_ZOOM && currnetZoom >0)) {
					parameter.setZoom(--currnetZoom);
				}
			}
			else

				mCamera.setParameters(parameter);
		}
	} 


	public void prepareCamera(Context context, LinearLayout linearLayout) {
		// Create an instance of Camera
		mCamera = getCameraInstance();
		// Set fixed orientation (this app always displays vertically
		mCamera.setDisplayOrientation(90);

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(context, mCamera);

		//imageCaptureThread = new Thread(captureLoop);

		myCamParam = mCamera.getParameters();

		linearLayout.addView(mPreview);
	}

	private boolean prepareVideoRecorder() {

		/* Weird trick to get the camera to record on the Galaxy Camera
		 * Normally I would just use the existing camera object... but
		 * that doesn't work on this particular device... weird :/
		 */
		if(mCamera == null){
			mCamera = getCameraInstance();
		}
		mCamera.stopPreview();
		mCamera.lock();
		mCamera.release();
		mCamera = Camera.open();
		mCamera.setDisplayOrientation(90);

		mMediaRecorder = new MediaRecorder();

		Log.e("CAMERA!", String.valueOf(mCamera));

		// Step 1: Unlock and set camera to MediaRecorder
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);

		/* if exceed max file size */
		mMediaRecorder.setMaxFileSize(50000000);
		mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					if (StateMediator.cameraRunning) {
						stopVideo();
						StateMediator.setCameraRunningStatus(false);

					}
					/* do not overload */
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();  					}

					if(!StateMediator.cameraRunning){
						MainActivity.getNewOutputFolder();
					}
					StateMediator.setCameraRunningStatus(true);

					recordVideo();


				}
			}
		});

		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
		mMediaRecorder.setCaptureRate(captureRate);

		// Step 6: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Log.d("CameraActivity", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			Log.d("CameraActivity", "IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;
	}

	/* start taking continuous pictures */
	public void takeContinuousPictures() {
		StateMediator.cameraRunning = true;
		alarm = new Alarm(this);
		alarm.start();

	}

	public void stopTakingPictures(){
		try {
			StateMediator.cameraRunning = false;
			write_thread.wHandler.sendEmptyMessage(Protocol.STOP_TAKING_PICTURE);
			alarm.join();
		} catch (InterruptedException e) {
			e.printStackTrace(); 
		}

	}
	public String[] getResolutions() {
		List sizes;
		//if(sm ==null) sm = MainActivity.sm;
		if (StateMediator.cameraMode) {
			sizes = myCamParam.getSupportedPictureSizes();
		}
		else {
			sizes = myCamParam.getSupportedVideoSizes();
		}
		String[] result = new String[sizes.size()];
		Camera.Size t;
		for (int i = 0; i < sizes.size(); i++) {
			t = (Camera.Size) sizes.get(i);
			result[i] = t.height + ", " + t.width;
		}
		return result;
	}

	public void setResolution(int index) {
		List sizes;
		Camera.Size setting;
		//if(sm == null) sm = MainActivity.sm;
		if (StateMediator.cameraMode) {
			sizes = myCamParam.getSupportedPictureSizes();
		}
		else {
			sizes = myCamParam.getSupportedVideoSizes();
		}
		setting = (Camera.Size) sizes.get(index);
		myCamParam.setPictureSize(setting.width, setting.height);
	}

	private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			//			pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			//			if (pictureFile == null){
			//				Log.d("CameraActivity", "Error creating media file, check storage permissions: ");
			//				return;
			//			}
			cache.add(data);

			Log.v("Picture CAllback", cache.size()+"");
			if (mCamera != null) {
				mCamera.startPreview();
			}
			/* inform alarm thread that the photo taking is successful*/
			if(StateMediator.cameraRunning = false) {
				alarm.alarmHandler.sendEmptyMessage(Protocol.PICTURE_CALLBACK_FINISHED);
			}

		}
	};

	public void capturePicture() {
		mCamera.takePicture(null, null, mPicture);
	}

	public void recordVideo() {

		// initialize video camera
		if (prepareVideoRecorder()) {
			mMediaRecorder.start();
			StateMediator.startCapturing();
			if(!MainActivity.sensor_thread.isAlive())MainActivity.sensor_thread.start();
			//startWriteThread();	
		} else {
			// prepare didn't work, release the camera
			releaseMediaRecorder();
		}
	}
	public void stopVideo(){
		mMediaRecorder.stop();  // stop the recording

		mCamera.lock();         // take camera access back from MediaRecorder
		releaseMediaRecorder(); // release the MediaRecorder object
		StateMediator.stopCapturing();
	}

	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = MetadataLogger.outputFolder;
		Log.e("outputFolder", String.valueOf(mediaStorageDir));
		//String timeStamp = new SimpleDateFormat(Protocol.dateFormat).format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_"+ MainActivity.current_folder_timeStamp + ".jpg");
		} else if(type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"VID_"+ MainActivity.current_folder_timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}

	public void kill() {
		StateMediator.setCameraRunningStatus(false);
		releasePreview();
		releaseCamera();              // release the camera immediately on pause event
		releaseMediaRecorder();       // if you are using MediaRecorder, release it first

	}
	public void pause() {
		StateMediator.setCameraRunningStatus(false);

		releaseCamera();              // release the camera immediately on pause event
		releaseMediaRecorder();       // if you are using MediaRecorder, release it first

	}


	private void releaseMediaRecorder(){
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();   // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;
			if(mCamera !=null) mCamera.lock();           // lock camera for later use
		}
	}

	private void releaseCamera(){
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();        // release the camera for other applications
			mCamera = null;
		}
	}
	private void releasePreview(){
		if(mPreview!=null){
			mPreview.getHolder().removeCallback(mPreview);
		}
	}





}