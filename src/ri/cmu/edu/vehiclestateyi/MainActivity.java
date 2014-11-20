package ri.cmu.edu.vehiclestateyi;

import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.hardware.SensorManager;
import android.os.*;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;

//import ri.cmu.edu.vehiclestateyi.dropbox.DBRoulette;

public class MainActivity extends Activity {

	static CameraController mCamera = null;
	static MetadataLogger mMetadataLogger = null;
	static DirectoryUpload mDirectoryUpload = null;

	public static final String PREFS_NAME = "MyPrefsFile";

	int FRAME_RATE = 10;   /* Frames of video per second */
	static int SENSOR_RATE = SensorManager.SENSOR_DELAY_NORMAL;
	static String MasterDir = "VehicleStateEstimation";

	private static boolean externalStore = false;
	private String[] saveLocations = {"MediaFolder", "ExternalSD"};

	public static boolean isRunning = false;

	public static boolean upToDate = false;
	public static boolean uploading = false;
	public static boolean noFiles = false;
	public static boolean interruptedUpload = false;

	//volatile boolean cameraOn = false;
	long clickDelay = 1000;
	long lastClick;

	private String[] updateRates = {"Normal", "Game", "UI", "Fast"};

	private boolean screenOn = false;

	/* Sensor */
	public static ContextParams cp;
	public GPSTracker gpsTracker;
	/* Dropbox */

	private static final String appKey = "490rsj1cg518cqv";
	private static final String appSecret = "6y4kj34bqt1pgxu";

	private DbxAccountManager mDbxAcctMgr;

	private final static int REQUEST_LINK_TO_DBX = 14;
	private static final String TAG = "MainActivity";

	private MainActivity self = this;
	//public static StateMediator sm;
	public static SensorThread sensor_thread;
	/*
	 * Called when the activity is first created.
	 */

	private Button toggleButton = null;
	private Button uploadButton;
	public static String current_folder_timeStamp;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		lastClick = System.currentTimeMillis();
		/* Keep screen lit */
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		//sm = new StateMediator(this);
		StateMediator.setCameraMode(settings.getBoolean("cameraOn", false));

		//		BluetoothChatService ble = new BluetoothChatService(this, h);
		//		ble.start();
		//		//startService(new Intent(this, BluetoothChatService.class));
		//		//OBDII obd = new OBDII(this, BluetoothChatService.mAdapter, mMetadataLogger);
		//		//obd.initOBD();
		//		//Log.e(TAG, obd.isConnected() +"");

		mMetadataLogger = new MetadataLogger();
		mCamera = new CameraController(this);
		if(sensor_thread != null){
			sensor_thread = null;
		}
		sensor_thread = new SensorThread(this,mMetadataLogger);


		upToDate = settings.getBoolean("upToDate", false);
		externalStore = settings.getInt("saveLocation", 0) == 1;
		mCamera.setResolution(settings.getInt("resolution", 0));
		noFiles = settings.getBoolean("noFiles", false);

		mDbxAcctMgr = null;

		//mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret);

		if (mDbxAcctMgr != null && !mDbxAcctMgr.hasLinkedAccount()) {
			mDbxAcctMgr.startLink(this, REQUEST_LINK_TO_DBX);
		}
		else {
			finalizeOutput();

			setMainScreen();
		}
		cp = new ContextParams();
		if(gpsTracker != null){
			gpsTracker = null;

		} 
		gpsTracker = new GPSTracker(this); 
		Intent intent = new Intent(this, GPSTracker.class);
		startService(intent);


		toggleButton = (Button) findViewById(R.id.button_toggle);
		uploadButton = (Button) findViewById(R.id.button_upload);
		uploadButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.dropbox.android.sample");
				startActivity(LaunchIntent);
			}
		});
	}

	private void finalizeOutput() {
		try {
			mDirectoryUpload = new DirectoryUpload(mDbxAcctMgr);
		} catch (DbxException.Unauthorized unauthorized) {
			unauthorized.printStackTrace();
		}
	}

	public static long freeMemory() {
		if (mMetadataLogger == null) {
			return 0;
		}
		if(MetadataLogger.outputFolder != null){
			Log.e("clicked", "getting mem for " + MetadataLogger.outputFolder.getPath());
			StatFs statFs = new StatFs(MetadataLogger.outputFolder.getPath());


			return (long) ((statFs.getAvailableBlocks()) *(statFs.getBlockSize() / 1048576.0));
		}
		return 1;
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


		
		
		//getNewOutputFolder();

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
		ArrayAdapter saveLocationAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, saveLocations);
		saveLocationSpinner.setAdapter(saveLocationAdapter);
		saveLocationSpinner.setOnItemSelectedListener(saveLocationListener);

		resolutionSpinner = (Spinner) findViewById(R.id.spinner_resolution);
		ArrayAdapter resolutionAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,
				mCamera.getResolutions());
		resolutionSpinner.setAdapter(resolutionAdapter);
		resolutionSpinner.setOnItemSelectedListener(resolutionListener);

		updateRateSpinner = (Spinner) findViewById(R.id.spinner_updaterate);
		ArrayAdapter updateRateAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, updateRates);
		updateRateSpinner.setAdapter(updateRateAdapter);
		updateRateSpinner.setOnItemSelectedListener(updateRateListener);

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		resolutionSpinner.setSelection(settings.getInt("resolution", 0));
		saveLocationSpinner.setSelection(settings.getInt("saveLocation", 0));
		updateRateSpinner.setSelection(settings.getInt("updateRate", 0));

	}

	Handler h = new Handler() {
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
		if (StateMediator.cameraMode) {
			captureButton.setText("Capture Image");
		} else {
			captureButton.setText("Capture Video");
		}
		captureButton.getBackground().clearColorFilter();
	}

	public void takePicture() {
		upToDate = false;
		noFiles = false;
		interruptedUpload = true;
		Log.e("clicked", "Is there free memory?");
		if (freeMemory() < 70) {
			Log.e("clicked", "there is " + freeMemory());
			return;
		}

		mCamera.takeContinuousPictures();
		getNewOutputFolder();
		if(sensor_thread == null || sensor_thread.isInterrupted()) sensor_thread.start();

		Log.e(TAG,"take Continuous Pictures");

	}


	private void setToggleText(Button toggleButton) {
		if (StateMediator.cameraMode) {
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
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
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

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
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
			if (val.equals(saveLocations[0])) {
				externalStore = false;
			}
			else if(val.equals(saveLocations[1])) {
				externalStore = true;
			}
			else {
				throw new IllegalArgumentException("Not sure how this got here");
			}
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
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

			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
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
			if (System.currentTimeMillis() - lastClick < clickDelay) {
				return;
			}
			lastClick = System.currentTimeMillis();
			Log.w(TAG,"LISTENER ");
			Log.w(TAG,""+StateMediator.cameraRunning);
			//toggleCapture();
			if(StateMediator.cameraMode){
				if(!StateMediator.cameraRunning) { 
					takePicture(); 
					StateMediator.setCameraRunningStatus(true);
				}
				else{ 
					mCamera.stopTakingPictures();
					StateMediator.setCameraRunningStatus(false);
				}
			}
			else{
				/* start new video captreu */
				if(!StateMediator.cameraRunning){
					if(!StateMediator.cameraRunning){
						getNewOutputFolder();
					}
					StateMediator.setCameraRunningStatus(true);

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

	private View.OnClickListener uploadListener;

	{
		uploadListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uploading = true;
				new Thread(new Runnable () {
					@Override
					public void run() {
						mDirectoryUpload.upload(h);
					}
				}).start();
			}
		};
	}


	private View.OnClickListener exitListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			finish();
		}
	};

	private View.OnClickListener settingsListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			setSettingsScreen();
		}
	};

	public static File getNewOutputFolder() {
		String ext = "";
		if (StateMediator.cameraMode) {
			ext = mDirectoryUpload.imgDir;
		} else {
			ext = mDirectoryUpload.vidDir;
			//ext = Environment.getExternalStorageDirectory().toString()+"/Pictures/VehicleStateEstimation/vid/";

		}
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.
		File mediaStorageDir;
		if (!externalStore) {
			mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_PICTURES), MasterDir);
		}
		else {
			mediaStorageDir = new File("/storage/extSdCard/VehicleStateEstimation");
		}
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.e("MainActivity", "failed to create VehicleStateEstimation directory");
				return null;
			}
		}

		if (mDirectoryUpload != null) {
			mDirectoryUpload.setUploadDir(mediaStorageDir);
		}

		File subMediaStorageDir = new File(mediaStorageDir.getPath()  + File.separator + ext);

		// Create the storage directory if it does not exist
		if (! subMediaStorageDir.exists()) {
			if (! subMediaStorageDir.mkdirs()){
				Log.e("MainActivity", "failed to create VehicleStateEstimation directory");
				return null;
			}
		}

		// Create a media file name
		current_folder_timeStamp = new SimpleDateFormat(Protocol.dateFormat).format(new Date());
		File outputFolder;

		outputFolder = new File(subMediaStorageDir.getPath() + File.separator + current_folder_timeStamp);
		if (! outputFolder.exists()) {
			if (! outputFolder.mkdirs()) {
				Log.e("MainActivity", "failed to create local directory");
				return null;
			}
		}

		Log.e("mmetadatalogger", String.valueOf(mMetadataLogger));

		if (mMetadataLogger != null) {
			mMetadataLogger.setOutputDirectory(outputFolder);
			mMetadataLogger.setTime(current_folder_timeStamp);
		}

		return outputFolder;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_LINK_TO_DBX) {
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
		if(sensor_thread!=null) sensor_thread.pause();

	}


	@Override
	protected void onResume() {
		super.onResume();

		Log.w("On resume", "On Resume");
		if(mMetadataLogger == null) mMetadataLogger = new MetadataLogger();
		if(mCamera == null) {
			mCamera = new CameraController(this);
		}
		if(gpsTracker == null){
			gpsTracker = new GPSTracker(this); 
			Intent intent = new Intent(this, GPSTracker.class);
			startService(intent);
		}

		if(sensor_thread == null){
			sensor_thread = new SensorThread(this,mMetadataLogger);
		}

	}

	@Override
	protected void onPause() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
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
		onDestroy();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(sensor_thread !=null){
			sensor_thread.kill();
			sensor_thread = null;

		}
		mCamera.kill();
		Intent intent = new Intent(this, GPSTracker.class);
		stopService(intent);
	}

	void showToast(String msg) {
		Toast error = Toast.makeText(self, msg, Toast.LENGTH_LONG);
		error.show();
	}

}