package ri.cmu.edu.vehiclestateyi;


import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


public class GPSCollector extends Service implements LocationListener {

	public static final String TAG = "GPSCollector";
	private boolean isGPSEnabled;
	private boolean isNetworkEnabled;
	private static final int MIN_TIME_BY_UPDATES = 500;
	private static final int MIN_DISTANCE_BY_UPDATES = 1;
	private Location currentBestLocation = null;
	private LocationManager lm = null;
	private boolean fineGrainedGPS = true;
	private volatile static String curDirName = null;
	private static boolean startLogging = false;
	private static GPSCollector self = null;
	private MetadataLogger logger = null;
	//public Timer timer = new Timer();
	public class PeriodicThread extends Thread{

		@Override
		public void run() {
			if(startLogging){
				Log.e(TAG,"periodic work started");
				try {
					Thread.sleep(1000);
					getLocation();
					new PeriodicThread().start();

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}


		}

	}

	public static class GPSReceiver extends BroadcastReceiver{
		public GPSReceiver(){}
		@Override
		public void onReceive(Context context, Intent intent) {
			// if (!Strings.isNullOrEmpty(intent.getAction())) {
			//MainApplication myApplication = (MainApplication) context.getApplicationContext();
			//SharedPreferences sharedPreferences = myApplication.getSharedPreferences(AlarmService.PREFS_NAME,0);
			if(intent.getAction().equals(Protocol.BROADCAST_ACTION_START)){
				Log.d(TAG, "GPS Service Start Logging");
				startLogging = true;
				if(intent.hasExtra(Protocol.CURRENT_DIR_NAME)){
					curDirName = intent.getStringExtra(Protocol.CURRENT_DIR_NAME);
					Log.d(TAG, curDirName);
					self.new PeriodicThread().start();

				}
				else{
					Log.e(TAG,"CURDIR NAME IS NULL");
				}
			}
			if(intent.getAction().equals(Protocol.BROADCAST_ACTION_STOP)){
				startLogging = false;
				Log.d(TAG,"GPS Service Stop Logging");
				MetadataLogger logger = new MetadataLogger(MainActivity.curDirName);
				logger.setTimestampFooter();
			}

		}
	}

	final Messenger gpsMessager = new Messenger(new IncomingHandler());
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == Protocol.REQUEST_GPS)
				getLocation();



		}
	}

	public class GPSBinder extends Binder{
		Messenger getGPSMessenger(){
			return gpsMessager;
		} 
	}


	public void doPeriodicWork(){
		getLocation();
	}

	//	public static synchronized GPSCollector getGPSCollectorInstance(Context context){
	//		if(gpsCollector == null || !gpsCollector.context.equals(context)){
	//			gpsCollector = new GPSCollector(context);
	//		}
	//		return GPSCollector.gpsCollector;
	//	}

	private IBinder gpsBinder = new GPSBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG,"onBind");
		return gpsBinder;
	}

	public GPSCollector(){

	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {	
		getLocation();
		self = this;
		//stopSelf(startId);
		return 1;

	}

	/* how to utilize the mode */
	public int getLocation(){
		if(lm == null)
			lm = (LocationManager) getApplicationContext().getSystemService(Service.LOCATION_SERVICE);

		isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		Location newLocation = null;
		if(!isGPSEnabled && !isNetworkEnabled){
			return -1;
		}
		else{
			if(isGPSEnabled && fineGrainedGPS){
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
						MIN_TIME_BY_UPDATES, MIN_DISTANCE_BY_UPDATES, this);

				newLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); 
				currentBestLocation = getBetterLocation(newLocation, currentBestLocation);
				//if(currentBestLocation!=null){Log.w("GPS Enabled", currentBestLocation.toString());}

			}     

			if(isNetworkEnabled){
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
						MIN_TIME_BY_UPDATES,MIN_DISTANCE_BY_UPDATES, this);
				newLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); 
				currentBestLocation = getBetterLocation(newLocation, currentBestLocation);
				//if(currentBestLocation!=null){Log.w("GPS Enabled", currentBestLocation.toString());}


			}
			if(currentBestLocation!=null){
				if(logger == null || logger.curDirName == null){
					logger = new MetadataLogger(curDirName);
				}
				logger.appendToFileGPS(currentBestLocation.getLatitude(), currentBestLocation.getLongitude());
				Log.w("GPS Enabled", currentBestLocation.toString());
			}
		}
		return 0;
	}


	public double getlatitude(){
		if(currentBestLocation !=null){
			if(currentBestLocation.getLatitude() == 0){
				//enableDialog();
			}
			return currentBestLocation.getLatitude();
		}
		return 0;
	}
	public double getlongitude(){
		if(currentBestLocation !=null){
			return currentBestLocation.getLongitude();
		}
		return 0;
	}

	//	public void enableDialog(){
	//
	//		AlertDialog.Builder builder = new AlertDialog.Builder(context);
	//		builder.setTitle("GPS Settings");
	//		builder.setMessage("GPS is not enabled, do you want to enable the GPS Service?");
	//
	//		builder.setPositiveButton("Enable",new DialogInterface.OnClickListener(){
	//			@Override 
	//			public void onClick(DialogInterface dialog, int whichButton){
	//				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	//				context.startActivity(intent);
	//			}
	//		});
	//		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
	//			@Override
	//			public void onClick(DialogInterface dialog, int whichButton){
	//				dialog.dismiss();
	//			}
	//		});
	//
	//		builder.show();
	//	}
	//

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	private static final int VALID_TIME_WINDOW = 1000 * 10;

	/** Determines whether one Location reading is better than the current Location fix
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	protected Location getBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return location;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());


		if(timeDelta > VALID_TIME_WINDOW ) return location;
		if(timeDelta < -VALID_TIME_WINDOW) return currentBestLocation;

		if(accuracyDelta < 0) return location;
		if(accuracyDelta > 0 && timeDelta == 0) return location;
		if(timeDelta >0 && accuracyDelta < 200 && isFromSameProvider) return location;
		return currentBestLocation;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}
}
