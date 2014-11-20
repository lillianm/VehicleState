package ri.cmu.edu.vehiclestateyi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.Set;
import java.util.UUID;

public class OBDII {
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private static final String TAG = "OBDII";

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String NAME = "ROADVID";
    String dateFormat = "yyyyMMdd_HHmmss_SSS";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothChatService mBluetoothCS;
    private MetadataLogger mMetadataLogger;

    private String buff = "";

    private int initSeq = 1;

    public MainActivity parent;

    public OBDII(MainActivity p, BluetoothAdapter mBluetoothAdapter, MetadataLogger metadataLogger) {
        parent = p;
        this.mMetadataLogger = metadataLogger;
        this.mBluetoothAdapter = mBluetoothAdapter;
    }

    public boolean isConnected() {
    	Log.e("OBD2",mBluetoothCS+"");
        return mBluetoothCS != null;
    }

    public void tryConnect() {
        Log.e("Bluetooth_Device", "FINDING");
        BluetoothDevice mBluetoothDevice = null;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().contains("CBT")) {
                mBluetoothDevice = device;
                break;
            }
        }

        if (mBluetoothDevice == null) {
            parent.showToast("Could not find CBT device (make sure it's paired)");
            return;
        }

        mBluetoothCS = new BluetoothChatService(null, mHandler);

        mBluetoothCS.connect(mBluetoothDevice, false);
    }

    private void sendMessage(String msg) {
        if (mBluetoothCS != null) {
            mBluetoothCS.write(msg.getBytes());
        }
    }

    public void initOBD() {
        initSeq = -3;
        makeRequest(initSeq);
    }

    public void makeRequest(int messagenumber) {
        switch(messagenumber) {
            case -3:
                sendMessage("atz" + '\r');
                break;
            case -2:
                sendMessage("atsp0" + '\r');
                break;
            case -1:
                sendMessage("ate0" + '\r');
                break;
            case 0:
                sendMessage("01 00" + '\r');
                break;
            case 1:
                sendMessage("01 0C" + '\r'); //get RPM
                break;
            case 2:
                sendMessage("01 0D" + '\r'); //get MPH
                break;
            case 3:
                sendMessage("01 04" + '\r'); //get Engine Load
                break;
            case 4:
                sendMessage("01 05" + '\r'); //get Coolant Temperature
                break;
            case 5:
                sendMessage("01 0F" + '\r'); //get Intake Temperature
                break;
            case 6:
                sendMessage("AT RV" + '\r'); //get Voltage
                break;
        }
    }

    private void bufferFull() {
        Log.v(TAG, buff);
        buff = "";
        if (initSeq < 0) {
            initSeq += 1;
            makeRequest(initSeq);
            return;
        }
        /* We have a valid response buffer */
        if (buff.startsWith("41 0D")) {
            int response = Integer.parseInt(buff.substring(7, 9), 16);
            //mMetadataLogger.appendToFile(mMetadataLogger.EXT_KPH, new SimpleDateFormat(dateFormat).format(new Date()) + ": " + response);


        }
        
        makeRequest(2);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.v(TAG, msg.arg2 + ", " + msg.arg1);
                    if (msg.arg1 == 3) {
                        initOBD();
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.v(TAG, writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    int PID = 0;
                    int value = 0;

                    if (readMessage.contains(">")) {
                        buff += readMessage;
                        bufferFull();
                    } else {
                        buff += readMessage;
                    }

                    break;
                   /*
                    if (initSeq < 1 && readMessage.contains(">")) {
                        initSeq += 1;
                        makeRequest(initSeq);
                        break;
                    }

                    if((readMessage != null) &&
                            (readMessage.matches("\\s*[0-9A-Fa-f]{2} [0-9A-Fa-f]{2}\\s*\r?\n?" ))) {

                        readMessage = readMessage.trim();
                        String[] bytes = readMessage.split(" ");

                        if((bytes[0] != null) && (bytes[1] != null)) {
                            PID = Integer.parseInt(bytes[0].trim(), 16);
                            value = Integer.parseInt(bytes[1].trim(), 16);
                            Log.v(TAG, "PID: " + PID + ", VAL: " + value);
                        }
                    }
                     */
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    break;
                case MESSAGE_TOAST:
                    parent.showToast(msg.getData().getString(TOAST));
                    break;
            }
        }
    };
}