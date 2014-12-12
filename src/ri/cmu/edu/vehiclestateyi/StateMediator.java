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


	/* device states */
	public static boolean externalStore = false;

	

	public StateMediator(MainActivity m){
		mainActivity = m;
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
	public static void startCapturing(){
		StateMediator.cameraRunning = true;
		
	}
	public static void stopCapturing(){
		StateMediator.cameraRunning = false;
		
	}
}
