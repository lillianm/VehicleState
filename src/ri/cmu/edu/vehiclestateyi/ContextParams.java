package ri.cmu.edu.vehiclestateyi;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


public class ContextParams {

	public final static int ID_CONTEXTPARAMS = "ContextParams".hashCode();
	public final static String ACCELEROMETER = "ACCELEROMETER";
	public final static int ID_ACCELEROMETER =  0 + ID_CONTEXTPARAMS;

	public final static String DEVICEID = "DEVICEID";
	public final static String GPS = "GPS";
	public final static String GPS_LAT = "GPS-LAT";
	public final static String GPS_LONG = "GPS-LONG";
	public final static int ID_GPS =  1 + ID_CONTEXTPARAMS;


	public final static String ORIENTATION = "ORIENTATION";
	public final static String AZIMUTH = "AZIMUTH";
	public final static String PITCH = "PITCH";
	public final static String ROLL = "ROLL";
	public final static int ID_ORIENTATION =  2 + ID_CONTEXTPARAMS;

	public final static String MAGNOMETER = "MAGNOMETER";
	public final static int ID_MAGNOMETER =  3 + ID_CONTEXTPARAMS;

	public final static String GYROSCOPE = "GYROSCOPE";
	public final static String GRAVITY = "GRAVITY";
	public final static String LINEAR_ACCELERATION = "LINEAR_ACCELERATION";
	/* public device id */
	//public final String deviceId;

	/* prompt Id */
	public int promptId;
	public String promptStr;

	/* proximity */
	public float proximity;
	/* z, x, y*/
	public float azimuth;//z
	public float pitch;//x
	public float roll; //y, inclination
	public float[] orientation;
	/* GPS */
	public double longitude;
	public double latitude;

	/* linear_acceleration ,x,y,z*/
	public float[] linear_acceleration;

	public float[] gyroscope;
	public float[] gravity;
	/* time*/
	public long start_time;
	public long end_time;


	public ContextParams(){
		linear_acceleration = new float[3];
		orientation = new float[3];
		gyroscope = new float[3];
		gravity = new float[3];
	}

	public void reset(){
		proximity = 0;
		azimuth = 0;
		pitch = 0;
		roll = 0;
		longitude = 0;
		latitude = 0;
		linear_acceleration = new float[3];
	}
	public void setPrompt(int id, String s){
		promptId = id;
		promptStr = s;
	}
	public void setProximity(float pro){
		this.proximity = pro;
	}
	public void setGyroscope(float[] gyro){
		this.gyroscope = gyro;
	}
	public void setGravity(float[] gravity){
		this.gravity = gravity;
	}
	public void setOrientation(float[] values){
		azimuth = values[0];
		pitch = values[1];
		roll = values[2];
		orientation = values;
	}
	public void setAcceleration(float[] values){
		linear_acceleration = values;
	}
	public void setGPS(double longitude, double latitude){
		this.longitude = longitude;
		this.latitude = latitude;
		Log.d(longitude+"",latitude+"");
	}
	
	public String returnJSONString(){
		String jsonString = null;
		JSONObject obj = new JSONObject();
		JSONObject obj1 = new JSONObject();
		try {
			obj.put(GPS_LAT, latitude);
			obj.put(GPS_LONG, longitude);
			obj1.put(AZIMUTH, orientation[0]);
			obj1.put(PITCH, orientation[1]);
			obj1.put(ROLL, orientation[2]);
			obj.put(ORIENTATION,obj1);
			jsonString = obj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonString;
	}
	public JSONObject returnJSONObject(){
		JSONObject obj = new JSONObject();
		try {
			obj.put(Protocol.SensorNames.GPS.name(), latitude+" "+longitude);
			obj.put(Protocol.SensorNames.ORIENTATION.name(), format(orientation));
			obj.put(Protocol.SensorNames.GYROSCOPE.name(), format(gyroscope));
			obj.put(Protocol.SensorNames.LINEAR_ACCELERATION.name(),format(linear_acceleration));
			obj.put(Protocol.SensorNames.GRAVITY.name(), format(gravity));
			//obj.put(Protocol.SensorNames.GRAVITY.name(), format(gravity));
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
	public String format(float[] param){
		String result = "";
		for(int i = 0;i<param.length -1;i++){
			result = result + param[i]+", ";
		}
		result+=param[param.length-1];
		return result;
	}
}
