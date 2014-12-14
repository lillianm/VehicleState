package ri.cmu.edu.vehiclestateyi;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class Util {
	public static String getNewOutputFolder() {
		String ext = "";
		if (StateMediator.cameraMode.equals(Protocol.CAMERA_MODE)) {
			ext = Protocol.UPLOAD_IMAGE_DIR;
		} else {
			ext = Protocol.UPLOAD_VIDEO_DIR;

		}
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.
		File mediaStorageDir;
		if (!StateMediator.externalStore) {
			mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_PICTURES), Protocol.MASTER_DIR);
		}
		else {
			mediaStorageDir = new File(Protocol.MEDIA_STORAGE_DIR);
		}
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				Log.e("MainActivity", "failed to create VehicleStateEstimation directory");
				return null;
			}
		}

		//		if (mDirectoryUpload != null) {
		//			mDirectoryUpload.setUploadDir(mediaStorageDir);
		//		}

		File subMediaStorageDir = new File(mediaStorageDir.getPath()  + File.separator + ext);

		// Create the storage directory if it does not exist
		if (! subMediaStorageDir.exists()) {
			if (! subMediaStorageDir.mkdirs()){
				Log.e("MainActivity", "failed to create VehicleStateEstimation directory");
				return null;
			}
		}

		// Create a media file name
		String current_folder_timeStamp = new SimpleDateFormat(Protocol.dateFormat).format(new Date());
		File outputFolder;

		outputFolder = new File(subMediaStorageDir.getPath() + File.separator + current_folder_timeStamp);
		if (! outputFolder.exists()) {
			if (! outputFolder.mkdirs()) {
				Log.e("MainActivity", "failed to create local directory");
			}
		}
		return subMediaStorageDir.getPath() + File.separator + current_folder_timeStamp;

		//
		//		Log.e("mmetadatalogger", String.valueOf(mMetadataLogger));
		//
		//		if (mMetadataLogger != null) {
		//			mMetadataLogger.setOutputDirectory(outputFolder);
		//			mMetadataLogger.setTime(current_folder_timeStamp);
		//		}
		//
		//		return outputFolder;
	}

	public static long getFreeMemory(String dirPath) {


		Log.e("clicked", "getting mem for " + dirPath);
		StatFs statFs = new StatFs(dirPath);


		return (long) ((statFs.getAvailableBlocks()) *(statFs.getBlockSize() / 1048576.0));


	}



}
