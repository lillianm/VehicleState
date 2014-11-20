package ri.cmu.edu.vehiclestateyi;

import java.util.HashMap;

public class Protocol {
	
	final public  static String dateFormat = "yyyyMMdd_HHmmss_SSS";
	
	final public String EXT_KPH = "kph.txt";
	final public static String EXT_GPS = "gps.txt";
	final public static String EXT_GYRO = "gyro.txt";
	final public static String EXT_LINACCEL = "linaccel.txt";
	final public String EXT_ACCEL = "accel.txt";
	final public static String EXT_ORIENT = "orient.txt";
	final public String EXT_SHUTTER = "shutter.txt";
	final public static String EXT_GRAVITY = "gravity.txt";
	final public String EXT_STSP = "startstop.txt";
	final public String EXT_ALL = "all.txt";

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
	public final static int GPS_OUTPUT_ZERO = "GPS_OUTPUT_ZERO".hashCode();
	public final static String STARTSTOP_FILENAME = "startstop.txt";
	public final static int STOP_ALARM = "STOP_ALARM".hashCode();
	public final static int PICTURE_CALLBACK_FINISHED = "PICTURE_CALLBACK_FINISHED".hashCode();
	public final static int TAKE_PICTURE = "TAKE_PICTURE".hashCode();
	public final static int STOP_TAKING_PICTURE = "STOP_TAKING_PICTURE".hashCode();
	public final static int  LOG_CONTEXT = "LOG_CONTEXT".hashCode();
}
