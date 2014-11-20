package ri.cmu.edu.vehiclestateyi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class WriteThread extends Thread{

	public final static String TAG = "Write_thread";
	public StateMediator sm;
	private boolean cameraRunning = true;
	public Queue<byte[]> cache = null;
	private static String timeStamp;
	private static String dateFormat = "yyyyMMdd_HHmmss_SSS";
	public MetadataLogger logger = null;
	CameraController cameraController = null;
	int cnt = 0;
	
	public Handler wHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == Protocol.STOP_TAKING_PICTURE){
				cameraRunning = false;
			}
		}
	};
	public WriteThread(CameraController cameraController, Queue<byte[]> cache){
		this.cache = cache;
		this.cameraController = cameraController;	
		//this.logger = logger;
		timeStamp = new SimpleDateFormat(dateFormat).format(new Date());
	}
	@Override
	public void run(){
		/* if the camera is running or the still remaining pictures in the cache */
		while(cameraRunning || !cache.isEmpty()){
			/* if cache is empty, sleep */
			if(cache.isEmpty()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else{
				byte[] img = cache.remove();
				try {
					FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+MainActivity.MasterDir+"/"+cnt+".jpg"));
					fos.write(img);
					fos.close();
					cnt++;
					Log.w(TAG+"cache written",cnt+"");
				} catch (FileNotFoundException e) {
					Log.d("CameraActivity", "File not found: " + e.getMessage());
				} catch (IOException e) {
					Log.d("CameraActivity", "Error accessing file: " + e.getMessage());
				}

			}
		}
		/* finalize after writing all the photos */
		try {
			
			finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			Log.w(TAG,"Write thread finalized");
			cameraController.write_thread = null;
			sm.setWriteThread(null);
		}
	}
}
