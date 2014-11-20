
package ri.cmu.edu.vehiclestateyi;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Alarm extends Thread{
	private CameraController ctx;
	private String TAG = "Alarm Thread";
	private SensorThread sensor_thread= null;

	public Handler alarmHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == Protocol.PICTURE_CALLBACK_FINISHED){
				ctx.cHandler.sendEmptyMessage(Protocol.TAKE_PICTURE);
				//sensor_thread.sHandler.sendEmptyMessage(Protocol.LOG_CONTEXT);
				Log.w(TAG, ""+System.currentTimeMillis());
			}
			else{
				if(msg.what == Protocol.STOP_TAKING_PICTURE){
					try {
						finalize();
						Log.w(TAG, "Alarm Thread finalized ");
					} catch (Throwable e){}

				}
			}

		}
	};

	public Alarm(CameraController ctx){
		this.ctx = ctx;
		Log.w(TAG,"ALARM started" );
		//this.sensor_thread = sensor_thread;
	}
	@Override
	public void run(){
		try {
			Log.w(TAG, "Alarm started");
			sleep(500);
			ctx.cHandler.sendEmptyMessage(Protocol.TAKE_PICTURE);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}



}
