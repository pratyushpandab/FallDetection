package com.aware.plugin.collapse_detector;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Accelerometer_Provider;
import com.aware.utils.Aware_Plugin;





public class Plugin extends Aware_Plugin {
    private static AccelerometerObserver accelerometer_observer;
    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "collapse_detector";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        if( DEBUG ) Log.d(TAG, "collapse_detector plugin running");




        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_ACCELEROMETER, 0);

        Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(applySettings);

        accelerometer_observer = new AccelerometerObserver(new Handler());
        getContentResolver().registerContentObserver(Accelerometer_Provider.Accelerometer_Data.CONTENT_URI, true, accelerometer_observer);


    }
    public class AccelerometerObserver extends ContentObserver {

        CharSequence text = "I fell down!";
        int duration = Toast.LENGTH_SHORT;
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, text, duration);
        //toast.show();
        public AccelerometerObserver(Handler handler){
            super(handler);
        }
        public void onChange(boolean selfChange){
            super.onChange(selfChange);
            Cursor acceleration;
            acceleration = getContentResolver().query(Accelerometer_Provider.Accelerometer_Data.CONTENT_URI, null, null, null, Accelerometer_Provider.Accelerometer_Data.TIMESTAMP + " DESC LIMIT 1");
            if(acceleration != null && acceleration.moveToFirst()){
                double x_value = acceleration.getDouble(acceleration.getColumnIndex(Accelerometer_Provider.Accelerometer_Data.VALUES_0));
                double y_value = acceleration.getDouble(acceleration.getColumnIndex(Accelerometer_Provider.Accelerometer_Data.VALUES_1));
                double z_value = acceleration.getDouble(acceleration.getColumnIndex(Accelerometer_Provider.Accelerometer_Data.VALUES_2));

                // value of vector_sum is about 9.87 when the phone is stationary
                Double vector_sum = Math.sqrt(x_value*x_value + y_value*y_value + z_value*z_value );
                if (x_value > 9.81 || y_value > 9.81 || z_value > 9.81 ){
                    toast.show();
                }

            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if( DEBUG ) Log.d(TAG, "collapse_detector plugin terminated");

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, false);
        Intent applySettings = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(applySettings);

        getContentResolver().unregisterContentObserver(accelerometer_observer);

    }
}
