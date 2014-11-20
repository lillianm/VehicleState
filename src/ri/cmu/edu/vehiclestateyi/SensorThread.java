package ri.cmu.edu.vehiclestateyi;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class SensorThread extends Thread{

	MainActivity ctx;
	SensorManager sensorManager;
	Sensor orientation;
	Sensor accelerometer;
	Sensor proximity;
	Sensor gyro;
	Sensor rotate;
	Sensor grav;

	public ContextParams cp;
	//public StateMediator sm;

	private final static String TAG = "CONTEXTTHREAD";
	private final static int EPSILON = 100;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private MetadataLogger logger = null;

	//private GPSTracker gpsTracker;

	//	private float p_proximity = 0;
	public Handler sHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == Protocol.STOP_TAKING_PICTURE){
				logger.setTimestampFooter();
				Log.w(TAG,"STOP");
				//blockSensors();
			}
			if(msg.what == Protocol.TAKE_PICTURE){
				logger.setTimestampHeader();
				//registerSensors();
			}

		}
	};
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
			case Sensor.TYPE_PROXIMITY:
				cp.setProximity(event.values[0]);
				break;
			case Sensor.TYPE_GRAVITY:
				cp.setGravity(event.values);
				if(StateMediator.cameraRunning)

					logger.appendToFileSingleSensor(Protocol.SensorNames.GRAVITY, event.values);
				break;
			case Sensor.TYPE_ACCELEROMETER:
				final float alpha = (float) 0.8;

				gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
				gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
				gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

				cp.linear_acceleration[0] = event.values[0] - gravity[0];
				cp.linear_acceleration[1] = event.values[1] - gravity[1];
				cp.linear_acceleration[2] = event.values[2] - gravity[2];
				if(StateMediator.cameraRunning)
					logger.appendToFileSingleSensor(Protocol.SensorNames.LINEAR_ACCELERATION, cp.linear_acceleration);
				//				ctx.usr_wrapper.compassView.updateData(linear_acceleration[1]);
				//Log.v(TAG,cp.linear_acceleration[0]+":" +cp.linear_acceleration[1]+":"+cp.linear_acceleration[2]);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				geomagnetic[0] = event.values[0];
				geomagnetic[1] = event.values[1];
				geomagnetic[2] = event.values[2];

				break;
			case Sensor.TYPE_ORIENTATION:

				cp.setOrientation(event.values);
				if(StateMediator.cameraRunning)//if(sm.cameraRunning)
					logger.appendToFileSingleSensor(Protocol.SensorNames.ORIENTATION, event.values);
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
					cp.setGyroscope(event.values);
					if(StateMediator.cameraRunning)//if(sm.cameraRunning)
						logger.appendToFileSingleSensor(Protocol.SensorNames.GYROSCOPE, event.values);

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
				SensorManager.getRotationMatrix(rotationMatrix, inclineMatrix, cp.linear_acceleration, geomagnetic);

				break;
			}


		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {


		}

	};

	public SensorThread(MainActivity ctx, MetadataLogger logger){
		this.ctx =ctx;
		cp = new ContextParams();
		this.logger = logger;

	}

	public void pause(){
		blockSensors();
		
	}
	public void kill(){
		blockSensors();
		Intent intent = new Intent(ctx, GPSTracker.class);
		ctx.stopService(intent);
		ctx.gpsTracker = null;

		try {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}
	public void init(){

		sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		rotate = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		grav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

		sensorManager.registerListener(sensorListener, proximity, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, gyro, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, rotate, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, orientation, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, grav, SensorManager.SENSOR_DELAY_NORMAL);
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
	public void run(){
		init();
		//logger.setTimestampHeader();
		while(ctx!=null){
			try {
				Thread.sleep(1000);
				populateContextParams();
				Log.e(Thread.currentThread().toString(), ""+System.currentTimeMillis());

				if(StateMediator.cameraRunning)
					if(ctx.gpsTracker !=null) logger.appendToFileGPS(ctx.gpsTracker.getlatitude(), ctx.gpsTracker.getlongitude());

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}	
	}

	private void populateContextParams(){
		if(ctx == null || ctx.gpsTracker==null) return;
		if(ctx.gpsTracker.getlatitude() == 0.0){
			ctx.h.sendEmptyMessage(Protocol.GPS_OUTPUT_ZERO);
		}
		cp.setGPS(ctx.gpsTracker.getlatitude(), ctx.gpsTracker.getlongitude());
	}


}
