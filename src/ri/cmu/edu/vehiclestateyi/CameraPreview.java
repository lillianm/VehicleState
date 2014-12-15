package ri.cmu.edu.vehiclestateyi;


import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.ZoomControls;

import java.io.IOException;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	public String TAG = "CameraPreview";
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private ZoomControls zoomControl;
	private SeekBar zoomBar;
	private Activity context;
	private Parameters p;
	private int minZoomLevel = 0;
	private int maxZoomLevel;
	private int currentZoomLevel = 0;
	public CameraPreview(Context context, Camera camera) {
		super(context);
		this.context = (Activity) context;
		mCamera = camera;
		mHolder = getHolder();
		mHolder.addCallback(this);

		//
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the preview.
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();

		} catch (IOException e) {

			Log.d("CameraPreview", "Error setting camera preview: " + e.getMessage());
		}
		setTouchEvent();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e){
			Log.d("CameraPreview", "Error starting camera preview: " + e.getMessage());
		}
	}

	private void setZoomControlListeners(){
		if (p.isZoomSupported() && p.isSmoothZoomSupported()) {



			zoomControl.setIsZoomInEnabled(true);
			zoomControl.setIsZoomOutEnabled(true);

			zoomControl.setOnZoomInClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentZoomLevel < maxZoomLevel) {
						int currentlevel = currentZoomLevel++;
						mCamera.startSmoothZoom(currentlevel);
						Log.v(TAG,""+currentlevel);
					}
				}
			});

			zoomControl.setOnZoomOutClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentZoomLevel > 0) {
						int currentlevel = currentZoomLevel--;
						mCamera.startSmoothZoom(currentlevel);
						Log.v(TAG,""+currentlevel);

					}
				}
			});
		} else if (p.isZoomSupported() && !p.isSmoothZoomSupported()){
			//stupid HTC phones


			zoomControl.setIsZoomInEnabled(true);
			zoomControl.setIsZoomOutEnabled(true);

			zoomControl.setOnZoomInClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentZoomLevel < maxZoomLevel) {
						int currentlevel = currentZoomLevel++;
						p.setZoom(currentlevel);
						mCamera.setParameters(p);

						Log.v(TAG,""+currentlevel);


					}
				}
			});

			zoomControl.setOnZoomOutClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentZoomLevel > 0) {
						int currentlevel = currentZoomLevel--;
						p.setZoom(currentlevel);
						mCamera.setParameters(p);
						zoomBar.setProgress(currentlevel);
						Log.v(TAG,""+currentlevel);

					}
				}
			});




		}else{
			//no zoom on phone
			zoomControl.setVisibility(View.GONE);
		}

		mCamera.setParameters(p); 

		try {
			mCamera.setPreviewDisplay(getHolder()); 
		} // end try
		catch (IOException e) {
			//Log.v( e.toString());
		} 		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}


	}
	public void setTouchEvent(){
		this.setOnTouchListener(new View.OnTouchListener() {


			@Override
			public boolean onTouch(View v, MotionEvent event) {

				int pointerCount = event.getPointerCount();
				int action = event.getActionMasked();
				String actionString = "";
				int pointerIndex = ((event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
				if(pointerIndex >=pointerCount){
					return false;
				}
				if(pointerCount == 1){
					if(action == MotionEvent.ACTION_DOWN){
						Log.e(TAG,"DOWN");
					}
				}
				if(pointerCount>1){
					for(int i = 0;i<pointerCount;i++){
						int activePosition = event.getPointerId(i);
						if(activePosition == -1 ){
							return false;
						}
					}
					switch (action)
					{
					case MotionEvent.ACTION_DOWN:
						actionString = "DOWN";
						Log.w("Multitouch","down");
						break;
					case MotionEvent.ACTION_UP:
						actionString = "UP";
						Log.w("Multitouch","up");
						break;	
					case MotionEvent.ACTION_POINTER_DOWN:
						actionString = "DOWN";
						Log.w("Multitouch","pointer down");
						break;
					case MotionEvent.ACTION_POINTER_UP:
						actionString = "UP";
						Log.w("Multitouch","pointer up");
						break;
					case MotionEvent.ACTION_MOVE:
						//actionString = "MOVE";
						break;
					default:
						actionString = "";
					}

					if(pointerCount>1 && pointerCount<=2){
						Log.w("actionString",actionString);
						if(actionString.equals("DOWN")){


							return true;
						}
					}
				}
				return false;
			}
		});
	}

}