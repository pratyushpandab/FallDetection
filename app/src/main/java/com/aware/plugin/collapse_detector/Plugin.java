package com.aware.plugin.collapse_detector;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;
import com.aware.utils.Aware_Plugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Plugin extends Aware_Plugin implements SensorEventListener {

    private static SensorManager mSensorManager = null;
    private static Sensor mAccelerometer = null;

    private  final ESMStatusListener esm_statuses = new ESMStatusListener();
    public static Intent intent2;


    //server details
    int UDP_SERVER_PORT = 80;
    String UDP_SERVER_IP = "85.23.168.159";


    @Override
    public void onCreate() {
        super.onCreate();

        Toast.makeText(getApplicationContext(),"Monitoring Started", Toast.LENGTH_SHORT).show();

        TAG = "collapse_detector";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        if( DEBUG ) Log.d(TAG, "collapse_detector plugin running");



        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);


        // ESM plugin for pop-up question after a fall is detected
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        IntentFilter esm_filter = new IntentFilter();
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        registerReceiver(esm_statuses, esm_filter);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        double vector_sum = Math.sqrt(x*x + y*y + z*z );

        //the acceleration is around 0.3 as its lowest when in free fall. This may depend on the phone used.
        if (vector_sum < 0.3){
            Toast.makeText(getApplicationContext(),"I fell down!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Vector sum: " + vector_sum);
            notifyUser();

            // send data to server
            new Thread(new Client()).start();

            // add code to send data to server
            // Timestamp and GPS coordinates
        }
    }

    // notify user that a fall was detected
    void notifyUser() {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setContentTitle("Phone fall detected");
        mBuilder.setContentText("Please confirm");


        int mNotificationId = 001;
        Intent intent1 = new Intent(this, PopUp.class);

        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 999, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendingIntent).build();
        mBuilder.setAutoCancel(true);
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }

    public class ESMStatusListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED))
                Log.d(TAG, "Pop Up was dismissed");

            if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {

                Log.d(TAG, "Pop Up was answered");
                Cursor esm_answers = context.getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, null, null, null);
                if (esm_answers != null && esm_answers.moveToLast()) {
                    String ans = esm_answers.getString(esm_answers.getColumnIndex(ESM_Provider.ESM_Data.ANSWER));

                    Log.d(TAG, "User answer ----- " + ans);

                    if (ans.equalsIgnoreCase("Yes")) {
                        Log.d(TAG, "answer is yes, homescreen shows up");
                        //shows map UI
                        intent2 = new Intent(getApplicationContext(), Homescreen.class);
                        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent2);

                        //trying google maps
//                        Intent intent3 = new Intent(android.content.Intent.ACTION_VIEW,
//                                Uri.parse("http://maps.google.com/maps?daddr=60.1708, 24.9375"));
//
//                        startActivity(intent3);

                    }
                    if (esm_answers != null && !esm_answers.isClosed()) esm_answers.close();
                }
            }
        }
    }


    // sending data to server
    public class Client implements Runnable {

        @Override
        public void run() {
            try {
                //current timestamp
                Long timestamp = System.currentTimeMillis();

                // Here we convert Java Object to JSON

                JSONObject jsonObj = new JSONObject();
                jsonObj.put("timestamp", timestamp);




                // Retrieve the ServerName
                InetAddress serverAddr = InetAddress.getByName(UDP_SERVER_IP);
                Log.d("UDP", "C: Connecting...");
                //Create new UDP-Socket
                DatagramSocket socket = new DatagramSocket();

                //Prepare some data to be sent
                byte[] buf = jsonObj.toString().getBytes();
                //Create UDP-packet with data & destination(url+port)
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, UDP_SERVER_PORT);
                Log.d("UDP", "C: Sending: '" + new String(buf) + "'");

                //Send out the packet */
                socket.send(packet);
                Log.d("UDP", "C: Sent.");
                Log.d("UDP", "C: Done.");

                socket.receive(packet);
                Log.d("UDP", "C: Received: '" + new String(packet.getData()) + "'");

            } catch (Exception e) {
                Log.e("UDP", "C: Error", e);
                e.printStackTrace();
            }

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(esm_statuses);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        if( DEBUG ) Log.d(TAG, "collapse_detector plugin terminated");
        mSensorManager.unregisterListener(this, mAccelerometer);
    }
}
