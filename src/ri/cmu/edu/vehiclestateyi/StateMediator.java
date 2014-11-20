package ri.cmu.edu.vehiclestateyi;

import android.os.Handler;

public class StateMediator {
	public static volatile boolean isRunning = false;
	/* cameraMode = true : camera, false = video */
	public static volatile boolean cameraMode = false;
	/* recording or taking pictures */
	public static volatile boolean cameraRunning = false;

	/* Authority to change cameraMode*/
	public MainActivity mainActivity = null;
	/* Authority to change cameraMode*/
	public CameraController cController = null;
	private WriteThread w;
	private Handler write_handler;
	private Handler sensor_handler;
	public StateMediator(MainActivity m){
		mainActivity = m;
	}

	public StateMediator(MainActivity m, WriteThread w, SensorThread s){
		mainActivity = m;
		this.write_handler = w.wHandler;
		this.sensor_handler = s.sHandler;
		this.w = w;
		w.sm = this;
		//s.sm = this;
	}

	public static void setCameraMode(boolean state){
		StateMediator.cameraMode = state;
	}
	public static void setCameraRunningStatus(boolean state){
		StateMediator.cameraRunning = state;
	}
	public static void swapCameraMode(){
		StateMediator.cameraMode = !StateMediator.cameraMode;
	}
	public void setWriteThread(WriteThread w){
		this.w = w;
	}
	public static void startCapturing(){
		StateMediator.cameraRunning = true;
		MainActivity.sensor_thread.sHandler.sendEmptyMessage(Protocol.TAKE_PICTURE);
	}
	public static void stopCapturing(){
		StateMediator.cameraRunning = false;
		MainActivity.sensor_thread.sHandler.sendEmptyMessage(Protocol.STOP_TAKING_PICTURE);
	}
}
