package ri.cmu.edu.vehiclestateyi;

import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import ri.cmu.edu.vehiclestateyi.Protocol.SensorNames;

public class MetadataLogger {

	public String curDirName = null;
	public MetadataLogger(String dir){
		this.curDirName = dir;
	}
	public static File outputFolder = null;
	public String timeStamp;

	/* File extensions */

	public void setOutputDirectory(File out) {
		outputFolder = out;
	}

	public void setOutputDirectory(String out) {
		outputFolder = new File(out);
	}

	public void setTime(String timestamp) {
		timeStamp = timestamp;
	}
	public void setTimestampHeader() {
		try {

			String filename = Protocol.STARTSTOP_FILENAME;
			String starttime = new SimpleDateFormat(Protocol.dateFormat).format(new Date());


			Log.e("LOGGER",filename);
			if(curDirName!=null){
				String fullPath = new File( curDirName+ "/"+ filename).getPath();
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fullPath, true)));

				out.println(starttime);

				out.close();
			}

		} catch (IOException e) {
			// Uh oh!
			Log.e("MetadataLogger", e.getMessage());
		} 	
	}

	public void setTimestampFooter() {
		try {

			String endtime = new SimpleDateFormat(Protocol.dateFormat).format(new Date());

			String filename = Protocol.STARTSTOP_FILENAME;
			Log.e("LOGGER",filename);
			String fullPath = new File(curDirName + "/" + filename).getPath();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fullPath, true)));

			out.println(endtime);

			out.close();


		} catch (IOException e) {
			// Uh oh!
			Log.e("MetadataLogger", e.getMessage());
		} 	}


	public void appendToFileSingleSensor(SensorNames sensorName, float[] value) {
		try {
			String time = new SimpleDateFormat(Protocol.dateFormat).format(new Date());


			String filename = Protocol.fileExtension.get(sensorName);
			//Log.e("LOGGER",filename);
			String fullPath = new File(curDirName + "/"+ filename).getPath();
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fullPath, true)));
			/* current time*/
			out.println(time +": "+value[0] + ", " + value[1] + ", "+value[2]);

			out.close();


		} catch (IOException e) {
			// Uh oh!
			Log.e("MetadataLogger", e.getMessage());
		} 	}

	public void appendToFileGPS(double d, double f) {
		try {
			String time = new SimpleDateFormat(Protocol.dateFormat).format(new Date());


			String filename = Protocol.fileExtension.get(Protocol.SensorNames.GPS);
			
			String fullPath = new File(curDirName + "/" + filename).getPath();
			Log.e("LOGGER",fullPath);
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fullPath, true)));
			/* current time*/
			out.println(time +": "+d+", "+f);

			out.close();


		} catch (IOException e) {
			// Uh oh!
			Log.e("MetadataLogger", e.getMessage());
		} 	}


	public void appendToFile(JSONObject obj) {
		try {
			String time = new SimpleDateFormat(Protocol.dateFormat).format(new Date());

			for(SensorNames name:Protocol.SensorNames.values()){

				String filename = Protocol.fileExtension.get(name);
				Log.e("LOGGER",filename);
				String fullPath = new File(outputFolder.getPath() + File.separator + timeStamp + "-" + filename).getPath();
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fullPath, true)));
				/* current time*/
				out.println(time +": "+obj.getString(name.name()));

				out.close();
			}

		} catch (IOException e) {
			// Uh oh!
			Log.e("MetadataLogger", e.getMessage());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}
}
