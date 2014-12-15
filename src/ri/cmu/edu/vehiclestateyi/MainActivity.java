package ri.cmu.edu.vehiclestateyi;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.hardware.SensorManager;
import android.os.*;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;

public class MainActivity extends Activity {

	public static boolean DEBUG = false;

	/* Components */
	static CameraController mCamera = null;
	static MetadataLogger mMetadataLogger = null;
	static DirectoryUpload mDirectoryUpload = null;

	/* the folder name of video and context data files*/
	public static volatile String curDirName;
	
	public static boolean upToDate = false;
	public static boolean uploading = false;
	public static boolean noFiles = false;
	public static boolean interruptedUpload = false;

	


	/* Dropbox */
	private static final String appKey = "490rsj1cg518cqv";
	private static final String appSecret = "6y4kj34bqt1pgxu";
	private DbxAccountManager mDbxAcctMgr;


	/* inner states */
	private long lastClick;
	private boolean screenOn = false;
	private static final String TAG = "MainActivity";
	private MainActivity self = this;
	private Button toggleButton = null;

	/* Handler */
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0) {
				Log.e("uploading!", String.valueOf(uploading));
				setUploadText((Button) findViewById(R.id.button_upload));
			}
			if(msg.what == Protocol.GPS_OUTPUT_ZERO){
				Toast.makeText(getApplicationContext(), "GPS output is ZERO!", Toast.LENGTH_SHORT).show();;
			}
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		lastClick = System.currentTimeMillis();
		/* Keep screen lit */
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		SharedPreferences settings = getSharedPreferences(Protocol.PREFS_NAME, 0);
		//sm = new StateMediator(this);
		StateMediator.setCameraMode(settings.getBoolean("cameraOn", Protocol.VIDEO_MODE));
		mCamera = new CameraController(this);

		upToDate = settings.getBoolean("upToDate", false);
		StateMediator.externalStore = settings.getInt("saveLocation", 0) == 1;
		mCamera.setResolution(settings.getInt("resolution", 0));
		//noFiles = settings.getBoolean("noFiles", false);

		mDbxAcctMgr = null;
		if (mDbxAcctMgr != null && !mDbxAcctMgr.hasLinkedAccount()) {
			mDbxAcctMgr.startLink(this, Protocol.REQUEST_LINK_TO_DBX);
		}
		else {
			finalizeOutput();
			setMainScreen();
		}

		toggleButton = (Button) findViewById(R.id.button_toggle);
	}


	/* GPS and Sensor Service Helper functions */
	private boolean isGPSServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (service.service.getShortClassName().equals(GPSCollector.class.getSimpleName())) {
				return true;
			}
		}
		return false;
	}

	/* GPS and Sensor Connecton */
	private ServiceConnection gpsServiceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.d(TAG,"GPS Connected");
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG,"GPS disconnected");			
		}
	};

	private ServiceConnection sensorServiceConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.w(TAG,"Sensor connected");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG,"Sensor service disconnected");			
		}

	};

	/* End of GPS helper functions*/

	private void finalizeOutput() {
		try {
			mDirectoryUpload = new DirectoryUpload(mDbxAcctMgr);
		} catch (DbxException.Unauthorized unauthorized) {
			unauthorized.printStackTrace();
		}
	}

	private void setMainScreen() {
		setContentView(R.layout.main);

		Button captureButton;
		Button exitButton;
		Button settingsButton;

		/* Prepare Listeners/Buttons */
		captureButton = (Button) findViewById(R.id.button_capture);
		captureButton.setOnClickListener(captureListener);
		setCaptureText(captureButton);

		exitButton = (Button) findViewById(R.id.button_exit);
		exitButton.setOnClickListener(exitListener);

		settingsButton = (Button) findViewById(R.id.button_settings);
		settingsButton.setOnClickListener(settingsListener);

		mCamera.prepareCamera(this, (LinearLayout) findViewById(R.id.view_preview));
	}

	private void setSettingsScreen() {
		pause();
		setContentView(R.layout.settings);

		screenOn = true;

		Button backButton;
		final Button uploadButton;
		Button deleteButton;
		Button toggleButton;
		Button licenseButton;
		Button touButton;

		Spinner saveLocationSpinner;
		Spinner resolutionSpinner;
		Spinner updateRateSpinner;

		backButton = (Button) findViewById(R.id.back_button);
		backButton.setOnClickListener(backListener);

		uploadButton = (Button) findViewById(R.id.button_upload);
		uploadButton.setOnClickListener(uploadListener);
		setUploadText(uploadButton);

		deleteButton = (Button) findViewById(R.id.button_delete);
		deleteButton.setOnClickListener(deleteListener);

		toggleButton = (Button) findViewById(R.id.button_toggle);
		toggleButton.setOnClickListener(toggleListener);
		setToggleText(toggleButton);

		licenseButton = (Button) findViewById(R.id.button_license);
		licenseButton.setOnClickListener(licenseListener);

		touButton = (Button) findViewById(R.id.button_tou);
		touButton.setOnClickListener(touListener);

		saveLocationSpinner = (Spinner) findViewById(R.id.spinner_savelocation);
		ArrayAdapter saveLocationAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Protocol.saveLocations);
		saveLocationSpinner.setAdapter(saveLocationAdapter);
		saveLocationSpinner.setOnItemSelectedListener(saveLocationListener);

		resolutionSpinner = (Spinner) findViewById(R.id.spinner_resolution);
		ArrayAdapter resolutionAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
				mCamera.getResolutions());
		resolutionSpinner.setAdapter(resolutionAdapter);
		resolutionSpinner.setOnItemSelectedListener(resolutionListener);

		updateRateSpinner = (Spinner) findViewById(R.id.spinner_updaterate);
		ArrayAdapter updateRateAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Protocol.updateRates);
		updateRateSpinner.setAdapter(updateRateAdapter);
		updateRateSpinner.setOnItemSelectedListener(updateRateListener);

		SharedPreferences settings = getSharedPreferences(Protocol.PREFS_NAME, 0);
		resolutionSpinner.setSelection(settings.getInt("resolution", 0));
		saveLocationSpinner.setSelection(settings.getInt("saveLocation", 0));
		updateRateSpinner.setSelection(settings.getInt("updateRate", 0));

	}

	/* Dropbox uploading status updating */
	private void setUploadText(Button uploadButton) {
		if (uploading) {
			Log.e("uploading!", "now it should say the right thing...");
			uploadButton.setText("Upload (uploading...)");
		}
		if (noFiles) {
			uploadButton.setText("Upload (no files)");
		}
		else if (upToDate) {
			uploadButton.setText("Upload (up to date)");
		}
		else {
			uploadButton.setText("Upload (new data available)");
		}
	}

	void setCaptureText(Button captureButton) {
		if (StateMediator.cameraRunning) {
			captureButton.setText("Stop");
			captureButton.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFFF0000));
			return;
		}
		if (StateMediator.cameraMode == Protocol.CAMERA_MODE) {
			captureButton.setText("Capture Image");
		} else {
			captureButton.setText("Capture Video");
		}
		captureButton.getBackground().clearColorFilter();
	}

	/* functions for future development */
	public void takePicture() {

	}


	private void setToggleText(Button toggleButton) {
		if (StateMediator.cameraMode == Protocol.CAMERA_MODE) {
			toggleButton.setText("Capture Image");
		} else {
			toggleButton.setText("Capture Video");
		}
	}


	public void showMemoryWarning() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(self);

		// set title
		alertDialogBuilder.setTitle("Memory Warning");

		// set dialog message
		alertDialogBuilder
		.setMessage("There is not enough disk-space")
		.setCancelable(false)
		.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	public void showDeleteWarning() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set title
		alertDialogBuilder.setTitle("Delete Warning");

		// set dialog message
		alertDialogBuilder
		.setMessage("Delete all stored files?")
		.setCancelable(false)
		.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				mDirectoryUpload.delete();
				noFiles = true;
				setUploadText((Button) findViewById(R.id.button_upload));
			}
		})
		.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	AdapterView.OnItemSelectedListener updateRateListener = new AdapterView.OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			SharedPreferences settings = getSharedPreferences(Protocol.PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("updateRate", position);

			// Commit the edits!
			editor.commit();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			//To change body of implemented methods use File | Settings | File Templates.
		}
	};

	AdapterView.OnItemSelectedListener resolutionListener = new AdapterView.OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			mCamera.setResolution(position);

			SharedPreferences settings = getSharedPreferences(Protocol.PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("resolution", position);

			// Commit the edits!
			editor.commit();

		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing...
		}
	};

	AdapterView.OnItemSelectedListener saveLocationListener = new AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View view1, int pos, long id) {
			String val = (String) arg0.getItemAtPosition(pos);
			if (val.equals(Protocol.saveLocations[0])) {
				StateMediator.externalStore = false;
			}
			else if(val.equals(Protocol.saveLocations[1])) {
				StateMediator.externalStore = true;
			}
			else {
				throw new IllegalArgumentException("Not sure how this got here");
			}
			SharedPreferences settings = getSharedPreferences(Protocol.PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("saveLocation", pos);

			// Commit the edits!
			editor.commit();
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing...
		}
	};


	private View.OnClickListener licenseListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			setContentView(R.layout.tou);
			Button back = (Button) findViewById(R.id.back1);
			back.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					setSettingsScreen();
				}
			});
			((TextView) findViewById(R.id.text_license)).setMovementMethod(new ScrollingMovementMethod());
		}
	};

	private View.OnClickListener touListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			setContentView(R.layout.license);
			Button back = (Button) findViewById(R.id.back2);
			back.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					setSettingsScreen();
				}
			});

			((TextView) findViewById(R.id.text_tou)).setMovementMethod(new ScrollingMovementMethod());
		}
	};

	private View.OnClickListener toggleListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			StateMediator.swapCameraMode();

			if(toggleButton == null) toggleButton = (Button) findViewById(R.id.button_toggle);
			setToggleText(toggleButton);

			Spinner resolutionSpinner = (Spinner) findViewById(R.id.spinner_resolution);
			ArrayAdapter resolutionAdapter = new ArrayAdapter(self, android.R.layout.simple_spinner_item,
					mCamera.getResolutions());
			resolutionSpinner.setAdapter(resolutionAdapter);
			resolutionSpinner.setOnItemSelectedListener(resolutionListener);

			SharedPreferences settings = getSharedPreferences(Protocol.PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("cameraOn", StateMediator.cameraMode);

			// Commit the edits!
			editor.commit();
		}
	};

	private View.OnClickListener deleteListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			showDeleteWarning();
		}
	};

	private View.OnClickListener backListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			setMainScreen();
		}
	};

	private View.OnClickListener captureListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (System.currentTimeMillis() - lastClick < Parameters.CLICK_DELAY) {
				return;
			}
			lastClick = System.currentTimeMillis();
			Log.w(TAG,"LISTENER ");
			Log.w(TAG,""+StateMediator.cameraRunning);
			
			/* */
			if(StateMediator.cameraMode == Protocol.CAMERA_MODE){
				if(!StateMediator.cameraRunning) { 
					//takePicture(); 
					StateMediator.setCameraRunningStatus(true);
				}
				else{ 
					//mCamera.stopTakingPictures();
					StateMediator.setCameraRunningStatus(false);
				}
			}
			else{
				/* start new video captreu */
				if(!StateMediator.cameraRunning){
					if(!StateMediator.cameraRunning){
						if (mDirectoryUpload != null) {

							mDirectoryUpload.setUploadDir(Protocol.MEDIA_STORAGE_DIR);
						}


					}
					StateMediator.setCameraRunningStatus(true);

					curDirName = Util.getNewOutputFolder();

					mCamera.recordVideo();
					Log.e(TAG,"RECORDING ");


				}
				else{
					mCamera.stopVideo();
					StateMediator.setCameraRunningStatus(false);
				}
			}
			setCaptureText((Button) findViewById(R.id.button_capture));
		}
	};

	private View.OnClickListener uploadListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dropbox.android.sample");
			startActivity(LaunchIntent);
		}
	};



	private View.OnClickListener exitListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			finish();
		}
	};

	private View.OnClickListener settingsListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if(StateMediator.cameraRunning){
				mCamera.stopVideo();
				StateMediator.setCameraRunningStatus(false);
			}
			setSettingsScreen();
		}
	};


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Protocol.REQUEST_LINK_TO_DBX) {
			if (resultCode == Activity.RESULT_OK) {
				finalizeOutput();
				setMainScreen();
			} else {
				setMainScreen();
				// Maybe warn the user sometime
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void pause() {
		if(mCamera!=null) mCamera.pause();

	}


	@Override
	protected void onResume() {
		super.onResume();

		Log.w("On resume", "On Resume");
		//if(mMetadataLogger == null) mMetadataLogger = new MetadataLogger();
		if(mCamera == null) {
			mCamera = new CameraController(this);
		}

		Intent gpsIntent  = new Intent(this, GPSCollector.class);
		if(!isGPSServiceRunning()){
			//Log.d(TAG,"GPS service is not running");
			startService(gpsIntent);
			

		}
		bindService(gpsIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);

		Intent sensorIntent  = new Intent(this, SensorService.class);
		if(!isGPSServiceRunning()){
			//Log.d(TAG,"GPS service is not running");
			startService(sensorIntent);
			

		}
		bindService(sensorIntent, sensorServiceConnection, Context.BIND_AUTO_CREATE);

		/* start GPS service is null*/

	}

	@Override
	protected void onPause() {
		SharedPreferences settings = getSharedPreferences(Protocol.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("upToDate", upToDate);
		editor.putBoolean("noFiles", noFiles);

		// Commit the edits!
		editor.commit();
		super.onPause();
		pause();


	}

	@Override
	protected void onStop(){
		super.onStop();
		/* stop video has already send the stop broadcast */
		mCamera.stopVideo();
		mCamera.kill();
		
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCamera.stopVideo();
		mCamera.kill();
		Intent gpsintent = new Intent(this, GPSCollector.class);
		unbindService(gpsServiceConnection);
		stopService(gpsintent);
		Intent sensorIntent = new Intent(this, SensorService.class);
		unbindService(sensorServiceConnection);
		stopService(sensorIntent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK)) { //Back key pressed
	       setMainScreen();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}

	void showToast(String msg) {
		Toast error = Toast.makeText(self, msg, Toast.LENGTH_LONG);
		error.show();
	}



}