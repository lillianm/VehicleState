package ri.cmu.edu.vehiclestateyi;

import java.util.HashMap;

import android.content.Intent;

public class Protocol {

	public static final boolean CAMERA_MODE = true;
	public static final boolean VIDEO_MODE = false;
	public static final String PREFS_NAME = "MyPrefsFile";

	static String[] saveLocations = {"MediaFolder", "ExternalSD"};
	static String[] updateRates = {"Normal", "Game", "UI", "Fast"};

	final static int REQUEST_LINK_TO_DBX = 14;
	final public static int ALARM_SERVICE = "ALARM_SERVICE".hashCode();
	final public static int REQUEST_GPS = "REQUEST_GPS".hashCode();
	final public  static String dateFormat = "yyyyMMdd_HHmmss_SSS";

	final public String EXT_KPH = "kph.txt";
	final public static String EXT_GPS = "gps";
	final public static String EXT_GYRO = "gyro";
	final public static String EXT_LINACCEL = "linaccel";
	final public String EXT_ACCEL = "accel";
	final public static String EXT_ORIENT = "orient";
	final public String EXT_SHUTTER = "shutter";
	final public static String EXT_GRAVITY = "gravity";
	final public String EXT_ALL = "all";
	final public static String FILE_FORMAT = ".txt";

	/* Intents */
	public static String BROADCAST_ACTION_START  = "ri.cmu.edu.vehiclestateyi.MainActivity.start";
	public static String BROADCAST_ACTION_STOP  = "ri.cmu.edu.vehiclestateyi.MainActivity.stop";

	/* File constants */

	public static String CURRENT_DIR_NAME = "CURRENT_DIR_NAME";
	public static String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";
	public static String UPLOAD_IMAGE_DIR = "img";
	public static String UPLOAD_VIDEO_DIR = "vid";
	public static String MASTER_DIR = "VehicleStateEstimation";
	public static String MEDIA_STORAGE_DIR = "/storage/extSdCard/VehicleStateEstimation/";
	public static enum SensorNames {
		GPS, GYROSCOPE, LINEAR_ACCELERATION,ORIENTATION,GRAVITY //ACCELEROMETER
	};
	public static HashMap<SensorNames, String> fileExtension;
	static{
		fileExtension = new HashMap<SensorNames, String>();
		fileExtension.put(SensorNames.GPS, EXT_GPS);
		fileExtension.put(SensorNames.GRAVITY, EXT_GRAVITY);
		fileExtension.put(SensorNames.GYROSCOPE, EXT_GYRO);
		fileExtension.put(SensorNames.LINEAR_ACCELERATION, EXT_LINACCEL);
		fileExtension.put(SensorNames.ORIENTATION, EXT_ORIENT);

		//fileExtension.put(SensorNames., EXT_GPS);

	}
	public final static String GPS_OUTPUT_ZERO = "ri.cmu.edu.vehiclestateyi.MainActivity.stop.GPS_OUTPUT_ZERO";
	public final static String STARTSTOP_FILENAME = "startstop";

}
