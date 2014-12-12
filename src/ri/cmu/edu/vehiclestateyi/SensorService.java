package ri.cmu.edu.vehiclestateyi;

import java.io.FileDescriptor;

import ri.cmu.edu.vehiclestateyi.GPSCollector.PeriodicThread;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class SensorService extends Service{


	SensorManager sensorManager;
	Sensor orientation;
	Sensor accelerometer;
	Sensor proximity;
	Sensor gyro;
	Sensor rotate;
	Sensor grav;
	
	private static boolean startLogging = false;

	private final static String TAG = "CONTEXTTHREAD";
	private final static int EPSILON = 100;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private volatile static String curDirName = null;
	private static SensorService self;
	private static MetadataLogger logger;
	public class SensorBinder extends Binder{
		public Messenger getMessenger(){
			return sensorMessenger;
		}
	}

	public static class SensorReceiver extends BroadcastReceiver{
		public SensorReceiver(){}
		@Override
		public void onReceive(Context context, Intent intent) {
			// if (!Strings.isNullOrEmpty(intent.getAction())) {
			//MainApplication myApplication = (MainApplication) context.getApplicationContext();
			//SharedPreferences sharedPreferences = myApplication.getSharedPreferences(AlarmService.PREFS_NAME,0);
			if(intent.getAction().equals(Protocol.BROADCAST_ACTION_START)){
				Log.d(TAG, "Sensor service start logging");
				startLogging = true;
				if(intent.hasExtra(Protocol.CURRENT_DIR_NAME)){
					curDirName = intent.getStringExtra(Protocol.CURRENT_DIR_NAME);
					Log.d(TAG, curDirName);

				}
				else{
					Log.e(TAG,"CURDIR NAME IS NULL");
				}
			}
			if(intent.getAction().equals(Protocol.BROADCAST_ACTION_STOP)){
				startLogging = false;
				Log.d(TAG,"GPS Service Stop Logging");
			}

		}
	}

	private IBinder sensorBinder = new SensorBinder();
	final Messenger sensorMessenger = new Messenger(new IncomingHandler());
	public class IncomingHandler extends Handler{
		@Override
		public void handleMessage(Message msg){
		}

	}

	private float timestamp = 0;
	float[] gravity = new float[3];
	float[] geomagnetic = new float[3];
	//float[] orient = new float[3];
	private final float[] deltaRotationVector = new float[4];

	public SensorEventListener sensorListener = new SensorEventListener(){

		@Override
		public void onSensorChanged(SensorEvent event) {
			int type = event.sensor.getType();
			switch(type){

			case Sensor.TYPE_GRAVITY:
				//cp.setGravity(event.values);
				if(startLogging){
					if(logger == null)
						logger = new MetadataLogger(curDirName);
					logger.appendToFileSingleSensor(Protocol.SensorNames.GRAVITY, event.values);
				}
				break;
			case Sensor.TYPE_ACCELEROMETER:
				final float alpha = (float) 0.8;
				float[] linear_acceleration = new float[3];
				gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
				gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
				gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

				linear_acceleration[0] = event.values[0] - gravity[0];
				linear_acceleration[1] = event.values[1] - gravity[1];
				linear_acceleration[2] = event.values[2] - gravity[2];
				if(startLogging){
					if(logger == null)
						logger = new MetadataLogger(curDirName);
					logger.appendToFileSingleSensor(Protocol.SensorNames.LINEAR_ACCELERATION, linear_acceleration);
				}
				//				ctx.usr_wrapper.compassView.updateData(linear_acceleration[1]);
				//Log.v(TAG,cp.linear_acceleration[0]+":" +cp.linear_acceleration[1]+":"+cp.linear_acceleration[2]);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				geomagnetic[0] = event.values[0];
				geomagnetic[1] = event.values[1];
				geomagnetic[2] = event.values[2];

				break;
			case Sensor.TYPE_ORIENTATION:

				//cp.setOrientation(event.values);
				if(startLogging)//if(sm.cameraRunning)
				{
					if(logger == null)
						logger = new MetadataLogger(curDirName);
					logger.appendToFileSingleSensor(Protocol.SensorNames.ORIENTATION, event.values);
				}
				//Log.d(TAG,"Orientation"+event.values[0]+cp.azimuth);
				break;

			case Sensor.TYPE_GYROSCOPE:
				// This timestep's delta rotation to be multiplied by the current rotation
				// after computing it from the gyro sample data.
				if (timestamp != 0) {
					final float dT = (event.timestamp - timestamp) * NS2S;
					// Axis of the rotation sample, not normalized yet.
					float axisX = event.values[0];
					float axisY = event.values[1];
					float axisZ = event.values[2];
					//cp.setGyroscope(event.values);
					if(startLogging){
						if(logger == null)
							logger = new MetadataLogger(curDirName);
						logger.appendToFileSingleSensor(Protocol.SensorNames.GYROSCOPE, event.values);
					}

					// Calculate the angular speed of the sample
					float omegaMagnitude = (float) Math.sqrt((double) axisX*axisX + axisY*axisY + axisZ*axisZ);

					// Normalize the rotation vector if it's big enough to get the axis
					if (omegaMagnitude > EPSILON) {
						axisX /= omegaMagnitude;
						axisY /= omegaMagnitude;
						axisZ /= omegaMagnitude;
					}

					// Integrate around this axis with the angular speed by the timestep
					// in order to get a delta rotation from this sample over the timestep
					// We will convert this axis-angle representation of the delta rotation
					// into a quaternion before turning it into the rotation matrix.
					float thetaOverTwo = omegaMagnitude * dT / 2.0f;
					float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
					float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
					deltaRotationVector[0] = sinThetaOverTwo * axisX;
					deltaRotationVector[1] = sinThetaOverTwo * axisY;
					deltaRotationVector[2] = sinThetaOverTwo * axisZ;
					deltaRotationVector[3] = cosThetaOverTwo;
				}
				timestamp = event.timestamp;
				float[] deltaRotationMatrix = new float[9];
				SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
				float[] rotationMatrix = new float[9];
				float[] inclineMatrix = new float[9];
				//SensorManager.getRotationMatrix(rotationMatrix, inclineMatrix, linear_acceleration, geomagnetic);

				break;
			}


		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {


		}

	};


	public void pause(){
		blockSensors();

	}

	public void init(){

		sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
		proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		rotate = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		grav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		registerSensors();
		//Log.e("registered","hehe");

	}
	public void registerSensors(){
		sensorManager.registerListener(sensorListener, proximity, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, gyro, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, rotate, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, orientation, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, grav, SensorManager.SENSOR_DELAY_NORMAL);
		Log.e(TAG,"register sensors");
	}

	public void blockSensors(){

		//		sensorManager.unregisterListener(sensorListener, accelerometer);
		if(sensorManager!=null) sensorManager.unregisterListener(sensorListener);
		//sensorManager.unregisterListener(sensorListener, grav);
		//		sensorManager.cancelTriggerSensor(sensorListener, proximity, SensorManager.SENSOR_DELAY_NORMAL);
		//		sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		//		sensorManager.registerListener(sensorListener, gyro, SensorManager.SENSOR_DELAY_NORMAL);
		//		sensorManager.registerListener(sensorListener, rotate, SensorManager.SENSOR_DELAY_NORMAL);
		//		sensorManager.registerListener(sensorListener, orientation, SensorManager.SENSOR_DELAY_NORMAL);
		//		sensorManager.registerListener(sensorListener, grav, SensorManager.SENSOR_DELAY_NORMAL);
		Log.w(TAG, "block Sensors");
	}
	@Override
	public IBinder onBind(Intent intent) {
		return sensorBinder;
	}
	@Override
	public int onStartCommand(Intent intent, int startLoggings, int startId){
		Log.e(TAG, "SENSORservice started");
		self = this;
		//logger = new MetadataLogger();
		init();
		return 1;
	}






}
